package org.example.campusmarket.controller;

import org.example.campusmarket.entity.Review;
import org.example.campusmarket.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    @Autowired
    private ReviewService reviewService;

    // 添加评价
    @PostMapping("/add")
    public Map<String, Object> addReview(@RequestBody Review review) {
        reviewService.addReview(review);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "评价成功");
        return result;
    }

    // 获取商品评价列表
    @GetMapping("/product/list")
    public Map<String, Object> getProductReviews(@RequestParam Integer productId) {
        List<Review> reviews = reviewService.getProductReviews(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviews);
        return result;
    }

    // 获取商家评价列表
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantReviews(@RequestParam Integer merchantId) {
        List<Review> reviews = reviewService.getMerchantReviews(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviews);
        return result;
    }

    // 获取买家评价列表
    @GetMapping("/buyer/list")
    public Map<String, Object> getBuyerReviews(@RequestParam Integer buyerId) {
        List<Review> reviews = reviewService.getBuyerReviews(buyerId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviews);
        return result;
    }

    // 获取用户的评价列表
    @GetMapping("/user/list")
    public Map<String, Object> getUserReviews(@RequestParam Integer userId) {
        List<org.example.campusmarket.dto.UserReviewDTO> reviews = reviewService.getUserReviews(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviews);
        return result;
    }

    // 获取商品的平均评分
    @GetMapping("/product/rating")
    public Map<String, Object> getProductAverageRating(@RequestParam Integer productId) {
        double rating = reviewService.getProductAverageRating(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        
        if (rating == 0) {
            result.put("message", "暂无评分");
        } else {
            result.put("message", "获取成功");
        }
        
        result.put("rating", rating);
        return result;
    }

    // 获取商家的平均评分
    @GetMapping("/merchant/rating")
    public Map<String, Object> getMerchantAverageRating(@RequestParam Integer merchantId) {
        double rating = reviewService.getMerchantAverageRating(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("rating", rating);
        return result;
    }
    
    // 获取买家（用户）的平均评分（商家对买家的评价）
    @GetMapping("/buyer/rating")
    public Map<String, Object> getBuyerAverageRating(@RequestParam Integer buyerId) {
        double rating = reviewService.getBuyerAverageRating(buyerId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        if (rating == 0) {
            result.put("message", "暂无评分");
        } else {
            result.put("message", "获取成功");
        }
        result.put("rating", rating);
        return result;
    }
    
    // 获取用户待评价的商品列表
    @GetMapping("/pending/products")
    public Map<String, Object> getPendingProductReviews(@RequestParam Integer userId) {
        List<org.example.campusmarket.dto.PendingProductReviewDTO> products = reviewService.getPendingProductReviews(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", products);
        return result;
    }
    
    // 获取用户待评价的商家列表
    @GetMapping("/pending/merchants")
    public Map<String, Object> getPendingMerchantReviews(@RequestParam Integer userId) {
        List<org.example.campusmarket.dto.PendingMerchantReviewDTO> merchants = reviewService.getPendingMerchantReviews(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", merchants);
        return result;
    }
    
    // 获取商家待评价的买家列表
    @GetMapping("/pending/buyers")
    public Map<String, Object> getPendingBuyerReviews(@RequestParam Integer merchantId) {
        List<org.example.campusmarket.dto.PendingBuyerReviewDTO> buyers = reviewService.getPendingBuyerReviews(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", buyers);
        return result;
    }
    
    // 获取商家已评价的买家列表
    @GetMapping("/reviewed/buyers")
    public Map<String, Object> getReviewedBuyers(@RequestParam Integer merchantId) {
        List<org.example.campusmarket.dto.ReviewedBuyerDTO> buyers = reviewService.getReviewedBuyers(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", buyers);
        return result;
    }
}