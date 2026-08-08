package com.petadoption.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("adoption")
public class Adoption {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    private String reason;
    private String address;
    private String experience;
    private Integer status;
    private String reviewComment;
    private Long reviewerId;
    private LocalDateTime reviewTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private User user;
    @TableField(exist = false)
    private Pet pet;
    @TableField(exist = false)
    private User reviewer;
}
