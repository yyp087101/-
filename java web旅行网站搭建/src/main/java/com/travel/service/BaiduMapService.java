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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BaiduMapService {

    private static final Logger logger = LoggerFactory.getLogger(BaiduMapService.class);
    private static final String USER_AGENT = "travel-recommend/1.0 (+https://www.amap.com)";
    private static final String DEFAULT_OPEN_TIME = "\u6682\u672a\u63d0\u4f9b";
    private static final String DEFAULT_BEST_SEASON = "\u56db\u5b63\u7686\u5b9c";
    private static final String DEFAULT_SCENIC_WORD = "\u666f\u70b9";
    private static final String TYPE_NATURAL = "\u81ea\u7136";
    private static final String TYPE_CULTURAL = "\u4eba\u6587";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    @Value("${amap.web.key:}")
    private String amapWebKey;

    @Value("${amap.place.text.url:https://restapi.amap.com/v5/place/text}")
    private String amapPlaceTextUrl;

    @Value("${amap.place.around.url:https://restapi.amap.com/v5/place/around}")
    private String amapPlaceAroundUrl;

    @Value("${amap.search.types:110000|110100|110101|110102|110103|110104|110200|110201|110202|110203|110204}")
    private String amapSearchTypes;

    @Value("${amap.search.radius:10000}")
    private Integer amapSearchRadiusMeters;

    @Value("${amap.default.city:\u957f\u6c99}")
    private String amapDefaultCity;

    @Value("${amap.default.keyword:\u666f\u70b9}")
    private String amapDefaultKeyword;

    @Value("${amap.request.timeout-ms:8000}")
    private Integer amapRequestTimeoutMs;

    private final ScenicImageResolver scenicImageResolver;

    public BaiduMapService(ScenicImageResolver scenicImageResolver) {
        this.scenicImageResolver = scenicImageResolver;
    }

    public List<Scenic> searchScenic(String keyword, String city, Integer num, Integer page) {
        String normalizedKeyword = normalize(keyword);
        String normalizedCity = normalize(city);
        int pageSize = normalizePageSize(num);
        int pageNum = normalizePageNum(page);

        if (normalize(amapWebKey).isEmpty()) {
            logger.warn("AMap search skipped because amap.web.key is empty");
            return new ArrayList<>();
        }

        if (normalizedKeyword.isEmpty() && normalizedCity.isEmpty()) {
            normalizedCity = normalize(amapDefaultCity);
        }

        GeoPoint center = parseCoordinateInput(normalizedCity);
        if (center != null) {
            return searchByAround(normalizedKeyword, center, pageSize, pageNum);
        }
        return searchByText(normalizedKeyword, normalizedCity, pageSize, pageNum);
    }

    public List<Scenic> searchByCity(String city, Integer num) {
        return searchScenic("", city, num, 1);
    }

    public List<Scenic> searchByKeyword(String keyword, Integer num) {
        return searchScenic(keyword, "", num, 1);
    }

    private List<Scenic> searchByText(String keyword, String city, int pageSize, int pageNum) {
        String effectiveKeyword = buildSearchKeyword(keyword);
        String region = normalize(city);
        if (region.isEmpty()) {
            region = normalize(amapDefaultCity);
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", amapWebKey);
        params.put("keywords", effectiveKeyword);
        params.put("types", normalize(amapSearchTypes));
        params.put("page_size", String.valueOf(pageSize));
        params.put("page_num", String.valueOf(pageNum));
        params.put("show_fields", "business,photos");
        if (!region.isEmpty()) {
            params.put("region", region);
            params.put("city_limit", "true");
        }

        List<Scenic> scenics = callAmapPlaceApi(amapPlaceTextUrl, params, region);
        if (!scenics.isEmpty()) {
            logger.info("Scenic search resolved via AMap text search, keyword='{}', city='{}', count={}",
                    keyword, city, scenics.size());
            return scenics;
        }

        if (!keyword.isEmpty()) {
            params.put("keywords", keyword + " " + DEFAULT_SCENIC_WORD);
            scenics = callAmapPlaceApi(amapPlaceTextUrl, params, region);
            if (!scenics.isEmpty()) {
                logger.info("Scenic search resolved via AMap text fallback, keyword='{}', city='{}', count={}",
                        keyword, city, scenics.size());
            }
        }
        return scenics;
    }

    private List<Scenic> searchByAround(String keyword, GeoPoint center, int pageSize, int pageNum) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", amapWebKey);
        params.put("location", center.toAmapLocation());
        params.put("radius", String.valueOf(normalizeRadius(amapSearchRadiusMeters)));
        params.put("sortrule", "distance");
        params.put("types", normalize(amapSearchTypes));
        params.put("page_size", String.valueOf(pageSize));
        params.put("page_num", String.valueOf(pageNum));
        params.put("show_fields", "business,photos");
        params.put("keywords", buildSearchKeyword(keyword));

        List<Scenic> scenics = callAmapPlaceApi(amapPlaceAroundUrl, params, "");
        if (!scenics.isEmpty()) {
            logger.info("Scenic search resolved via AMap around search, keyword='{}', location='{}', count={}",
                    keyword, center.toAmapLocation(), scenics.size());
            return scenics;
        }

        if (!keyword.isEmpty()) {
            params.remove("keywords");
            scenics = callAmapPlaceApi(amapPlaceAroundUrl, params, "");
            if (!scenics.isEmpty()) {
                logger.info("Scenic search resolved via AMap around fallback, location='{}', count={}",
                        center.toAmapLocation(), scenics.size());
            }
        }
        return scenics;
    }

    private List<Scenic> callAmapPlaceApi(String url, Map<String, String> params, String fallbackCity) {
        RequestConfig requestConfig = buildRequestConfig(amapRequestTimeoutMs);
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
            URIBuilder builder = new URIBuilder(url);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (!normalize(entry.getValue()).isEmpty()) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
            HttpGet request = new HttpGet(builder.build());
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    logger.warn("AMap request failed, url={}, status={}", url, statusCode);
                    return new ArrayList<>();
                }
                String body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                return parseAmapResponse(body, fallbackCity);
            }
        } catch (Exception e) {
            logger.warn("AMap request failed, url={}, message={}", url, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Scenic> parseAmapResponse(String body, String fallbackCity) {
        List<Scenic> scenics = new ArrayList<>();
        JSONObject root = JSON.parseObject(body);
        if (root == null) {
            return scenics;
        }

        String status = normalize(root.getString("status"));
        if (!"1".equals(status)) {
            logger.warn("AMap returned non-success status={}, info={}, infocode={}",
                    status, normalize(root.getString("info")), normalize(root.getString("infocode")));
            return scenics;
        }

        JSONArray pois = root.getJSONArray("pois");
        if (pois == null || pois.isEmpty()) {
            return scenics;
        }

        Map<String, Scenic> uniqueScenics = new LinkedHashMap<>();
        for (int i = 0; i < pois.size(); i++) {
            Scenic scenic = toScenic(pois.getJSONObject(i), fallbackCity);
            if (scenic == null) {
                continue;
            }
            uniqueScenics.putIfAbsent(uniqueKey(scenic), scenic);
        }
        scenics.addAll(uniqueScenics.values());
        return scenics;
    }

    private Scenic toScenic(JSONObject poi, String fallbackCity) {
        if (poi == null) {
            return null;
        }
        String name = normalizeField(poi.getString("name"));
        if (name.isEmpty()) {
            return null;
        }

        Scenic scenic = new Scenic();
        scenic.setName(name);
        scenic.setCity(normalizePlaceName(firstNonBlank(
                normalizeField(poi.getString("cityname")),
                normalizeField(fallbackCity))));
        scenic.setProvince(normalizePlaceName(normalizeField(poi.getString("pname"))));

        String district = normalizePlaceName(normalizeField(poi.getString("adname")));
        String plainAddress = normalizeField(poi.getString("address"));
        scenic.setAddress(buildAddress(scenic.getProvince(), scenic.getCity(), district, plainAddress));

        String type = normalizeField(poi.getString("type"));
        String typeCode = normalizeField(poi.getString("typecode"));
        scenic.setScenicType(resolveScenicType(type, typeCode, scenic.getName()));

        JSONObject business = parseBusinessObject(poi.get("business"));
        scenic.setOpenTime(firstNonBlank(
                normalizeField(business == null ? "" : business.getString("opentime_today")),
                normalizeField(business == null ? "" : business.getString("opentime_week")),
                DEFAULT_OPEN_TIME));
        scenic.setBestSeason(DEFAULT_BEST_SEASON);
        scenic.setContent(buildContent(type, scenic.getAddress(), business));
        scenic.setIsApi(1);
        scenic.setApiId(firstNonBlank(normalizeField(poi.getString("id")),
                scenic.getName() + "_" + scenic.getCity()));
        scenic.setRating(resolveRating(business));
        scenic.setTicketPrice(resolveTicketPrice(business));
        scenic.setViewCount(0);

        String photoUrl = extractPhotoUrl(poi.get("photos"));
        scenic.setCoverImage(scenicImageResolver.resolveCoverImage(photoUrl, scenic.getName(), scenic.getScenicType()));
        return scenic;
    }

    private JSONObject parseBusinessObject(Object business) {
        if (business instanceof JSONObject) {
            return (JSONObject) business;
        }
        if (business instanceof String) {
            String text = normalizeField((String) business);
            if (text.startsWith("{") && text.endsWith("}")) {
                try {
                    return JSON.parseObject(text);
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private String extractPhotoUrl(Object photosObject) {
        if (photosObject instanceof JSONArray) {
            JSONArray photos = (JSONArray) photosObject;
            for (int i = 0; i < photos.size(); i++) {
                JSONObject photo = photos.getJSONObject(i);
                String url = normalizeField(photo == null ? "" : photo.getString("url"));
                if (!url.isEmpty()) {
                    return url;
                }
            }
            return "";
        }
        if (photosObject instanceof JSONObject) {
            return normalizeField(((JSONObject) photosObject).getString("url"));
        }
        if (photosObject instanceof String) {
            String value = normalizeField((String) photosObject);
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return value;
            }
        }
        return "";
    }

    private BigDecimal resolveRating(JSONObject business) {
        BigDecimal defaultRating = new BigDecimal("4.0");
        if (business == null) {
            return defaultRating;
        }
        BigDecimal value = parseNumericValue(normalizeField(business.getString("rating")));
        if (value == null) {
            return defaultRating;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return defaultRating;
        }
        return value.compareTo(new BigDecimal("5.0")) > 0 ? new BigDecimal("5.0") : value;
    }

    private BigDecimal resolveTicketPrice(JSONObject business) {
        if (business == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal value = parseNumericValue(normalizeField(business.getString("cost")));
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    private BigDecimal parseNumericValue(String text) {
        String normalized = normalizeField(text).replace(",", "");
        if (normalized.isEmpty()) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String buildSearchKeyword(String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (!normalizedKeyword.isEmpty()) {
            return normalizedKeyword;
        }
        String defaultKeyword = normalize(amapDefaultKeyword);
        return defaultKeyword.isEmpty() ? DEFAULT_SCENIC_WORD : defaultKeyword;
    }

    private String buildAddress(String province, String city, String district, String detailAddress) {
        StringBuilder address = new StringBuilder();
        appendAddressPart(address, province);
        appendAddressPart(address, city);
        appendAddressPart(address, district);
        appendAddressPart(address, detailAddress);
        String merged = normalize(address.toString());
        return merged.isEmpty() ? firstNonBlank(city, province) : merged;
    }

    private void appendAddressPart(StringBuilder builder, String part) {
        String normalized = normalize(part);
        if (normalized.isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(normalized);
    }

    private String buildContent(String type, String address, JSONObject business) {
        StringBuilder content = new StringBuilder("Data source: AMap Place API.");
        if (!normalize(type).isEmpty()) {
            content.append(" Type: ").append(type).append('.');
        }
        if (!normalize(address).isEmpty()) {
            content.append(" Address: ").append(address).append('.');
        }
        if (business != null) {
            String tel = normalizeField(business.getString("tel"));
            if (!tel.isEmpty()) {
                content.append(" Tel: ").append(tel).append('.');
            }
        }
        return content.toString();
    }

    private String resolveScenicType(String type, String typeCode, String name) {
        String merged = (normalize(type) + "|" + normalize(name)).toLowerCase(Locale.ROOT);
        if (normalize(typeCode).startsWith("11")
                || containsAny(merged,
                "\u98ce\u666f", "\u516c\u56ed", "\u5c71", "\u6e56", "\u6d77",
                "\u5ce1\u8c37", "\u6e7f\u5730", "\u7011\u5e03", "\u68ee\u6797", "\u5c9b", "\u81ea\u7136")) {
            return TYPE_NATURAL;
        }
        return TYPE_CULTURAL;
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private String uniqueKey(Scenic scenic) {
        String apiId = normalize(scenic.getApiId());
        if (!apiId.isEmpty()) {
            return apiId;
        }
        return normalize(scenic.getName()).toLowerCase(Locale.ROOT)
                + "|"
                + normalize(scenic.getCity()).toLowerCase(Locale.ROOT);
    }

    private RequestConfig buildRequestConfig(Integer timeoutMs) {
        int timeout = normalizeTimeout(timeoutMs);
        return RequestConfig.custom()
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
    }

    private int normalizeTimeout(Integer timeoutMs) {
        if (timeoutMs == null) {
            return 8000;
        }
        return Math.max(2000, Math.min(timeoutMs, 20000));
    }

    private int normalizePageSize(Integer num) {
        if (num == null) {
            return 10;
        }
        return Math.max(1, Math.min(num, 20));
    }

    private int normalizePageNum(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }
        return page;
    }

    private int normalizeRadius(Integer radius) {
        if (radius == null) {
            return 10000;
        }
        return Math.max(1000, Math.min(radius, 50000));
    }

    private String normalizePlaceName(String value) {
        String normalized = normalizeField(value);
        if (normalized.length() <= 2) {
            return normalized;
        }
        String[] suffixes = {
                "\u7701",
                "\u5e02",
                "\u81ea\u6cbb\u533a",
                "\u81ea\u6cbb\u5dde",
                "\u5730\u533a",
                "\u7279\u522b\u884c\u653f\u533a"
        };
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length() + 1) {
                return normalized.substring(0, normalized.length() - suffix.length());
            }
        }
        return normalized;
    }

    private GeoPoint parseCoordinateInput(String input) {
        String normalized = normalize(input);
        if (normalized.isEmpty()) {
            return null;
        }

        String[] parts = normalized.split("[,，\\s]+");
        if (parts.length != 2) {
            return null;
        }
        try {
            double first = Double.parseDouble(parts[0].trim());
            double second = Double.parseDouble(parts[1].trim());

            if (Math.abs(first) <= 90 && Math.abs(second) <= 180) {
                return new GeoPoint(first, second);
            }
            if (Math.abs(first) <= 180 && Math.abs(second) <= 90) {
                return new GeoPoint(second, first);
            }
            return null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeField(String text) {
        String normalized = normalize(text);
        if ("[]".equals(normalized) || "{}".equals(normalized) || "null".equalsIgnoreCase(normalized)) {
            return "";
        }
        return normalized;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeField(value);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    private static final class GeoPoint {
        private final double lat;
        private final double lon;

        private GeoPoint(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        private String toAmapLocation() {
            return String.format(Locale.ROOT, "%.6f,%.6f", lon, lat);
        }
    }
}
