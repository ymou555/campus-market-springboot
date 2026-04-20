package org.example.campusmarket.controller;

import org.example.campusmarket.entity.PointsRecord;
import org.example.campusmarket.service.PointsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
public class PointsController {
    @Autowired
    private PointsService pointsService;

    // 获取用户总积分
    @GetMapping("/total")
    public Map<String, Object> getTotalPoints(@RequestParam Integer userId) {
        int totalPoints = pointsService.getTotalPoints(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("totalPoints", totalPoints);
        return result;
    }

    // 增加积分
    @PostMapping("/add")
    public Map<String, Object> addPoints(
            @RequestParam Integer userId,
            @RequestParam Integer points,
            @RequestParam String remark) {
        pointsService.addPoints(userId, points, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "积分添加成功");
        return result;
    }

    // 扣减积分
    @PostMapping("/deduct")
    public Map<String, Object> deductPoints(
            @RequestParam Integer userId,
            @RequestParam Integer points,
            @RequestParam String remark) {
        pointsService.deductPoints(userId, points, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "积分扣减成功");
        return result;
    }

    // 获取积分流水记录
    @GetMapping("/records")
    public Map<String, Object> getPointsRecords(
            @RequestParam Integer userId,
            @RequestParam(required = false) String type) {
        List<PointsRecord> records = pointsService.getPointsRecords(userId, type);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", records);
        return result;
    }

    // 消费获得积分
    @PostMapping("/earn-by-consumption")
    public Map<String, Object> earnPointsByConsumption(
            @RequestParam Integer userId,
            @RequestParam Double amount) {
        pointsService.earnPointsByConsumption(userId, amount);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "积分获取成功");
        return result;
    }

    // 积分抵扣
    @PostMapping("/deduct-for-payment")
    public Map<String, Object> deductPointsForPayment(
            @RequestParam Integer userId,
            @RequestParam Integer points) {
        double deductionAmount = pointsService.deductPointsForPayment(userId, points);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "积分抵扣成功");
        result.put("deductionAmount", deductionAmount);
        return result;
    }
}