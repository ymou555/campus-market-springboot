package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.OrderItem;
import org.example.campusmarket.service.CartService;
import org.example.campusmarket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;

    // 创建订单
    @PostMapping("/create")
    public Map<String, Object> createOrder(
            @RequestParam Integer userId,
            @RequestParam Integer pointsDeducted) {
        // 获取选中的购物车商品
        List<Cart> selectedItems = cartService.getSelectedCartItems(userId);
        if (selectedItems.isEmpty()) {
            throw new RuntimeException("请选择要购买的商品");
        }

        OrderInfo order = orderService.createOrder(userId, selectedItems, pointsDeducted);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "订单创建成功");
        result.put("order", order);
        return result;
    }

    // 支付订单
    @PostMapping("/pay")
    public Map<String, Object> payOrder(@RequestParam Integer orderId) {
        orderService.payOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "支付成功");
        return result;
    }

    // 商家发货
    @PostMapping("/ship")
    public Map<String, Object> shipOrder(@RequestParam Integer orderId) {
        orderService.shipOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发货成功");
        return result;
    }

    // 用户确认收货
    @PostMapping("/receive")
    public Map<String, Object> receiveOrder(@RequestParam Integer orderId) {
        orderService.receiveOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "收货成功");
        return result;
    }

    // 申请退货
    @PostMapping("/refund")
    public Map<String, Object> applyRefund(
            @RequestParam Integer orderId,
            @RequestParam String reason) {
        orderService.applyRefund(orderId, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退货申请成功");
        return result;
    }

    // 获取订单详情
    @GetMapping("/detail")
    public Map<String, Object> getOrderDetail(@RequestParam Integer orderId) {
        OrderInfo order = orderService.getOrderById(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("order", order);
        result.put("items", items);
        return result;
    }

    // 获取用户订单列表
    @GetMapping("/user/list")
    public Map<String, Object> getUserOrders(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer userId,
            @RequestParam(required = false) String status) {
        Page<OrderInfo> orderPage = orderService.getUserOrders(page, size, userId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", orderPage.getRecords());
        result.put("total", orderPage.getTotal());
        return result;
    }

    // 获取商家订单列表
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantOrders(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String status) {
        Page<OrderInfo> orderPage = orderService.getMerchantOrders(page, size, merchantId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", orderPage.getRecords());
        result.put("total", orderPage.getTotal());
        return result;
    }
}