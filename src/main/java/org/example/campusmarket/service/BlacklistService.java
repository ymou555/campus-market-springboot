package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.dto.BlacklistUserDTO;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserBlacklist;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserBlacklistMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BlacklistService {
    @Autowired
    private UserBlacklistMapper userBlacklistMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    // 拉黑用户（商家拉黑买家）
    @Transactional
    public void blacklistUser(Integer userId, Integer blacklistedBy, String reason) {
        // 检查是否已经拉黑
        if (isUserBlacklisted(userId, blacklistedBy)) {
            throw new RuntimeException("该用户已被拉黑");
        }
        
        // 添加拉黑记录
        UserBlacklist blacklist = new UserBlacklist();
        blacklist.setUserId(userId);
        blacklist.setBlacklistedBy(blacklistedBy);
        blacklist.setReason(reason);
        blacklist.setCreateTime(new Date());
        userBlacklistMapper.insert(blacklist);
        
        // 更新用户状态为 blocked
        LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
        userWrapper.eq(SysUser::getId, userId);
        userWrapper.set(SysUser::getStatus, "blocked");
        sysUserMapper.update(null, userWrapper);
    }

    // 移除拉黑
    @Transactional
    public void removeFromBlacklist(Integer userId, Integer blacklistedBy) {
        // 删除拉黑记录
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId);
        wrapper.eq(UserBlacklist::getBlacklistedBy, blacklistedBy);
        userBlacklistMapper.delete(wrapper);
        
        // 检查用户是否还被其他商家或平台拉黑
        LambdaQueryWrapper<UserBlacklist> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(UserBlacklist::getUserId, userId);
        long count = userBlacklistMapper.selectCount(checkWrapper);
        
        // 如果没有被任何拉黑，恢复用户状态为 active
        if (count == 0) {
            LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
            userWrapper.eq(SysUser::getId, userId);
            userWrapper.set(SysUser::getStatus, "active");
            sysUserMapper.update(null, userWrapper);
        }
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
    
    // 获取用户的拉黑类型（用于前端判断）
    public String getUserBlacklistType(Integer userId) {
        boolean blacklistedByAdmin = isUserBlacklistedByPlatform(userId);
        
        // 检查是否被商家拉黑
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBlacklist::getUserId, userId);
        List<UserBlacklist> records = userBlacklistMapper.selectList(wrapper);
        
        // 查询所有管理员ID
        LambdaQueryWrapper<SysUser> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(SysUser::getRole, "admin");
        List<SysUser> admins = sysUserMapper.selectList(adminWrapper);
        List<Integer> adminIds = new ArrayList<>();
        for (SysUser admin : admins) {
            adminIds.add(admin.getId());
        }
        
        // 判断拉黑类型
        boolean blacklistedByMerchant = false;
        for (UserBlacklist record : records) {
            if (!adminIds.contains(record.getBlacklistedBy())) {
                blacklistedByMerchant = true;
                break;
            }
        }
        
        if (blacklistedByAdmin && blacklistedByMerchant) {
            return "both"; // 同时被管理员和商家拉黑
        } else if (blacklistedByAdmin) {
            return "admin"; // 仅被管理员拉黑
        } else if (blacklistedByMerchant) {
            return "merchant"; // 仅被商家拉黑
        } else {
            return "none"; // 未被拉黑
        }
    }
    
    // 获取黑名单用户详细信息列表
    public List<BlacklistUserDTO> getBlacklistUserList(String username, String blacklistType) {
        List<BlacklistUserDTO> result = new ArrayList<>();
        
        // 查询所有管理员ID
        LambdaQueryWrapper<SysUser> adminQueryWrapper = new LambdaQueryWrapper<>();
        adminQueryWrapper.eq(SysUser::getRole, "admin");
        List<SysUser> admins = sysUserMapper.selectList(adminQueryWrapper);
        List<Integer> adminIds = new ArrayList<>();
        for (SysUser admin : admins) {
            adminIds.add(admin.getId());
        }
        
        // 查询所有拉黑记录
        LambdaQueryWrapper<UserBlacklist> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(UserBlacklist::getCreateTime);
        List<UserBlacklist> blacklistRecords = userBlacklistMapper.selectList(wrapper);
        
        // 遍历每条拉黑记录，构建DTO
        for (UserBlacklist record : blacklistRecords) {
            BlacklistUserDTO dto = new BlacklistUserDTO();
            
            // 获取被拉黑用户信息
            SysUser blacklistedUser = sysUserMapper.selectById(record.getUserId());
            if (blacklistedUser == null) {
                continue;
            }
            
            dto.setId(blacklistedUser.getId());
            dto.setUsername(blacklistedUser.getUsername());
            dto.setName(blacklistedUser.getName());
            dto.setReason(record.getReason());
            dto.setTime(record.getCreateTime());
            dto.setBlockedById(record.getBlacklistedBy());
            
            // 获取拉黑者信息
            SysUser blocker = sysUserMapper.selectById(record.getBlacklistedBy());
            if (blocker != null) {
                dto.setBlockedBy(blocker.getName());
            }
            
            // 判断拉黑类型
            if (adminIds.contains(record.getBlacklistedBy())) {
                dto.setBlacklistType("admin");
                dto.setMerchantName(null);
            } else {
                dto.setBlacklistType("merchant");
                dto.setMerchantName(blocker != null ? blocker.getName() : null);
            }
            
            // 根据用户名筛选
            if (username != null && !username.trim().isEmpty()) {
                if (dto.getUsername() == null || !dto.getUsername().contains(username)) {
                    continue;
                }
            }
            
            // 根据拉黑类型筛选
            if (blacklistType != null && !blacklistType.trim().isEmpty()) {
                if (!blacklistType.equals(dto.getBlacklistType())) {
                    continue;
                }
            }
            
            result.add(dto);
        }
        
        return result;
    }
}
