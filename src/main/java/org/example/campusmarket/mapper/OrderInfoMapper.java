package org.example.campusmarket.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.campusmarket.entity.OrderInfo;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}