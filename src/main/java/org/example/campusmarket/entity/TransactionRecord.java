package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[transaction_record]")
public class TransactionRecord {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Double amount;
    private String type; // deposit, withdraw, payment, refund
    private String status; // success, failed, pending
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date transactionTime;
    private String remark;
}