package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("[category]")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String categoryName;
    private Integer parentId;
    private Integer level;
    private Date createTime;
}