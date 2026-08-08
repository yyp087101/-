package com.petadoption.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.petadoption.common.Result;
import com.petadoption.entity.Feedback;
import com.petadoption.entity.User;
import com.petadoption.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/feedback")
public class FeedbackController {

    @Autowired
    private FeedbackService feedbackService;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        Long userId = (loginUser != null && loginUser.getRole() == 0) ? loginUser.getId() : null;
        IPage<Feedback> page = feedbackService.pageList(pageNum, pageSize, userId);
        model.addAttribute("page", page);
        return "feedback/list";
    }

    @PostMapping("/add")
    @ResponseBody
    public Result<Void> add(Feedback feedback, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        feedback.setUserId(loginUser.getId());
        if (feedbackService.save(feedback)) {
            return Result.success("留言成功", null);
        }
        return Result.error("留言失败");
    }
}
