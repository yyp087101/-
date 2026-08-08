package com.travel.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.travel.entity.Scenic;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AmapRouteService {

    private static final Logger logger = LoggerFactory.getLogger(AmapRouteService.class);
    private static final String USER_AGENT = "travel-recommend/1.0 (free-route)";

    private static final String MODE_WALKING = "walking";
    private static final String MODE_RIDING = "riding";
    private static final String MODE_DRIVING = "driving";
    private static final Map<String, GeoPoint> LOCAL_GEOCODE_INDEX = buildLocalGeocodeIndex();

    @Value("${free.route.request.timeout-ms:3500}")
    private Integer requestTimeoutMs;

    @Value("${free.geocode.url:https://nominatim.openstreetmap.org/search}")
    private String freeGeocodeUrl;

    @Value("${free.geocode.photon.url:https://photon.komoot.io/api}")
    private String freeGeocodePhotonUrl;

    @Value("${free.geocode.local.enabled:true}")
    private Boolean freeGeocodeLocalEnabled;

    @Value("${free.geocode.photon.enabled:false}")
    private Boolean freeGeocodePhotonEnabled;

    @Value("${free.geocode.nominatim.enabled:false}")
    private Boolean freeGeocodeNominatimEnabled;

    @Value("${free.geocode.countrycodes:cn}")
    private String freeGeocodeCountryCodes;

    @Value("${free.route.osrm.url:https://router.project-osrm.org/route/v1}")
    private String freeRouteOsrmUrl;

    @Value("${free.route.osrm.max-distance-km:600}")
    private Integer freeRouteOsrmMaxDistanceKm;

    public Scenic enrichScenicCoordinate(Scenic scenic) {
        if (scenic == null) {
            return null;
        }
        if (isValidCoordinate(scenic.getLatitude(), scenic.getLongitude())) {
            return scenic;
        }

        GeoPoint fromAddress = parseCoordinateInput(scenic.getAddress());
        if (fromAddress != null) {
            scenic.setLatitude(fromAddress.lat);
            scenic.setLongitude(fromAddress.lon);
            return scenic;
        }

        for (String q : buildGeocodeCandidates(scenic)) {
            GeoPoint point = geocodeByFree(q, scenic.getCity(), true);
            if (point == null) {
                point = geocodeByFree(q, scenic.getCity(), false);
            }
            if (point != null) {
                scenic.setLatitude(point.lat);
                scenic.setLongitude(point.lon);
                return scenic;
            }
        }
        return scenic;
    }

    public Map<String, Object> planRoute(Scenic scenic, String originInput) {
        if (scenic == null) {
            return response(404, "\u666f\u70b9\u4e0d\u5b58\u5728");
        }

        enrichScenicCoordinate(scenic);
        if (!isValidCoordinate(scenic.getLatitude(), scenic.getLongitude())) {
            return response(404, "\u666f\u70b9\u5750\u6807\u89e3\u6790\u5931\u8d25\uff0c\u8bf7\u8865\u5145\u666f\u70b9\u5730\u5740\u540e\u91cd\u8bd5");
        }

        GeoPoint origin = resolveOrigin(originInput);
        if (origin == null) {
            return response(400, "\u51fa\u53d1\u5730\u65e0\u6548\uff0c\u8bf7\u8f93\u5165\u5730\u5740\u6216\u7ecf\u7eac\u5ea6");
        }

        GeoPoint destination = new GeoPoint(scenic.getLatitude(), scenic.getLongitude());
        double directDistanceMeters = estimateDirectDistanceMeters(origin, destination);
        List<RouteOption> routes = buildRouteOptions(origin, destination, directDistanceMeters);

        if (routes.isEmpty()) {
            return response(502, "\u8def\u7ebf\u89c4\u5212\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5");
        }

        RouteOption recommended = recommendRoute(routes);
        boolean estimatedOnly = routes.stream().allMatch(route -> route.estimated);
        Map<String, Object> data = response(200, "ok");
        data.put("provider", estimatedOnly ? "estimate" : "osm-osrm");
        data.put("estimated", estimatedOnly);
        data.put("origin", toPointMap(origin));
        data.put("destination", toDestinationMap(destination, scenic));
        data.put("recommendedMode", recommended.mode);
        data.put("recommendedModeName", recommended.modeName);
        data.put("recommendationReason", buildRecommendationReason(recommended));
        data.put("routes", routes.stream().map(RouteOption::toMap).collect(Collectors.toList()));
        return data;
    }

    private GeoPoint resolveOrigin(String originInput) {
        GeoPoint byCoord = parseCoordinateInput(originInput);
        if (byCoord != null) {
            return byCoord;
        }
        if (!hasText(originInput)) {
            return null;
        }

        GeoPoint byQuery = geocodeByFree(originInput, "", true);
        if (byQuery == null) {
            byQuery = geocodeByFree(originInput, "", false);
        }
        return byQuery;
    }

    private List<String> buildGeocodeCandidates(Scenic scenic) {
        List<String> candidates = new ArrayList<>();
        addCandidate(candidates, joinParts(scenic.getProvince(), scenic.getCity(), scenic.getName(), scenic.getAddress()));
        addCandidate(candidates, joinParts(scenic.getCity(), scenic.getName()));
        addCandidate(candidates, scenic.getName());
        addCandidate(candidates, scenic.getAddress());
        return candidates;
    }

    private void addCandidate(List<String> candidates, String value) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return;
        }
        if (!candidates.contains(text)) {
            candidates.add(text);
        }
    }

    private String joinParts(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!hasText(value)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private GeoPoint geocodeByFree(String address, String city, boolean strictCountry) {
        GeoPoint localPoint = geocodeByLocal(address, city);
        if (localPoint != null) {
            return localPoint;
        }
        if (Boolean.TRUE.equals(freeGeocodePhotonEnabled)) {
            GeoPoint photonPoint = geocodeByPhoton(address, city, strictCountry);
            if (photonPoint != null) {
                return photonPoint;
            }
        }
        if (Boolean.TRUE.equals(freeGeocodeNominatimEnabled)) {
            return geocodeByNominatim(address, city, strictCountry);
        }
        return null;
    }

    private GeoPoint geocodeByLocal(String address, String city) {
        if (!Boolean.TRUE.equals(freeGeocodeLocalEnabled)) {
            return null;
        }
        List<String> queries = new ArrayList<>();
        addCandidate(queries, joinParts(city, address));
        addCandidate(queries, address);
        addCandidate(queries, city);
        for (String query : queries) {
            GeoPoint point = matchLocalCoordinate(query);
            if (point != null) {
                return point;
            }
        }
        return null;
    }

    private GeoPoint matchLocalCoordinate(String input) {
        if (!hasText(input)) {
            return null;
        }
        String normalized = normalizeLower(input);
        GeoPoint exact = LOCAL_GEOCODE_INDEX.get(normalized);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, GeoPoint> entry : LOCAL_GEOCODE_INDEX.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static Map<String, GeoPoint> buildLocalGeocodeIndex() {
        Map<String, GeoPoint> index = new LinkedHashMap<>();

        putLocalGeocode(index, 39.917431, 116.390782, "故宫", "故宫博物院", "紫禁城");
        putLocalGeocode(index, 30.245984, 120.143132, "西湖", "杭州西湖");
        putLocalGeocode(index, 34.384895, 109.278245, "兵马俑", "秦始皇兵马俑");
        putLocalGeocode(index, 29.316667, 110.483056, "张家界国家森林公园", "张家界");
        putLocalGeocode(index, 33.262097, 103.918493, "九寨沟");
        putLocalGeocode(index, 30.132500, 118.165500, "黄山");
        putLocalGeocode(index, 26.872222, 100.238056, "丽江古城", "丽江");
        putLocalGeocode(index, 24.446944, 118.067778, "鼓浪屿");
        putLocalGeocode(index, 29.657778, 91.117222, "布达拉宫");
        putLocalGeocode(index, 25.274000, 110.299000, "漓江", "桂林漓江");
        putLocalGeocode(index, 36.255000, 117.102500, "泰山");
        putLocalGeocode(index, 29.525000, 103.335000, "峨眉山");
        putLocalGeocode(index, 34.485000, 110.084000, "华山");
        putLocalGeocode(index, 29.345000, 117.583000, "婺源篁岭", "篁岭");
        putLocalGeocode(index, 30.744000, 120.489000, "乌镇");
        putLocalGeocode(index, 30.998000, 103.648000, "都江堰", "都江堰景区");
        putLocalGeocode(index, 40.112400, 113.122200, "云冈石窟");
        putLocalGeocode(index, 36.886000, 100.147000, "青海湖");
        putLocalGeocode(index, 28.462000, 100.297000, "稻城亚丁");
        putLocalGeocode(index, 48.695000, 87.020000, "喀纳斯", "喀纳斯景区");

        putLocalGeocode(index, 39.904200, 116.407400, "北京", "北京市", "beijing");
        putLocalGeocode(index, 31.230400, 121.473700, "上海", "上海市", "shanghai");
        putLocalGeocode(index, 23.129100, 113.264400, "广州", "广州市", "guangzhou");
        putLocalGeocode(index, 22.543100, 114.057900, "深圳", "深圳市", "shenzhen");
        putLocalGeocode(index, 30.274100, 120.155100, "杭州", "杭州市", "hangzhou");
        putLocalGeocode(index, 28.228200, 112.938800, "长沙", "长沙市", "changsha");
        putLocalGeocode(index, 34.341600, 108.939800, "西安", "西安市", "xian", "xi'an");
        putLocalGeocode(index, 30.572800, 104.066800, "成都", "成都市", "chengdu");
        putLocalGeocode(index, 29.563000, 106.551600, "重庆", "重庆市", "chongqing");
        putLocalGeocode(index, 25.038900, 102.718300, "昆明", "昆明市", "kunming");
        putLocalGeocode(index, 30.592800, 114.305500, "武汉", "武汉市", "wuhan");
        putLocalGeocode(index, 32.060300, 118.796900, "南京", "南京市", "nanjing");
        putLocalGeocode(index, 39.343400, 117.361600, "天津", "天津市", "tianjin");
        putLocalGeocode(index, 34.747300, 113.624900, "郑州", "郑州市", "zhengzhou");
        putLocalGeocode(index, 24.479800, 118.089400, "厦门", "厦门市", "xiamen");
        putLocalGeocode(index, 29.652000, 91.172100, "拉萨", "拉萨市", "lasa", "lhasa");
        putLocalGeocode(index, 25.273600, 110.290000, "桂林", "桂林市", "guilin");
        putLocalGeocode(index, 36.067100, 120.382600, "青岛", "青岛市", "qingdao");
        putLocalGeocode(index, 36.200100, 117.087600, "泰安", "泰安市", "taian");
        putLocalGeocode(index, 29.552100, 103.765400, "乐山", "乐山市", "leshan");
        putLocalGeocode(index, 34.499400, 109.510200, "渭南", "渭南市", "weinan");
        putLocalGeocode(index, 28.454900, 117.943400, "上饶", "上饶市", "shangrao");
        putLocalGeocode(index, 30.747400, 120.755500, "嘉兴", "嘉兴市", "jiaxing");
        putLocalGeocode(index, 40.076800, 113.300100, "大同", "大同市", "datong");
        putLocalGeocode(index, 47.848400, 88.139600, "阿勒泰", "阿勒泰地区", "aletai", "altay");
        putLocalGeocode(index, 31.619800, 100.000000, "甘孜", "甘孜州", "ganzi");
        putLocalGeocode(index, 36.959400, 100.900500, "海北", "海北州", "haibei");

        return Collections.unmodifiableMap(index);
    }

    private static void putLocalGeocode(Map<String, GeoPoint> index, double lat, double lon, String... keys) {
        GeoPoint point = new GeoPoint(lat, lon);
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String normalized = key.trim().toLowerCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                index.put(normalized, point);
            }
        }
    }

    private GeoPoint geocodeByPhoton(String address, String city, boolean strictCountry) {
        if (!hasText(address)) {
            return null;
        }

        String query = hasText(city) ? city.trim() + " " + address.trim() : address.trim();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("limit", "5");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");

        JSONObject root = callJsonObject(freeGeocodePhotonUrl, params, headers);
        if (root == null) {
            return null;
        }
        JSONArray features = root.getJSONArray("features");
        if (features == null || features.isEmpty()) {
            return null;
        }
        return pickPhotonCoordinate(features, city, strictCountry);
    }

    private GeoPoint geocodeByNominatim(String address, String city, boolean strictCountry) {
        if (!hasText(address)) {
            return null;
        }

        String query = hasText(city) ? city.trim() + " " + address.trim() : address.trim();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", query);
        params.put("format", "jsonv2");
        params.put("limit", "1");
        params.put("addressdetails", "0");
        if (strictCountry && hasText(freeGeocodeCountryCodes)) {
            params.put("countrycodes", freeGeocodeCountryCodes.trim());
        }

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");

        JSONArray array = callJsonArray(freeGeocodeUrl, params, headers);
        if (array == null || array.isEmpty()) {
            return null;
        }

        JSONObject first = array.getJSONObject(0);
        if (first == null) {
            return null;
        }

        Double lat = parseDouble(first.getString("lat"));
        Double lon = parseDouble(first.getString("lon"));
        if (!isValidCoordinate(lat, lon)) {
            return null;
        }
        return new GeoPoint(lat, lon);
    }

    private GeoPoint pickPhotonCoordinate(JSONArray features, String city, boolean strictCountry) {
        Set<String> countryCodes = parseCountryCodeSet(freeGeocodeCountryCodes);
        GeoPoint fallback = null;
        GeoPoint countryFallback = null;

        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = features.getJSONObject(i);
            if (feature == null) {
                continue;
            }

            GeoPoint point = parsePhotonCoordinate(feature.getJSONObject("geometry"));
            if (point == null) {
                continue;
            }
            if (fallback == null) {
                fallback = point;
            }

            JSONObject properties = feature.getJSONObject("properties");
            String countryCode = properties == null ? "" : properties.getString("countrycode");
            boolean countryMatched = countryCodes.isEmpty() || countryCodes.contains(normalizeLower(countryCode));
            if (countryMatched && countryFallback == null) {
                countryFallback = point;
            }
            if (strictCountry && !countryMatched) {
                continue;
            }

            if (!hasText(city)) {
                return point;
            }
            if (properties != null) {
                String merged = joinParts(
                        properties.getString("city"),
                        properties.getString("district"),
                        properties.getString("county"),
                        properties.getString("state"),
                        properties.getString("name")
                );
                if (containsIgnoreCase(merged, city)) {
                    return point;
                }
            }

            if (!strictCountry && countryMatched) {
                return point;
            }
        }

        if (strictCountry) {
            return countryFallback;
        }
        return countryFallback != null ? countryFallback : fallback;
    }

    private Set<String> parseCountryCodeSet(String text) {
        Set<String> countryCodes = new HashSet<>();
        if (!hasText(text)) {
            return countryCodes;
        }
        String[] parts = text.trim().split("[,|;\\s]+");
        for (String part : parts) {
            if (hasText(part)) {
                countryCodes.add(normalizeLower(part));
            }
        }
        return countryCodes;
    }

    private GeoPoint parsePhotonCoordinate(JSONObject geometry) {
        if (geometry == null) {
            return null;
        }
        JSONArray coordinates = geometry.getJSONArray("coordinates");
        if (coordinates == null || coordinates.size() < 2) {
            return null;
        }
        Double lon = parseDouble(String.valueOf(coordinates.get(0)));
        Double lat = parseDouble(String.valueOf(coordinates.get(1)));
        if (!isValidCoordinate(lat, lon)) {
            return null;
        }
        return new GeoPoint(lat, lon);
    }

    private RouteOption requestOsrmRoute(String profile,
                                         String mode,
                                         String modeName,
                                         GeoPoint origin,
                                         GeoPoint destination) {
        String base = trimEndSlash(freeRouteOsrmUrl);
        String url = String.format(Locale.ROOT,
                "%s/%s/%.6f,%.6f;%.6f,%.6f",
                base,
                profile,
                origin.lon,
                origin.lat,
                destination.lon,
                destination.lat);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("overview", "full");
        params.put("geometries", "geojson");
        params.put("steps", "false");
        params.put("alternatives", "false");

        JSONObject root = callJsonObject(url, params, null);
        if (root == null || !"Ok".equalsIgnoreCase(root.getString("code"))) {
            return null;
        }

        JSONObject route = firstObject(root, "routes");
        if (route == null) {
            return null;
        }

        RouteOption option = new RouteOption(mode, modeName);
        option.distanceMeters = parsePositiveDouble(String.valueOf(route.get("distance")));
        option.durationSeconds = parsePositiveDouble(String.valueOf(route.get("duration")));
        JSONObject geometry = route.getJSONObject("geometry");
        option.polyline = parseGeoJsonCoordinates(geometry == null ? null : geometry.getJSONArray("coordinates"));
        option.ensureMinimalPolyline(origin, destination);
        return option;
    }

    private List<RouteOption> buildRouteOptions(GeoPoint origin, GeoPoint destination, double directDistanceMeters) {
        List<RouteOption> routes = new ArrayList<>();
        if (shouldUseEstimatedRoutesOnly(directDistanceMeters)) {
            addIfPresent(routes, buildEstimatedRoute(MODE_WALKING, "\u6b65\u884c", origin, destination, directDistanceMeters));
            addIfPresent(routes, buildEstimatedRoute(MODE_RIDING, "\u9a91\u884c", origin, destination, directDistanceMeters));
            addIfPresent(routes, buildEstimatedRoute(MODE_DRIVING, "\u9a7e\u8f66", origin, destination, directDistanceMeters));
            return routes;
        }

        CompletableFuture<RouteOption> walkingFuture = CompletableFuture.supplyAsync(
                () -> requestOsrmRoute("foot", MODE_WALKING, "\u6b65\u884c", origin, destination));
        CompletableFuture<RouteOption> ridingFuture = CompletableFuture.supplyAsync(
                () -> requestOsrmRoute("bike", MODE_RIDING, "\u9a91\u884c", origin, destination));
        CompletableFuture<RouteOption> drivingFuture = CompletableFuture.supplyAsync(
                () -> requestOsrmRoute("driving", MODE_DRIVING, "\u9a7e\u8f66", origin, destination));

        addIfPresent(routes, resolveRouteOrEstimate(walkingFuture, MODE_WALKING, "\u6b65\u884c", origin, destination, directDistanceMeters));
        addIfPresent(routes, resolveRouteOrEstimate(ridingFuture, MODE_RIDING, "\u9a91\u884c", origin, destination, directDistanceMeters));
        addIfPresent(routes, resolveRouteOrEstimate(drivingFuture, MODE_DRIVING, "\u9a7e\u8f66", origin, destination, directDistanceMeters));
        return routes;
    }

    private RouteOption resolveRouteOrEstimate(CompletableFuture<RouteOption> future,
                                               String mode,
                                               String modeName,
                                               GeoPoint origin,
                                               GeoPoint destination,
                                               double directDistanceMeters) {
        try {
            RouteOption option = future.join();
            if (option != null && option.isValid()) {
                return option;
            }
        } catch (Exception ignored) {
            // ignore and fallback to local estimate
        }
        return buildEstimatedRoute(mode, modeName, origin, destination, directDistanceMeters);
    }

    private RouteOption buildEstimatedRoute(String mode,
                                            String modeName,
                                            GeoPoint origin,
                                            GeoPoint destination,
                                            double directDistanceMeters) {
        if (directDistanceMeters <= 0) {
            return null;
        }
        RouteOption option = new RouteOption(mode, modeName);
        option.distanceMeters = directDistanceMeters * estimateDistanceFactor(mode);
        double speedKmh = estimateSpeedKmh(mode);
        if (speedKmh > 0) {
            option.durationSeconds = option.distanceMeters / (speedKmh * 1000D / 3600D);
        }
        option.estimated = true;
        option.ensureMinimalPolyline(origin, destination);
        return option;
    }

    private boolean shouldUseEstimatedRoutesOnly(double directDistanceMeters) {
        int maxDistanceKm = normalizeMaxDistanceKm(freeRouteOsrmMaxDistanceKm);
        return directDistanceMeters > maxDistanceKm * 1000D;
    }

    private int normalizeMaxDistanceKm(Integer value) {
        if (value == null) {
            return 600;
        }
        return Math.max(50, Math.min(value, 5000));
    }

    private double estimateDistanceFactor(String mode) {
        if (MODE_WALKING.equals(mode)) {
            return 1.08D;
        }
        if (MODE_RIDING.equals(mode)) {
            return 1.12D;
        }
        return 1.25D;
    }

    private double estimateSpeedKmh(String mode) {
        if (MODE_WALKING.equals(mode)) {
            return 4.8D;
        }
        if (MODE_RIDING.equals(mode)) {
            return 15D;
        }
        return 65D;
    }

    private double estimateDirectDistanceMeters(GeoPoint origin, GeoPoint destination) {
        double lat1 = Math.toRadians(origin.lat);
        double lon1 = Math.toRadians(origin.lon);
        double lat2 = Math.toRadians(destination.lat);
        double lon2 = Math.toRadians(destination.lon);
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000D * c;
    }

    private RouteOption recommendRoute(List<RouteOption> options) {
        Map<String, RouteOption> byMode = options.stream()
                .collect(Collectors.toMap(opt -> opt.mode, opt -> opt, (l, r) -> l));

        double minDistance = options.stream()
                .mapToDouble(opt -> opt.distanceMeters)
                .filter(d -> d > 0)
                .min()
                .orElse(Double.MAX_VALUE);

        if (minDistance <= 2500 && byMode.containsKey(MODE_WALKING)) {
            return byMode.get(MODE_WALKING);
        }
        if (minDistance <= 12000 && byMode.containsKey(MODE_RIDING)) {
            return byMode.get(MODE_RIDING);
        }
        if (minDistance > 12000 && byMode.containsKey(MODE_DRIVING)) {
            return byMode.get(MODE_DRIVING);
        }

        return options.stream()
                .min(Comparator.comparingDouble(opt -> opt.durationSeconds <= 0 ? Double.MAX_VALUE : opt.durationSeconds))
                .orElse(options.get(0));
    }

    private String buildRecommendationReason(RouteOption option) {
        String distance = formatDistance(option.distanceMeters);
        String duration = formatDuration(option.durationSeconds);
        String suffix = option.estimated ? "\uff08\u57fa\u4e8e\u5f00\u6e90\u5730\u56fe\u4f30\u7b97\uff09" : "";
        if (MODE_WALKING.equals(option.mode)) {
            return "\u5168\u7a0b\u7ea6" + distance + "\uff0c\u9884\u8ba1" + duration + "\uff0c\u77ed\u8ddd\u79bb\u6b65\u884c\u66f4\u7701\u65f6\u3002" + suffix;
        }
        if (MODE_RIDING.equals(option.mode)) {
            return "\u5168\u7a0b\u7ea6" + distance + "\uff0c\u9884\u8ba1" + duration + "\uff0c\u4e2d\u77ed\u8ddd\u79bb\u9a91\u884c\u66f4\u7075\u6d3b\u3002" + suffix;
        }
        return "\u5168\u7a0b\u7ea6" + distance + "\uff0c\u9884\u8ba1" + duration + "\uff0c\u8ddd\u79bb\u8f83\u8fdc\u5efa\u8bae\u9a7e\u8f66\u3002" + suffix;
    }

    private JSONObject callJsonObject(String url, Map<String, String> params, Map<String, String> headers) {
        String body = doHttpGet(url, params, headers);
        if (!hasText(body)) {
            return null;
        }
        try {
            return JSON.parseObject(body);
        } catch (Exception e) {
            logger.warn("Parse JSON object failed, url={}, message={}", url, e.getMessage());
            return null;
        }
    }

    private JSONArray callJsonArray(String url, Map<String, String> params, Map<String, String> headers) {
        String body = doHttpGet(url, params, headers);
        if (!hasText(body)) {
            return null;
        }
        try {
            return JSON.parseArray(body);
        } catch (Exception e) {
            logger.warn("Parse JSON array failed, url={}, message={}", url, e.getMessage());
            return null;
        }
    }

    private String doHttpGet(String url, Map<String, String> params, Map<String, String> headers) {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(normalizeTimeout(requestTimeoutMs))
                .setConnectionRequestTimeout(normalizeTimeout(requestTimeoutMs))
                .setSocketTimeout(normalizeTimeout(requestTimeoutMs))
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build()) {
            URIBuilder builder = new URIBuilder(url);
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (hasText(entry.getValue())) {
                        builder.addParameter(entry.getKey(), entry.getValue());
                    }
                }
            }

            HttpGet request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept", "application/json");
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    if (hasText(header.getValue())) {
                        request.setHeader(header.getKey(), header.getValue());
                    }
                }
            }

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    logger.warn("HTTP request failed, url={}, status={}", url, statusCode);
                    return null;
                }
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warn("HTTP request exception, url={}, message={}", url, e.getMessage());
            return null;
        }
    }

    private JSONObject firstObject(JSONObject root, String arrayField) {
        if (root == null) {
            return null;
        }
        JSONArray array = root.getJSONArray(arrayField);
        if (array == null || array.isEmpty()) {
            return null;
        }
        return array.getJSONObject(0);
    }

    private List<List<Double>> parseGeoJsonCoordinates(JSONArray coordinates) {
        List<List<Double>> points = new ArrayList<>();
        if (coordinates == null) {
            return points;
        }

        for (int i = 0; i < coordinates.size(); i++) {
            JSONArray pair = coordinates.getJSONArray(i);
            if (pair == null || pair.size() < 2) {
                continue;
            }
            Double lon = parseDouble(String.valueOf(pair.get(0)));
            Double lat = parseDouble(String.valueOf(pair.get(1)));
            if (!isValidCoordinate(lat, lon)) {
                continue;
            }
            List<Double> latLon = new ArrayList<>(2);
            latLon.add(lat);
            latLon.add(lon);
            points.add(latLon);
        }
        return points;
    }

    private GeoPoint parseCoordinateInput(String input) {
        if (!hasText(input)) {
            return null;
        }
        String[] parts = input.trim().split("[,\uFF0C\\s]+");
        if (parts.length != 2) {
            return null;
        }

        Double first = parseDouble(parts[0]);
        Double second = parseDouble(parts[1]);
        if (first == null || second == null) {
            return null;
        }
        if (Math.abs(first) <= 90 && Math.abs(second) <= 180) {
            return new GeoPoint(first, second);
        }
        if (Math.abs(first) <= 180 && Math.abs(second) <= 90) {
            return new GeoPoint(second, first);
        }
        return null;
    }

    private boolean isValidCoordinate(Double lat, Double lon) {
        if (lat == null || lon == null) {
            return false;
        }
        return Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 8000;
        }
        return Math.max(2000, Math.min(timeoutMs, 20000));
    }

    private Double parseDouble(String text) {
        if (!hasText(text)) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private double parsePositiveDouble(String text) {
        Double value = parseDouble(text);
        if (value == null) {
            return 0D;
        }
        return value > 0D ? value : 0D;
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private String normalizeLower(String text) {
        return hasText(text) ? text.trim().toLowerCase(Locale.ROOT) : "";
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (!hasText(source) || !hasText(target)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(target.trim().toLowerCase(Locale.ROOT));
    }

    private String trimEndSlash(String value) {
        if (!hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String formatDistance(double meters) {
        if (meters <= 0) {
            return "\u672a\u77e5\u8ddd\u79bb";
        }
        if (meters >= 1000) {
            return String.format(Locale.ROOT, "%.1f\u516c\u91cc", meters / 1000D);
        }
        return String.format(Locale.ROOT, "%.0f\u7c73", meters);
    }

    private static String formatDuration(double seconds) {
        if (seconds <= 0) {
            return "\u672a\u77e5\u65f6\u957f";
        }
        long minutes = Math.max(1L, Math.round(seconds / 60D));
        long hours = minutes / 60L;
        long remain = minutes % 60L;
        if (hours <= 0) {
            return minutes + "\u5206\u949f";
        }
        if (remain == 0) {
            return hours + "\u5c0f\u65f6";
        }
        return hours + "\u5c0f\u65f6" + remain + "\u5206\u949f";
    }

    private Map<String, Object> response(int code, String msg) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("msg", msg);
        return data;
    }

    private Map<String, Object> toPointMap(GeoPoint point) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lat", point.lat);
        data.put("lon", point.lon);
        return data;
    }

    private Map<String, Object> toDestinationMap(GeoPoint point, Scenic scenic) {
        Map<String, Object> data = toPointMap(point);
        data.put("name", scenic.getName());
        data.put("address", scenic.getAddress());
        return data;
    }

    private void addIfPresent(List<RouteOption> options, RouteOption option) {
        if (option != null && option.isValid()) {
            options.add(option);
        }
    }

    private static final class RouteOption {
        private final String mode;
        private final String modeName;
        private double distanceMeters;
        private double durationSeconds;
        private boolean estimated;
        private List<List<Double>> polyline = new ArrayList<>();

        private RouteOption(String mode, String modeName) {
            this.mode = mode;
            this.modeName = modeName;
        }

        private boolean isValid() {
            return distanceMeters > 0 || durationSeconds > 0;
        }

        private void ensureMinimalPolyline(GeoPoint origin, GeoPoint destination) {
            if (polyline != null && polyline.size() >= 2) {
                return;
            }
            polyline = new ArrayList<>();
            polyline.add(origin.toLatLonPair());
            polyline.add(destination.toLatLonPair());
        }

        private Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("mode", mode);
            data.put("modeName", modeName);
            data.put("distanceMeters", Math.round(distanceMeters));
            data.put("durationSeconds", Math.round(durationSeconds));
            data.put("distanceText", formatDistance(distanceMeters));
            data.put("durationText", formatDuration(durationSeconds));
            data.put("estimated", estimated);
            data.put("polyline", polyline == null ? new ArrayList<>() : polyline);
            return data;
        }
    }

    private static final class GeoPoint {
        private final double lat;
        private final double lon;

        private GeoPoint(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        private List<Double> toLatLonPair() {
            List<Double> point = new ArrayList<>(2);
            point.add(lat);
            point.add(lon);
            return point;
        }
    }
}
