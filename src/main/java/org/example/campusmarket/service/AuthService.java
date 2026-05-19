package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.MerchantInfoMapper;
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
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public SysUser register(SysUser user, Map<String, Object> registerData) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, user.getUsername());
        if (sysUserMapper.selectOne(wrapper) != null) {
            throw new RuntimeException("用户名已存在");
        }

        user.setStatus("pending");
        sysUserMapper.insert(user);

        UserAudit audit = new UserAudit();
        audit.setUserId(user.getId());
        audit.setAuditStatus("pending");
        audit.setAuditTime(new Date());
        userAuditMapper.insert(audit);

        if ("merchant".equals(user.getRole())) {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setUserId(user.getId());
            merchantInfo.setBusinessLicense((String) registerData.get("businessLicense"));
            merchantInfo.setIdCardPhoto((String) registerData.get("idCardPhoto"));
            merchantInfo.setShopName((String) registerData.get("shopName"));
            merchantInfo.setLevelId(5);
            merchantInfo.setCreateTime(new Date());
            merchantInfo.setUpdateTime(new Date());
            merchantInfoMapper.insert(merchantInfo);
        }

        return user;
    }

    public Map<String, Object> login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = sysUserMapper.selectOne(wrapper);

        if (user == null || !password.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!"active".equals(user.getStatus()) && !"blocked".equals(user.getStatus())) {
            if ("pending".equals(user.getStatus())) {
                throw new RuntimeException("账号待审核，请等待管理员审核通过");
            } else {
                throw new RuntimeException("账号状态异常，请联系客服");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("token", "user_" + user.getId() + "_" + user.getUsername());

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
    
    // 修改密码
    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword) {
        // 查找用户
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 验证旧密码
        if (!oldPassword.equals(user.getPassword())) {
            throw new RuntimeException("旧密码错误");
        }
        
        // 验证新密码不能与旧密码相同
        if (oldPassword.equals(newPassword)) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
        
        // 更新密码（明文存储，方便测试）
        user.setPassword(newPassword);
        user.setUpdateTime(new Date());
        sysUserMapper.updateById(user);
    }
    
    // 忘记密码
    @Transactional
    public void forgotPassword(String username, String email, String newPassword) {
        // 验证用户名和邮箱是否匹配
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        wrapper.eq(SysUser::getEmail, email);
        
        SysUser user = sysUserMapper.selectOne(wrapper);
        if (user == null) {
            throw new RuntimeException("用户名或邮箱错误");
        }
        
        // 验证新密码不能与旧密码相同
        if (newPassword.equals(user.getPassword())) {
            throw new RuntimeException("新密码不能与旧密码相同");
        }
        
        // 更新密码
        user.setPassword(newPassword);
        user.setUpdateTime(new Date());
        sysUserMapper.updateById(user);
    }
}
