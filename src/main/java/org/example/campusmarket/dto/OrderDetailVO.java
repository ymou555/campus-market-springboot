package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class OrderDetailVO {
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
    private Double buyerOfferPrice;
    private OrderDetailDeliveryVO delivery;
    private List<OrderDetailProductVO> products;
    private Double productTotal;
    private Double pointsDiscount;
    private Double totalAmount;
}
