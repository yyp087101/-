package com.petadoption.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.petadoption.common.Result;
import com.petadoption.entity.*;
import com.petadoption.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;
    @Autowired
    private PetService petService;
    @Autowired
    private AdoptionService adoptionService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private FeedbackService feedbackService;

    @GetMapping({"", "/index"})
    public String index(Model model) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userService.count(new LambdaQueryWrapper<User>().eq(User::getRole, 0)));
        stats.put("petCount", petService.count());
        stats.put("adoptionCount", adoptionService.count());
        stats.put("pendingCount", adoptionService.count(new LambdaQueryWrapper<Adoption>().eq(Adoption::getStatus, 0)));
        stats.put("adoptedCount", petService.count(new LambdaQueryWrapper<Pet>().eq(Pet::getStatus, 1)));
        stats.put("noticeCount", noticeService.count());
        stats.put("feedbackCount", feedbackService.count());
        model.addAttribute("stats", stats);
        return "admin/index";
    }

    // ========== 用户管理 ==========
    @GetMapping("/user/list")
    public String userList(@RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String keyword,
                           Model model) {
        IPage<User> page = userService.pageList(pageNum, pageSize, keyword);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        return "admin/user-list";
    }

    @PostMapping("/user/status")
    @ResponseBody
    public Result<Void> toggleUserStatus(@RequestParam Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        if (userService.updateById(user)) {
            return Result.success();
        }
        return Result.error("操作失败");
    }

    @PostMapping("/user/delete/{id}")
    @ResponseBody
    public Result<Void> deleteUser(@PathVariable Long id) {
        if (userService.removeById(id)) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @PostMapping("/user/resetPassword/{id}")
    @ResponseBody
    public Result<Void> resetPassword(@PathVariable Long id) {
        User user = new User();
        user.setId(id);
        user.setPassword(com.petadoption.util.MD5Util.md5("123456"));
        if (userService.updateById(user)) {
            return Result.success("密码已重置为123456", null);
        }
        return Result.error("重置失败");
    }

    // ========== 宠物管理 ==========
    @GetMapping("/pet/list")
    public String petList(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String type,
                          @RequestParam(required = false) Integer status,
                          Model model) {
        IPage<Pet> page = petService.pageList(pageNum, pageSize, keyword, type, status);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        model.addAttribute("status", status);
        return "admin/pet-list";
    }

    @GetMapping("/pet/add")
    public String petAddPage() {
        return "admin/pet-edit";
    }

    @GetMapping("/pet/edit/{id}")
    public String petEditPage(@PathVariable Long id, Model model) {
        model.addAttribute("pet", petService.getById(id));
        return "admin/pet-edit";
    }

    @PostMapping("/pet/save")
    @ResponseBody
    public Result<Void> savePet(Pet pet) {
        if (pet.getId() == null) {
            pet.setStatus(0);
            if (petService.save(pet)) return Result.success();
        } else {
            if (petService.updateById(pet)) return Result.success();
        }
        return Result.error("操作失败");
    }

    @PostMapping("/pet/delete/{id}")
    @ResponseBody
    public Result<Void> deletePet(@PathVariable Long id) {
        if (petService.removeById(id)) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    @PostMapping("/pet/status")
    @ResponseBody
    public Result<Void> updatePetStatus(@RequestParam Long id, @RequestParam Integer status) {
        Pet pet = new Pet();
        pet.setId(id);
        pet.setStatus(status);
        if (petService.updateById(pet)) {
            return Result.success();
        }
        return Result.error("操作失败");
    }

    // ========== 领养审核 ==========
    @GetMapping("/adoption/list")
    public String adoptionList(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) Integer status,
                               Model model) {
        IPage<Adoption> page = adoptionService.pageList(pageNum, pageSize, null, status);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        return "admin/adoption-list";
    }

    @GetMapping("/adoption/detail/{id}")
    public String adoptionDetail(@PathVariable Long id, Model model) {
        model.addAttribute("adoption", adoptionService.getDetail(id));
        return "admin/adoption-detail";
    }

    @PostMapping("/adoption/review")
    @ResponseBody
    public Result<Void> reviewAdoption(@RequestParam Long id, @RequestParam Integer status,
                                        @RequestParam(required = false) String reviewComment,
                                        HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (adoptionService.review(id, status, reviewComment, loginUser.getId())) {
            return Result.success();
        }
        return Result.error("审核失败");
    }

    // ========== 公告管理 ==========
    @GetMapping("/notice/list")
    public String noticeList(@RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestParam(required = false) String keyword,
                             Model model) {
        IPage<Notice> page = noticeService.pageList(pageNum, pageSize, keyword);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        return "admin/notice-list";
    }

    @GetMapping("/notice/add")
    public String noticeAddPage() {
        return "admin/notice-edit";
    }

    @GetMapping("/notice/edit/{id}")
    public String noticeEditPage(@PathVariable Long id, Model model) {
        model.addAttribute("notice", noticeService.getById(id));
        return "admin/notice-edit";
    }

    @PostMapping("/notice/save")
    @ResponseBody
    public Result<Void> saveNotice(Notice notice, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (notice.getId() == null) {
            notice.setPublisherId(loginUser.getId());
            notice.setStatus(1);
            if (noticeService.save(notice)) return Result.success();
        } else {
            if (noticeService.updateById(notice)) return Result.success();
        }
        return Result.error("操作失败");
    }

    @PostMapping("/notice/delete/{id}")
    @ResponseBody
    public Result<Void> deleteNotice(@PathVariable Long id) {
        if (noticeService.removeById(id)) {
            return Result.success();
        }
        return Result.error("删除失败");
    }

    // ========== 反馈管理 ==========
    @GetMapping("/feedback/list")
    public String feedbackList(@RequestParam(defaultValue = "1") int pageNum,
                               @RequestParam(defaultValue = "10") int pageSize,
                               Model model) {
        IPage<Feedback> page = feedbackService.pageList(pageNum, pageSize, null);
        model.addAttribute("page", page);
        return "admin/feedback-list";
    }

    @PostMapping("/feedback/reply")
    @ResponseBody
    public Result<Void> replyFeedback(@RequestParam Long id, @RequestParam String reply, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (feedbackService.reply(id, reply, loginUser.getId())) {
            return Result.success();
        }
        return Result.error("回复失败");
    }

    @PostMapping("/feedback/delete/{id}")
    @ResponseBody
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        if (feedbackService.removeById(id)) {
            return Result.success();
        }
        return Result.error("删除失败");
    }
}
