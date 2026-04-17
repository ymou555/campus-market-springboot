package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[user_audit]")
public class UserAudit {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String auditStatus; // pending, approved, rejected
    private Date auditTime;
    private String auditRemark;
}