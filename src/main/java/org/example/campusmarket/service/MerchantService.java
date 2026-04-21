package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.MerchantLevelMapper;
import org.example.campusmarket.mapper.OrderInfoMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MerchantService {
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private MerchantLevelMapper merchantLevelMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private ProductMapper productMapper;

    // 获取商家信息
    public MerchantInfo getMerchantInfo(Integer userId) {
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getUserId, userId);
        return merchantInfoMapper.selectOne(wrapper);
    }

    // 更新商家信息
    public void updateMerchantInfo(MerchantInfo merchantInfo) {
        merchantInfo.setUpdateTime(new Date());
        merchantInfoMapper.updateById(merchantInfo);
    }

    // 获取商家等级列表
    public List<MerchantLevel> getMerchantLevelList() {
        return merchantLevelMapper.selectList(null);
    }

    // 获取商家等级详情
    public MerchantLevel getMerchantLevel(Integer id) {
        return merchantLevelMapper.selectById(id);
    }

    // 更新商家等级
    public void updateMerchantLevel(MerchantLevel level) {
        merchantLevelMapper.updateById(level);
    }

    // 调整商家等级
    public void adjustMerchantLevel(Integer userId, Integer levelId) {
        LambdaUpdateWrapper<MerchantInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MerchantInfo::getUserId, userId);
        wrapper.set(MerchantInfo::getLevelId, levelId);
        wrapper.set(MerchantInfo::getUpdateTime, new Date());
        merchantInfoMapper.update(null, wrapper);
    }

    // 封禁商家
    public void banMerchant(Integer merchantId, String reason, Date endTime) {
        // 这里可以实现商家封禁逻辑
        // 例如更新商家状态为封禁，记录封禁原因和时间等
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, merchantId);
        wrapper.set(SysUser::getStatus, "blocked");
        sysUserMapper.update(null, wrapper);
    }

    // 解除商家封禁
    public void unbanMerchant(Integer merchantId) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, merchantId);
        wrapper.set(SysUser::getStatus, "active");
        sysUserMapper.update(null, wrapper);
    }

    // 动态调整所有商家等级
    public void adjustMerchantLevels() {
        // 获取所有商家
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getRole, "merchant");
        userWrapper.eq(SysUser::getStatus, "active");
        List<SysUser> merchants = sysUserMapper.selectList(userWrapper);
        
        // 计算上一个月的时间范围
        Date now = new Date();
        Date lastMonth = new Date(now.getTime() - 30L * 24 * 60 * 60 * 1000);
        
        for (SysUser merchant : merchants) {
            // 计算商家的月交易额
            LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
            orderWrapper.eq(OrderInfo::getMerchantId, merchant.getId());
            orderWrapper.in(OrderInfo::getStatus, "received", "completed");
            orderWrapper.ge(OrderInfo::getCreateTime, lastMonth);
            List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
            
            double totalAmount = 0;
            for (OrderInfo order : orders) {
                totalAmount += order.getActualAmount();
            }
            
            // 计算商家的满意度（基于评价）
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(Review::getTargetId, merchant.getId());
            reviewWrapper.eq(Review::getTargetType, "merchant");
            reviewWrapper.ge(Review::getCreateTime, lastMonth);
            List<Review> reviews = reviewMapper.selectList(reviewWrapper);
            
            double avgRating = 0;
            if (!reviews.isEmpty()) {
                int totalRating = 0;
                for (Review review : reviews) {
                    totalRating += review.getRating();
                }
                avgRating = (double) totalRating / reviews.size();
            }
            
            // 根据交易额和满意度调整商家等级
            adjustMerchantLevelBasedOnPerformance(merchant.getId(), totalAmount, avgRating);
        }
    }

    // 根据绩效调整商家等级
    private void adjustMerchantLevelBasedOnPerformance(Integer merchantId, double totalAmount, double avgRating) {
        MerchantInfo merchantInfo = getMerchantInfo(merchantId);
        if (merchantInfo == null) {
            return;
        }

        List<MerchantLevel> levels = merchantLevelMapper.selectList(null);

        levels.sort((a, b) -> Double.compare(b.getMinAmount(), a.getMinAmount()));

        int newLevelId = 5;
        for (MerchantLevel level : levels) {
            if (totalAmount >= level.getMinAmount()) {
                newLevelId = level.getId();
                break;
            }
        }

        if (avgRating < 3 && newLevelId < 5) {
            newLevelId++;
        }

        if (merchantInfo.getLevelId() != newLevelId) {
            adjustMerchantLevel(merchantId, newLevelId);
            System.out.println("商家ID: " + merchantId + " 等级调整为: " + newLevelId);
        }
    }
    
    // 获取商家统计信息
    public Map<String, Object> getMerchantStats(Integer userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取商家信息
        MerchantInfo merchantInfo = getMerchantInfo(userId);
        if (merchantInfo == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 设置用户ID和店铺名称
        stats.put("userId", userId);
        stats.put("shopName", merchantInfo.getShopName());
        
        // 计算商家总销量
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getMerchantId, userId);
        List<Product> products = productMapper.selectList(productWrapper);
        
        int totalSales = 0;
        for (Product product : products) {
            totalSales += product.getSalesCount();
        }
        stats.put("totalSales", totalSales);
        
        // 计算商家平均评分
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getTargetId, userId);
        reviewWrapper.eq(Review::getTargetType, "merchant");
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);
        
        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            int totalRating = 0;
            for (Review review : reviews) {
                totalRating += review.getRating();
            }
            avgRating = (double) totalRating / reviews.size();
            // 保留一位小数
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }
        stats.put("avgRating", avgRating);
        stats.put("reviewCount", reviews.size());
        
        return stats;
    }
    
    // 获取商家统计信息（包含等级信息）
    public Map<String, Object> getMerchantStatsWithLevel(Integer userId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 获取商家信息
        MerchantInfo merchantInfo = getMerchantInfo(userId);
        if (merchantInfo == null) {
            throw new RuntimeException("商家信息不存在");
        }
        
        // 设置用户ID、店铺名称和等级ID
        stats.put("userId", userId);
        stats.put("shopName", merchantInfo.getShopName());
        stats.put("levelId", merchantInfo.getLevelId());
        
        // 计算商家总销量
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getMerchantId, userId);
        List<Product> products = productMapper.selectList(productWrapper);
        
        int totalSales = 0;
        for (Product product : products) {
            totalSales += product.getSalesCount();
        }
        stats.put("totalSales", totalSales);
        
        // 计算商家平均评分
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getTargetId, userId);
        reviewWrapper.eq(Review::getTargetType, "merchant");
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);
        
        double avgRating = 0.0;
        if (!reviews.isEmpty()) {
            int totalRating = 0;
            for (Review review : reviews) {
                totalRating += review.getRating();
            }
            avgRating = (double) totalRating / reviews.size();
            // 保留一位小数
            avgRating = Math.round(avgRating * 10.0) / 10.0;
        }
        stats.put("avgRating", avgRating);
        stats.put("reviewCount", reviews.size());
        
        return stats;
    }
    
    // 根据商品ID获取商家统计信息
    public Map<String, Object> getMerchantStatsByProduct(Integer productId) {
        // 根据商品ID查询商品信息
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        
        // 获取商家ID
        Integer merchantId = product.getMerchantId();
        
        // 调用原有的获取商家统计信息方法
        return getMerchantStats(merchantId);
    }
}