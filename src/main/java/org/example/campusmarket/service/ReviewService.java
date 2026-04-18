package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class ReviewService {
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    // 添加评价
    public void addReview(Review review) {
        review.setCreateTime(new Date());
        reviewMapper.insert(review);
    }

    // 获取商品评价列表
    public List<Review> getProductReviews(Integer productId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, productId);
        wrapper.eq(Review::getTargetType, "product");
        wrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        // 设置用户名
        for (Review review : reviews) {
            SysUser user = sysUserMapper.selectById(review.getUserId());
            if (user != null) {
                review.setUsername(user.getUsername());
            }
        }
        
        return reviews;
    }

    // 获取商家评价列表
    public List<Review> getMerchantReviews(Integer merchantId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, merchantId);
        wrapper.eq(Review::getTargetType, "merchant");
        wrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        // 设置用户名
        for (Review review : reviews) {
            SysUser user = sysUserMapper.selectById(review.getUserId());
            if (user != null) {
                review.setUsername(user.getUsername());
            }
        }
        
        return reviews;
    }

    // 获取买家评价列表
    public List<Review> getBuyerReviews(Integer buyerId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, buyerId);
        wrapper.eq(Review::getTargetType, "buyer");
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectList(wrapper);
    }

    // 获取用户的评价列表
    public List<Review> getUserReviews(Integer userId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId);
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectList(wrapper);
    }

    // 获取商品的平均评分
    public double getProductAverageRating(Integer productId) {
        // 先查询是否有评价
        LambdaQueryWrapper<Review> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Review::getTargetId, productId);
        countWrapper.eq(Review::getTargetType, "product");
        Long count = reviewMapper.selectCount(countWrapper);
        
        if (count == null || count == 0) {
            return 0;
        }
        
        // 使用QueryWrapper来支持SQL聚合函数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("target_id", productId);
        queryWrapper.eq("target_type", "product");
        queryWrapper.select("AVG(rating) as avg_rating");
        Object result = reviewMapper.selectObjs(queryWrapper).stream().findFirst().orElse(0);
        return result != null ? Double.parseDouble(result.toString()) : 0;
    }

    // 获取商家的平均评分
    public double getMerchantAverageRating(Integer merchantId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, merchantId);
        wrapper.eq(Review::getTargetType, "merchant");
        // 使用QueryWrapper来支持SQL聚合函数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("target_id", merchantId);
        queryWrapper.eq("target_type", "merchant");
        queryWrapper.select("AVG(rating) as avg_rating");
        Object result = reviewMapper.selectObjs(queryWrapper).stream().findFirst().orElse(0);
        return result != null ? Double.parseDouble(result.toString()) : 0;
    }

    // 删除评价
    public void deleteReview(Integer reviewId) {
        reviewMapper.deleteById(reviewId);
    }
}