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
    private List<OrderProductVO> products;
}
