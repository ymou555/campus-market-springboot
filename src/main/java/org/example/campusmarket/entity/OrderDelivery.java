package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[order_delivery]")
public class OrderDelivery {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private String deliveryType; // face_to_face, express

    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String trackingNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date meetTime;
    private String meetLocation;
    private String meetStatus; // pending_seller, pending_buyer, confirmed, rejected
    private String meetLastUpdater; // buyer, seller

    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;
}
