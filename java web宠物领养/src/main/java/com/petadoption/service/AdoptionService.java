package com.petadoption.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petadoption.entity.Adoption;

public interface AdoptionService extends IService<Adoption> {
    boolean apply(Adoption adoption);
    boolean review(Long id, Integer status, String reviewComment, Long reviewerId);
    IPage<Adoption> pageList(int pageNum, int pageSize, Long userId, Integer status);
    Adoption getDetail(Long id);
}
