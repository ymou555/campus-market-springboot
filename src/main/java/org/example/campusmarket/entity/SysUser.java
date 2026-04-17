package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[sys_user]")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String city;
    private String gender;
    private String bankAccount;
    private String role; // user, merchant, admin
    private String status; // pending, active, blocked
    private Date createTime;
    private Date updateTime;
}