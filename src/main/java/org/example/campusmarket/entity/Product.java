package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[product]")
public class Product {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer merchantId;
    private Integer categoryId;
    private String productName;
    private Double originalPrice;
    private Double discountPrice;
    private String size;
    private String description;
    private Boolean isNegotiable;
    private Integer stock;
    private String status; // pending, published, offline, sold_out
    private String newness; // new, like_new, good, fair
    private Integer salesCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String merchantName;
    
    @TableField(exist = false)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String firstImage;
}