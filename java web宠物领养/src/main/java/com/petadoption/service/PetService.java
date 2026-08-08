package com.petadoption.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petadoption.entity.Pet;

public interface PetService extends IService<Pet> {
    IPage<Pet> pageList(int pageNum, int pageSize, String keyword, String type, Integer status);
}
