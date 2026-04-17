package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.mapper.CartMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class CartService {
    @Autowired
    private CartMapper cartMapper;

    // 添加商品到购物车
    public void addToCart(Integer userId, Integer productId, Integer quantity) {
        // 检查是否已存在
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getProductId, productId);
        Cart existingCart = cartMapper.selectOne(wrapper);

        if (existingCart != null) {
            // 更新数量
            existingCart.setQuantity(existingCart.getQuantity() + quantity);
            existingCart.setUpdateTime(new Date());
            cartMapper.updateById(existingCart);
        } else {
            // 新增
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

    // 获取用户购物车列表
    public List<Cart> getCartList(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        return cartMapper.selectList(wrapper);
    }

    // 更新购物车商品数量
    public void updateQuantity(Integer cartId, Integer quantity) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId);
        wrapper.set(Cart::getQuantity, quantity);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    // 更新购物车商品选中状态
    public void updateSelectedStatus(Integer cartId, Boolean isSelected) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getId, cartId);
        wrapper.set(Cart::getIsSelected, isSelected);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    // 批量更新购物车商品选中状态
    public void batchUpdateSelectedStatus(Integer userId, Boolean isSelected) {
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.set(Cart::getIsSelected, isSelected);
        wrapper.set(Cart::getUpdateTime, new Date());
        cartMapper.update(null, wrapper);
    }

    // 删除购物车商品
    public void deleteCartItem(Integer cartId) {
        cartMapper.deleteById(cartId);
    }

    // 清空购物车
    public void clearCart(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        cartMapper.delete(wrapper);
    }

    // 获取选中的购物车商品
    public List<Cart> getSelectedCartItems(Integer userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        wrapper.eq(Cart::getIsSelected, true);
        return cartMapper.selectList(wrapper);
    }
}