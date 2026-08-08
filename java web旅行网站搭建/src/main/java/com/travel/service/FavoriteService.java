package com.travel.service;

import com.travel.entity.Favorite;
import com.travel.entity.Scenic;
import com.travel.mapper.FavoriteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private ScenicImageResolver scenicImageResolver;

    public boolean add(Integer userId, Integer scenicId) {
        return favoriteMapper.insert(userId, scenicId) > 0;
    }

    public boolean remove(Integer userId, Integer scenicId) {
        return favoriteMapper.delete(userId, scenicId) > 0;
    }

    public boolean isFavorited(Integer userId, Integer scenicId) {
        return favoriteMapper.findByUserAndScenic(userId, scenicId) != null;
    }

    public boolean toggle(Integer userId, Integer scenicId) {
        if (favoriteMapper.delete(userId, scenicId) > 0) {
            return false;
        }
        return favoriteMapper.insert(userId, scenicId) > 0;
    }

    public List<Favorite> getUserFavorites(Integer userId) {
        List<Favorite> favorites = favoriteMapper.findByUserId(userId);
        if (favorites == null) {
            return favorites;
        }
        for (Favorite favorite : favorites) {
            Scenic scenic = favorite.getScenic();
            if (scenic != null) {
                scenic.setCoverImage(scenicImageResolver.resolveCoverImage(
                        scenic.getCoverImage(), scenic.getName(), scenic.getScenicType()));
            }
        }
        return favorites;
    }

    public int countByUserId(Integer userId) {
        return favoriteMapper.countByUserId(userId);
    }
}
