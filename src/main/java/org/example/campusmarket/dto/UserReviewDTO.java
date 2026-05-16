package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class UserReviewDTO {
    private Integer id;
    private String reviewType;
    private Integer orderId;
    private String orderNo;
    
    // 商品评价字段
    private String productName;
    private String productImage;
    private String merchant;
    private Double price;
    private Integer quantity;
    
    // 商家评价字段
    private Integer merchantId;
    private String merchantName;
    private String merchantRealName;
    
    // 通用字段
    private Integer rating;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
