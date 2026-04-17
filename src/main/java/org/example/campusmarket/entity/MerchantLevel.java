package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("[merchant_level]")
public class MerchantLevel {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String levelName;
    private Double rate;
    private Double minAmount;
}