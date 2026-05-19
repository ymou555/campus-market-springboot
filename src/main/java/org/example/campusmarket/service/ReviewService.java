package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.dto.PendingBuyerReviewDTO;
import org.example.campusmarket.dto.PendingMerchantReviewDTO;
import org.example.campusmarket.dto.PendingProductReviewDTO;
import org.example.campusmarket.dto.ReviewedBuyerDTO;
import org.example.campusmarket.dto.UserReviewDTO;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.OrderItem;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.Review;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.OrderInfoMapper;
import org.example.campusmarket.mapper.OrderItemMapper;
import org.example.campusmarket.mapper.ProductImageMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.ReviewMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class ReviewService {
    @Autowired
    private ReviewMapper reviewMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private ProductImageMapper productImageMapper;

    // 添加评价
    public void addReview(Review review) {
        review.setCreateTime(new Date());
        reviewMapper.insert(review);
    }

    // 获取商品评价列表
    public List<Review> getProductReviews(Integer productId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, productId);
        wrapper.eq(Review::getTargetType, "product");
        wrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        // 设置用户名
        for (Review review : reviews) {
            SysUser user = sysUserMapper.selectById(review.getUserId());
            if (user != null) {
                review.setUsername(user.getUsername());
            }
        }
        
        return reviews;
    }

    // 获取商家评价列表
    public List<Review> getMerchantReviews(Integer merchantId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, merchantId);
        wrapper.eq(Review::getTargetType, "merchant");
        wrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        // 设置用户名
        for (Review review : reviews) {
            SysUser user = sysUserMapper.selectById(review.getUserId());
            if (user != null) {
                review.setUsername(user.getUsername());
            }
        }
        
        return reviews;
    }

    // 获取买家评价列表
    public List<Review> getBuyerReviews(Integer buyerId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, buyerId);
        wrapper.eq(Review::getTargetType, "buyer");
        wrapper.orderByDesc(Review::getCreateTime);
        return reviewMapper.selectList(wrapper);
    }

    // 获取用户的评价列表
    public List<UserReviewDTO> getUserReviews(Integer userId) {
        List<UserReviewDTO> result = new ArrayList<>();
        
        // 查询用户的评价列表
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getUserId, userId);
        wrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        // 遍历评价，组装数据
        for (Review review : reviews) {
            UserReviewDTO dto = new UserReviewDTO();
            dto.setId(review.getId());
            dto.setReviewType(review.getTargetType());
            dto.setOrderId(review.getOrderId());
            dto.setRating(review.getRating());
            dto.setContent(review.getContent());
            dto.setCreateTime(review.getCreateTime());
            
            // 获取订单信息
            OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
            if (order != null) {
                dto.setOrderNo(order.getOrderNo());
            }
            
            // 根据评价类型组装不同的数据
            if ("product".equals(review.getTargetType())) {
                // 商品评价
                Product product = productMapper.selectById(review.getTargetId());
                if (product != null) {
                    dto.setProductName(product.getProductName());
                    dto.setPrice(product.getDiscountPrice());
                    dto.setMerchantId(product.getMerchantId());
                    
                    // 获取商品第一张图片
                    LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                    imageWrapper.eq(ProductImage::getProductId, product.getId());
                    imageWrapper.orderByAsc(ProductImage::getSortOrder);
                    List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                    if (!images.isEmpty()) {
                        dto.setProductImage(images.get(0).getImageUrl());
                    }
                    
                    // 获取商家名称
                    SysUser merchant = sysUserMapper.selectById(product.getMerchantId());
                    if (merchant != null) {
                        dto.setMerchant(merchant.getName());
                    }
                    
                    // 获取购买数量
                    if (order != null) {
                        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                        itemWrapper.eq(OrderItem::getOrderId, order.getId());
                        itemWrapper.eq(OrderItem::getProductId, product.getId());
                        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
                        if (!items.isEmpty()) {
                            dto.setQuantity(items.get(0).getQuantity());
                        }
                    }
                }
            } else if ("merchant".equals(review.getTargetType())) {
                // 商家评价
                SysUser merchant = sysUserMapper.selectById(review.getTargetId());
                if (merchant != null) {
                    dto.setMerchantId(merchant.getId());
                    dto.setMerchantRealName(merchant.getName());
                    
                    // 获取商家店铺名
                    LambdaQueryWrapper<MerchantInfo> merchantInfoWrapper = new LambdaQueryWrapper<>();
                    merchantInfoWrapper.eq(MerchantInfo::getUserId, merchant.getId());
                    MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantInfoWrapper);
                    if (merchantInfo != null) {
                        dto.setMerchantName(merchantInfo.getShopName());
                    } else {
                        dto.setMerchantName(merchant.getName() + "的店铺");
                    }
                }
            }
            
            result.add(dto);
        }
        
        return result;
    }

    // 获取商品的平均评分
    public double getProductAverageRating(Integer productId) {
        // 先查询是否有评价
        LambdaQueryWrapper<Review> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Review::getTargetId, productId);
        countWrapper.eq(Review::getTargetType, "product");
        Long count = reviewMapper.selectCount(countWrapper);
        
        if (count == null || count == 0) {
            return 0;
        }
        
        // 使用QueryWrapper来支持SQL聚合函数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("target_id", productId);
        queryWrapper.eq("target_type", "product");
        queryWrapper.select("AVG(rating) as avg_rating");
        Object result = reviewMapper.selectObjs(queryWrapper).stream().findFirst().orElse(0);
        return result != null ? Double.parseDouble(result.toString()) : 0;
    }

    // 获取商家的平均评分
    public double getMerchantAverageRating(Integer merchantId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, merchantId);
        wrapper.eq(Review::getTargetType, "merchant");
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Review> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("target_id", merchantId);
        queryWrapper.eq("target_type", "merchant");
        queryWrapper.select("AVG(rating) as avg_rating");
        Object result = reviewMapper.selectObjs(queryWrapper).stream().findFirst().orElse(0);
        return result != null ? Double.parseDouble(result.toString()) : 0;
    }
    
    public double getBuyerAverageRating(Integer buyerId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getTargetId, buyerId);
        wrapper.eq(Review::getTargetType, "buyer");
        
        List<Review> reviews = reviewMapper.selectList(wrapper);
        
        if (reviews.isEmpty()) {
            return 0;
        }
        
        double totalRating = reviews.stream().mapToInt(Review::getRating).sum();
        return Math.round(totalRating / reviews.size() * 100.0) / 100.0;
    }

    // 获取用户待评价的商品列表
    public List<PendingProductReviewDTO> getPendingProductReviews(Integer userId) {
        List<PendingProductReviewDTO> result = new ArrayList<>();
        
        // 1. 查询用户已收货、已完成或已退款的订单
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getUserId, userId);
        orderWrapper.in(OrderInfo::getStatus, "received", "completed", "refunded");
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        // 2. 遍历订单，获取未评价的商品
        int idCounter = 1;
        for (OrderInfo order : orders) {
            // 获取订单商品
            LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.eq(OrderItem::getOrderId, order.getId());
            List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
            
            for (OrderItem item : items) {
                // 检查是否已评价
                LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
                reviewWrapper.eq(Review::getOrderId, order.getId());
                reviewWrapper.eq(Review::getTargetId, item.getProductId());
                reviewWrapper.eq(Review::getTargetType, "product");
                reviewWrapper.eq(Review::getUserId, userId);
                
                Long count = reviewMapper.selectCount(reviewWrapper);
                
                // 如果未评价，添加到列表
                if (count == 0) {
                    PendingProductReviewDTO dto = new PendingProductReviewDTO();
                    dto.setId(idCounter++);
                    dto.setOrderId(order.getId());
                    dto.setOrderNo(order.getOrderNo());
                    dto.setPrice(item.getPrice());
                    dto.setQuantity(item.getQuantity());
                    dto.setCreateTime(order.getCreateTime());
                    
                    // 获取商品信息
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        dto.setProductId(product.getId());
                        dto.setProductName(product.getProductName());
                        dto.setMerchantId(product.getMerchantId());
                        
                        // 获取商品第一张图片
                        LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                        imageWrapper.eq(ProductImage::getProductId, product.getId());
                        imageWrapper.orderByAsc(ProductImage::getSortOrder);
                        List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                        if (!images.isEmpty()) {
                            dto.setProductImage(images.get(0).getImageUrl());
                        }
                        
                        // 获取商家信息
                        SysUser merchant = sysUserMapper.selectById(product.getMerchantId());
                        if (merchant != null) {
                            dto.setMerchant(merchant.getName());
                        }
                    }
                    
                    result.add(dto);
                }
            }
        }
        
        return result;
    }
    
    // 获取用户待评价的商家列表
    public List<PendingMerchantReviewDTO> getPendingMerchantReviews(Integer userId) {
        List<PendingMerchantReviewDTO> result = new ArrayList<>();
        
        // 1. 查询用户已收货、已完成或已退款的订单
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getUserId, userId);
        orderWrapper.in(OrderInfo::getStatus, "received", "completed", "refunded");
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        // 2. 遍历订单，获取未评价的商家
        int idCounter = 1;
        for (OrderInfo order : orders) {
            // 检查是否已评价该订单的商家
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(Review::getOrderId, order.getId());
            reviewWrapper.eq(Review::getTargetId, order.getMerchantId());
            reviewWrapper.eq(Review::getTargetType, "merchant");
            reviewWrapper.eq(Review::getUserId, userId);
            
            Long count = reviewMapper.selectCount(reviewWrapper);
            
            // 如果未评价，添加到列表
            if (count == 0) {
                PendingMerchantReviewDTO dto = new PendingMerchantReviewDTO();
                dto.setId(idCounter++);
                dto.setOrderId(order.getId());
                dto.setOrderNo(order.getOrderNo());
                dto.setMerchantId(order.getMerchantId());
                dto.setCreateTime(order.getCreateTime());
                
                // 获取商家信息
                SysUser merchant = sysUserMapper.selectById(order.getMerchantId());
                if (merchant != null) {
                    // 获取商家店铺名
                    LambdaQueryWrapper<MerchantInfo> merchantInfoWrapper = new LambdaQueryWrapper<>();
                    merchantInfoWrapper.eq(MerchantInfo::getUserId, order.getMerchantId());
                    MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantInfoWrapper);
                    
                    if (merchantInfo != null) {
                        dto.setMerchantName(merchantInfo.getShopName());
                    } else {
                        dto.setMerchantName(merchant.getName() + "的店铺");
                    }
                    dto.setMerchantRealName(merchant.getName());
                }
                
                // 获取订单商品信息
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
                
                List<String> productNames = new ArrayList<>();
                String firstProductImage = null;
                
                for (OrderItem item : items) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        productNames.add(product.getProductName());
                        
                        // 获取商品第一张图片
                        if (firstProductImage == null) {
                            LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                            imageWrapper.eq(ProductImage::getProductId, product.getId());
                            imageWrapper.orderByAsc(ProductImage::getSortOrder);
                            List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                            if (!images.isEmpty()) {
                                firstProductImage = images.get(0).getImageUrl();
                            }
                        }
                    }
                }
                
                dto.setProductNames(productNames);
                dto.setProductImage(firstProductImage);
                
                result.add(dto);
            }
        }
        
        return result;
    }
    
    public List<PendingBuyerReviewDTO> getPendingBuyerReviews(Integer merchantId) {
        List<PendingBuyerReviewDTO> result = new ArrayList<>();
        
        // 查询商家已收货、已完成或已退款的订单
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getMerchantId, merchantId);
        orderWrapper.in(OrderInfo::getStatus, "received", "completed", "refunded");
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);
        
        int idCounter = 1;
        for (OrderInfo order : orders) {
            LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
            reviewWrapper.eq(Review::getOrderId, order.getId());
            reviewWrapper.eq(Review::getTargetId, order.getUserId());
            reviewWrapper.eq(Review::getTargetType, "buyer");
            reviewWrapper.eq(Review::getUserId, merchantId);
            
            Long count = reviewMapper.selectCount(reviewWrapper);
            
            if (count == 0) {
                PendingBuyerReviewDTO dto = new PendingBuyerReviewDTO();
                dto.setId(idCounter++);
                dto.setOrderId(order.getId());
                dto.setOrderNo(order.getOrderNo());
                dto.setBuyerId(order.getUserId());
                dto.setCreateTime(order.getCreateTime());
                
                SysUser buyer = sysUserMapper.selectById(order.getUserId());
                if (buyer != null) {
                    dto.setBuyerName(buyer.getName());
                    dto.setBuyerPhone(buyer.getPhone());
                }
                
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
                
                List<String> productNames = new ArrayList<>();
                String firstProductImage = null;
                
                for (OrderItem item : items) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        productNames.add(product.getProductName());
                        
                        if (firstProductImage == null) {
                            LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                            imageWrapper.eq(ProductImage::getProductId, product.getId());
                            imageWrapper.orderByAsc(ProductImage::getSortOrder);
                            List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                            if (!images.isEmpty()) {
                                firstProductImage = images.get(0).getImageUrl();
                            }
                        }
                    }
                }
                
                dto.setProductNames(productNames);
                dto.setProductImage(firstProductImage);
                
                result.add(dto);
            }
        }
        
        return result;
    }
    
    public List<ReviewedBuyerDTO> getReviewedBuyers(Integer merchantId) {
        List<ReviewedBuyerDTO> result = new ArrayList<>();
        
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getUserId, merchantId);
        reviewWrapper.eq(Review::getTargetType, "buyer");
        reviewWrapper.orderByDesc(Review::getCreateTime);
        List<Review> reviews = reviewMapper.selectList(reviewWrapper);
        
        int idCounter = 1;
        for (Review review : reviews) {
            ReviewedBuyerDTO dto = new ReviewedBuyerDTO();
            dto.setId(idCounter++);
            dto.setOrderId(review.getOrderId());
            dto.setBuyerId(review.getTargetId());
            dto.setRating(review.getRating());
            dto.setContent(review.getContent());
            dto.setReviewTime(review.getCreateTime());
            
            OrderInfo order = orderInfoMapper.selectById(review.getOrderId());
            if (order != null) {
                dto.setOrderNo(order.getOrderNo());
                dto.setCreateTime(order.getCreateTime());
            }
            
            SysUser buyer = sysUserMapper.selectById(review.getTargetId());
            if (buyer != null) {
                dto.setBuyerName(buyer.getName());
                dto.setBuyerPhone(buyer.getPhone());
            }
            
            if (order != null) {
                LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
                itemWrapper.eq(OrderItem::getOrderId, order.getId());
                List<OrderItem> items = orderItemMapper.selectList(itemWrapper);
                
                List<String> productNames = new ArrayList<>();
                String firstProductImage = null;
                
                for (OrderItem item : items) {
                    Product product = productMapper.selectById(item.getProductId());
                    if (product != null) {
                        productNames.add(product.getProductName());
                        
                        if (firstProductImage == null) {
                            LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                            imageWrapper.eq(ProductImage::getProductId, product.getId());
                            imageWrapper.orderByAsc(ProductImage::getSortOrder);
                            List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                            if (!images.isEmpty()) {
                                firstProductImage = images.get(0).getImageUrl();
                            }
                        }
                    }
                }
                
                dto.setProductNames(productNames);
                dto.setProductImage(firstProductImage);
            }
            
            result.add(dto);
        }
        
        return result;
    }
}