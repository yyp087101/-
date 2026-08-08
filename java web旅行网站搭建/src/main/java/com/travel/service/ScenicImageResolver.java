package com.travel.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class ScenicImageResolver {

    private static final String DEFAULT_NATURE_IMAGE = "/images/carousel/3.jpg";
    private static final String DEFAULT_CULTURE_IMAGE = "/images/carousel/2.jpg";

    private static final Map<String, String> SCENIC_IMAGE_MAP = new LinkedHashMap<>();

    static {
        SCENIC_IMAGE_MAP.put("故宫", "/images/scenic/1.jpg");
        SCENIC_IMAGE_MAP.put("西湖", "/images/scenic/2.jpg");
        SCENIC_IMAGE_MAP.put("兵马俑", "/images/scenic/3.jpg");
        SCENIC_IMAGE_MAP.put("张家界", "/images/scenic/4.jpg");
        SCENIC_IMAGE_MAP.put("九寨沟", "/images/scenic/5.jpg");
        SCENIC_IMAGE_MAP.put("黄山", "/images/scenic/6.jpg");
        SCENIC_IMAGE_MAP.put("丽江古城", "/images/scenic/7.jpg");
        SCENIC_IMAGE_MAP.put("鼓浪屿", "/images/scenic/8.jpg");
        SCENIC_IMAGE_MAP.put("布达拉宫", "/images/scenic/9.jpg");
        SCENIC_IMAGE_MAP.put("漓江", "/images/scenic/10.jpg");
    }

    public String resolveCoverImage(String currentCoverImage, String scenicName, String scenicType) {
        if (!needsBetterImage(currentCoverImage)) {
            return currentCoverImage.trim();
        }
        String mappedImage = findImageByName(scenicName);
        if (mappedImage != null) {
            return mappedImage;
        }
        return pickFallbackImage(scenicName, scenicType);
    }

    public boolean needsBetterImage(String coverImage) {
        return !hasText(coverImage)
                || isMapSnapshot(coverImage)
                || isUnreliableImageSource(coverImage)
                || isFallbackImage(coverImage);
    }

    public boolean isFallbackImage(String url) {
        if (!hasText(url)) {
            return false;
        }
        String normalized = url.trim();
        return DEFAULT_NATURE_IMAGE.equals(normalized) || DEFAULT_CULTURE_IMAGE.equals(normalized);
    }

    public boolean isMapSnapshot(String url) {
        if (!hasText(url)) {
            return true;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("staticmap")
                || normalized.contains("maptype=satellite")
                || normalized.contains("api.map.baidu.com/staticimage")
                || normalized.contains("maps.googleapis.com/maps/api/staticmap")
                || normalized.contains("restapi.amap.com/v3/staticmap")
                || normalized.contains("apis.map.qq.com/ws/staticmap");
    }

    private boolean isUnreliableImageSource(String url) {
        if (!hasText(url)) {
            return true;
        }
        String normalized = url.trim().toLowerCase(Locale.ROOT);
        return normalized.contains(".baidu.com/it/u=")
                || normalized.contains("image.baidu.com/search");
    }

    private String pickFallbackImage(String scenicName, String scenicType) {
        return isNature(scenicName, scenicType) ? DEFAULT_NATURE_IMAGE : DEFAULT_CULTURE_IMAGE;
    }

    private String findImageByName(String scenicName) {
        if (!hasText(scenicName)) {
            return null;
        }
        String normalizedName = scenicName.trim();
        for (Map.Entry<String, String> entry : SCENIC_IMAGE_MAP.entrySet()) {
            if (normalizedName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isNature(String scenicName, String scenicType) {
        String type = scenicType == null ? "" : scenicType;
        if (type.contains("自然")) {
            return true;
        }
        if (type.contains("人文")) {
            return false;
        }
        String name = scenicName == null ? "" : scenicName;
        return containsAny(name, "湖", "江", "山", "沟", "森林", "公园", "海", "河", "瀑布", "草原", "峡谷");
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
