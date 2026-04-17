package org.example.campusmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.campusmarket.entity.Product;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}