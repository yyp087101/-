package com.travel.mapper;

import com.travel.entity.Scenic;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

public interface ScenicMapper {
    Scenic findById(@Param("id") Integer id);
    List<Scenic> findAll();
    List<Scenic> search(@Param("keyword") String keyword, @Param("city") String city, @Param("scenicType") String scenicType);
    List<Scenic> findByFilter(@Param("city") String city, @Param("scenicLevel") String scenicLevel, 
                               @Param("scenicType") String scenicType, @Param("bestSeason") String bestSeason,
                               @Param("isFree") Boolean isFree);
    List<Scenic> findTopByRating(@Param("limit") Integer limit);
    List<Scenic> findTopByViewCount(@Param("limit") Integer limit);
    List<Scenic> findByCity(@Param("city") String city, @Param("excludeId") Integer excludeId);
    List<Scenic> findByType(@Param("scenicType") String scenicType, @Param("excludeId") Integer excludeId);
    List<Scenic> findByIds(@Param("ids") List<Integer> ids);
    int insert(Scenic scenic);
    int update(Scenic scenic);
    int delete(@Param("id") Integer id);
    int incrementViewCount(@Param("id") Integer id);
    int countAll();
    Scenic findByApiId(@Param("apiId") String apiId);
    Scenic findByNameAndCity(@Param("name") String name, @Param("city") String city);
    List<String> findAllCities();
    List<Scenic> findRecommendedByCitiesOrTypes(@Param("cities") Set<String> cities,
                                                @Param("types") Set<String> types,
                                                @Param("excludeIds") List<Integer> excludeIds,
                                                @Param("limit") Integer limit);
    List<Scenic> findTopByRatingExcludingIds(@Param("excludeIds") List<Integer> excludeIds,
                                             @Param("limit") Integer limit);
}
