package com.travel.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Comment {
    private Integer id;
    private Integer userId;
    private Integer scenicId;
    private String content;
    private Integer rating;
    private Integer status;
    private Date createTime;
    
    // 关联字段
    private User user;
    private Scenic scenic;
}
