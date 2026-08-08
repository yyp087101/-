package com.travel.mapper;

import com.travel.entity.Comment;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface CommentMapper {
    int insert(Comment comment);
    int deleteById(@Param("id") Integer id);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    List<Comment> findByScenicId(@Param("scenicId") Integer scenicId);
    List<Comment> findAll();
    int countByScenicId(@Param("scenicId") Integer scenicId);
    int countAll();
    Double avgRatingByScenicId(@Param("scenicId") Integer scenicId);
}
