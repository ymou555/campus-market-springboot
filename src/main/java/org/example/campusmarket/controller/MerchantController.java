package org.example.campusmarket.controller;

import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.entity.UserBlacklist;
import org.example.campusmarket.service.BlacklistService;
import org.example.campusmarket.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private BlacklistService blacklistService;

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
    public Map<String, Object> getMerchantLevelList() {
        List<MerchantLevel> levelList = merchantService.getMerchantLevelList();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", levelList);
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
    
    // 根据商品ID获取商家统计信息
    @GetMapping("/stats")
    public Map<String, Object> getMerchantStatsByProduct(@RequestParam Integer productId) {
        Map<String, Object> stats = merchantService.getMerchantStatsByProduct(productId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", stats);
        return result;
    }
    
    // 根据商家ID获取商家统计信息
    @GetMapping("/stats/by-user")
    public Map<String, Object> getMerchantStatsByUser(@RequestParam Integer userId) {
        Map<String, Object> stats = merchantService.getMerchantStatsWithLevel(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", stats);
        return result;
    }
    
    // 商家拉黑买家
    @PostMapping("/blacklist/add")
    public Map<String, Object> blacklistUser(
            @RequestParam Integer merchantId,
            @RequestParam Integer userId,
            @RequestParam String reason) {
        blacklistService.blacklistUser(userId, merchantId, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "拉黑成功");
        return result;
    }
    
    // 商家移除拉黑
    @PostMapping("/blacklist/remove")
    public Map<String, Object> removeFromBlacklist(
            @RequestParam Integer merchantId,
            @RequestParam Integer userId) {
        blacklistService.removeFromBlacklist(userId, merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "移除拉黑成功");
        return result;
    }
    
    // 获取商家的黑名单列表
    @GetMapping("/blacklist/list")
    public Map<String, Object> getBlacklist(@RequestParam Integer merchantId) {
        List<UserBlacklist> blacklist = blacklistService.getMerchantBlacklist(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", blacklist);
        return result;
    }
    
    // 检查用户是否被商家拉黑
    @GetMapping("/blacklist/check")
    public Map<String, Object> checkBlacklist(
            @RequestParam Integer merchantId,
            @RequestParam Integer userId) {
        boolean isBlacklisted = blacklistService.isUserBlacklisted(userId, merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "查询成功");
        result.put("isBlacklisted", isBlacklisted);
        return result;
    }
}