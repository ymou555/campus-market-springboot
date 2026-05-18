package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.UserAuditDTO;
import org.example.campusmarket.dto.UserWithBlacklistDTO;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private UserAuditMapper userAuditMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private BlacklistService blacklistService;
    @Autowired
    private WalletService walletService;

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
    
    // 查询用户列表（不分页）
    public List<SysUser> getUserList(String role, String status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        return sysUserMapper.selectList(wrapper);
    }
    
    // 查询用户列表（支持用户名搜索）
    public List<SysUser> getUserList(String role, String status, String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        return sysUserMapper.selectList(wrapper);
    }
    
    // 查询用户列表（带拉黑类型）
    public List<UserWithBlacklistDTO> getUserListWithBlacklistType(String role, String status, String username) {
        // 查询用户列表
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (role != null && !role.isEmpty()) {
            wrapper.eq(SysUser::getRole, role);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        List<SysUser> users = sysUserMapper.selectList(wrapper);
        
        // 转换为带拉黑类型的DTO
        List<UserWithBlacklistDTO> result = new ArrayList<>();
        for (SysUser user : users) {
            UserWithBlacklistDTO dto = new UserWithBlacklistDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setPhone(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setCity(user.getCity());
            dto.setGender(user.getGender());
            dto.setBankAccount(user.getBankAccount());
            dto.setRole(user.getRole());
            dto.setStatus(user.getStatus());
            dto.setCreateTime(user.getCreateTime());
            dto.setUpdateTime(user.getUpdateTime());
            
            // 获取拉黑类型
            String blacklistType = blacklistService.getUserBlacklistType(user.getId());
            dto.setBlacklistType(blacklistType);
            
            result.add(dto);
        }
        
        return result;
    }

    // 审核用户
    @Transactional
    public void auditUser(Integer userId, String auditStatus, String remark) {
        if (!"approved".equals(auditStatus) && !"rejected".equals(auditStatus)) {
            throw new RuntimeException("审核状态无效，只能是 approved 或 rejected");
        }
        
        if ("rejected".equals(auditStatus) && (remark == null || remark.isEmpty())) {
            throw new RuntimeException("审核拒绝时必须填写备注");
        }

        if ("approved".equals(auditStatus)) {
            LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
            userWrapper.eq(SysUser::getId, userId);
            userWrapper.set(SysUser::getStatus, "active");
            sysUserMapper.update(null, userWrapper);

            walletService.getWallet(userId);
        }

        LambdaUpdateWrapper<UserAudit> auditWrapper = new LambdaUpdateWrapper<>();
        auditWrapper.eq(UserAudit::getUserId, userId);
        auditWrapper.set(UserAudit::getAuditStatus, auditStatus);
        auditWrapper.set(UserAudit::getAuditRemark, remark);
        userAuditMapper.update(null, auditWrapper);
    }

    public List<UserAuditDTO> getUserAuditList(String auditStatus) {
        List<UserAuditDTO> result = new ArrayList<>();

        LambdaQueryWrapper<UserAudit> auditWrapper = new LambdaQueryWrapper<>();
        if (auditStatus != null && !auditStatus.isEmpty()) {
            auditWrapper.eq(UserAudit::getAuditStatus, auditStatus);
        }
        List<UserAudit> auditList = userAuditMapper.selectList(auditWrapper);

        for (UserAudit audit : auditList) {
            SysUser user = sysUserMapper.selectById(audit.getUserId());
            if (user == null) continue;

            UserAuditDTO dto = new UserAuditDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setName(user.getName());
            dto.setPhone(user.getPhone());
            dto.setType(user.getRole());
            dto.setRegisterTime(user.getCreateTime());
            dto.setAuditStatus(audit.getAuditStatus());
            dto.setAuditTime(audit.getAuditTime());
            dto.setAuditRemark(audit.getAuditRemark());

            if ("merchant".equals(user.getRole())) {
                LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
                merchantWrapper.eq(MerchantInfo::getUserId, user.getId());
                MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantWrapper);
                if (merchantInfo != null) {
                    dto.setShopName(merchantInfo.getShopName());
                    dto.setBusinessLicense(merchantInfo.getBusinessLicense());
                    dto.setIdCardPhoto(merchantInfo.getIdCardPhoto());
                }
            }

            result.add(dto);
        }

        return result;
    }
}
