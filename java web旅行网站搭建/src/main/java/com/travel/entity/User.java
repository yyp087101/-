package com.travel.entity;

import lombok.Data;
import java.util.Date;

@Data
public class User {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer role; // 0普通用户 1管理员
    private Integer status; // 0禁用 1正常
    private Date createTime;
    private Date updateTime;
}
