package com.travel.mapper;

import com.travel.entity.Favorite;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface FavoriteMapper {
    int insert(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);
    int delete(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);
    Favorite findByUserAndScenic(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);
    List<Favorite> findByUserId(@Param("userId") Integer userId);
    int countByUserId(@Param("userId") Integer userId);
    List<Integer> findScenicIdsByUserId(@Param("userId") Integer userId);
    List<Integer> findRecentScenicIdsByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);
}
