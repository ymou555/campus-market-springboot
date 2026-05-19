package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.campusmarket.dto.FavoriteProductVO;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductFavorite;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.ProductFavoriteMapper;
import org.example.campusmarket.mapper.ProductImageMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FavoriteService {
    
    @Autowired
    private ProductFavoriteMapper productFavoriteMapper;
    
    @Autowired
    private ProductMapper productMapper;
    
    @Autowired
    private ProductImageMapper productImageMapper;
    
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    
    public void addFavorite(Integer userId, Integer productId) {
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId);
        wrapper.eq(ProductFavorite::getProductId, productId);
        
        ProductFavorite existingFavorite = productFavoriteMapper.selectOne(wrapper);
        
        if (existingFavorite != null) {
            throw new RuntimeException("该商品已在收藏列表中");
        }
        
        ProductFavorite favorite = new ProductFavorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreateTime(new Date());
        
        productFavoriteMapper.insert(favorite);
    }
    
    public void removeFavorite(Integer userId, Integer productId) {
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId);
        wrapper.eq(ProductFavorite::getProductId, productId);
        
        int deleted = productFavoriteMapper.delete(wrapper);
        
        if (deleted == 0) {
            throw new RuntimeException("收藏记录不存在");
        }
    }
    
    public List<FavoriteProductVO> getUserFavorites(Integer userId) {
        List<FavoriteProductVO> result = new ArrayList<>();
        
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId);
        wrapper.orderByDesc(ProductFavorite::getCreateTime);
        
        List<ProductFavorite> favorites = productFavoriteMapper.selectList(wrapper);
        
        for (ProductFavorite favorite : favorites) {
            FavoriteProductVO vo = new FavoriteProductVO();
            vo.setId(favorite.getId());
            vo.setProductId(favorite.getProductId());
            vo.setFavoriteTime(favorite.getCreateTime());
            
            Product product = productMapper.selectById(favorite.getProductId());
            if (product != null) {
                vo.setProductName(product.getProductName());
                vo.setPrice(product.getDiscountPrice());
                vo.setStock(product.getStock());
                vo.setStatus(product.getStatus());
                vo.setMerchantId(product.getMerchantId());
                
                LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                imageWrapper.eq(ProductImage::getProductId, product.getId());
                imageWrapper.orderByAsc(ProductImage::getSortOrder);
                List<ProductImage> images = productImageMapper.selectList(imageWrapper);
                if (!images.isEmpty()) {
                    vo.setProductImage(images.get(0).getImageUrl());
                }
                
                LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
                merchantWrapper.eq(MerchantInfo::getUserId, product.getMerchantId());
                List<MerchantInfo> merchants = merchantInfoMapper.selectList(merchantWrapper);
                if (!merchants.isEmpty()) {
                    vo.setMerchant(merchants.get(0).getShopName());
                }
            }
            
            result.add(vo);
        }
        
        return result;
    }
    
    public boolean isFavorited(Integer userId, Integer productId) {
        LambdaQueryWrapper<ProductFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductFavorite::getUserId, userId);
        wrapper.eq(ProductFavorite::getProductId, productId);
        
        Long count = productFavoriteMapper.selectCount(wrapper);
        return count > 0;
    }
}
