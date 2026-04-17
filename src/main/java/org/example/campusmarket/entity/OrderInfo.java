package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[order_info]")
public class OrderInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private Integer userId;
    private Integer merchantId;
    private Double totalAmount;
    private Double actualAmount;
    private Integer pointsDeducted;
    private String status; // pending, paid, shipped, received, completed, refunded
    private Date createTime;
    private Date updateTime;
}