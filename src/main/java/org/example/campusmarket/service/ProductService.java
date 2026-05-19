package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.ProductWithAuditDTO;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductAudit;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.MerchantLevel;
import org.example.campusmarket.entity.Category;
import org.example.campusmarket.entity.CategoryTree;
import org.example.campusmarket.entity.MerchantBanRecord;
import org.example.campusmarket.mapper.MerchantBanRecordMapper;
import org.example.campusmarket.mapper.ProductAuditMapper;
import org.example.campusmarket.mapper.ProductImageMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.MerchantLevelMapper;
import org.example.campusmarket.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private MerchantBanRecordMapper merchantBanRecordMapper;

    // 发布商品
    @Transactional
    public void publishProduct(Product product, List<String> imageUrls) {
        // 检查商家是否被封禁
        if (isMerchantBanned(product.getMerchantId())) {
            throw new RuntimeException("商家已被封禁，无法发布商品");
        }
        
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
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        String currentStatus = product.getStatus();
        
        if ("published".equals(status)) {
            if ("pending".equals(currentStatus)) {
                throw new RuntimeException("待审核商品不能直接上架");
            }
            
            if ("offline".equals(currentStatus)) {
                LambdaQueryWrapper<ProductAudit> auditWrapper = new LambdaQueryWrapper<>();
                auditWrapper.eq(ProductAudit::getProductId, productId);
                ProductAudit audit = productAuditMapper.selectOne(auditWrapper);
                
                if (audit != null && "rejected".equals(audit.getAuditStatus())) {
                    throw new RuntimeException("审核未通过的商品不能上架");
                }
            }
        }

        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getId, productId);
        wrapper.set(Product::getStatus, status);
        wrapper.set(Product::getUpdateTime, new Date());
        productMapper.update(null, wrapper);
    }

    // 搜索商品
    public List<Product> searchProducts(String keyword, Integer categoryId, String sortBy, Double minPrice, Double maxPrice, String newness) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, "published");

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Product::getProductName, keyword);
        }

        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }

        if (minPrice != null) {
            wrapper.ge(Product::getDiscountPrice, minPrice);
        }

        if (maxPrice != null) {
            wrapper.le(Product::getDiscountPrice, maxPrice);
        }

        if (newness != null && !newness.isEmpty()) {
            wrapper.eq(Product::getNewness, newness);
        }

        // 排序
        if ("price_asc".equals(sortBy)) {
            wrapper.orderByAsc(Product::getDiscountPrice);
        } else if ("price_desc".equals(sortBy)) {
            wrapper.orderByDesc(Product::getDiscountPrice);
        } else if ("sales".equals(sortBy)) {
            wrapper.orderByDesc(Product::getSalesCount);
        } else if ("rating".equals(sortBy)) {
            // 按好评率排序
            List<Product> products = productMapper.selectList(wrapper);
            products.sort((p1, p2) -> {
                double rating1 = getProductAverageRating(p1.getId());
                double rating2 = getProductAverageRating(p2.getId());
                return Double.compare(rating2, rating1);
            });
            // 设置商家名称和首图
            setMerchantName(products);
            setFirstImage(products);
            return products;
        } else {
            wrapper.orderByDesc(Product::getCreateTime);
        }

        List<Product> products = productMapper.selectList(wrapper);
        // 设置商家名称和首图
        setMerchantName(products);
        setFirstImage(products);
        return products;
    }
    
    // 设置商品列表的商家名称
    private void setMerchantName(List<Product> products) {
        for (Product product : products) {
            // 从merchant_info表获取店铺名称
            LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MerchantInfo::getUserId, product.getMerchantId());
            MerchantInfo merchantInfo = merchantInfoMapper.selectOne(wrapper);
            
            if (merchantInfo != null && merchantInfo.getShopName() != null) {
                // 如果有店铺名称，使用店铺名称
                product.setMerchantName(merchantInfo.getShopName());
            } else {
                // 如果没有店铺名称，使用用户真实姓名
                SysUser merchant = sysUserMapper.selectById(product.getMerchantId());
                if (merchant != null) {
                    product.setMerchantName(merchant.getName());
                }
            }
        }
    }
    
    // 设置商品列表的首图
    private void setFirstImage(List<Product> products) {
        for (Product product : products) {
            LambdaQueryWrapper<ProductImage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProductImage::getProductId, product.getId());
            wrapper.orderByAsc(ProductImage::getSortOrder);
            List<ProductImage> images = productImageMapper.selectList(wrapper);
            if (images != null && !images.isEmpty()) {
                product.setFirstImage(images.get(0).getImageUrl());
            }
        }
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
    public List<ProductWithAuditDTO> getMerchantProducts(Integer merchantId, String status) {
        String actualStatus = status;
        if ("approved".equals(status)) {
            actualStatus = "published";
        } else if ("rejected".equals(status)) {
            actualStatus = "offline";
        }
        
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getMerchantId, merchantId);
        if (actualStatus != null) {
            wrapper.eq(Product::getStatus, actualStatus);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        List<Product> products = productMapper.selectList(wrapper);
        
        // 设置商家名称和首图
        setMerchantName(products);
        setFirstImage(products);
        
        // 转换为包含审核状态的DTO
        List<ProductWithAuditDTO> result = new ArrayList<>();
        for (Product product : products) {
            ProductWithAuditDTO dto = new ProductWithAuditDTO();
            dto.setId(product.getId());
            dto.setMerchantId(product.getMerchantId());
            dto.setCategoryId(product.getCategoryId());
            dto.setProductName(product.getProductName());
            dto.setOriginalPrice(product.getOriginalPrice());
            dto.setDiscountPrice(product.getDiscountPrice());
            dto.setSize(product.getSize());
            dto.setDescription(product.getDescription());
            dto.setIsNegotiable(product.getIsNegotiable());
            dto.setStock(product.getStock());
            dto.setStatus(product.getStatus());
            dto.setNewness(product.getNewness());
            dto.setSalesCount(product.getSalesCount());
            dto.setCreateTime(product.getCreateTime());
            dto.setUpdateTime(product.getUpdateTime());
            dto.setMerchantName(product.getMerchantName());
            dto.setFirstImage(product.getFirstImage());
            
            // 获取审核状态
            LambdaQueryWrapper<ProductAudit> auditWrapper = new LambdaQueryWrapper<>();
            auditWrapper.eq(ProductAudit::getProductId, product.getId());
            ProductAudit audit = productAuditMapper.selectOne(auditWrapper);
            if (audit != null) {
                dto.setAuditStatus(audit.getAuditStatus());
                dto.setAuditTime(audit.getAuditTime());
                dto.setAuditRemark(audit.getAuditRemark());
            }
            
            result.add(dto);
        }
        
        return result;
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
        if (product.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        product.setUpdateTime(new Date());
        productMapper.updateById(product);
    }

    // 更新商品信息（包含图片）
    @Transactional
    public void updateProductWithImages(Product product, List<String> imageUrls) {
        if (product.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }
        
        Product existingProduct = productMapper.selectById(product.getId());
        String originalStatus = existingProduct.getStatus();
        
        product.setUpdateTime(new Date());
        productMapper.updateById(product);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
            imageWrapper.eq(ProductImage::getProductId, product.getId());
            productImageMapper.delete(imageWrapper);

            for (int i = 0; i < imageUrls.size(); i++) {
                ProductImage image = new ProductImage();
                image.setProductId(product.getId());
                image.setImageUrl(imageUrls.get(i));
                image.setSortOrder(i);
                productImageMapper.insert(image);
            }
        }

        if ("offline".equals(originalStatus)) {
            LambdaQueryWrapper<ProductAudit> auditWrapper = new LambdaQueryWrapper<>();
            auditWrapper.eq(ProductAudit::getProductId, product.getId());
            ProductAudit audit = productAuditMapper.selectOne(auditWrapper);
            
            if (audit != null && "rejected".equals(audit.getAuditStatus())) {
                audit.setAuditStatus("pending");
                audit.setAuditRemark(null);
                audit.setAuditTime(new Date());
                productAuditMapper.updateById(audit);
                
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, product.getId());
                productWrapper.set(Product::getStatus, "pending");
                productMapper.update(null, productWrapper);
            }
        }
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
    
    // 获取商品分类树
    public List<CategoryTree> getCategoryTree() {
        // 查询所有分类
        List<Category> allCategories = categoryMapper.selectList(null);
        
        // 构建分类树
        return buildCategoryTree(allCategories, 0);
    }
    
    // 递归构建分类树
    private List<CategoryTree> buildCategoryTree(List<Category> categories, Integer parentId) {
        return categories.stream()
                .filter(category -> category.getParentId().equals(parentId))
                .map(category -> {
                    CategoryTree tree = new CategoryTree(category.getId(), category.getCategoryName());
                    List<CategoryTree> children = buildCategoryTree(categories, category.getId());
                    if (!children.isEmpty()) {
                        tree.setChildren(children);
                    }
                    return tree;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    public Page<Product> getProductList(int page, int size, String status) {
        String actualStatus = status;
        if ("approved".equals(status)) {
            actualStatus = "published";
        } else if ("rejected".equals(status)) {
            actualStatus = "offline";
        }
        
        LambdaQueryWrapper<Product> countWrapper = new LambdaQueryWrapper<>();
        if (actualStatus != null && !actualStatus.isEmpty()) {
            countWrapper.eq(Product::getStatus, actualStatus);
        }
        Long total = productMapper.selectCount(countWrapper);
        
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (actualStatus != null && !actualStatus.isEmpty()) {
            wrapper.eq(Product::getStatus, actualStatus);
        }
        wrapper.orderByDesc(Product::getCreateTime);
        
        int offset = (page - 1) * size;
        wrapper.last("OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY");
        
        List<Product> products = productMapper.selectList(wrapper);
        
        setMerchantName(products);
        setFirstImage(products);
        
        Page<Product> result = new Page<>(page, size);
        result.setRecords(products);
        result.setTotal(total);
        
        return result;
    }
    
    // 批量下架商家所有商品（惩罚功能）
    @Transactional
    public int offlineAllMerchantProducts(Integer merchantId) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Product::getMerchantId, merchantId);
        wrapper.in(Product::getStatus, "published", "pending");
        wrapper.set(Product::getStatus, "offline");
        wrapper.set(Product::getUpdateTime, new Date());
        
        return productMapper.update(null, wrapper);
    }
    
    // 检查商家是否被封禁
    private boolean isMerchantBanned(Integer merchantId) {
        // 检查商家店铺状态
        LambdaQueryWrapper<MerchantInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantInfo::getUserId, merchantId);
        MerchantInfo merchantInfo = merchantInfoMapper.selectOne(wrapper);
        
        if (merchantInfo == null) {
            return false;
        }
        
        // 如果店铺状态为banned或closed，返回true
        return "banned".equals(merchantInfo.getShopStatus()) || "closed".equals(merchantInfo.getShopStatus());
    }
}