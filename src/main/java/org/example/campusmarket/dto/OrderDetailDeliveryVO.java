package org.example.campusmarket.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class OrderDetailDeliveryVO {
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String trackingNumber;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date meetTime;
    private String meetLocation;
    private String meetStatus;
    private String sellerPhone;
}
