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
        List<Review> reviews = reviewService.getUserReviews(userId);
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

    // 删除评价
    @DeleteMapping("/delete")
    public Map<String, Object> deleteReview(@RequestParam Integer reviewId) {
        reviewService.deleteReview(reviewId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }
}