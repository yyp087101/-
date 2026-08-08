package com.petadoption.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.petadoption.entity.Feedback;
import com.petadoption.mapper.FeedbackMapper;
import com.petadoption.service.FeedbackService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Override
    public IPage<Feedback> pageList(int pageNum, int pageSize, Long userId) {
        return baseMapper.selectPageWithUser(new Page<>(pageNum, pageSize), userId);
    }

    @Override
    public boolean reply(Long id, String reply, Long replyId) {
        Feedback feedback = getById(id);
        if (feedback == null) {
            return false;
        }
        feedback.setReply(reply);
        feedback.setReplyId(replyId);
        feedback.setReplyTime(LocalDateTime.now());
        return updateById(feedback);
    }
}
