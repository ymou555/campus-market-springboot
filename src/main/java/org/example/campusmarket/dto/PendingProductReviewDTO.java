package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PendingProductReviewDTO {
    private Integer id;
    private Integer orderId;
    private String orderNo;
    private Integer productId;
    private String productName;
    private String productImage;
    private String merchant;
    private Integer merchantId;
    private Double price;
    private Integer quantity;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
