package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserBlacklist;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserBlacklistMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BlacklistService {
    @Autowired
    private UserBlacklistMapper userBlacklistMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    // 拉黑用户（商家拉黑买家）
    public void blacklistUser(Integer userId, Integer blacklistedBy, String reason) {
        // 检查是否已经拉黑
        if (isUserBlacklisted(userId, blacklistedBy)) {
            throw new RuntimeException("该用户已被拉黑");
        }
        
        UserBlacklist blacklist = new UserBlacklist();
        blacklist.setUserId(userId);
        blacklist.setBlacklistedBy(blacklistedBy);
        blacklist.setReason(reason);
        blacklist.setCreateTime(new Date());
        
        userBlacklistMapper.insert(blacklist);
    }

    // 移除拉黑
    public void removeFromBlacklist(Integer userId, Integer blacklistedBy) {
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId);
        wrapper.eq(UserBlacklist::getBlacklistedBy, blacklistedBy);
        
        userBlacklistMapper.delete(wrapper);
    }

    // 检查用户是否被特定商家拉黑
    public boolean isUserBlacklisted(Integer userId, Integer merchantId) {
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId);
        wrapper.eq(UserBlacklist::getBlacklistedBy, merchantId);
        
        return userBlacklistMapper.selectCount(wrapper) > 0;
    }

    // 检查用户是否被平台拉黑（被管理员拉黑）
    public boolean isUserBlacklistedByPlatform(Integer userId) {
        // 查询所有管理员
        LambdaQueryWrapper<SysUser> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(SysUser::getRole, "admin");
        List<SysUser> admins = sysUserMapper.selectList(adminWrapper);
        
        // 检查是否被任一管理员拉黑
        for (SysUser admin : admins) {
            if (isUserBlacklisted(userId, admin.getId())) {
                return true;
            }
        }
        
        return false;
    }

    // 检查用户是否可以购买某商家的商品
    public boolean canUserBuyFromMerchant(Integer userId, Integer merchantId) {
        // 检查是否被平台拉黑
        if (isUserBlacklistedByPlatform(userId)) {
            return false;
        }
        
        // 检查是否被该商家拉黑
        if (isUserBlacklisted(userId, merchantId)) {
            return false;
        }
        
        return true;
    }

    // 获取商家的黑名单列表
    public List<UserBlacklist> getMerchantBlacklist(Integer merchantId) {
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getBlacklistedBy, merchantId);
        wrapper.orderByDesc(UserBlacklist::getCreateTime);
        
        return userBlacklistMapper.selectList(wrapper);
    }

    // 获取用户被拉黑的记录
    public List<UserBlacklist> getUserBlacklistRecords(Integer userId) {
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId);
        wrapper.orderByDesc(UserBlacklist::getCreateTime);
        
        return userBlacklistMapper.selectList(wrapper);
    }
}
