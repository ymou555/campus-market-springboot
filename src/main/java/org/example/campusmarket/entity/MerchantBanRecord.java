package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[merchant_ban_record]")
public class MerchantBanRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer merchantId;
    private String banReason;
    private Date banStartTime;
    private Date banEndTime;
    private String status; // active, expired
}
