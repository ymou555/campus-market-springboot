package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private UserAuditMapper userAuditMapper;

    // 获取用户信息
    public SysUser getUserById(Integer id) {
        return sysUserMapper.selectById(id);
    }

    // 更新用户信息
    @Transactional
    public void updateUser(SysUser user) {
        sysUserMapper.updateById(user);
    }

    // 分页查询用户列表
    public Page<SysUser> getUserList(int page, int size, String role, String status) {
        Page<SysUser> userPage = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        return sysUserMapper.selectPage(userPage, wrapper);
    }

    // 审核用户
    @Transactional
    public void auditUser(Integer userId, String auditStatus, String remark) {
        // 更新用户状态
        LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
        userWrapper.eq(SysUser::getId, userId);
        if ("approved".equals(auditStatus)) {
            userWrapper.set(SysUser::getStatus, "active");
        } else if ("rejected".equals(auditStatus)) {
            userWrapper.set(SysUser::getStatus, "blocked");
        }
        sysUserMapper.update(null, userWrapper);

        // 更新审核记录
        LambdaUpdateWrapper<UserAudit> auditWrapper = new LambdaUpdateWrapper<>();
        auditWrapper.eq(UserAudit::getUserId, userId);
        auditWrapper.set(UserAudit::getAuditStatus, auditStatus);
        auditWrapper.set(UserAudit::getAuditRemark, remark);
        userAuditMapper.update(null, auditWrapper);
    }

    // 封禁用户
    @Transactional
    public void blockUser(Integer userId) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId);
        wrapper.set(SysUser::getStatus, "blocked");
        sysUserMapper.update(null, wrapper);
    }

    // 解封用户
    @Transactional
    public void unblockUser(Integer userId) {
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysUser::getId, userId);
        wrapper.set(SysUser::getStatus, "active");
        sysUserMapper.update(null, wrapper);
    }
}