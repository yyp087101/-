package com.travel.controller;

import com.travel.entity.User;
import com.travel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect, HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        String safeRedirect = sanitizeRedirect(redirect);
        if (user != null) {
            return "redirect:" + (safeRedirect != null ? safeRedirect : "/");
        }
        model.addAttribute("redirect", safeRedirect);
        return "user/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(required = false) String redirect,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        String safeRedirect = sanitizeRedirect(redirect);
        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("user", user);
            return "redirect:" + (safeRedirect != null ? safeRedirect : "/");
        }
        redirectAttributes.addFlashAttribute("error", "用户名或密码错误，或账号已被禁用");
        if (safeRedirect != null) {
            redirectAttributes.addAttribute("redirect", safeRedirect);
        }
        return "redirect:/user/login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "user/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String nickname,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String phone,
                           RedirectAttributes redirectAttributes) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setEmail(email);
        user.setPhone(phone);

        if (userService.register(user)) {
            redirectAttributes.addFlashAttribute("success", "注册成功，请登录");
            return "redirect:/user/login";
        }
        redirectAttributes.addFlashAttribute("error", "用户名已存在");
        return "redirect:/user/register";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/user/login";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        model.addAttribute("userInfo", userService.findById(user.getId()));
        return "user/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String nickname,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phone,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname(nickname);
        updateUser.setEmail(email);
        updateUser.setPhone(phone);

        if (userService.update(updateUser)) {
            User updatedUser = userService.findById(user.getId());
            session.setAttribute("user", updatedUser);
            redirectAttributes.addFlashAttribute("success", "个人信息更新成功");
        } else {
            redirectAttributes.addFlashAttribute("error", "更新失败");
        }
        return "redirect:/user/profile";
    }

    @PostMapping("/password/update")
    public String updatePassword(@RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }
        User dbUser = userService.findById(user.getId());
        if (!dbUser.getPassword().equals(oldPassword)) {
            redirectAttributes.addFlashAttribute("error", "原密码错误");
            return "redirect:/user/profile";
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPassword(newPassword);
        userService.update(updateUser);
        redirectAttributes.addFlashAttribute("success", "密码修改成功");
        return "redirect:/user/profile";
    }

    private String sanitizeRedirect(String redirect) {
        if (redirect == null) {
            return null;
        }
        String trimmed = redirect.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!trimmed.startsWith("/") || trimmed.startsWith("//") || trimmed.contains("://")) {
            return null;
        }
        if (trimmed.contains("\r") || trimmed.contains("\n")) {
            return null;
        }
        if (trimmed.startsWith("/user/login")) {
            return "/";
        }
        return trimmed;
    }
}
