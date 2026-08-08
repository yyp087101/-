package com.travel.service;

import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ScenicImageSearchService {

    private static final Logger logger = LoggerFactory.getLogger(ScenicImageSearchService.class);
    private static final String USER_AGENT = "travel-recommend/1.0 (+https://cn.bing.com)";
    private static final Pattern DIRECT_IMAGE_PATTERN = Pattern.compile("murl&quot;:&quot;(.*?)&quot;");
    private static final Pattern THUMB_IMAGE_PATTERN = Pattern.compile("turl&quot;:&quot;(.*?)&quot;");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("^-?\\d+(?:\\.\\d+)?\\s*,\\s*-?\\d+(?:\\.\\d+)?$");
    private static final String NO_IMAGE_CACHE_VALUE = "__NONE__";

    @Value("${scenic.image.search.enabled:true}")
    private boolean imageSearchEnabled;

    @Value("${scenic.image.search.provider-url:https://cn.bing.com/images/search}")
    private String imageSearchProviderUrl;

    @Value("${scenic.image.search.timeout-ms:4500}")
    private Integer timeoutMs;

    @Value("${scenic.image.search.max-candidates:3}")
    private Integer maxCandidates;

    private final Map<String, String> imageCache = new ConcurrentHashMap<>();

    public String searchImageUrl(String scenicName, String city) {
        String query = buildQuery(scenicName, city);
        if (!imageSearchEnabled || query.isEmpty()) {
            return null;
        }
        String cacheKey = query.toLowerCase(Locale.ROOT);
        if (imageCache.containsKey(cacheKey)) {
            String cached = imageCache.get(cacheKey);
            return NO_IMAGE_CACHE_VALUE.equals(cached) ? null : cached;
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String html = fetchSearchHtml(httpClient, query);
            if (html.isEmpty()) {
                imageCache.put(cacheKey, NO_IMAGE_CACHE_VALUE);
                return null;
            }
            List<String> candidates = parseCandidates(html, normalizeMaxCandidates(maxCandidates));
            for (String candidate : candidates) {
                if (isReachableImage(httpClient, candidate)) {
                    imageCache.put(cacheKey, candidate);
                    return candidate;
                }
            }
        } catch (Exception e) {
            logger.warn("Image search failed for query={}", query, e);
        }
        imageCache.put(cacheKey, NO_IMAGE_CACHE_VALUE);
        return null;
    }

    private String fetchSearchHtml(CloseableHttpClient httpClient, String query) {
        try {
            URIBuilder builder = new URIBuilder(imageSearchProviderUrl);
            builder.addParameter("q", query);
            builder.addParameter("form", "HDRSC2");
            builder.addParameter("first", "1");
            builder.addParameter("tsc", "ImageBasicHover");

            HttpGet request = new HttpGet(builder.build());
            request.setConfig(buildRequestConfig(timeoutMs));
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            request.setHeader("Accept", "text/html,application/xhtml+xml");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 300) {
                    logger.warn("Image search HTTP failed, query={}, status={}", query, status);
                    return "";
                }
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            logger.warn("Image search request failed for query={}", query, e);
            return "";
        }
    }

    private List<String> parseCandidates(String html, int limit) {
        Set<String> uniqueUrls = new LinkedHashSet<>();
        appendMatches(uniqueUrls, html, DIRECT_IMAGE_PATTERN, limit);
        appendMatches(uniqueUrls, html, THUMB_IMAGE_PATTERN, limit);
        return new ArrayList<>(uniqueUrls);
    }

    private void appendMatches(Set<String> target, String html, Pattern pattern, int limit) {
        Matcher matcher = pattern.matcher(html);
        while (matcher.find() && target.size() < limit) {
            String candidate = decodeHtmlValue(matcher.group(1));
            if (isHttpUrl(candidate)) {
                target.add(candidate);
            }
        }
    }

    private boolean isReachableImage(CloseableHttpClient httpClient, String imageUrl) {
        if (!isHttpUrl(imageUrl)) {
            return false;
        }
        return checkByHead(httpClient, imageUrl) || checkByGet(httpClient, imageUrl);
    }

    private boolean checkByHead(CloseableHttpClient httpClient, String imageUrl) {
        try {
            HttpHead request = new HttpHead(imageUrl);
            request.setConfig(buildRequestConfig(timeoutMs));
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept", "image/*,*/*;q=0.8");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 400) {
                    return false;
                }
                return isImageContentType(response.getFirstHeader("Content-Type"));
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean checkByGet(CloseableHttpClient httpClient, String imageUrl) {
        try {
            HttpGet request = new HttpGet(imageUrl);
            request.setConfig(buildRequestConfig(timeoutMs));
            request.setHeader("User-Agent", USER_AGENT);
            request.setHeader("Accept", "image/*,*/*;q=0.8");
            request.setHeader("Range", "bytes=0-1");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int status = response.getStatusLine().getStatusCode();
                if (status < 200 || status >= 400) {
                    return false;
                }
                return isImageContentType(response.getFirstHeader("Content-Type"));
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isImageContentType(Header contentType) {
        if (contentType == null || contentType.getValue() == null) {
            return false;
        }
        return contentType.getValue().toLowerCase(Locale.ROOT).startsWith("image/");
    }

    private RequestConfig buildRequestConfig(Integer timeout) {
        int normalized = normalizeTimeout(timeout);
        return RequestConfig.custom()
                .setConnectTimeout(normalized)
                .setConnectionRequestTimeout(normalized)
                .setSocketTimeout(normalized)
                .build();
    }

    private int normalizeTimeout(Integer value) {
        if (value == null) {
            return 4500;
        }
        return Math.max(1000, Math.min(value, 15000));
    }

    private int normalizeMaxCandidates(Integer value) {
        if (value == null) {
            return 3;
        }
        return Math.max(1, Math.min(value, 12));
    }

    private String buildQuery(String scenicName, String city) {
        String normalizedName = normalize(scenicName);
        if (normalizedName.isEmpty()) {
            return "";
        }
        String normalizedCity = normalize(city);
        if (normalizedCity.isEmpty() || COORDINATE_PATTERN.matcher(normalizedCity).matches()) {
            return normalizedName + " 景点";
        }
        return normalizedName + " " + normalizedCity + " 景点";
    }

    private String decodeHtmlValue(String text) {
        String normalized = normalize(text);
        return normalized
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("\\/", "/")
                .replace("\\u002f", "/");
    }

    private boolean isHttpUrl(String text) {
        String normalized = normalize(text);
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
