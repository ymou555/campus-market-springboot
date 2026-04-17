package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.mapper.ReviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ReviewService {
    @Autowired
    private ReviewMapper reviewMapper;

    // 添加评价
    public void addReview(Review review) {
        review.setCreateTime(new Date());
        reviewMapper.insert(review);
    }

    // 获取商品评价列表
    public Page<Review> getProductReviews(int page, int size, Integer productId) {
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, productId);
        wrapper.eq(Review::getTargetType, "product");
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectPage(reviewPage, wrapper);
    }

    // 获取商家评价列表
    public Page<Review> getMerchantReviews(int page, int size, Integer merchantId) {
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, merchantId);
        wrapper.eq(Review::getTargetType, "merchant");
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectPage(reviewPage, wrapper);
    }

    // 获取买家评价列表
    public Page<Review> getBuyerReviews(int page, int size, Integer buyerId) {
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, buyerId);
        wrapper.eq(Review::getTargetType, "buyer");
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectPage(reviewPage, wrapper);
    }

    // 获取用户的评价列表
    public Page<Review> getUserReviews(int page, int size, Integer userId) {
        Page<Review> reviewPage = new Page<>(page, size);
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId);
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectPage(reviewPage, wrapper);
    }

    // 获取商品的平均评分
    public double getProductAverageRating(Integer productId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, productId);
        wrapper.eq(Review::getTargetType, "product");
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