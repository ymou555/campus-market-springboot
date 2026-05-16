package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PendingMerchantReviewDTO {
    private Integer id;
    private Integer orderId;
    private String orderNo;
    private Integer merchantId;
    private String merchantName;
    private String merchantRealName;
    private String productImage;
    private List<String> productNames;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
