package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public Map<String, Object> getProductReviews(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer productId) {
        Page<Review> reviewPage = reviewService.getProductReviews(page, size, productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviewPage.getRecords());
        result.put("total", reviewPage.getTotal());
        return result;
    }

    // 获取商家评价列表
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantReviews(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer merchantId) {
        Page<Review> reviewPage = reviewService.getMerchantReviews(page, size, merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviewPage.getRecords());
        result.put("total", reviewPage.getTotal());
        return result;
    }

    // 获取买家评价列表
    @GetMapping("/buyer/list")
    public Map<String, Object> getBuyerReviews(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer buyerId) {
        Page<Review> reviewPage = reviewService.getBuyerReviews(page, size, buyerId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviewPage.getRecords());
        result.put("total", reviewPage.getTotal());
        return result;
    }

    // 获取用户的评价列表
    @GetMapping("/user/list")
    public Map<String, Object> getUserReviews(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer userId) {
        Page<Review> reviewPage = reviewService.getUserReviews(page, size, userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", reviewPage.getRecords());
        result.put("total", reviewPage.getTotal());
        return result;
    }

    // 获取商品的平均评分
    @GetMapping("/product/rating")
    public Map<String, Object> getProductAverageRating(@RequestParam Integer productId) {
        double rating = reviewService.getProductAverageRating(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
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