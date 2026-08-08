package com.travel.service;

import com.travel.entity.User;
import com.travel.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            if (user.getStatus() == 0) {
                return null; // 账号已禁用
            }
            return user;
        }
        return null;
    }

    public User adminLogin(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user != null && user.getPassword().equals(password) && user.getRole() == 1) {
            return user;
        }
        return null;
    }

    public boolean register(User user) {
        User existing = userMapper.findByUsername(user.getUsername());
        if (existing != null) {
            return false;
        }
        user.setRole(0);
        user.setStatus(1);
        return userMapper.insert(user) > 0;
    }

    public User findById(Integer id) {
        return userMapper.findById(id);
    }

    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }

    public boolean update(User user) {
        return userMapper.update(user) > 0;
    }

    public boolean updateStatus(Integer id, Integer status) {
        return userMapper.updateStatus(id, status) > 0;
    }

    public List<User> findAll() {
        return userMapper.findAll();
    }

    public int countAll() {
        return userMapper.countAll();
    }
}
