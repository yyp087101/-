package com.petadoption.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petadoption.entity.Adoption;
import com.petadoption.entity.Pet;
import com.petadoption.mapper.AdoptionMapper;
import com.petadoption.mapper.PetMapper;
import com.petadoption.service.AdoptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdoptionServiceImpl extends ServiceImpl<AdoptionMapper, Adoption> implements AdoptionService {

    @Autowired
    private PetMapper petMapper;

    @Override
    public boolean apply(Adoption adoption) {
        LambdaQueryWrapper<Adoption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Adoption::getUserId, adoption.getUserId())
               .eq(Adoption::getPetId, adoption.getPetId())
               .in(Adoption::getStatus, 0, 1);
        if (count(wrapper) > 0) {
            return false;
        }
        Pet pet = petMapper.selectById(adoption.getPetId());
        if (pet == null || pet.getStatus() != 0) {
            return false;
        }
        adoption.setStatus(0);
        return save(adoption);
    }

    @Override
    @Transactional
    public boolean review(Long id, Integer status, String reviewComment, Long reviewerId) {
        Adoption adoption = getById(id);
        if (adoption == null || adoption.getStatus() != 0) {
            return false;
        }
        adoption.setStatus(status);
        adoption.setReviewComment(reviewComment);
        adoption.setReviewerId(reviewerId);
        adoption.setReviewTime(LocalDateTime.now());
        boolean result = updateById(adoption);
        if (result && status == 1) {
            Pet pet = petMapper.selectById(adoption.getPetId());
            pet.setStatus(1);
            petMapper.updateById(pet);
            LambdaQueryWrapper<Adoption> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Adoption::getPetId, adoption.getPetId())
                   .eq(Adoption::getStatus, 0)
                   .ne(Adoption::getId, id);
            Adoption reject = new Adoption();
            reject.setStatus(2);
            reject.setReviewComment("该宠物已被其他申请人领养");
            reject.setReviewerId(reviewerId);
            reject.setReviewTime(LocalDateTime.now());
            update(reject, wrapper);
        }
        return result;
    }

    @Override
    public IPage<Adoption> pageList(int pageNum, int pageSize, Long userId, Integer status) {
        return baseMapper.selectPageWithDetail(new Page<>(pageNum, pageSize), userId, status);
    }

    @Override
    public Adoption getDetail(Long id) {
        return baseMapper.selectDetailById(id);
    }
}
