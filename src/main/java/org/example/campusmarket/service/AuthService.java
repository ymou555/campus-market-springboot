package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private UserAuditMapper userAuditMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public SysUser register(SysUser user) {
        // 检查用户名是否已存在
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (sysUserMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 暂时不加密密码，方便测试
        // user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置初始状态
        user.setStatus("pending");
        // 保存用户
        sysUserMapper.insert(user);

        // 创建审核记录
        UserAudit audit = new UserAudit();
        audit.setUserId(user.getId());
        audit.setAuditStatus("pending");
        audit.setAuditTime(new Date());
        userAuditMapper.insert(audit);

        return user;
    }

    public Map<String, Object> login(String username, String password) {
        // 查找用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);

        // 直接比较明文密码，方便测试
        if (user == null || !password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!"active".equals(user.getStatus())) {
            throw new RuntimeException("账号未激活或已被封禁");
        }

        // 返回token和用户信息（排除密码字段）
        Map<String, Object> result = new HashMap<>();
        result.put("token", "user_" + user.getId() + "_" + user.getUsername());
        
        // 复制用户信息，排除密码
        SysUser userInfo = new SysUser();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setName(user.getName());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setCity(user.getCity());
        userInfo.setGender(user.getGender());
        userInfo.setBankAccount(user.getBankAccount());
        userInfo.setRole(user.getRole());
        userInfo.setStatus(user.getStatus());
        userInfo.setCreateTime(user.getCreateTime());
        userInfo.setUpdateTime(user.getUpdateTime());
        
        result.put("user", userInfo);
        return result;
    }

    public SysUser getUserByToken(String token) {
        try {
            // 简化token解析，直接从token中提取用户ID
            String[] parts = token.split("_");
            if (parts.length < 3) {
                throw new RuntimeException("无效的Token");
            }
            Integer userId = Integer.parseInt(parts[1]);
            return sysUserMapper.selectById(userId);
        } catch (Exception e) {
            throw new RuntimeException("无效的Token");
        }
    }
}