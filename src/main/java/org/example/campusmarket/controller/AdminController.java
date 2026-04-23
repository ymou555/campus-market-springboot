package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.UserWithBlacklistDTO;
import org.example.campusmarket.entity.*;
import org.example.campusmarket.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private UserService userService;
    @Autowired
    private ProductService productService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private WalletService walletService;
    @Autowired
    private BlacklistService blacklistService;

    // 用户审核
    @PostMapping("/user/audit")
    public Map<String, Object> auditUser(
            @RequestParam Integer userId,
            @RequestParam String auditStatus,
            @RequestParam(required = false) String remark) {
        userService.auditUser(userId, auditStatus, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "审核成功");
        return result;
    }

    // 商品审核
    @PostMapping("/product/audit")
    public Map<String, Object> auditProduct(
            @RequestParam Integer productId,
            @RequestParam String auditStatus,
            @RequestParam String remark) {
        productService.auditProduct(productId, auditStatus, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "审核成功");
        return result;
    }

    // 获取待审核用户列表
    @GetMapping("/user/pending")
    public Map<String, Object> getPendingUsers(
            @RequestParam int page,
            @RequestParam int size) {
        Page<SysUser> userPage = userService.getUserList(page, size, null, "pending");
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", userPage.getRecords());
        result.put("total", userPage.getTotal());
        return result;
    }
    
    // 获取用户列表（支持筛选和搜索，带拉黑类型）
    @GetMapping("/user/list")
    public Map<String, Object> getUserList(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username) {
        List<UserWithBlacklistDTO> users = userService.getUserListWithBlacklistType(role, status, username);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", users);
        result.put("total", users.size());
        return result;
    }

    // 获取待审核商品列表
    @GetMapping("/product/pending")
    public Map<String, Object> getPendingProducts(
            @RequestParam int page,
            @RequestParam int size) {
        // 这里需要在ProductService中添加相应的方法
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        return result;
    }

    // 封禁商家（限时封禁）
    @PostMapping("/merchant/ban")
    public Map<String, Object> banMerchant(
            @RequestParam Integer merchantId,
            @RequestParam String reason,
            @RequestParam String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date banEndTime = sdf.parse(endTime);
            merchantService.banMerchant(merchantId, reason, banEndTime);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 200);
            result.put("message", "封禁成功");
            return result;
        } catch (ParseException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
            return result;
        }
    }

    // 解除商家封禁
    @PostMapping("/merchant/unban")
    public Map<String, Object> unbanMerchant(@RequestParam Integer merchantId) {
        merchantService.unbanMerchant(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "解除封禁成功");
        return result;
    }

    // 调整商家等级
    @PostMapping("/merchant/level/adjust")
    public Map<String, Object> adjustMerchantLevel(
            @RequestParam Integer userId,
            @RequestParam Integer levelId) {
        merchantService.adjustMerchantLevel(userId, levelId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "调整成功");
        return result;
    }

    // 获取系统数据统计
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        // 这里可以实现各种数据统计功能
        Map<String, Object> statistics = new HashMap<>();
        // 例如：用户数量、商家数量、商品数量、订单数量、总销售额等
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", statistics);
        return result;
    }

    // 商品下架惩罚（违规商家全部商品下架）
    @PostMapping("/merchant/products/offline")
    public Map<String, Object> offlineMerchantProducts(@RequestParam Integer merchantId) {
        int count = productService.offlineAllMerchantProducts(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "下架成功，共下架 " + count + " 件商品");
        result.put("offlineCount", count);
        return result;
    }

    // 用户拉黑（平台拉黑）
    @PostMapping("/user/blacklist")
    public Map<String, Object> blacklistUser(
            @RequestParam Integer userId,
            @RequestParam Integer adminId,
            @RequestParam String reason) {
        blacklistService.blacklistUser(userId, adminId, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "拉黑成功");
        return result;
    }
    
    // 移除拉黑
    @PostMapping("/user/blacklist/remove")
    public Map<String, Object> removeFromBlacklist(
            @RequestParam Integer userId,
            @RequestParam Integer adminId) {
        blacklistService.removeFromBlacklist(userId, adminId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "移除拉黑成功");
        return result;
    }
    
    // 获取商家封禁记录
    @GetMapping("/merchant/ban/history")
    public Map<String, Object> getMerchantBanHistory(@RequestParam Integer merchantId) {
        List<MerchantBanRecord> history = merchantService.getBanHistory(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", history);
        return result;
    }
    
    // 获取商家当前封禁状态
    @GetMapping("/merchant/ban/status")
    public Map<String, Object> getMerchantBanStatus(@RequestParam Integer merchantId) {
        MerchantBanRecord banRecord = merchantService.getActiveBanRecord(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", banRecord);
        result.put("isBanned", banRecord != null);
        return result;
    }
}
