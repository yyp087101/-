package com.travel.entity;

import lombok.Data;
import java.util.Date;

@Data
public class BrowseHistory {
    private Integer id;
    private Integer userId;
    private Integer scenicId;
    private Date browseTime;
    
    // 关联字段
    private Scenic scenic;
}
