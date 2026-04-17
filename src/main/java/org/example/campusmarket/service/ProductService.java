package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductAudit;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.mapper.ProductAuditMapper;
import org.example.campusmarket.mapper.ProductImageMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.MerchantLevelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private ProductAuditMapper productAuditMapper;
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private MerchantLevelMapper merchantLevelMapper;

    // 发布商品
    @Transactional
    public void publishProduct(Product product, List<String> imageUrls) {
        // 设置初始状态
        product.setStatus("pending");
        product.setSalesCount(0);
        product.setCreateTime(new Date());
        product.setUpdateTime(new Date());
        // 保存商品
        productMapper.insert(product);

        // 保存商品图片
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(product.getId());
                image.setImageUrl(imageUrls.get(i));
                image.setSortOrder(i);
                productImageMapper.insert(image);
            }
        }

        // 创建审核记录
        ProductAudit audit = new ProductAudit();
        audit.setProductId(product.getId());
        audit.setAuditStatus("pending");
        audit.setAuditTime(new Date());
        productAuditMapper.insert(audit);
    }

    // 审核商品
    @Transactional
    public void auditProduct(Integer productId, String auditStatus, String remark) {
        // 更新商品状态
        LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
        productWrapper.eq(Product::getId, productId);
        if ("approved".equals(auditStatus)) {
            productWrapper.set(Product::getStatus, "published");
        } else if ("rejected".equals(auditStatus)) {
            productWrapper.set(Product::getStatus, "offline");
        }
        productWrapper.set(Product::getUpdateTime, new Date());
        productMapper.update(null, productWrapper);

        // 更新审核记录
        LambdaUpdateWrapper<ProductAudit> auditWrapper = new LambdaUpdateWrapper<>();
        auditWrapper.eq(ProductAudit::getProductId, productId);
        auditWrapper.set(ProductAudit::getAuditStatus, auditStatus);
        auditWrapper.set(ProductAudit::getAuditRemark, remark);
        productAuditMapper.update(null, auditWrapper);
    }

    // 商品上下架
    public void toggleProductStatus(Integer productId, String status) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId);
        wrapper.set(Product::getStatus, status);
        wrapper.set(Product::getUpdateTime, new Date());
        productMapper.update(null, wrapper);
    }

    // 搜索商品
    public Page<Product> searchProducts(int page, int size, String keyword, Integer categoryId, String sortBy) {
        Page<Product> productPage = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, "published");

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getProductName, keyword);
        }

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }

        // 排序
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getDiscountPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getDiscountPrice);
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else if ("rating".equals(sortBy)) {
            // 按好评率排序（这里使用自定义查询，实际项目中可能需要使用联表查询）
            // 由于MyBatis Plus的LambdaQueryWrapper不支持直接的联表查询和聚合函数
            // 这里我们先获取所有符合条件的商品，然后根据好评率排序
            List<Product> products = productMapper.selectList(wrapper);
            // 按好评率排序
            products.sort((p1, p2) -> {
                double rating1 = getProductAverageRating(p1.getId());
                double rating2 = getProductAverageRating(p2.getId());
                return Double.compare(rating2, rating1); // 降序
            });
            // 手动分页
            int start = (page - 1) * size;
            int end = Math.min(start + size, products.size());
            if (start < products.size()) {
                List<Product> pageProducts = products.subList(start, end);
                productPage.setRecords(pageProducts);
                productPage.setTotal(products.size());
                return productPage;
            } else {
                productPage.setRecords(List.of());
                productPage.setTotal(0);
                return productPage;
            }
        }

        return productMapper.selectPage(productPage, wrapper);
    }

    // 获取商品的平均评分
    private double getProductAverageRating(Integer productId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, productId);
        wrapper.eq(Review::getTargetType, "product");
        List<Review> reviews = reviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return 0;
        }
        int totalRating = 0;
        for (Review review : reviews) {
            totalRating += review.getRating();
        }
        return (double) totalRating / reviews.size();
    }

    // 获取商品详情
    public Product getProductById(Integer id) {
        return productMapper.selectById(id);
    }

    // 获取商品图片
    public List<ProductImage> getProductImages(Integer productId) {
        LambdaQueryWrapper<ProductImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductImage::getProductId, productId);
        wrapper.orderByAsc(ProductImage::getSortOrder);
        return productImageMapper.selectList(wrapper);
    }

    // 获取商家商品列表
    public Page<Product> getMerchantProducts(int page, int size, Integer merchantId, String status) {
        Page<Product> productPage = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getMerchantId, merchantId);
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        return productMapper.selectPage(productPage, wrapper);
    }

    // 获取商家店铺信息
    public Map<String, Object> getMerchantShopInfo(Integer merchantId) {
        Map<String, Object> shopInfo = new HashMap<>();
        // 获取商家基本信息
        SysUser merchant = sysUserMapper.selectById(merchantId);
        shopInfo.put("merchant", merchant);
        
        // 获取商家等级信息
        MerchantInfo merchantInfo = merchantInfoMapper.selectOne(new LambdaQueryWrapper<MerchantInfo>().eq(MerchantInfo::getUserId, merchantId));
        if (merchantInfo != null) {
            MerchantLevel merchantLevel = merchantLevelMapper.selectById(merchantInfo.getLevelId());
            shopInfo.put("merchantLevel", merchantLevel);
        }
        
        // 获取商家商品统计信息
        long totalProducts = productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        long publishedProducts = productMapper.selectCount(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId).eq(Product::getStatus, "published"));
        
        // 计算总销量
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getMerchantId, merchantId));
        int totalSales = products.stream().mapToInt(Product::getSalesCount).sum();
        
        shopInfo.put("totalProducts", totalProducts);
        shopInfo.put("publishedProducts", publishedProducts);
        shopInfo.put("totalSales", totalSales);
        
        return shopInfo;
    }

    // 获取商家店铺的商品列表（支持排序）
    public Page<Product> getShopProducts(int page, int size, Integer merchantId, String sortBy) {
        Page<Product> productPage = new Page<>(page, size);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getMerchantId, merchantId);
        wrapper.eq(Product::getStatus, "published");
        
        // 支持排序
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getDiscountPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getDiscountPrice);
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }
        
        return productMapper.selectPage(productPage, wrapper);
    }

    // 更新商品信息
    public void updateProduct(Product product) {
        product.setUpdateTime(new Date());
        productMapper.updateById(product);
    }

    // 删除商品
    @Transactional
    public void deleteProduct(Integer productId) {
        // 删除商品图片
        LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
        imageWrapper.eq(ProductImage::getProductId, productId);
        productImageMapper.delete(imageWrapper);

        // 删除商品审核记录
        LambdaQueryWrapper<ProductAudit> auditWrapper = new LambdaQueryWrapper<>();
        auditWrapper.eq(ProductAudit::getProductId, productId);
        productAuditMapper.delete(auditWrapper);

        // 删除商品
        productMapper.deleteById(productId);
    }
}