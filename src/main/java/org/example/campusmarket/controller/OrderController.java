package org.example.campusmarket.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.MerchantCartVO;
import org.example.campusmarket.dto.OrderDetailVO;
import org.example.campusmarket.dto.OrderListVO;
import org.example.campusmarket.dto.SelectedCartItemVO;
import org.example.campusmarket.entity.Cart;
import org.example.campusmarket.entity.OrderDelivery;
import org.example.campusmarket.entity.OrderInfo;
import org.example.campusmarket.entity.OrderItem;
import org.example.campusmarket.entity.OrderReturnRequest;
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

    // 创建订单（支持配送信息和立即购买）
    @PostMapping("/create")
    public Map<String, Object> createOrder(
            @RequestParam Integer userId,
            @RequestParam Integer pointsDeducted,
            @RequestParam(required = false) Double buyerOfferPrice,
            @RequestParam String deliveryType,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) String receiverPhone,
            @RequestParam(required = false) String receiverAddress,
            @RequestParam(required = false) String remark,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer quantity) {

        OrderDelivery delivery = new OrderDelivery();
        delivery.setDeliveryType(deliveryType);
        if ("express".equals(deliveryType)) {
            delivery.setReceiverName(receiverName);
            delivery.setReceiverPhone(receiverPhone);
            delivery.setReceiverAddress(receiverAddress);
        }
        delivery.setRemark(remark);

        OrderInfo order;
        if (productId != null && quantity != null) {
            order = orderService.createOrderDirect(userId, productId, quantity, pointsDeducted, buyerOfferPrice, delivery);
        } else {
            List<Cart> selectedItems = cartService.getSelectedCartItems(userId);
            if (selectedItems.isEmpty()) {
                throw new RuntimeException("请选择要购买的商品");
            }
            order = orderService.createOrder(userId, selectedItems, pointsDeducted, buyerOfferPrice, delivery);
        }

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
        OrderDetailVO detail = orderService.getOrderDetail(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", detail);
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

    // 获取用户最近一次使用的快递地址
    @GetMapping("/last-address")
    public Map<String, Object> getLastUsedAddress(@RequestParam Integer userId) {
        OrderDelivery delivery = orderService.getLastUsedExpressAddress(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        if (delivery != null) {
            Map<String, Object> address = new HashMap<>();
            address.put("receiverName", delivery.getReceiverName());
            address.put("receiverPhone", delivery.getReceiverPhone());
            address.put("receiverAddress", delivery.getReceiverAddress());
            result.put("data", address);
        } else {
            result.put("data", null);
        }
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

    // 商家确认面交发货
    @PostMapping("/meet/ship")
    public Map<String, Object> shipFaceToFaceOrder(@RequestParam Integer orderId) {
        orderService.shipFaceToFaceOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "发货成功，等待买家确认收货");
        return result;
    }

    // 面交订单买家确认收货
    @PostMapping("/meet/complete")
    public Map<String, Object> completeFaceToFaceOrder(@RequestParam Integer orderId) {
        orderService.completeFaceToFaceOrder(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "收货成功");
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

    // ==================== 退货申请相关接口 ====================

    // 用户申请退货
    @PostMapping("/return/apply")
    public Map<String, Object> applyReturn(
            @RequestParam Integer orderId,
            @RequestParam String reason) {
        orderService.applyRefund(orderId, reason);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退货申请已提交，等待商家审核");
        return result;
    }

    // 商家同意退货申请
    @PostMapping("/return/approve")
    public Map<String, Object> approveReturn(
            @RequestParam Integer requestId,
            @RequestParam(required = false) String auditRemark) {
        orderService.approveReturnRequest(requestId, auditRemark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已同意退货申请，退款已执行");
        return result;
    }

    // 商家拒绝退货申请
    @PostMapping("/return/reject")
    public Map<String, Object> rejectReturn(
            @RequestParam Integer requestId,
            @RequestParam(required = false) String auditRemark) {
        orderService.rejectReturnRequest(requestId, auditRemark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "已拒绝退货申请");
        return result;
    }

    // 获取商家的退货申请列表
    @GetMapping("/return/merchant/list")
    public Map<String, Object> getMerchantReturnRequests(
            @RequestParam Integer merchantId,
            @RequestParam(required = false) String status) {
        List<OrderReturnRequest> requests = orderService.getMerchantReturnRequests(merchantId, status);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", requests);
        return result;
    }

    // 获取用户的退货申请列表
    @GetMapping("/return/user/list")
    public Map<String, Object> getUserReturnRequests(@RequestParam Integer userId) {
        List<OrderReturnRequest> requests = orderService.getUserReturnRequests(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", requests);
        return result;
    }

    // 获取退货申请详情
    @GetMapping("/return/detail")
    public Map<String, Object> getReturnRequestDetail(@RequestParam Integer requestId) {
        OrderReturnRequest request = orderService.getReturnRequestById(requestId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", request);
        return result;
    }

    // 获取订单的退货申请状态
    @GetMapping("/return/order")
    public Map<String, Object> getReturnRequestByOrder(@RequestParam Integer orderId) {
        OrderReturnRequest request = orderService.getReturnRequestByOrderId(orderId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", request);
        return result;
    }
}
