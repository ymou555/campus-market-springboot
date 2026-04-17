package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[merchant_info]")
public class MerchantInfo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String businessLicense;
    private String idCardPhoto;
    private Integer levelId;
    private String shopName; // 店铺名称
    private Date createTime;
    private Date updateTime;
}