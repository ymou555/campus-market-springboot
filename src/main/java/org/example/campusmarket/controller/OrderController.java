package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.MerchantCartVO;
import org.example.campusmarket.dto.OrderListVO;
import org.example.campusmarket.dto.SelectedCartItemVO;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.entity.OrderDelivery;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.OrderItem;
import org.example.campusmarket.service.CartService;
import org.example.campusmarket.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    // 创建订单（支持配送信息）
    @PostMapping("/create")
    public Map<String, Object> createOrder(
            @RequestParam Integer userId,
            @RequestParam Integer pointsDeducted,
            @RequestParam(required = false) Double buyerOfferPrice,
            @RequestParam String deliveryType,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String receiverAddress,
            @RequestParam(required = false) String remark) {
        List<Cart> selectedItems = cartService.getSelectedCartItems(userId);
        if (selectedItems.isEmpty()) {
            throw new RuntimeException("请选择要购买的商品");
        }

        OrderDelivery delivery = new OrderDelivery();
        delivery.setDeliveryType(deliveryType);
        if ("express".equals(deliveryType)) {
            delivery.setReceiverName(receiverName);
            delivery.setReceiverPhone(receiverPhone);
            delivery.setReceiverAddress(receiverAddress);
        }
        delivery.setRemark(remark);

        OrderInfo order = orderService.createOrder(userId, selectedItems, pointsDeducted, buyerOfferPrice, delivery);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", buyerOfferPrice != null ? "议价订单已创建，等待商家确认" : "订单创建成功");
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

    // 商家发货（快递）- 填写快递单号
    @PostMapping("/ship")
    public Map<String, Object> shipOrder(
            @RequestParam Integer orderId,
            @RequestParam String trackingNumber) {
        orderService.shipOrderWithTracking(orderId, trackingNumber);
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

    // 用户取消订单
    @PostMapping("/cancel")
    public Map<String, Object> cancelOrder(@RequestParam Integer orderId) {
        orderService.cancelOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "订单已取消");
        return result;
    }

    // 商家接受议价
    @PostMapping("/accept-bargaining")
    public Map<String, Object> acceptBargaining(@RequestParam Integer orderId) {
        orderService.acceptBargaining(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已接受议价，等待买家付款");
        return result;
    }

    // 商家拒绝议价
    @PostMapping("/reject-bargaining")
    public Map<String, Object> rejectBargaining(@RequestParam Integer orderId) {
        orderService.rejectBargaining(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已拒绝议价，订单已取消");
        return result;
    }

    // 获取订单详情（含配送信息）
    @GetMapping("/detail")
    public Map<String, Object> getOrderDetail(@RequestParam Integer orderId) {
        OrderInfo order = orderService.getOrderById(orderId);
        List<OrderItem> items = orderService.getOrderItems(orderId);
        OrderDelivery delivery = orderService.getOrderDelivery(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("order", order);
        result.put("items", items);
        result.put("delivery", delivery);
        return result;
    }

    // 获取配送信息
    @GetMapping("/delivery")
    public Map<String, Object> getOrderDelivery(@RequestParam Integer orderId) {
        OrderDelivery delivery = orderService.getOrderDelivery(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", delivery);
        return result;
    }

    // 商家填写面交信息
    @PostMapping("/meet/set")
    public Map<String, Object> setMeetInfo(
            @RequestParam Integer orderId,
            @RequestParam String meetTime,
            @RequestParam String meetLocation) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time;
        try {
            time = sdf.parse(meetTime);
        } catch (ParseException e) {
            throw new RuntimeException("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
        orderService.setMeetInfo(orderId, time, meetLocation);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "面交信息已设置，等待买家确认");
        return result;
    }

    // 买家确认面交信息
    @PostMapping("/meet/confirm")
    public Map<String, Object> confirmMeetInfo(@RequestParam Integer orderId) {
        orderService.confirmMeetInfo(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "面交信息已确认");
        return result;
    }

    // 买家拒绝面交信息并提出新建议
    @PostMapping("/meet/reject")
    public Map<String, Object> rejectMeetInfo(
            @RequestParam Integer orderId,
            @RequestParam String newMeetTime,
            @RequestParam String newMeetLocation) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time;
        try {
            time = sdf.parse(newMeetTime);
        } catch (ParseException e) {
            throw new RuntimeException("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
        orderService.rejectMeetInfo(orderId, time, newMeetLocation);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已拒绝商家建议，等待商家重新设置");
        return result;
    }

    // 商家重新设置面交信息（买家拒绝后）
    @PostMapping("/meet/resubmit")
    public Map<String, Object> resubmitMeetInfo(
            @RequestParam Integer orderId,
            @RequestParam String meetTime,
            @RequestParam String meetLocation) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date time;
        try {
            time = sdf.parse(meetTime);
        } catch (ParseException e) {
            throw new RuntimeException("时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
        }
        orderService.resubmitMeetInfo(orderId, time, meetLocation);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "面交信息已重新设置，等待买家确认");
        return result;
    }

    // 面交订单确认完成
    @PostMapping("/meet/complete")
    public Map<String, Object> completeFaceToFaceOrder(@RequestParam Integer orderId) {
        orderService.completeFaceToFaceOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "订单已完成");
        return result;
    }

    // 获取用户订单列表
    @GetMapping("/user/list")
    public Map<String, Object> getUserOrders(
            @RequestParam Integer userId,
            @RequestParam(required = false) String status) {
        List<OrderListVO> orders = orderService.getUserOrders(userId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", orders);
        return result;
    }

    // 获取商家订单列表
    @GetMapping("/merchant/list")
    public Map<String, Object> getMerchantOrders(
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String status) {
        List<OrderInfo> orders = orderService.getMerchantOrders(merchantId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", orders);
        return result;
    }
}
