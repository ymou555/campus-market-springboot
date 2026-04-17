package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.MerchantLevelMapper;
import org.example.campusmarket.mapper.OrderInfoMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

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
    public Page<MerchantLevel> getMerchantLevelList(int page, int size) {
        Page<MerchantLevel> levelPage = new Page<>(page, size);
        return merchantLevelMapper.selectPage(levelPage, null);
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
            orderWrapper.eq(OrderInfo::getStatus, "completed");
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
        // 获取商家当前等级
        MerchantInfo merchantInfo = getMerchantInfo(merchantId);
        if (merchantInfo == null) {
            return;
        }
        
        // 获取所有等级配置
        List<MerchantLevel> levels = merchantLevelMapper.selectList(null);
        
        // 根据交易额确定等级
        int newLevelId = 1; // 默认1级
        for (MerchantLevel level : levels) {
            if (totalAmount >= level.getMinAmount()) {
                newLevelId = level.getId();
            }
        }
        
        // 如果满意度低于3星，降低一级
        if (avgRating < 3 && newLevelId > 1) {
            newLevelId--;
        }
        
        // 如果当前等级与新等级不同，更新等级
        if (merchantInfo.getLevelId() != newLevelId) {
            adjustMerchantLevel(merchantId, newLevelId);
            System.out.println("商家ID: " + merchantId + " 等级调整为: " + newLevelId);
        }
    }
}