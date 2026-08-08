package com.travel.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Favorite {
    private Integer id;
    private Integer userId;
    private Integer scenicId;
    private Date createTime;
    
    // 关联字段
    private Scenic scenic;
}
