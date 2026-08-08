package com.petadoption.controller;

import com.petadoption.common.Result;
import com.petadoption.entity.User;
import com.petadoption.service.UserService;
import com.petadoption.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    public String profilePage() {
        return "user/profile";
    }

    @PostMapping("/update")
    @ResponseBody
    public Result<Void> updateProfile(User user, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        user.setId(loginUser.getId());
        user.setPassword(null);
        user.setRole(null);
        user.setStatus(null);
        if (userService.updateById(user)) {
            User updated = userService.getById(loginUser.getId());
            updated.setPassword(null);
            session.setAttribute("loginUser", updated);
            return Result.success();
        }
        return Result.error("更新失败");
    }

    @PostMapping("/changePassword")
    @ResponseBody
    public Result<Void> changePassword(@RequestParam String oldPassword, @RequestParam String newPassword, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        User dbUser = userService.getById(loginUser.getId());
        if (!dbUser.getPassword().equals(MD5Util.md5(oldPassword))) {
            return Result.error("原密码错误");
        }
        User update = new User();
        update.setId(loginUser.getId());
        update.setPassword(MD5Util.md5(newPassword));
        if (userService.updateById(update)) {
            return Result.success();
        }
        return Result.error("修改失败");
    }
}
