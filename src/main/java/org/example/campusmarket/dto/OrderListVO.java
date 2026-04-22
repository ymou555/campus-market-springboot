package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OrderListVO {
    private Integer id;
    private String orderNo;
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    private Double totalAmount;
    private Double actualAmount;
    private Integer totalQuantity;
    private String deliveryType;
    private List<OrderProductVO> products;
    private ReturnRequestVO returnRequest;

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
