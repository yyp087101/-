package com.petadoption.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petadoption.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    boolean register(User user);
    IPage<User> pageList(int pageNum, int pageSize, String keyword);
}
