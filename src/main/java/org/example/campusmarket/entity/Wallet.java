package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[wallet]")
public class Wallet {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Double balance;
    private Date lastUpdateTime;
}