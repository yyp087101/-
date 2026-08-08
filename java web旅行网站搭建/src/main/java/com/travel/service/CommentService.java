package com.travel.service;

import com.travel.entity.Comment;
import com.travel.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    public boolean add(Comment comment) {
        return commentMapper.insert(comment) > 0;
    }

    public boolean delete(Integer id) {
        return commentMapper.deleteById(id) > 0;
    }

    public List<Comment> findByScenicId(Integer scenicId) {
        return commentMapper.findByScenicId(scenicId);
    }

    public List<Comment> findAll() {
        return commentMapper.findAll();
    }

    public int countByScenicId(Integer scenicId) {
        return commentMapper.countByScenicId(scenicId);
    }

    public int countAll() {
        return commentMapper.countAll();
    }

    public Double avgRatingByScenicId(Integer scenicId) {
        return commentMapper.avgRatingByScenicId(scenicId);
    }
}
