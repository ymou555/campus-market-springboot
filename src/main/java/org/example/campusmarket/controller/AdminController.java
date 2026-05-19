package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.BlacklistUserDTO;
import org.example.campusmarket.dto.MerchantDetailDTO;
import org.example.campusmarket.dto.MerchantListDTO;
import org.example.campusmarket.dto.StatisticsVO;
import org.example.campusmarket.dto.UserAuditDTO;
import org.example.campusmarket.dto.UserWithBlacklistDTO;
import org.example.campusmarket.entity.*;
import org.example.campusmarket.mapper.OrderInfoMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.SysUserMapper;
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
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;

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
            @RequestParam(required = false) String auditStatus) {
        List<UserAuditDTO> users = userService.getUserAuditList(auditStatus);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", users);
        result.put("total", users.size());
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

    // 获取商品列表（支持状态筛选）
    @GetMapping("/product/list")
    public Map<String, Object> getProductList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        Page<Product> productPage = productService.getProductList(page, size, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", productPage.getRecords());
        result.put("total", productPage.getTotal());
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
        StatisticsVO statistics = new StatisticsVO();
        
        Long userCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "user"));
        statistics.setUserCount(userCount);
        
        Long productCount = productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getStatus, "published"));
        statistics.setProductCount(productCount);
        
        List<OrderInfo> orders = orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>().in(OrderInfo::getStatus, "received"));
        Double totalOrderAmount = orders.stream().mapToDouble(o -> o.getActualAmount() != null ? o.getActualAmount() : 0).sum();
        statistics.setTotalOrderAmount(totalOrderAmount);
        
        Long orderCount = orderInfoMapper.selectCount(new LambdaQueryWrapper<OrderInfo>().ne(OrderInfo::getStatus, "cancelled"));
        statistics.setOrderCount(orderCount);
        
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
    
    // 获取黑名单用户详细信息列表
    @GetMapping("/user/blacklist/list")
    public Map<String, Object> getBlacklistUserList(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String blacklistType) {
        List<BlacklistUserDTO> blacklistUsers = blacklistService.getBlacklistUserList(username, blacklistType);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", blacklistUsers);
        result.put("total", blacklistUsers.size());
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
    
    // 获取商家列表（管理员端）
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantList(
            @RequestParam(required = false) String shopName,
            @RequestParam(required = false) String status) {
        List<MerchantListDTO> merchants = merchantService.getMerchantList(shopName, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", merchants);
        result.put("total", merchants.size());
        return result;
    }
    
    // 获取商家详细信息（管理员端）
    @GetMapping("/merchant/detail")
    public Map<String, Object> getMerchantDetail(@RequestParam Integer merchantId) {
        MerchantDetailDTO detail = merchantService.getMerchantDetail(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", detail);
        return result;
    }
    
    // 关闭店铺
    @PostMapping("/merchant/close")
    public Map<String, Object> closeShop(@RequestParam Integer merchantId) {
        merchantService.closeShop(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "店铺已关闭");
        return result;
    }
    
    // 重新开放店铺
    @PostMapping("/merchant/reopen")
    public Map<String, Object> reopenShop(@RequestParam Integer merchantId) {
        merchantService.reopenShop(merchantId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "店铺已重新开放");
        return result;
    }
}
