package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.MerchantApplicationDTO;
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
import java.util.Date;
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
            // 当查询商家时，包含纯商家和既是买家又是商家的用户
            // 当查询买家时，包含纯买家和既是买家又是商家的用户
            if ("merchant".equals(role)) {
                wrapper.in(SysUser::getRole, "merchant", "both");
            } else if ("user".equals(role)) {
                wrapper.in(SysUser::getRole, "user", "both");
            } else {
                wrapper.eq(SysUser::getRole, role);
            }
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
            // 当查询商家时，包含纯商家和既是买家又是商家的用户
            // 当查询买家时，包含纯买家和既是买家又是商家的用户
            if ("merchant".equals(role)) {
                wrapper.in(SysUser::getRole, "merchant", "both");
            } else if ("user".equals(role)) {
                wrapper.in(SysUser::getRole, "user", "both");
            } else {
                wrapper.eq(SysUser::getRole, role);
            }
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
            // 当查询商家时，包含纯商家和既是买家又是商家的用户
            // 当查询买家时，包含纯买家和既是买家又是商家的用户
            if ("merchant".equals(role)) {
                wrapper.in(SysUser::getRole, "merchant", "both");
            } else if ("user".equals(role)) {
                wrapper.in(SysUser::getRole, "user", "both");
            } else {
                wrapper.eq(SysUser::getRole, role);
            }
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
            // 当查询商家时，包含纯商家和既是买家又是商家的用户
            // 当查询买家时，包含纯买家和既是买家又是商家的用户
            if ("merchant".equals(role)) {
                wrapper.in(SysUser::getRole, "merchant", "both");
            } else if ("user".equals(role)) {
                wrapper.in(SysUser::getRole, "user", "both");
            } else {
                wrapper.eq(SysUser::getRole, role);
            }
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
        // 审核拒绝时必须填写备注
        if ("rejected".equals(auditStatus) && (remark == null || remark.isEmpty())) {
            throw new RuntimeException("审核拒绝时必须填写备注");
        }
        
        // 获取用户信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 更新用户状态
        LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
        userWrapper.eq(SysUser::getId, userId);
        if ("approved".equals(auditStatus)) {
            userWrapper.set(SysUser::getStatus, "active");
            
            // 检查是否是商家申请审核（用户角色为user且有merchant_info记录）
            if ("user".equals(user.getRole())) {
                LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
                merchantWrapper.eq(MerchantInfo::getUserId, userId);
                MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantWrapper);
                
                // 如果有商家信息记录，说明是申请成为商家，审核通过后将role改为both
                if (merchantInfo != null) {
                    userWrapper.set(SysUser::getRole, "both");
                }
            }
        } else if ("rejected".equals(auditStatus)) {
            // 审核拒绝时用户状态保持不变
        }
        sysUserMapper.update(null, userWrapper);

        // 更新审核记录
        LambdaUpdateWrapper<UserAudit> auditWrapper = new LambdaUpdateWrapper<>();
        auditWrapper.eq(UserAudit::getUserId, userId);
        auditWrapper.set(UserAudit::getAuditStatus, auditStatus);
        auditWrapper.set(UserAudit::getAuditRemark, remark);
        userAuditMapper.update(null, auditWrapper);
    }
    
    // 普通用户申请成为商家
    @Transactional
    public void applyForMerchant(MerchantApplicationDTO applicationDTO) {
        Integer userId = applicationDTO.getUserId();
        
        // 检查用户是否存在
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户角色（只有普通用户可以申请）
        if (!"user".equals(user.getRole())) {
            throw new RuntimeException("只有普通用户可以申请成为商家");
        }
        
        // 检查是否已经申请过（merchant_info表中是否已存在记录）
        LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
        merchantWrapper.eq(MerchantInfo::getUserId, userId);
        MerchantInfo existingMerchant = merchantInfoMapper.selectOne(merchantWrapper);
        
        if (existingMerchant != null) {
            // 检查审核状态
            LambdaQueryWrapper<UserAudit> auditWrapper = new LambdaQueryWrapper<>();
            auditWrapper.eq(UserAudit::getUserId, userId);
            auditWrapper.orderByDesc(UserAudit::getAuditTime);
            UserAudit latestAudit = userAuditMapper.selectOne(auditWrapper);
            
            if (latestAudit != null && "pending".equals(latestAudit.getAuditStatus())) {
                throw new RuntimeException("您已提交申请，请等待审核");
            }
            
            // 如果之前被拒绝，允许重新申请，更新merchant_info
            existingMerchant.setShopName(applicationDTO.getShopName());
            existingMerchant.setBusinessLicense(applicationDTO.getBusinessLicense());
            existingMerchant.setIdCardPhoto(applicationDTO.getIdCardPhoto());
            existingMerchant.setUpdateTime(new Date());
            merchantInfoMapper.updateById(existingMerchant);
        } else {
            // 首次申请，创建merchant_info记录
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setUserId(userId);
            merchantInfo.setShopName(applicationDTO.getShopName());
            merchantInfo.setBusinessLicense(applicationDTO.getBusinessLicense());
            merchantInfo.setIdCardPhoto(applicationDTO.getIdCardPhoto());
            merchantInfo.setLevelId(5); // 默认5级
            merchantInfo.setShopStatus("active"); // 默认店铺状态为正常
            merchantInfo.setCreateTime(new Date());
            merchantInfo.setUpdateTime(new Date());
            merchantInfoMapper.insert(merchantInfo);
        }
        
        // 创建审核记录
        UserAudit audit = new UserAudit();
        audit.setUserId(userId);
        audit.setAuditStatus("pending");
        audit.setAuditTime(new Date());
        userAuditMapper.insert(audit);
        
        // 注意：role保持'user'不变，审核通过后才变为'both'
    }
    
    // 商家开通买家功能
    @Transactional
    public void enableBuyerFunction(Integer userId) {
        // 获取用户信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        // 检查用户角色（只有纯商家可以开通买家功能）
        if (!"merchant".equals(user.getRole())) {
            throw new RuntimeException("只有纯商家可以开通买家功能");
        }
        
        // 将role从merchant改为both
        LambdaUpdateWrapper<SysUser> userWrapper = new LambdaUpdateWrapper<>();
        userWrapper.eq(SysUser::getId, userId);
        userWrapper.set(SysUser::getRole, "both");
        sysUserMapper.update(null, userWrapper);
    }
}
