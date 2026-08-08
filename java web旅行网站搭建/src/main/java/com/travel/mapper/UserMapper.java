package com.travel.mapper;

import com.travel.entity.User;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Integer id);
    int insert(User user);
    int update(User user);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    List<User> findAll();
    int countAll();
}
