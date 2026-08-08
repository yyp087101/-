package com.petadoption.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.petadoption.entity.Pet;
import com.petadoption.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pet")
public class PetController {

    @Autowired
    private PetService petService;

    @GetMapping("/list")
    public String list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "8") int pageSize,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String type,
                       Model model) {
        IPage<Pet> page = petService.pageList(pageNum, pageSize, keyword, type, 0);
        model.addAttribute("page", page);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        return "pet/list";
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Pet pet = petService.getById(id);
        model.addAttribute("pet", pet);
        return "pet/detail";
    }
}
