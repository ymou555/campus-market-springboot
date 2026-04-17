package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;

    // 获取商家信息
    @GetMapping("/info")
    public Map<String, Object> getMerchantInfo(@RequestParam Integer userId) {
        MerchantInfo merchantInfo = merchantService.getMerchantInfo(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("merchantInfo", merchantInfo);
        return result;
    }

    // 更新商家信息
    @PutMapping("/info")
    public Map<String, Object> updateMerchantInfo(@RequestBody MerchantInfo merchantInfo) {
        merchantService.updateMerchantInfo(merchantInfo);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 获取商家等级列表
    @GetMapping("/level/list")
    public Map<String, Object> getMerchantLevelList(
            @RequestParam int page,
            @RequestParam int size) {
        Page<MerchantLevel> levelPage = merchantService.getMerchantLevelList(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", levelPage.getRecords());
        result.put("total", levelPage.getTotal());
        return result;
    }

    // 获取商家等级详情
    @GetMapping("/level/detail")
    public Map<String, Object> getMerchantLevel(@RequestParam Integer id) {
        MerchantLevel level = merchantService.getMerchantLevel(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("level", level);
        return result;
    }

    // 更新商家等级
    @PutMapping("/level/update")
    public Map<String, Object> updateMerchantLevel(@RequestBody MerchantLevel level) {
        merchantService.updateMerchantLevel(level);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 调整商家等级
    @PostMapping("/level/adjust")
    public Map<String, Object> adjustMerchantLevel(
            @RequestParam Integer userId,
            @RequestParam Integer levelId) {
        merchantService.adjustMerchantLevel(userId, levelId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "调整成功");
        return result;
    }

    // 封禁商家
    @PostMapping("/ban")
    public Map<String, Object> banMerchant(
            @RequestParam Integer merchantId,
            @RequestParam String reason,
            @RequestParam String endTime) {
        // 这里可以将endTime字符串转换为Date对象
        merchantService.banMerchant(merchantId, reason, new Date());
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "封禁成功");
        return result;
    }

    // 解除商家封禁
    @PostMapping("/unban")
    public Map<String, Object> unbanMerchant(@RequestParam Integer merchantId) {
        merchantService.unbanMerchant(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "解除封禁成功");
        return result;
    }
}