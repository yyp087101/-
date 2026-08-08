package com.petadoption.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.petadoption.entity.Adoption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdoptionMapper extends BaseMapper<Adoption> {
    IPage<Adoption> selectPageWithDetail(Page<Adoption> page, @Param("userId") Long userId, @Param("status") Integer status);
    Adoption selectDetailById(@Param("id") Long id);
}
