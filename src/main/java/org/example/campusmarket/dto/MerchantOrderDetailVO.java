package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MerchantOrderDetailVO {
    private Integer id;
    private String orderNo;
    private String status;
    private String deliveryType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date payTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date deliveryTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date receiveTime;
    private Integer buyerId;
    private String buyerName;
    private String buyerPhone;
    private Double buyerOfferPrice;
    private MerchantOrderDetailDeliveryVO delivery;
    private List<OrderDetailProductVO> products;
    private Double productTotal;
    private Double totalAmount;
    private Double actualAmount;
    private ReturnRequestVO returnRequest;
    private Boolean merchantReviewed;

    @Data
    public static class ReturnRequestVO {
        private Integer id;
        private String returnReason;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date requestTime;
        private String status;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date auditTime;
        private String auditRemark;
        private Double refundAmount;
    }
}
