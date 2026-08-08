package com.travel.mapper;

import com.travel.entity.BrowseHistory;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface BrowseHistoryMapper {
    int insert(@Param("userId") Integer userId, @Param("scenicId") Integer scenicId);
    List<BrowseHistory> findByUserId(@Param("userId") Integer userId);
    List<Integer> findRecentScenicIdsByUserId(@Param("userId") Integer userId, @Param("limit") Integer limit);
}
