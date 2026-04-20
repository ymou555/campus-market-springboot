package org.example.campusmarket.controller;

import org.example.campusmarket.dto.MerchantCartVO;
import org.example.campusmarket.dto.SelectedCartItemVO;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public Map<String, Object> addToCart(
            @RequestParam Integer userId,
            @RequestParam Integer productId,
            @RequestParam Integer quantity) {
        cartService.addToCart(userId, productId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "添加成功");
        return result;
    }

    @GetMapping("/list")
    public Map<String, Object> getCartList(@RequestParam Integer userId) {
        List<MerchantCartVO> cartList = cartService.getCartListGroupedByMerchant(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", cartList);
        return result;
    }

    // 更新购物车商品数量
    @PutMapping("/update-quantity")
    public Map<String, Object> updateQuantity(
            @RequestParam Integer cartId,
            @RequestParam Integer quantity) {
        cartService.updateQuantity(cartId, quantity);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 更新购物车商品选中状态
    @PutMapping("/update-selected")
    public Map<String, Object> updateSelectedStatus(
            @RequestParam Integer cartId,
            @RequestParam Boolean isSelected) {
        cartService.updateSelectedStatus(cartId, isSelected);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 批量更新购物车商品选中状态
    @PutMapping("/batch-update-selected")
    public Map<String, Object> batchUpdateSelectedStatus(
            @RequestParam Integer userId,
            @RequestParam Boolean isSelected) {
        cartService.batchUpdateSelectedStatus(userId, isSelected);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 批量更新购物车某商家的所有商品选中状态
    @PutMapping("/batch-update-selected-by-merchant")
    public Map<String, Object> batchUpdateSelectedStatusByMerchant(
            @RequestParam Integer userId,
            @RequestParam Integer merchantId,
            @RequestParam Boolean isSelected) {
        cartService.batchUpdateSelectedStatusByMerchant(userId, merchantId, isSelected);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "更新成功");
        return result;
    }

    // 删除购物车商品
    @DeleteMapping("/delete")
    public Map<String, Object> deleteCartItem(@RequestParam Integer cartId) {
        cartService.deleteCartItem(cartId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    // 清空购物车
    @DeleteMapping("/clear")
    public Map<String, Object> clearCart(@RequestParam Integer userId) {
        cartService.clearCart(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "清空成功");
        return result;
    }

    // 删除选中的购物车商品
    @DeleteMapping("/delete-selected")
    public Map<String, Object> deleteSelectedCartItems(@RequestParam Integer userId) {
        cartService.deleteSelectedCartItems(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "删除成功");
        return result;
    }

    // 获取选中的购物车商品
    @GetMapping("/selected")
    public Map<String, Object> getSelectedCartItems(@RequestParam Integer userId) {
        List<SelectedCartItemVO> selectedItems = cartService.getSelectedCartItemsWithDetails(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", selectedItems);
        return result;
    }
}