package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[order_status_log]")
public class OrderStatusLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private String oldStatus;
    private String newStatus;
    private String operator;
    private Date operateTime;
}