package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[user_blacklist]")
public class UserBlacklist {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer blacklistedBy;
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
