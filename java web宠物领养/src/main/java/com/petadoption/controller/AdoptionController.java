package com.petadoption.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.petadoption.common.Result;
import com.petadoption.entity.Adoption;
import com.petadoption.entity.User;
import com.petadoption.service.AdoptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/adoption")
public class AdoptionController {

    @Autowired
    private AdoptionService adoptionService;

    @GetMapping("/my")
    public String myAdoptions(@RequestParam(defaultValue = "1") int pageNum,
                              @RequestParam(defaultValue = "10") int pageSize,
                              @RequestParam(required = false) Integer status,
                              HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        IPage<Adoption> page = adoptionService.pageList(pageNum, pageSize, loginUser.getId(), status);
        model.addAttribute("page", page);
        model.addAttribute("status", status);
        return "adoption/my";
    }

    @PostMapping("/apply")
    @ResponseBody
    public Result<Void> apply(Adoption adoption, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        adoption.setUserId(loginUser.getId());
        if (adoptionService.apply(adoption)) {
            return Result.success("申请提交成功，请等待审核", null);
        }
        return Result.error("申请失败，您可能已申请过该宠物或该宠物已被领养");
    }

    @PostMapping("/cancel/{id}")
    @ResponseBody
    public Result<Void> cancel(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Adoption adoption = adoptionService.getById(id);
        if (adoption == null || !adoption.getUserId().equals(loginUser.getId()) || adoption.getStatus() != 0) {
            return Result.error("无法取消该申请");
        }
        adoption.setStatus(3);
        if (adoptionService.updateById(adoption)) {
            return Result.success();
        }
        return Result.error("取消失败");
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Adoption adoption = adoptionService.getDetail(id);
        model.addAttribute("adoption", adoption);
        return "adoption/detail";
    }
}
