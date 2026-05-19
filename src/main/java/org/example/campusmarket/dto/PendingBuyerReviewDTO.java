package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PendingBuyerReviewDTO {
    private Integer id;
    private Integer orderId;
    private String orderNo;
    private Integer buyerId;
    private String buyerName;
    private String buyerPhone;
    private String productImage;
    private List<String> productNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
