package com.petadoption.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petadoption.entity.Notice;
import com.petadoption.mapper.NoticeMapper;
import com.petadoption.service.NoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public IPage<Notice> pageList(int pageNum, int pageSize, String keyword) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Notice::getTitle, keyword);
        }
        wrapper.orderByDesc(Notice::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }
}
