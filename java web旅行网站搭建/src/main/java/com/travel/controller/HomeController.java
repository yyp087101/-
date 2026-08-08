package com.travel.controller;

import com.travel.entity.Scenic;
import com.travel.entity.User;
import com.travel.service.ScenicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private ScenicService scenicService;

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/user/login";
        }

        List<Scenic> hotScenics = scenicService.getTopByRating(10);
        model.addAttribute("hotScenics", hotScenics);

        List<Scenic> popularScenics = scenicService.getTopByViewCount(6);
        model.addAttribute("popularScenics", popularScenics);

        List<Scenic> recommendations = scenicService.getRecommendations(user.getId());
        model.addAttribute("recommendations", recommendations);

        return "index";
    }
}
