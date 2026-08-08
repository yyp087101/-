package com.petadoption.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.petadoption.entity.Feedback;

public interface FeedbackService extends IService<Feedback> {
    IPage<Feedback> pageList(int pageNum, int pageSize, Long userId);
    boolean reply(Long id, String reply, Long replyId);
}
