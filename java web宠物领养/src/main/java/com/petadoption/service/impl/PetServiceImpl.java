package com.petadoption.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petadoption.entity.Pet;
import com.petadoption.mapper.PetMapper;
import com.petadoption.service.PetService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PetServiceImpl extends ServiceImpl<PetMapper, Pet> implements PetService {

    @Override
    public IPage<Pet> pageList(int pageNum, int pageSize, String keyword, String type, Integer status) {
        LambdaQueryWrapper<Pet> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Pet::getName, keyword).or().like(Pet::getBreed, keyword));
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(Pet::getType, type);
        }
        if (status != null) {
            wrapper.eq(Pet::getStatus, status);
        }
        wrapper.orderByDesc(Pet::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
