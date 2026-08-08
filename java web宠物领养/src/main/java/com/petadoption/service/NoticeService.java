package com.petadoption.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petadoption.entity.Notice;

public interface NoticeService extends IService<Notice> {
    IPage<Notice> pageList(int pageNum, int pageSize, String keyword);
}
