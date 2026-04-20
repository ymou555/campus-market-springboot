package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.dto.CartItemVO;
import org.example.campusmarket.dto.MerchantCartVO;
import org.example.campusmarket.dto.SelectedCartItemVO;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.entity.MerchantInfo;
import org.example.campusmarket.entity.Product;
import org.example.campusmarket.entity.ProductImage;
import org.example.campusmarket.entity.SysUser;
import org.example.campusmarket.mapper.CartMapper;
import org.example.campusmarket.mapper.MerchantInfoMapper;
import org.example.campusmarket.mapper.ProductImageMapper;
import org.example.campusmarket.mapper.ProductMapper;
import org.example.campusmarket.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private ProductImageMapper productImageMapper;

    public void addToCart(Integer userId, Integer productId, Integer quantity) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getProductId, productId);
        Cart existingCart = cartMapper.selectOne(wrapper);

        if (existingCart != null) {
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            existingCart.setUpdateTime(new Date());
            cartMapper.updateById(existingCart);
        } else {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(quantity);
            cart.setIsSelected(true);
            cart.setCreateTime(new Date());
            cart.setUpdateTime(new Date());
            cartMapper.insert(cart);
        }
    }

    public List<Cart> getCartList(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        return cartMapper.selectList(wrapper);
    }

    public List<MerchantCartVO> getCartListGroupedByMerchant(Integer userId) {
        LambdaQueryWrapper<Cart> cartWrapper = new LambdaQueryWrapper<>();
        cartWrapper.eq(Cart::getUserId, userId);
        List<Cart> cartList = cartMapper.selectList(cartWrapper);

        if (cartList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> productIds = cartList.stream()
                .map(Cart::getProductId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.in(Product::getId, productIds);
        List<Product> products = productMapper.selectList(productWrapper);

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Integer, List<Cart>> cartsByMerchant = cartList.stream()
                .collect(Collectors.groupingBy(cart -> {
                    Product product = productMap.get(cart.getProductId());
                    return product != null ? product.getMerchantId() : 0;
                }));

        List<MerchantCartVO> result = new ArrayList<>();

        for (Map.Entry<Integer, List<Cart>> entry : cartsByMerchant.entrySet()) {
            Integer merchantId = entry.getKey();
            List<Cart> merchantCarts = entry.getValue();

            MerchantCartVO merchantCartVO = new MerchantCartVO();
            merchantCartVO.setMerchantId(merchantId);

            SysUser merchant = sysUserMapper.selectById(merchantId);
            if (merchant != null) {
                merchantCartVO.setMerchantName(merchant.getName());
            }

            LambdaQueryWrapper<MerchantInfo> merchantInfoWrapper = new LambdaQueryWrapper<>();
            merchantInfoWrapper.eq(MerchantInfo::getUserId, merchantId);
            MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantInfoWrapper);
            if (merchantInfo != null && merchantInfo.getShopName() != null) {
                merchantCartVO.setMerchantName(merchantInfo.getShopName());
            }

            List<CartItemVO> productVOs = new ArrayList<>();
            for (Cart cart : merchantCarts) {
                Product product = productMap.get(cart.getProductId());
                if (product != null) {
                    CartItemVO itemVO = new CartItemVO();
                    itemVO.setCartId(cart.getId());
                    itemVO.setProductId(product.getId());
                    itemVO.setProductName(product.getProductName());
                    itemVO.setPrice(BigDecimal.valueOf(product.getDiscountPrice()));
                    itemVO.setQuantity(cart.getQuantity());
                    itemVO.setIsSelected(cart.getIsSelected());
                    itemVO.setStock(product.getStock());

                    LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                    imageWrapper.eq(ProductImage::getProductId, product.getId());
                    imageWrapper.orderByAsc(ProductImage::getSortOrder);
                    imageWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
                    ProductImage firstImage = productImageMapper.selectOne(imageWrapper);
                    if (firstImage != null) {
                        itemVO.setProductImage(firstImage.getImageUrl());
                    }

                    productVOs.add(itemVO);
                }
            }

            merchantCartVO.setProducts(productVOs);
            result.add(merchantCartVO);
        }

        return result;
    }

    public void updateQuantity(Integer cartId, Integer quantity) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId);
        wrapper.set(Cart::getQuantity, quantity);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    public void updateSelectedStatus(Integer cartId, Boolean isSelected) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId);
        wrapper.set(Cart::getIsSelected, isSelected);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    public void batchUpdateSelectedStatus(Integer userId, Boolean isSelected) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.set(Cart::getIsSelected, isSelected);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    public void batchUpdateSelectedStatusByMerchant(Integer userId, Integer merchantId, Boolean isSelected) {
        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.eq(Product::getMerchantId, merchantId);
        List<Product> products = productMapper.selectList(productWrapper);

        if (products.isEmpty()) {
            return;
        }

        List<Integer> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        LambdaUpdateWrapper<Cart> cartWrapper = new LambdaUpdateWrapper<>();
        cartWrapper.eq(Cart::getUserId, userId);
        cartWrapper.in(Cart::getProductId, productIds);
        cartWrapper.set(Cart::getIsSelected, isSelected);
        cartWrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, cartWrapper);
    }

    public void deleteCartItem(Integer cartId) {
        cartMapper.deleteById(cartId);
    }

    public void clearCart(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        cartMapper.delete(wrapper);
    }

    public void deleteSelectedCartItems(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getIsSelected, true);
        cartMapper.delete(wrapper);
    }

    public List<Cart> getSelectedCartItems(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getIsSelected, true);
        return cartMapper.selectList(wrapper);
    }

    public List<SelectedCartItemVO> getSelectedCartItemsWithDetails(Integer userId) {
        LambdaQueryWrapper<Cart> cartWrapper = new LambdaQueryWrapper<>();
        cartWrapper.eq(Cart::getUserId, userId);
        cartWrapper.eq(Cart::getIsSelected, true);
        List<Cart> cartList = cartMapper.selectList(cartWrapper);

        if (cartList.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> productIds = cartList.stream()
                .map(Cart::getProductId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.in(Product::getId, productIds);
        List<Product> products = productMapper.selectList(productWrapper);

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<SelectedCartItemVO> result = new ArrayList<>();

        for (Cart cart : cartList) {
            Product product = productMap.get(cart.getProductId());
            if (product != null) {
                SelectedCartItemVO itemVO = new SelectedCartItemVO();
                itemVO.setCartId(cart.getId());
                itemVO.setProductId(product.getId());
                itemVO.setProductName(product.getProductName());
                itemVO.setPrice(BigDecimal.valueOf(product.getDiscountPrice()));
                itemVO.setQuantity(cart.getQuantity());
                itemVO.setMerchantId(product.getMerchantId());

                SysUser merchant = sysUserMapper.selectById(product.getMerchantId());
                if (merchant != null) {
                    itemVO.setMerchantName(merchant.getName());
                }

                LambdaQueryWrapper<MerchantInfo> merchantInfoWrapper = new LambdaQueryWrapper<>();
                merchantInfoWrapper.eq(MerchantInfo::getUserId, product.getMerchantId());
                MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantInfoWrapper);
                if (merchantInfo != null && merchantInfo.getShopName() != null) {
                    itemVO.setMerchantName(merchantInfo.getShopName());
                }

                LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                imageWrapper.eq(ProductImage::getProductId, product.getId());
                imageWrapper.orderByAsc(ProductImage::getSortOrder);
                imageWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
                ProductImage firstImage = productImageMapper.selectOne(imageWrapper);
                if (firstImage != null) {
                    itemVO.setProductImage(firstImage.getImageUrl());
                }

                result.add(itemVO);
            }
        }

        return result;
    }
}
