package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[review]")
public class Review {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer orderId;
    private Integer userId;
    private Integer targetId;
    private String targetType; // product, merchant, buyer
    private Integer rating; // 1-5 stars
    private String content;
    private Date createTime;
}