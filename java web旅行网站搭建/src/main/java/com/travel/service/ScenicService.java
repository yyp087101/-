package com.travel.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.travel.entity.Scenic;
import com.travel.mapper.BrowseHistoryMapper;
import com.travel.mapper.FavoriteMapper;
import com.travel.mapper.ScenicMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScenicService {

    private static final long CITY_CACHE_TTL_MS = 5 * 60 * 1000L;

    @Autowired
    private ScenicMapper scenicMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BrowseHistoryMapper browseHistoryMapper;

    @Autowired
    private BaiduMapService baiduMapService;

    @Autowired
    private ScenicImageResolver scenicImageResolver;

    @Autowired
    private ScenicImageSearchService scenicImageSearchService;

    private volatile List<String> cachedCities = Collections.emptyList();
    private volatile long cachedCitiesExpiresAt = 0L;

    public Scenic findById(Integer id) {
        return normalizeCoverImage(scenicMapper.findById(id));
    }

    public PageInfo<Scenic> findAll(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Scenic> list = scenicMapper.findAll();
        normalizeCoverImages(list);
        return new PageInfo<>(list);
    }

    public PageInfo<Scenic> search(String keyword, String city, String scenicType, int pageNum, int pageSize) {
        PageInfo<Scenic> pageInfo = searchFromDatabase(keyword, city, scenicType, pageNum, pageSize);
        if (shouldBackfillFromApi(pageInfo, keyword, city, pageNum)) {
            int importSize = Math.max(6, Math.min(pageSize, 20));
            int imported = importFromApi(keyword, city, importSize);
            if (imported > 0) {
                pageInfo = searchFromDatabase(keyword, city, scenicType, pageNum, pageSize);
            }
        }
        return pageInfo;
    }

    public PageInfo<Scenic> filter(String city, String scenicLevel, String scenicType,
                                   String bestSeason, Boolean isFree, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Scenic> list = scenicMapper.findByFilter(city, scenicLevel, scenicType, bestSeason, isFree);
        normalizeCoverImages(list);
        return new PageInfo<>(list);
    }

    public List<Scenic> getTopByRating(int limit) {
        return normalizeCoverImages(scenicMapper.findTopByRating(limit));
    }

    public List<Scenic> getTopByViewCount(int limit) {
        return normalizeCoverImages(scenicMapper.findTopByViewCount(limit));
    }

    public Scenic getDetail(Integer id, Integer userId) {
        Scenic scenic = scenicMapper.findById(id);
        if (scenic != null) {
            scenicMapper.incrementViewCount(id);
            scenic.setViewCount(scenic.getViewCount() + 1);
            if (userId != null) {
                browseHistoryMapper.insert(userId, id);
                scenic.setIsFavorited(favoriteMapper.findByUserAndScenic(userId, id) != null);
            }
        }
        return normalizeCoverImage(scenic);
    }

    public List<Scenic> getRecommendations(Integer userId) {
        if (userId == null) {
            return normalizeCoverImages(scenicMapper.findTopByRating(6));
        }

        List<Integer> recentBrowseIds = browseHistoryMapper.findRecentScenicIdsByUserId(userId, 5);
        List<Integer> recentFavoriteIds = favoriteMapper.findRecentScenicIdsByUserId(userId, 8);

        Set<Integer> relatedIds = new LinkedHashSet<>();
        relatedIds.addAll(recentBrowseIds);
        relatedIds.addAll(recentFavoriteIds);
        if (relatedIds.isEmpty()) {
            return normalizeCoverImages(scenicMapper.findTopByRating(6));
        }

        List<Scenic> relatedScenics = scenicMapper.findByIds(new ArrayList<>(relatedIds));
        Set<String> cities = relatedScenics.stream()
                .map(Scenic::getCity)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> types = relatedScenics.stream()
                .map(Scenic::getScenicType)
                .filter(this::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Scenic> recommendations = scenicMapper.findRecommendedByCitiesOrTypes(
                cities,
                types,
                new ArrayList<>(relatedIds),
                6);

        Set<Integer> excludedIds = recommendations.stream()
                .map(Scenic::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        excludedIds.addAll(relatedIds);

        if (recommendations.size() < 6) {
            recommendations.addAll(scenicMapper.findTopByRatingExcludingIds(
                    new ArrayList<>(excludedIds),
                    6 - recommendations.size()));
        }

        return normalizeCoverImages(recommendations);
    }

    public boolean save(Scenic scenic) {
        normalizeCoverImage(scenic);
        boolean saved = scenicMapper.insert(scenic) > 0;
        if (saved) {
            invalidateCityCache();
        }
        return saved;
    }

    public boolean update(Scenic scenic) {
        normalizeCoverImage(scenic);
        boolean updated = scenicMapper.update(scenic) > 0;
        if (updated) {
            invalidateCityCache();
        }
        return updated;
    }

    public boolean delete(Integer id) {
        boolean deleted = scenicMapper.delete(id) > 0;
        if (deleted) {
            invalidateCityCache();
        }
        return deleted;
    }

    public int countAll() {
        return scenicMapper.countAll();
    }

    public List<String> getAllCities() {
        long now = System.currentTimeMillis();
        if (now < cachedCitiesExpiresAt && !cachedCities.isEmpty()) {
            return cachedCities;
        }

        synchronized (this) {
            now = System.currentTimeMillis();
            if (now < cachedCitiesExpiresAt && !cachedCities.isEmpty()) {
                return cachedCities;
            }

            List<String> cities = scenicMapper.findAllCities();
            cachedCities = cities == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(new ArrayList<>(cities));
            cachedCitiesExpiresAt = now + CITY_CACHE_TTL_MS;
            return cachedCities;
        }
    }

    public int importFromApi(String keyword, String city, Integer num) {
        List<Scenic> apiScenics = baiduMapService.searchScenic(keyword, city, num, 1);
        int importedOrUpdated = 0;
        for (Scenic scenic : apiScenics) {
            Scenic existing = null;
            if (hasText(scenic.getApiId())) {
                existing = scenicMapper.findByApiId(scenic.getApiId());
            }
            if (existing == null) {
                existing = scenicMapper.findByNameAndCity(scenic.getName(), scenic.getCity());
            }
            if (existing != null && !scenicImageResolver.needsBetterImage(existing.getCoverImage())) {
                continue;
            }
            enrichCoverImageForImport(scenic);
            if (existing == null) {
                scenicMapper.insert(scenic);
                importedOrUpdated++;
            } else if (shouldUpdateExistingCover(existing, scenic.getCoverImage())) {
                existing.setCoverImage(scenic.getCoverImage());
                scenicMapper.update(existing);
                importedOrUpdated++;
            }
        }
        if (importedOrUpdated > 0) {
            invalidateCityCache();
        }
        return importedOrUpdated;
    }

    private Scenic normalizeCoverImage(Scenic scenic) {
        if (scenic == null) {
            return null;
        }
        scenic.setCoverImage(scenicImageResolver.resolveCoverImage(
                scenic.getCoverImage(), scenic.getName(), scenic.getScenicType()));
        return scenic;
    }

    private List<Scenic> normalizeCoverImages(List<Scenic> scenics) {
        if (scenics == null) {
            return scenics;
        }
        for (Scenic scenic : scenics) {
            normalizeCoverImage(scenic);
        }
        return scenics;
    }

    private Scenic enrichCoverImageForImport(Scenic scenic) {
        if (scenic == null) {
            return null;
        }
        if (scenicImageResolver.needsBetterImage(scenic.getCoverImage())) {
            String searchedImage = scenicImageSearchService.searchImageUrl(scenic.getName(), scenic.getCity());
            if (hasText(searchedImage)) {
                scenic.setCoverImage(searchedImage);
            }
        }
        return normalizeCoverImage(scenic);
    }

    private boolean shouldUpdateExistingCover(Scenic existing, String newCoverImage) {
        if (existing == null || !hasText(newCoverImage)) {
            return false;
        }
        if (scenicImageResolver.needsBetterImage(newCoverImage)) {
            return false;
        }
        String existingCoverImage = existing.getCoverImage();
        return scenicImageResolver.needsBetterImage(existingCoverImage)
                && !newCoverImage.equals(existingCoverImage);
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private PageInfo<Scenic> searchFromDatabase(String keyword, String city, String scenicType, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Scenic> list = scenicMapper.search(keyword, city, scenicType);
        normalizeCoverImages(list);
        return new PageInfo<>(list);
    }

    private boolean shouldBackfillFromApi(PageInfo<Scenic> pageInfo, String keyword, String city, int pageNum) {
        if (pageNum != 1 || pageInfo == null) {
            return false;
        }
        if (pageInfo.getTotal() > 0) {
            return false;
        }
        return hasText(keyword) || hasText(city);
    }

    private void invalidateCityCache() {
        cachedCities = Collections.emptyList();
        cachedCitiesExpiresAt = 0L;
    }
}
