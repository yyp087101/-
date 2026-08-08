package com.travel.controller;

import com.travel.entity.Scenic;
import com.travel.entity.User;
import com.travel.service.CommentService;
import com.travel.service.ScenicImageStorageService;
import com.travel.service.ScenicService;
import com.travel.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ScenicService scenicService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ScenicImageStorageService scenicImageStorageService;

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User admin = userService.adminLogin(username, password);
        if (admin != null) {
            session.setAttribute("admin", admin);
            return "redirect:/admin/dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "管理员账号或密码错误");
        return "redirect:/admin/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("admin");
        return "redirect:/admin/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("userCount", userService.countAll());
        model.addAttribute("scenicCount", scenicService.countAll());
        model.addAttribute("commentCount", commentService.countAll());
        return "admin/dashboard";
    }

    // ===== 景点管理 =====
    @GetMapping("/scenic/list")
    public String scenicList(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("scenics", scenicService.findAll(1, 100).getList());
        return "admin/scenic-list";
    }

    @GetMapping("/scenic/add")
    public String scenicAddPage(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        if (!model.containsAttribute("scenic")) {
            model.addAttribute("scenic", new Scenic());
        }
        return "admin/scenic-form";
    }

    @GetMapping("/scenic/edit/{id}")
    public String scenicEditPage(@PathVariable Integer id, HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        if (!model.containsAttribute("scenic")) {
            model.addAttribute("scenic", scenicService.findById(id));
        }
        return "admin/scenic-form";
    }

    @PostMapping("/scenic/save")
    public String saveScenic(Scenic scenic,
                             @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        if (!hasText(scenic.getName())) {
            redirectAttributes.addFlashAttribute("error", "景点名称不能为空");
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return redirectToScenicForm(scenic);
        }

        try {
            attachUploadedImage(scenic, imageFile);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return redirectToScenicForm(scenic);
        }

        boolean success;
        if (scenic.getId() != null) {
            success = scenicService.update(scenic);
        } else {
            scenic.setIsApi(0);
            scenic.setApiId(null);
            success = scenicService.save(scenic);
        }

        if (!success) {
            redirectAttributes.addFlashAttribute("error", "保存失败，请稍后重试");
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return redirectToScenicForm(scenic);
        }

        redirectAttributes.addFlashAttribute("success", "保存成功");
        return "redirect:/admin/scenic/list";
    }

    @GetMapping("/scenic/delete/{id}")
    public String deleteScenic(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        scenicService.delete(id);
        redirectAttributes.addFlashAttribute("success", "删除成功");
        return "redirect:/admin/scenic/list";
    }

    @GetMapping("/scenic/import")
    public String importPage(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        if (!model.containsAttribute("scenic")) {
            model.addAttribute("scenic", new Scenic());
        }
        return "admin/scenic-import";
    }

    @PostMapping("/scenic/import")
    public String importScenic(Scenic scenic,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }
        if (!hasText(scenic.getName())) {
            redirectAttributes.addFlashAttribute("error", "请先填写景点名称");
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return "redirect:/admin/scenic/import";
        }

        try {
            attachUploadedImage(scenic, imageFile);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return "redirect:/admin/scenic/import";
        }

        scenic.setId(null);
        scenic.setIsApi(0);
        scenic.setApiId(null);

        boolean saved = scenicService.save(scenic);
        if (!saved) {
            redirectAttributes.addFlashAttribute("error", "景点导入失败，请稍后重试");
            redirectAttributes.addFlashAttribute("scenic", scenic);
            return "redirect:/admin/scenic/import";
        }

        redirectAttributes.addFlashAttribute("success", "景点导入成功");
        return "redirect:/admin/scenic/list";
    }

    // ===== 评论管理 =====
    @GetMapping("/comment/list")
    public String commentList(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("comments", commentService.findAll());
        return "admin/comment-list";
    }

    @GetMapping("/comment/delete/{id}")
    public String deleteComment(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        commentService.delete(id);
        redirectAttributes.addFlashAttribute("success", "评论已删除");
        return "redirect:/admin/comment/list";
    }

    // ===== 用户管理 =====
    @GetMapping("/user/list")
    public String userList(HttpSession session, Model model) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("users", userService.findAll());
        return "admin/user-list";
    }

    @GetMapping("/user/toggle/{id}")
    public String toggleUserStatus(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        User admin = (User) session.getAttribute("admin");
        if (admin == null) {
            return "redirect:/admin/login";
        }

        User user = userService.findById(id);
        if (user != null && user.getRole() != 1) {
            int newStatus = user.getStatus() == 1 ? 0 : 1;
            userService.updateStatus(id, newStatus);
            redirectAttributes.addFlashAttribute("success", newStatus == 1 ? "已启用该账号" : "已禁用该账号");
        }
        return "redirect:/admin/user/list";
    }

    private void attachUploadedImage(Scenic scenic, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            scenic.setCoverImage(scenicImageStorageService.storeScenicImage(imageFile));
        }
    }

    private String redirectToScenicForm(Scenic scenic) {
        if (scenic != null && scenic.getId() != null) {
            return "redirect:/admin/scenic/edit/" + scenic.getId();
        }
        return "redirect:/admin/scenic/add";
    }

    private boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}
