package org.example.campusmarket.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("[product_image]")
public class ProductImage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer productId;
    private String imageUrl;
    private Integer sortOrder;
}