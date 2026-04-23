package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[merchant_ban_record]")
public class MerchantBanRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer merchantId;
    private String banReason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date banEndTime;
    private String status; // active, expired
}
