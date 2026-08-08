package com.petadoption.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.petadoption.common.Result;
import com.petadoption.entity.Notice;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.service.NoticeService;
import com.petadoption.service.PetService;
import com.petadoption.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private UserService userService;
    @Autowired
    private PetService petService;
    @Autowired
    private NoticeService noticeService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        IPage<Pet> petPage = petService.pageList(1, 8, null, null, 0);
        IPage<Notice> noticePage = noticeService.pageList(1, 5, null);
        model.addAttribute("pets", petPage.getRecords());
        model.addAttribute("notices", noticePage.getRecords());
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/doLogin")
    @ResponseBody
    public Result<User> doLogin(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userService.login(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误，或账号已被禁用");
        }
        user.setPassword(null);
        session.setAttribute("loginUser", user);
        return Result.success("登录成功", user);
    }

    @PostMapping("/doRegister")
    @ResponseBody
    public Result<Void> doRegister(User user) {
        if (userService.register(user)) {
            return Result.success();
        }
        return Result.error("用户名已存在");
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
