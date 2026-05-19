package org.example.campusmarket.service;

import org.example.campusmarket.dto.MerchantApplicationDTO;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserAuditMapper;
import org.example.campusmarket.util.FileUploadUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

@Service
public class MerchantApplicationService {
    
    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    
    @Autowired
    private UserAuditMapper userAuditMapper;
    
    @Autowired
    private FileUploadUtil fileUploadUtil;
    
    @Transactional
    public void apply(Integer userId, String shopName, MultipartFile businessLicense, MultipartFile idCardPhoto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        
        if (!"user".equals(user.getRole())) {
            throw new RuntimeException("只有普通用户可以申请成为商家");
        }
        
        String businessLicenseUrl = uploadFile(businessLicense, "license", "营业执照");
        String idCardPhotoUrl = uploadFile(idCardPhoto, "idcard", "身份证照片");
        
        MerchantInfo existingMerchant = merchantInfoMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MerchantInfo>()
                .eq(MerchantInfo::getUserId, userId)
        );
        
        if (existingMerchant != null) {
            UserAudit latestAudit = userAuditMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserAudit>()
                    .eq(UserAudit::getUserId, userId)
                    .orderByDesc(UserAudit::getAuditTime)
            );
            
            if (latestAudit != null && "pending".equals(latestAudit.getAuditStatus())) {
                throw new RuntimeException("您已提交申请，请等待审核");
            }
            
            existingMerchant.setShopName(shopName);
            existingMerchant.setBusinessLicense(businessLicenseUrl);
            existingMerchant.setIdCardPhoto(idCardPhotoUrl);
            existingMerchant.setUpdateTime(new Date());
            merchantInfoMapper.updateById(existingMerchant);
        } else {
            MerchantInfo merchantInfo = new MerchantInfo();
            merchantInfo.setUserId(userId);
            merchantInfo.setShopName(shopName);
            merchantInfo.setBusinessLicense(businessLicenseUrl);
            merchantInfo.setIdCardPhoto(idCardPhotoUrl);
            merchantInfo.setLevelId(5);
            merchantInfo.setShopStatus("active");
            merchantInfo.setCreateTime(new Date());
            merchantInfo.setUpdateTime(new Date());
            merchantInfoMapper.insert(merchantInfo);
        }
        
        UserAudit audit = new UserAudit();
        audit.setUserId(userId);
        audit.setAuditStatus("pending");
        audit.setAuditTime(new Date());
        userAuditMapper.insert(audit);
    }
    
    private String uploadFile(MultipartFile file, String documentType, String fileDescription) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(fileDescription + "不能为空");
        }
        
        try {
            return fileUploadUtil.uploadMerchantDocument(file, documentType);
        } catch (IOException e) {
            throw new RuntimeException(fileDescription + "上传失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(fileDescription + "格式错误: " + e.getMessage());
        }
    }
}
