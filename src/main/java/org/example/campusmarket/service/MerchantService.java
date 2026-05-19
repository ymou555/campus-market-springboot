package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.dto.MerchantDetailDTO;
import org.example.campusmarket.dto.MerchantListDTO;
import org.example.campusmarket.entity.MerchantBanRecord;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.UserAudit;
import org.example.campusmarket.mapper.MerchantBanRecordMapper;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.MerchantLevelMapper;
import org.example.campusmarket.mapper.OrderInfoMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.UserAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Autowired
    private MerchantBanRecordMapper merchantBanRecordMapper;
    @Autowired
    private UserAuditMapper userAuditMapper;

    // 获取商家信息（根据用户ID）
    public MerchantInfo getMerchantInfo(Integer userId) {
        if (userId == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getUserId, userId);
        return merchantInfoMapper.selectOne(wrapper);
    }

    // 获取商家信息（根据主键ID）
    public MerchantInfo getMerchantInfoById(Integer id) {
        if (id == null) {
            throw new RuntimeException("商家ID不能为空");
        }
        return merchantInfoMapper.selectById(id);
    }

    // 更新商家信息（包含图片）
    @Transactional
    public void updateMerchantInfo(MerchantInfo merchantInfo) {
        if (merchantInfo.getUserId() == null) {
            throw new RuntimeException("用户ID不能为空");
        }
        
        MerchantInfo existingInfo = getMerchantInfo(merchantInfo.getUserId());
        
        if (existingInfo == null) {
            merchantInfo.setCreateTime(new Date());
            merchantInfo.setUpdateTime(new Date());
            merchantInfoMapper.insert(merchantInfo);
        } else {
            merchantInfo.setId(existingInfo.getId());
            merchantInfo.setUpdateTime(new Date());
            if (merchantInfo.getCreateTime() == null) {
                merchantInfo.setCreateTime(existingInfo.getCreateTime());
            }
            if (merchantInfo.getLevelId() == null) {
                merchantInfo.setLevelId(existingInfo.getLevelId());
            }
            merchantInfoMapper.updateById(merchantInfo);
        }
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

    // 封禁商家（限时封禁）
    public void banMerchant(Integer merchantId, String reason, Date endTime) {
        // 更新商家店铺状态为封禁
        LambdaUpdateWrapper<MerchantInfo> merchantInfoWrapper = new LambdaUpdateWrapper<>();
        merchantInfoWrapper.eq(MerchantInfo::getUserId, merchantId);
        merchantInfoWrapper.set(MerchantInfo::getShopStatus, "banned");
        merchantInfoWrapper.set(MerchantInfo::getUpdateTime, new Date());
        merchantInfoMapper.update(null, merchantInfoWrapper);
        
        // 创建封禁记录
        MerchantBanRecord banRecord = new MerchantBanRecord();
        banRecord.setMerchantId(merchantId);
        banRecord.setBanReason(reason);
        banRecord.setBanStartTime(new Date());
        banRecord.setBanEndTime(endTime);
        banRecord.setStatus("active");
        merchantBanRecordMapper.insert(banRecord);
    }

    // 解除商家封禁
    public void unbanMerchant(Integer merchantId) {
        // 更新商家店铺状态为正常
        LambdaUpdateWrapper<MerchantInfo> merchantInfoWrapper = new LambdaUpdateWrapper<>();
        merchantInfoWrapper.eq(MerchantInfo::getUserId, merchantId);
        merchantInfoWrapper.set(MerchantInfo::getShopStatus, "active");
        merchantInfoWrapper.set(MerchantInfo::getUpdateTime, new Date());
        merchantInfoMapper.update(null, merchantInfoWrapper);
        
        // 更新封禁记录状态为cancelled（管理员提前解除）
        LambdaUpdateWrapper<MerchantBanRecord> banWrapper = new LambdaUpdateWrapper<>();
        banWrapper.eq(MerchantBanRecord::getMerchantId, merchantId);
        banWrapper.eq(MerchantBanRecord::getStatus, "active");
        banWrapper.set(MerchantBanRecord::getStatus, "cancelled");
        merchantBanRecordMapper.update(null, banWrapper);
    }
    
    // 检查商家是否被封禁
    public boolean isMerchantBanned(Integer merchantId) {
        // 检查商家店铺状态
        MerchantInfo merchantInfo = getMerchantInfo(merchantId);
        if (merchantInfo == null) {
            return false;
        }
        
        // 如果店铺状态为banned或closed，返回true
        return "banned".equals(merchantInfo.getShopStatus()) || "closed".equals(merchantInfo.getShopStatus());
    }
    
    // 获取商家当前封禁记录
    public MerchantBanRecord getActiveBanRecord(Integer merchantId) {
        LambdaQueryWrapper<MerchantBanRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantBanRecord::getMerchantId, merchantId);
        wrapper.eq(MerchantBanRecord::getStatus, "active");
        wrapper.gt(MerchantBanRecord::getBanEndTime, new Date());
        wrapper.orderByDesc(MerchantBanRecord::getBanStartTime);
        
        return merchantBanRecordMapper.selectOne(wrapper);
    }
    
    // 获取商家封禁历史
    public List<MerchantBanRecord> getBanHistory(Integer merchantId) {
        LambdaQueryWrapper<MerchantBanRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantBanRecord::getMerchantId, merchantId);
        wrapper.orderByDesc(MerchantBanRecord::getBanStartTime);
        
        return merchantBanRecordMapper.selectList(wrapper);
    }
    
    // 关闭店铺
    public void closeShop(Integer merchantId) {
        // 更新商家店铺状态为关闭
        LambdaUpdateWrapper<MerchantInfo> merchantInfoWrapper = new LambdaUpdateWrapper<>();
        merchantInfoWrapper.eq(MerchantInfo::getUserId, merchantId);
        merchantInfoWrapper.set(MerchantInfo::getShopStatus, "closed");
        merchantInfoWrapper.set(MerchantInfo::getUpdateTime, new Date());
        merchantInfoMapper.update(null, merchantInfoWrapper);
        
        // 下架该商家的所有商品
        LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
        productWrapper.eq(Product::getMerchantId, merchantId);
        productWrapper.eq(Product::getStatus, "published");
        productWrapper.set(Product::getStatus, "offline");
        productWrapper.set(Product::getUpdateTime, new Date());
        productMapper.update(null, productWrapper);
    }
    
    // 重新开放店铺
    public void reopenShop(Integer merchantId) {
        // 更新商家店铺状态为正常
        LambdaUpdateWrapper<MerchantInfo> merchantInfoWrapper = new LambdaUpdateWrapper<>();
        merchantInfoWrapper.eq(MerchantInfo::getUserId, merchantId);
        merchantInfoWrapper.set(MerchantInfo::getShopStatus, "active");
        merchantInfoWrapper.set(MerchantInfo::getUpdateTime, new Date());
        merchantInfoMapper.update(null, merchantInfoWrapper);
    }
    
    // 自动处理到期的封禁记录
    public void autoExpireBanRecords() {
        // 查询所有已到期但状态仍为active的封禁记录
        LambdaQueryWrapper<MerchantBanRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MerchantBanRecord::getStatus, "active");
        queryWrapper.lt(MerchantBanRecord::getBanEndTime, new Date());
        List<MerchantBanRecord> expiredRecords = merchantBanRecordMapper.selectList(queryWrapper);
        
        for (MerchantBanRecord record : expiredRecords) {
            // 更新封禁记录状态为expired
            record.setStatus("expired");
            merchantBanRecordMapper.updateById(record);
            
            // 更新商家店铺状态为active
            LambdaUpdateWrapper<MerchantInfo> merchantInfoWrapper = new LambdaUpdateWrapper<>();
            merchantInfoWrapper.eq(MerchantInfo::getUserId, record.getMerchantId());
            merchantInfoWrapper.set(MerchantInfo::getShopStatus, "active");
            merchantInfoWrapper.set(MerchantInfo::getUpdateTime, new Date());
            merchantInfoMapper.update(null, merchantInfoWrapper);
        }
    }

    // 动态调整所有商家等级
    public void adjustMerchantLevels() {
        // 获取所有商家（包括纯商家和既是买家又是商家的用户）
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(SysUser::getRole, "merchant", "both");
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
    
    // 获取商家列表（管理员端）
    public List<MerchantListDTO> getMerchantList(String shopName, String status) {
        List<MerchantListDTO> result = new ArrayList<>();
        
        // 查询所有商家（包括纯商家和既是买家又是商家的用户）
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.in(SysUser::getRole, "merchant", "both");
        List<SysUser> merchants = sysUserMapper.selectList(userWrapper);
        
        for (SysUser merchant : merchants) {
            MerchantListDTO dto = new MerchantListDTO();
            dto.setId(merchant.getId());
            
            // 获取商家信息
            MerchantInfo merchantInfo = getMerchantInfo(merchant.getId());
            if (merchantInfo != null) {
                dto.setShopName(merchantInfo.getShopName());
                dto.setLevelId(merchantInfo.getLevelId());
                
                // 查询审核状态
                LambdaQueryWrapper<UserAudit> auditWrapper = new LambdaQueryWrapper<>();
                auditWrapper.eq(UserAudit::getUserId, merchant.getId());
                auditWrapper.orderByDesc(UserAudit::getAuditTime);
                List<UserAudit> auditList = userAuditMapper.selectList(auditWrapper);
                UserAudit latestAudit = auditList.isEmpty() ? null : auditList.get(0);
                
                // 根据审核状态设置商家状态
                if (latestAudit == null) {
                    // 如果没有审核记录，默认为pending
                    dto.setStatus("pending");
                } else if ("approved".equals(latestAudit.getAuditStatus())) {
                    // 审核通过，返回店铺实际状态
                    dto.setStatus(merchantInfo.getShopStatus());
                } else {
                    // pending或rejected，返回pending
                    dto.setStatus("pending");
                }
            } else {
                // 如果没有商家信息，默认状态为pending
                dto.setStatus("pending");
            }
            
            // 根据店铺名称筛选
            if (shopName != null && !shopName.trim().isEmpty()) {
                if (dto.getShopName() == null || !dto.getShopName().contains(shopName)) {
                    continue;
                }
            }
            
            // 根据店铺状态筛选
            if (status != null && !status.trim().isEmpty()) {
                if (!status.equals(dto.getStatus())) {
                    continue;
                }
            }
            
            // 统计商品数量
            LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
            productWrapper.eq(Product::getMerchantId, merchant.getId());
            Long productCount = productMapper.selectCount(productWrapper);
            dto.setProductCount(productCount.intValue());
            
            // 统计销量
            LambdaQueryWrapper<Product> salesWrapper = new LambdaQueryWrapper<>();
            salesWrapper.eq(Product::getMerchantId, merchant.getId());
            List<Product> products = productMapper.selectList(salesWrapper);
            int totalSales = 0;
            for (Product product : products) {
                totalSales += product.getSalesCount();
            }
            dto.setTotalSales(totalSales);
            
            // 计算平均评分
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(Review::getTargetId, merchant.getId());
            reviewWrapper.eq(Review::getTargetType, "merchant");
            List<Review> reviews = reviewMapper.selectList(reviewWrapper);
            
            double avgRating = 0.0;
            if (!reviews.isEmpty()) {
                int totalRating = 0;
                for (Review review : reviews) {
                    totalRating += review.getRating();
                }
                avgRating = (double) totalRating / reviews.size();
                avgRating = Math.round(avgRating * 10.0) / 10.0;
            }
            dto.setAvgRating(avgRating);
            
            // 查询封禁信息（仅当状态为banned时才显示）
            if ("banned".equals(dto.getStatus())) {
                MerchantBanRecord banRecord = getActiveBanRecord(merchant.getId());
                if (banRecord != null) {
                    dto.setBanEndTime(banRecord.getBanEndTime());
                    dto.setBanReason(banRecord.getBanReason());
                }
            } else {
                dto.setBanEndTime(null);
                dto.setBanReason(null);
            }
            
            result.add(dto);
        }
        
        return result;
    }
    
    // 获取商家详细信息（管理员端）
    public MerchantDetailDTO getMerchantDetail(Integer merchantId) {
        MerchantDetailDTO result = new MerchantDetailDTO();
        
        // 查询用户基本信息
        SysUser user = sysUserMapper.selectById(merchantId);
        if (user == null) {
            throw new RuntimeException("商家不存在");
        }
        
        // 设置基本信息
        MerchantDetailDTO.BasicInfo basicInfo = new MerchantDetailDTO.BasicInfo();
        basicInfo.setUserId(user.getId());
        basicInfo.setName(user.getName());
        basicInfo.setPhone(user.getPhone());
        basicInfo.setEmail(user.getEmail());
        basicInfo.setGender(user.getGender());
        basicInfo.setCity(user.getCity());
        basicInfo.setBankAccount(user.getBankAccount());
        basicInfo.setStatus(user.getStatus());
        basicInfo.setCreateTime(user.getCreateTime());
        result.setBasicInfo(basicInfo);
        
        // 查询商家信息
        MerchantInfo merchantInfo = getMerchantInfo(merchantId);
        if (merchantInfo != null) {
            MerchantDetailDTO.ShopInfo shopInfo = new MerchantDetailDTO.ShopInfo();
            shopInfo.setShopName(merchantInfo.getShopName());
            shopInfo.setBusinessLicense(merchantInfo.getBusinessLicense());
            shopInfo.setIdCardPhoto(merchantInfo.getIdCardPhoto());
            shopInfo.setLevelId(merchantInfo.getLevelId());
            shopInfo.setCreateTime(merchantInfo.getCreateTime());
            shopInfo.setUpdateTime(merchantInfo.getUpdateTime());
            
            // 查询审核状态
            LambdaQueryWrapper<UserAudit> auditWrapper = new LambdaQueryWrapper<>();
            auditWrapper.eq(UserAudit::getUserId, merchantId);
            auditWrapper.orderByDesc(UserAudit::getAuditTime);
            List<UserAudit> auditList = userAuditMapper.selectList(auditWrapper);
            UserAudit latestAudit = auditList.isEmpty() ? null : auditList.get(0);
            
            // 根据审核状态设置店铺状态
            if (latestAudit == null) {
                // 如果没有审核记录，默认为pending
                shopInfo.setShopStatus("pending");
            } else if ("approved".equals(latestAudit.getAuditStatus())) {
                // 审核通过，返回店铺实际状态
                shopInfo.setShopStatus(merchantInfo.getShopStatus());
            } else {
                // pending或rejected，返回pending
                shopInfo.setShopStatus("pending");
            }
            
            // 查询等级对应的费率
            MerchantLevel level = merchantLevelMapper.selectById(merchantInfo.getLevelId());
            if (level != null) {
                shopInfo.setRate(level.getRate());
            }
            
            result.setShopInfo(shopInfo);
        }
        
        // 查询封禁记录
        List<MerchantBanRecord> banRecords = getBanHistory(merchantId);
        List<MerchantDetailDTO.BanRecord> banRecordList = new ArrayList<>();
        for (MerchantBanRecord record : banRecords) {
            MerchantDetailDTO.BanRecord banRecord = new MerchantDetailDTO.BanRecord();
            banRecord.setId(record.getId());
            banRecord.setBanReason(record.getBanReason());
            banRecord.setBanStartTime(record.getBanStartTime());
            banRecord.setBanEndTime(record.getBanEndTime());
            banRecord.setStatus(record.getStatus());
            banRecordList.add(banRecord);
        }
        result.setBanRecords(banRecordList);
        
        return result;
    }
}