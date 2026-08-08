package com.travel.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class Scenic {
    private Integer id;
    private String name;
    private String city;
    private String province;
    private String content;
    private String coverImage;
    private String address;
    private BigDecimal ticketPrice;
    private BigDecimal rating;
    private String scenicLevel;
    private String scenicType;
    private String bestSeason;
    private String openTime;
    private Integer viewCount;
    private Integer isApi;
    private String apiId;
    private Date createTime;
    private Date updateTime;

    // Non-database fields for view/API rendering.
    private Boolean isFavorited;
    private Integer commentCount;
    private Double latitude;
    private Double longitude;
}
