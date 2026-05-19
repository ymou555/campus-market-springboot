package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class ProductWithAuditDTO {
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
    private String status;
    private String newness;
    private Integer salesCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
    private String merchantName;
    private String firstImage;
    private String auditStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date auditTime;
    private String auditRemark;
}