package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.dto.OrderDetailDeliveryVO;
import org.example.campusmarket.dto.OrderDetailProductVO;
import org.example.campusmarket.dto.OrderDetailVO;
import org.example.campusmarket.dto.OrderListVO;
import org.example.campusmarket.dto.OrderProductVO;
import org.example.campusmarket.entity.*;
import org.example.campusmarket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService {
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private OrderStatusLogMapper orderStatusLogMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private OrderDeliveryMapper orderDeliveryMapper;
    @Autowired
    private PointsService pointsService;
    @Autowired
    private ProductImageMapper productImageMapper;
    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private WalletService walletService;
    @Autowired
    private OrderReturnRequestMapper orderReturnRequestMapper;
    @Autowired
    private EscrowAccountMapper escrowAccountMapper;
    @Autowired
    private MerchantInfoMapper merchantInfoMapper;
    @Autowired
    private MerchantLevelMapper merchantLevelMapper;
    @Autowired
    private BlacklistService blacklistService;

    // 创建订单
    @Transactional
    public OrderInfo createOrder(Integer userId, List<Cart> cartItems, Integer pointsDeducted) {
        return createOrder(userId, cartItems, pointsDeducted, null, null);
    }

    // 创建订单（支持议价）
    @Transactional
    public OrderInfo createOrder(Integer userId, List<Cart> cartItems, Integer pointsDeducted, Double buyerOfferPrice) {
        return createOrder(userId, cartItems, pointsDeducted, buyerOfferPrice, null);
    }

    // 创建订单（立即购买，直接购买商品）
    @Transactional
    public OrderInfo createOrderDirect(Integer userId, Integer productId, Integer quantity, Integer pointsDeducted, Double buyerOfferPrice, OrderDelivery delivery) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (product.getStock() < quantity) {
            throw new RuntimeException("商品库存不足");
        }
        
        // 检查用户是否被拉黑
        Integer merchantId = product.getMerchantId();
        if (!blacklistService.canUserBuyFromMerchant(userId, merchantId)) {
            throw new RuntimeException("您已被该商家或平台拉黑，无法购买该商家的商品");
        }

        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        double totalAmount = product.getDiscountPrice() * quantity;

        boolean isBargaining = buyerOfferPrice != null && buyerOfferPrice > 0;

        double actualAmount;
        String orderStatus;

        if (isBargaining) {
            actualAmount = totalAmount;
            orderStatus = "bargaining";
        } else {
            actualAmount = totalAmount - (pointsDeducted / 100.0);
            if (actualAmount < 0) actualAmount = 0;
            orderStatus = "pending";
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(totalAmount);
        order.setActualAmount(actualAmount);
        order.setPointsDeducted(isBargaining ? 0 : pointsDeducted);
        order.setBuyerOfferPrice(buyerOfferPrice);
        order.setStatus(orderStatus);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderInfoMapper.insert(order);

        OrderItem item = new OrderItem();
        item.setOrderId(order.getId());
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setPrice(product.getDiscountPrice());
        orderItemMapper.insert(item);

        if (!isBargaining) {
            LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
            productWrapper.eq(Product::getId, productId);
            productWrapper.set(Product::getStock, product.getStock() - quantity);
            if (product.getStock() - quantity <= 0) {
                productWrapper.set(Product::getStatus, "sold_out");
            }
            productMapper.update(null, productWrapper);
        }

        if (delivery != null) {
            delivery.setOrderId(order.getId());
            delivery.setCreateTime(new Date());
            delivery.setUpdateTime(new Date());
            if ("face_to_face".equals(delivery.getDeliveryType())) {
                delivery.setMeetStatus("pending_seller");
            }
            orderDeliveryMapper.insert(delivery);
        }

        recordOrderStatusChange(order.getId(), null, orderStatus, "系统");

        return order;
    }

    // 创建订单（支持配送信息）
    @Transactional
    public OrderInfo createOrder(Integer userId, List<Cart> cartItems, Integer pointsDeducted, Double buyerOfferPrice, OrderDelivery delivery) {
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);

        double totalAmount = 0;
        Integer merchantId = null;
        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            totalAmount += product.getDiscountPrice() * cart.getQuantity();
            if (merchantId == null) {
                merchantId = product.getMerchantId();
            }
        }
        
        // 检查用户是否被拉黑
        if (!blacklistService.canUserBuyFromMerchant(userId, merchantId)) {
            throw new RuntimeException("您已被该商家或平台拉黑，无法购买该商家的商品");
        }

        boolean isBargaining = buyerOfferPrice != null && buyerOfferPrice > 0;

        double actualAmount;
        String orderStatus;

        if (isBargaining) {
            actualAmount = totalAmount;
            orderStatus = "bargaining";
        } else {
            actualAmount = totalAmount - (pointsDeducted / 100.0);
            if (actualAmount < 0) actualAmount = 0;
            orderStatus = "pending";
        }

        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(merchantId);
        order.setTotalAmount(totalAmount);
        order.setActualAmount(actualAmount);
        order.setPointsDeducted(isBargaining ? 0 : pointsDeducted);
        order.setBuyerOfferPrice(buyerOfferPrice);
        order.setStatus(orderStatus);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderInfoMapper.insert(order);

        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(cart.getProductId());
            item.setQuantity(cart.getQuantity());
            item.setPrice(product.getDiscountPrice());
            orderItemMapper.insert(item);

            if (!isBargaining) {
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, cart.getProductId());
                productWrapper.set(Product::getStock, product.getStock() - cart.getQuantity());
                if (product.getStock() - cart.getQuantity() <= 0) {
                    productWrapper.set(Product::getStatus, "sold_out");
                }
                productMapper.update(null, productWrapper);
            }
        }

        for (Cart cart : cartItems) {
            cartMapper.deleteById(cart.getId());
        }

        if (delivery != null) {
            delivery.setOrderId(order.getId());
            delivery.setCreateTime(new Date());
            delivery.setUpdateTime(new Date());
            if ("face_to_face".equals(delivery.getDeliveryType())) {
                delivery.setMeetStatus("pending_seller");
            }
            orderDeliveryMapper.insert(delivery);
        }

        recordOrderStatusChange(order.getId(), null, orderStatus, "系统");

        return order;
    }

    // 支付订单
    @Transactional
    public void payOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"pending".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许支付");
        }

        if (order.getPointsDeducted() != null && order.getPointsDeducted() > 0) {
            pointsService.deductPoints(order.getUserId(), order.getPointsDeducted(), "订单支付抵扣，订单号：" + order.getOrderNo());
        }

        if (order.getActualAmount() != null && order.getActualAmount() > 0) {
            walletService.pay(order.getUserId(), order.getActualAmount(), "订单支付，订单号：" + order.getOrderNo());

            EscrowAccount escrowAccount = new EscrowAccount();
            escrowAccount.setOrderId(orderId);
            escrowAccount.setAmount(order.getActualAmount());
            escrowAccount.setStatus("holding");
            escrowAccount.setCreateTime(new Date());
            escrowAccountMapper.insert(escrowAccount);
        }

        List<OrderItem> items = getOrderItems(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, item.getProductId());
                productWrapper.set(Product::getSalesCount, product.getSalesCount() + item.getQuantity());
                productMapper.update(null, productWrapper);
            }
        }

        if (order.getActualAmount() != null && order.getActualAmount() > 0) {
            int earnedPoints = (int) Math.round(order.getActualAmount());
            if (earnedPoints > 0) {
                pointsService.addPoints(order.getUserId(), earnedPoints, "消费获得积分，订单号：" + order.getOrderNo());
            }
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "paid");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, "pending", "paid", "用户");
    }

    // 商家发货（快递）
    @Transactional
    public void shipOrderWithTracking(Integer orderId, String trackingNumber) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"express".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是快递订单");
        }

        LambdaUpdateWrapper<OrderDelivery> deliveryWrapper = new LambdaUpdateWrapper<>();
        deliveryWrapper.eq(OrderDelivery::getOrderId, orderId);
        deliveryWrapper.set(OrderDelivery::getTrackingNumber, trackingNumber);
        deliveryWrapper.set(OrderDelivery::getUpdateTime, new Date());
        orderDeliveryMapper.update(null, deliveryWrapper);

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "shipped");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, "paid", "shipped", "商家");
    }

    // 商家发货（旧方法，保留兼容）
    public void shipOrder(Integer orderId) {
        shipOrderWithTracking(orderId, null);
    }

    // 用户确认收货
    @Transactional
    public void receiveOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许确认收货");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "received");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        settleEscrowToMerchant(orderId);

        recordOrderStatusChange(orderId, "shipped", "received", "用户");
    }

    // 申请退货（创建退货申请）
    @Transactional
    public void applyRefund(Integer orderId, String reason) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"received".equals(order.getStatus())) {
            throw new RuntimeException("只有已收货的订单才能申请退货");
        }

        LambdaQueryWrapper<OrderStatusLog> logWrapper = new LambdaQueryWrapper<>();
        logWrapper.eq(OrderStatusLog::getOrderId, orderId);
        logWrapper.eq(OrderStatusLog::getNewStatus, "received");
        logWrapper.orderByAsc(OrderStatusLog::getOperateTime);
        logWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        OrderStatusLog receiveLog = orderStatusLogMapper.selectOne(logWrapper);

        if (receiveLog != null) {
            long hoursSinceReceive = (System.currentTimeMillis() - receiveLog.getOperateTime().getTime()) / (1000 * 60 * 60);
            if (hoursSinceReceive > 24) {
                throw new RuntimeException("超过24小时无法申请退货");
            }
        }

        LambdaQueryWrapper<OrderReturnRequest> existingWrapper = new LambdaQueryWrapper<>();
        existingWrapper.eq(OrderReturnRequest::getOrderId, orderId);
        existingWrapper.in(OrderReturnRequest::getStatus, "pending", "approved");
        existingWrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        existingWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        OrderReturnRequest existingRequest = orderReturnRequestMapper.selectOne(existingWrapper);
        if (existingRequest != null) {
            throw new RuntimeException("该订单已存在待处理的退货申请");
        }

        OrderReturnRequest returnRequest = new OrderReturnRequest();
        returnRequest.setOrderId(orderId);
        returnRequest.setUserId(order.getUserId());
        returnRequest.setReturnReason(reason);
        returnRequest.setRequestTime(new Date());
        returnRequest.setStatus("pending");
        returnRequest.setRefundAmount(order.getActualAmount());
        orderReturnRequestMapper.insert(returnRequest);

        // 将订单状态改为退货中
        LambdaUpdateWrapper<OrderInfo> orderWrapper = new LambdaUpdateWrapper<>();
        orderWrapper.eq(OrderInfo::getId, orderId);
        orderWrapper.set(OrderInfo::getStatus, "returning");
        orderWrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, orderWrapper);

        recordOrderStatusChange(orderId, "received", "returning", "用户");
    }

    // 用户取消订单
    @Transactional
    public void cancelOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        String status = order.getStatus();
        if (!"pending".equals(status) && !"paid".equals(status) && !"bargaining".equals(status)) {
            throw new RuntimeException("当前订单状态不允许取消");
        }

        if ("pending".equals(status) || "paid".equals(status)) {
            List<OrderItem> items = getOrderItems(orderId);
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                    productWrapper.eq(Product::getId, item.getProductId());
                    productWrapper.set(Product::getStock, product.getStock() + item.getQuantity());
                    if ("sold_out".equals(product.getStatus()) && product.getStock() + item.getQuantity() > 0) {
                        productWrapper.set(Product::getStatus, "on_sale");
                    }
                    productMapper.update(null, productWrapper);
                }
            }
        }

        if ("paid".equals(status) && order.getPointsDeducted() != null && order.getPointsDeducted() > 0) {
            pointsService.addPoints(order.getUserId(), order.getPointsDeducted(), "取消订单退还抵扣积分，订单号：" + order.getOrderNo());
        }

        if ("paid".equals(status) && order.getActualAmount() != null && order.getActualAmount() > 0) {
            int earnedPoints = (int) Math.round(order.getActualAmount());
            if (earnedPoints > 0) {
                pointsService.deductPoints(order.getUserId(), earnedPoints, "取消订单扣除消费积分，订单号：" + order.getOrderNo());
            }
        }

        if ("paid".equals(status) && order.getActualAmount() != null && order.getActualAmount() > 0) {
            LambdaQueryWrapper<EscrowAccount> escrowWrapper = new LambdaQueryWrapper<>();
            escrowWrapper.eq(EscrowAccount::getOrderId, orderId);
            escrowWrapper.eq(EscrowAccount::getStatus, "holding");
            EscrowAccount escrowAccount = escrowAccountMapper.selectOne(escrowWrapper);

            if (escrowAccount != null) {
                walletService.deposit(order.getUserId(), escrowAccount.getAmount(), 
                        "取消订单退款，订单号：" + order.getOrderNo());

                escrowAccount.setStatus("refunded");
                escrowAccount.setSettleTime(new Date());
                escrowAccountMapper.updateById(escrowAccount);
            }
        }

        if ("paid".equals(status)) {
            List<OrderItem> items = getOrderItems(orderId);
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                    productWrapper.eq(Product::getId, item.getProductId());
                    productWrapper.set(Product::getSalesCount, Math.max(0, product.getSalesCount() - item.getQuantity()));
                    productMapper.update(null, productWrapper);
                }
            }
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "cancelled");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, status, "cancelled", "用户");
    }

    // 商家接受议价
    @Transactional
    public void acceptBargaining(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"bargaining".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不是议价中");
        }

        if (order.getBuyerOfferPrice() == null || order.getBuyerOfferPrice() <= 0) {
            throw new RuntimeException("买家出价无效");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getActualAmount, order.getBuyerOfferPrice());
        wrapper.set(OrderInfo::getStatus, "pending");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        List<OrderItem> items = getOrderItems(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
            productWrapper.eq(Product::getId, item.getProductId());
            productWrapper.set(Product::getStock, product.getStock() - item.getQuantity());
            if (product.getStock() - item.getQuantity() <= 0) {
                productWrapper.set(Product::getStatus, "sold_out");
            }
            productMapper.update(null, productWrapper);
        }

        recordOrderStatusChange(orderId, "bargaining", "pending", "商家");
    }

    // 商家拒绝议价
    @Transactional
    public void rejectBargaining(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"bargaining".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不是议价中");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "cancelled");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, "bargaining", "cancelled", "商家");
    }

    // 获取订单详情
    public OrderInfo getOrderById(Integer orderId) {
        return orderInfoMapper.selectById(orderId);
    }

    // 获取订单明细
    public List<OrderItem> getOrderItems(Integer orderId) {
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        return orderItemMapper.selectList(wrapper);
    }

    // 获取配送信息
    public OrderDelivery getOrderDelivery(Integer orderId) {
        LambdaQueryWrapper<OrderDelivery> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId);
        return orderDeliveryMapper.selectOne(wrapper);
    }

    // 商家填写面交信息
    @Transactional
    public void setMeetInfo(Integer orderId, Date meetTime, String meetLocation) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId);
        wrapper.set(OrderDelivery::getMeetTime, meetTime);
        wrapper.set(OrderDelivery::getMeetLocation, meetLocation);
        wrapper.set(OrderDelivery::getMeetStatus, "pending_buyer");
        wrapper.set(OrderDelivery::getMeetLastUpdater, "seller");
        wrapper.set(OrderDelivery::getUpdateTime, new Date());
        orderDeliveryMapper.update(null, wrapper);
    }

    // 买家确认面交信息
    @Transactional
    public void confirmMeetInfo(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        if (!"pending_buyer".equals(delivery.getMeetStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }

        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId);
        wrapper.set(OrderDelivery::getMeetStatus, "confirmed");
        wrapper.set(OrderDelivery::getMeetLastUpdater, "buyer");
        wrapper.set(OrderDelivery::getUpdateTime, new Date());
        orderDeliveryMapper.update(null, wrapper);
    }

    // 买家拒绝面交信息并提出新建议
    @Transactional
    public void rejectMeetInfo(Integer orderId, Date newMeetTime, String newMeetLocation) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        if (!"pending_buyer".equals(delivery.getMeetStatus())) {
            throw new RuntimeException("当前状态不允许拒绝");
        }

        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId);
        wrapper.set(OrderDelivery::getMeetTime, newMeetTime);
        wrapper.set(OrderDelivery::getMeetLocation, newMeetLocation);
        wrapper.set(OrderDelivery::getMeetStatus, "rejected");
        wrapper.set(OrderDelivery::getMeetLastUpdater, "buyer");
        wrapper.set(OrderDelivery::getUpdateTime, new Date());
        orderDeliveryMapper.update(null, wrapper);
    }

    // 商家重新设置面交信息（买家拒绝后）
    @Transactional
    public void resubmitMeetInfo(Integer orderId, Date meetTime, String meetLocation) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        if (!"rejected".equals(delivery.getMeetStatus())) {
            throw new RuntimeException("当前状态不允许重新设置");
        }

        LambdaUpdateWrapper<OrderDelivery> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderDelivery::getOrderId, orderId);
        wrapper.set(OrderDelivery::getMeetTime, meetTime);
        wrapper.set(OrderDelivery::getMeetLocation, meetLocation);
        wrapper.set(OrderDelivery::getMeetStatus, "pending_buyer");
        wrapper.set(OrderDelivery::getMeetLastUpdater, "seller");
        wrapper.set(OrderDelivery::getUpdateTime, new Date());
        orderDeliveryMapper.update(null, wrapper);
    }

    // 商家确认面交发货
    @Transactional
    public void shipFaceToFaceOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        if (!"confirmed".equals(delivery.getMeetStatus())) {
            throw new RuntimeException("面交信息尚未确认");
        }

        if (!"paid".equals(order.getStatus())) {
            throw new RuntimeException("订单状态不允许发货");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "shipped");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, "paid", "shipped", "商家");
    }

    // 面交订单买家确认收货
    @Transactional
    public void completeFaceToFaceOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery == null || !"face_to_face".equals(delivery.getDeliveryType())) {
            throw new RuntimeException("该订单不是面交订单");
        }

        if (!"shipped".equals(order.getStatus())) {
            throw new RuntimeException("商家尚未确认发货，请等待商家确认");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "received");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        settleEscrowToMerchant(orderId);

        recordOrderStatusChange(orderId, "shipped", "received", "用户");
    }

    // 获取用户订单列表
    public List<OrderListVO> getUserOrders(Integer userId, String status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> orderIds = orders.stream()
                .map(OrderInfo::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(OrderItem::getOrderId, orderIds);
        List<OrderItem> allItems = orderItemMapper.selectList(itemWrapper);

        Map<Integer, List<OrderItem>> itemsByOrder = allItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<Integer> productIds = allItems.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Product> productWrapper = new LambdaQueryWrapper<>();
        productWrapper.in(Product::getId, productIds);
        List<Product> products = productMapper.selectList(productWrapper);

        Map<Integer, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Integer, String> productImageMap = new java.util.HashMap<>();
        for (Integer productId : productIds) {
            LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
            imageWrapper.eq(ProductImage::getProductId, productId);
            imageWrapper.orderByAsc(ProductImage::getSortOrder);
            imageWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
            ProductImage firstImage = productImageMapper.selectOne(imageWrapper);
            if (firstImage != null) {
                productImageMap.put(productId, firstImage.getImageUrl());
            }
        }

        LambdaQueryWrapper<OrderDelivery> deliveryWrapper = new LambdaQueryWrapper<>();
        deliveryWrapper.in(OrderDelivery::getOrderId, orderIds);
        List<OrderDelivery> deliveries = orderDeliveryMapper.selectList(deliveryWrapper);
        Map<Integer, OrderDelivery> deliveryMap = deliveries.stream()
                .collect(Collectors.toMap(OrderDelivery::getOrderId, d -> d));

        // 查询退货申请信息
        LambdaQueryWrapper<OrderReturnRequest> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.in(OrderReturnRequest::getOrderId, orderIds);
        returnWrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        List<OrderReturnRequest> returnRequests = orderReturnRequestMapper.selectList(returnWrapper);
        Map<Integer, OrderReturnRequest> returnRequestMap = returnRequests.stream()
                .collect(Collectors.toMap(OrderReturnRequest::getOrderId, r -> r, (r1, r2) -> r1));

        List<OrderListVO> result = new ArrayList<>();
        for (OrderInfo order : orders) {
            OrderListVO vo = new OrderListVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setActualAmount(order.getActualAmount());

            OrderDelivery delivery = deliveryMap.get(order.getId());
            if (delivery != null) {
                vo.setDeliveryType(delivery.getDeliveryType());
            }

            // 设置退货申请信息
            OrderReturnRequest returnRequest = returnRequestMap.get(order.getId());
            if (returnRequest != null) {
                OrderListVO.ReturnRequestVO returnRequestVO = new OrderListVO.ReturnRequestVO();
                returnRequestVO.setId(returnRequest.getId());
                returnRequestVO.setReturnReason(returnRequest.getReturnReason());
                returnRequestVO.setRequestTime(returnRequest.getRequestTime());
                returnRequestVO.setStatus(returnRequest.getStatus());
                returnRequestVO.setAuditTime(returnRequest.getAuditTime());
                returnRequestVO.setAuditRemark(returnRequest.getAuditRemark());
                returnRequestVO.setRefundAmount(returnRequest.getRefundAmount());
                vo.setReturnRequest(returnRequestVO);
            }

            List<OrderItem> orderItems = itemsByOrder.getOrDefault(order.getId(), new ArrayList<>());
            int totalQuantity = 0;
            List<OrderProductVO> productVOs = new ArrayList<>();

            for (OrderItem item : orderItems) {
                totalQuantity += item.getQuantity();
                Product product = productMap.get(item.getProductId());
                if (product != null) {
                    OrderProductVO productVO = new OrderProductVO();
                    productVO.setId(product.getId());
                    productVO.setProductName(product.getProductName());
                    productVO.setPrice(item.getPrice());
                    productVO.setQuantity(item.getQuantity());
                    productVO.setProductImage(productImageMap.get(item.getProductId()));
                    productVOs.add(productVO);
                }
            }

            vo.setTotalQuantity(totalQuantity);
            vo.setProducts(productVOs);
            result.add(vo);
        }

        return result;
    }

    // 获取商家订单列表
    public List<OrderInfo> getMerchantOrders(Integer merchantId, String status) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getMerchantId, merchantId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return orderInfoMapper.selectList(wrapper);
    }

    // 记录订单状态变更
    private void recordOrderStatusChange(Integer orderId, String oldStatus, String newStatus, String operator) {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderId(orderId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperator(operator);
        log.setOperateTime(new Date());
        orderStatusLogMapper.insert(log);
    }

    // 自动确认超过7天未收货的订单
    @Transactional
    public void autoCompleteOrders() {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, "shipped");
        wrapper.lt(OrderInfo::getUpdateTime, sevenDaysAgo);

        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);

        for (OrderInfo order : orders) {
            order.setStatus("received");
            order.setUpdateTime(new Date());
            orderInfoMapper.updateById(order);

            recordOrderStatusChange(order.getId(), "shipped", "received", "system");
        }
    }

    // 自动取消超过30分钟未支付的订单
    @Transactional
    public void autoCancelExpiredOrders() {
        Date thirtyMinutesAgo = new Date(System.currentTimeMillis() - 30 * 60 * 1000);

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, "pending");
        wrapper.lt(OrderInfo::getCreateTime, thirtyMinutesAgo);

        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);

        for (OrderInfo order : orders) {
            List<OrderItem> items = getOrderItems(order.getId());
            for (OrderItem item : items) {
                Product product = productMapper.selectById(item.getProductId());
                if (product != null) {
                    LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                    productWrapper.eq(Product::getId, item.getProductId());
                    productWrapper.set(Product::getStock, product.getStock() + item.getQuantity());
                    if ("sold_out".equals(product.getStatus()) && product.getStock() + item.getQuantity() > 0) {
                        productWrapper.set(Product::getStatus, "on_sale");
                    }
                    productMapper.update(null, productWrapper);
                }
            }

            order.setStatus("cancelled");
            order.setUpdateTime(new Date());
            orderInfoMapper.updateById(order);

            recordOrderStatusChange(order.getId(), "pending", "cancelled", "system");
        }
    }

    public OrderDetailVO getOrderDetail(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        OrderDetailVO vo = new OrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setCreateTime(order.getCreateTime());
        vo.setBuyerOfferPrice(order.getBuyerOfferPrice());

        OrderDelivery delivery = getOrderDelivery(orderId);
        if (delivery != null) {
            vo.setDeliveryType(delivery.getDeliveryType());
        }

        LambdaQueryWrapper<OrderStatusLog> payLogWrapper = new LambdaQueryWrapper<>();
        payLogWrapper.eq(OrderStatusLog::getOrderId, orderId);
        payLogWrapper.eq(OrderStatusLog::getNewStatus, "paid");
        payLogWrapper.orderByAsc(OrderStatusLog::getOperateTime);
        payLogWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        OrderStatusLog payLog = orderStatusLogMapper.selectOne(payLogWrapper);
        if (payLog != null) {
            vo.setPayTime(payLog.getOperateTime());
        }

        LambdaQueryWrapper<OrderStatusLog> shipLogWrapper = new LambdaQueryWrapper<>();
        shipLogWrapper.eq(OrderStatusLog::getOrderId, orderId);
        shipLogWrapper.eq(OrderStatusLog::getNewStatus, "shipped");
        shipLogWrapper.orderByAsc(OrderStatusLog::getOperateTime);
        shipLogWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        OrderStatusLog shipLog = orderStatusLogMapper.selectOne(shipLogWrapper);
        if (shipLog != null) {
            vo.setDeliveryTime(shipLog.getOperateTime());
        }

        OrderDetailDeliveryVO deliveryVO = new OrderDetailDeliveryVO();
        if (delivery != null) {
            deliveryVO.setReceiverName(delivery.getReceiverName());
            deliveryVO.setReceiverPhone(delivery.getReceiverPhone());
            deliveryVO.setReceiverAddress(delivery.getReceiverAddress());
            deliveryVO.setTrackingNumber(delivery.getTrackingNumber());
            deliveryVO.setMeetTime(delivery.getMeetTime());
            deliveryVO.setMeetLocation(delivery.getMeetLocation());
            deliveryVO.setMeetStatus(delivery.getMeetStatus());
        }

        SysUser merchant = sysUserMapper.selectById(order.getMerchantId());
        if (merchant != null) {
            deliveryVO.setSellerPhone(merchant.getPhone());
        }
        vo.setDelivery(deliveryVO);

        List<OrderItem> items = getOrderItems(orderId);
        List<OrderDetailProductVO> productVOs = new ArrayList<>();
        double productTotal = 0.0;

        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                OrderDetailProductVO productVO = new OrderDetailProductVO();
                productVO.setId(product.getId());
                productVO.setProductName(product.getProductName());
                productVO.setPrice(item.getPrice());
                productVO.setQuantity(item.getQuantity());

                LambdaQueryWrapper<ProductImage> imageWrapper = new LambdaQueryWrapper<>();
                imageWrapper.eq(ProductImage::getProductId, item.getProductId());
                imageWrapper.orderByAsc(ProductImage::getSortOrder);
                imageWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
                ProductImage firstImage = productImageMapper.selectOne(imageWrapper);
                if (firstImage != null) {
                    productVO.setProductImage(firstImage.getImageUrl());
                }

                productVOs.add(productVO);
                productTotal += item.getPrice() * item.getQuantity();
            }
        }

        vo.setProducts(productVOs);
        vo.setProductTotal(productTotal);
        vo.setPointsDiscount(order.getPointsDeducted() != null ? order.getPointsDeducted().doubleValue() : 0.0);
        vo.setTotalAmount(order.getActualAmount());

        // 查询退货申请信息（取最新一条）
        LambdaQueryWrapper<OrderReturnRequest> returnWrapper = new LambdaQueryWrapper<>();
        returnWrapper.eq(OrderReturnRequest::getOrderId, orderId);
        returnWrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        returnWrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        OrderReturnRequest returnRequest = orderReturnRequestMapper.selectOne(returnWrapper);
        
        if (returnRequest != null) {
            OrderDetailVO.ReturnRequestVO returnRequestVO = new OrderDetailVO.ReturnRequestVO();
            returnRequestVO.setId(returnRequest.getId());
            returnRequestVO.setReturnReason(returnRequest.getReturnReason());
            returnRequestVO.setRequestTime(returnRequest.getRequestTime());
            returnRequestVO.setStatus(returnRequest.getStatus());
            returnRequestVO.setAuditTime(returnRequest.getAuditTime());
            returnRequestVO.setAuditRemark(returnRequest.getAuditRemark());
            returnRequestVO.setRefundAmount(returnRequest.getRefundAmount());
            vo.setReturnRequest(returnRequestVO);
        }

        return vo;
    }

    // 商家审核退货申请（同意）
    @Transactional
    public void approveReturnRequest(Integer requestId, String auditRemark) {
        OrderReturnRequest returnRequest = orderReturnRequestMapper.selectById(requestId);
        if (returnRequest == null) {
            throw new RuntimeException("退货申请不存在");
        }

        if (!"pending".equals(returnRequest.getStatus())) {
            throw new RuntimeException("该退货申请已处理");
        }

        OrderInfo order = orderInfoMapper.selectById(returnRequest.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LambdaQueryWrapper<EscrowAccount> escrowWrapper = new LambdaQueryWrapper<>();
        escrowWrapper.eq(EscrowAccount::getOrderId, order.getId());
        escrowWrapper.eq(EscrowAccount::getStatus, "holding");
        EscrowAccount escrowAccount = escrowAccountMapper.selectOne(escrowWrapper);

        if (escrowAccount != null) {
            walletService.deposit(returnRequest.getUserId(), escrowAccount.getAmount(), 
                    "退货退款，订单号：" + order.getOrderNo());

            escrowAccount.setStatus("refunded");
            escrowAccount.setSettleTime(new Date());
            escrowAccountMapper.updateById(escrowAccount);
        } else {
            Double totalAmount = returnRequest.getRefundAmount();
            Double merchantAmount = totalAmount;
            Double platformFee = 0.0;

            LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
            merchantWrapper.eq(MerchantInfo::getUserId, order.getMerchantId());
            MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantWrapper);

            if (merchantInfo != null && merchantInfo.getLevelId() != null) {
                MerchantLevel merchantLevel = merchantLevelMapper.selectById(merchantInfo.getLevelId());
                if (merchantLevel != null && merchantLevel.getRate() != null) {
                    platformFee = Math.round(totalAmount * merchantLevel.getRate() * 100.0) / 100.0;
                    merchantAmount = totalAmount - platformFee;
                }
            }

            // 从商家钱包扣款（实收金额）
            walletService.pay(order.getMerchantId(), merchantAmount, 
                    "退货扣款，订单号：" + order.getOrderNo());

            // 从平台钱包扣款（手续费）
            if (platformFee > 0) {
                walletService.pay(0, platformFee, "退还订单手续费，订单号：" + order.getOrderNo());
            }

            // 退款给买家（全额）
            walletService.deposit(returnRequest.getUserId(), totalAmount, 
                    "退货退款，订单号：" + order.getOrderNo());
        }

        if (order.getPointsDeducted() != null && order.getPointsDeducted() > 0) {
            pointsService.addPoints(order.getUserId(), order.getPointsDeducted(), 
                    "退货退还抵扣积分，订单号：" + order.getOrderNo());
        }

        if (order.getActualAmount() != null && order.getActualAmount() > 0) {
            int earnedPoints = (int) Math.round(order.getActualAmount());
            if (earnedPoints > 0) {
                pointsService.deductPoints(order.getUserId(), earnedPoints, 
                        "退货扣除消费积分，订单号：" + order.getOrderNo());
            }
        }

        List<OrderItem> items = getOrderItems(order.getId());
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, item.getProductId());
                productWrapper.set(Product::getStock, product.getStock() + item.getQuantity());
                productWrapper.set(Product::getSalesCount, Math.max(0, product.getSalesCount() - item.getQuantity()));
                if ("sold_out".equals(product.getStatus()) && product.getStock() + item.getQuantity() > 0) {
                    productWrapper.set(Product::getStatus, "published");
                }
                productMapper.update(null, productWrapper);
            }
        }

        LambdaUpdateWrapper<OrderInfo> orderWrapper = new LambdaUpdateWrapper<>();
        orderWrapper.eq(OrderInfo::getId, order.getId());
        orderWrapper.set(OrderInfo::getStatus, "refunded");
        orderWrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, orderWrapper);

        returnRequest.setStatus("approved");
        returnRequest.setAuditTime(new Date());
        returnRequest.setAuditRemark(auditRemark);
        orderReturnRequestMapper.updateById(returnRequest);

        OrderReturnRequest completedRequest = new OrderReturnRequest();
        completedRequest.setId(returnRequest.getId());
        completedRequest.setStatus("completed");
        orderReturnRequestMapper.updateById(completedRequest);

        recordOrderStatusChange(order.getId(), "received", "refunded", "商家");
    }

    // 商家审核退货申请（拒绝）
    @Transactional
    public void rejectReturnRequest(Integer requestId, String auditRemark) {
        OrderReturnRequest returnRequest = orderReturnRequestMapper.selectById(requestId);
        if (returnRequest == null) {
            throw new RuntimeException("退货申请不存在");
        }

        if (!"pending".equals(returnRequest.getStatus())) {
            throw new RuntimeException("该退货申请已处理");
        }

        returnRequest.setStatus("rejected");
        returnRequest.setAuditTime(new Date());
        returnRequest.setAuditRemark(auditRemark);
        orderReturnRequestMapper.updateById(returnRequest);

        // 将订单状态改回已收货
        OrderInfo order = orderInfoMapper.selectById(returnRequest.getOrderId());
        if (order != null && "returning".equals(order.getStatus())) {
            LambdaUpdateWrapper<OrderInfo> orderWrapper = new LambdaUpdateWrapper<>();
            orderWrapper.eq(OrderInfo::getId, order.getId());
            orderWrapper.set(OrderInfo::getStatus, "received");
            orderWrapper.set(OrderInfo::getUpdateTime, new Date());
            orderInfoMapper.update(null, orderWrapper);

            recordOrderStatusChange(order.getId(), "returning", "received", "商家");
        }
    }

    // 获取商家的退货申请列表
    public List<OrderReturnRequest> getMerchantReturnRequests(Integer merchantId, String status) {
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getMerchantId, merchantId);
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);

        if (orders.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> orderIds = orders.stream()
                .map(OrderInfo::getId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<OrderReturnRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderReturnRequest::getOrderId, orderIds);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderReturnRequest::getStatus, status);
        }
        wrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        return orderReturnRequestMapper.selectList(wrapper);
    }

    // 获取用户的退货申请列表
    public List<OrderReturnRequest> getUserReturnRequests(Integer userId) {
        LambdaQueryWrapper<OrderReturnRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReturnRequest::getUserId, userId);
        wrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        return orderReturnRequestMapper.selectList(wrapper);
    }

    // 获取退货申请详情
    public OrderReturnRequest getReturnRequestById(Integer requestId) {
        return orderReturnRequestMapper.selectById(requestId);
    }

    // 获取订单的退货申请
    public OrderReturnRequest getReturnRequestByOrderId(Integer orderId) {
        LambdaQueryWrapper<OrderReturnRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderReturnRequest::getOrderId, orderId);
        wrapper.orderByDesc(OrderReturnRequest::getRequestTime);
        wrapper.last("OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY");
        return orderReturnRequestMapper.selectOne(wrapper);
    }

    // 发货后7天自动确认收货
    @Transactional
    public void autoCompleteShippedOrders() {
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000);

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, "shipped");
        wrapper.lt(OrderInfo::getUpdateTime, sevenDaysAgo);

        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);

        for (OrderInfo order : orders) {
            order.setStatus("received");
            order.setUpdateTime(new Date());
            orderInfoMapper.updateById(order);

            recordOrderStatusChange(order.getId(), "shipped", "received", "system");
        }
    }

    // 自动将超过24小时的 received 订单转为 completed
    @Transactional
    public void autoCompleteReceivedOrders() {
        Date twentyFourHoursAgo = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);

        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, "received");
        wrapper.lt(OrderInfo::getUpdateTime, twentyFourHoursAgo);

        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);

        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderReturnRequest> returnWrapper = new LambdaQueryWrapper<>();
            returnWrapper.eq(OrderReturnRequest::getOrderId, order.getId());
            returnWrapper.in(OrderReturnRequest::getStatus, "pending", "approved");
            OrderReturnRequest existingRequest = orderReturnRequestMapper.selectOne(returnWrapper);

            if (existingRequest == null) {
                order.setStatus("completed");
                order.setUpdateTime(new Date());
                orderInfoMapper.updateById(order);

                recordOrderStatusChange(order.getId(), "received", "completed", "system");
            }
        }
    }

    // 结算托管资金给商家（扣除平台费用）
    @Transactional
    public void settleEscrowToMerchant(Integer orderId) {
        LambdaQueryWrapper<EscrowAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EscrowAccount::getOrderId, orderId);
        wrapper.eq(EscrowAccount::getStatus, "holding");
        EscrowAccount escrowAccount = escrowAccountMapper.selectOne(wrapper);

        if (escrowAccount == null) {
            return;
        }

        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return;
        }

        Double totalAmount = escrowAccount.getAmount();
        Double merchantAmount = totalAmount;
        Double platformFee = 0.0;

        LambdaQueryWrapper<MerchantInfo> merchantWrapper = new LambdaQueryWrapper<>();
        merchantWrapper.eq(MerchantInfo::getUserId, order.getMerchantId());
        MerchantInfo merchantInfo = merchantInfoMapper.selectOne(merchantWrapper);

        if (merchantInfo != null && merchantInfo.getLevelId() != null) {
            MerchantLevel merchantLevel = merchantLevelMapper.selectById(merchantInfo.getLevelId());
            if (merchantLevel != null && merchantLevel.getRate() != null) {
                platformFee = Math.round(totalAmount * merchantLevel.getRate() * 100.0) / 100.0;
                merchantAmount = totalAmount - platformFee;
            }
        }

        walletService.deposit(order.getMerchantId(), merchantAmount, "订单结算，订单号：" + order.getOrderNo());

        if (platformFee > 0) {
            walletService.deposit(0, platformFee, "平台手续费，订单号：" + order.getOrderNo());
        }

        escrowAccount.setStatus("settled");
        escrowAccount.setSettleTime(new Date());
        escrowAccountMapper.updateById(escrowAccount);
    }

    // 获取用户最近一次使用的快递地址
    public OrderDelivery getLastUsedExpressAddress(Integer userId) {
        LambdaQueryWrapper<OrderInfo> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(OrderInfo::getUserId, userId);
        orderWrapper.orderByDesc(OrderInfo::getCreateTime);
        List<OrderInfo> orders = orderInfoMapper.selectList(orderWrapper);

        for (OrderInfo order : orders) {
            LambdaQueryWrapper<OrderDelivery> deliveryWrapper = new LambdaQueryWrapper<>();
            deliveryWrapper.eq(OrderDelivery::getOrderId, order.getId());
            deliveryWrapper.eq(OrderDelivery::getDeliveryType, "express");
            OrderDelivery delivery = orderDeliveryMapper.selectOne(deliveryWrapper);
            if (delivery != null && delivery.getReceiverName() != null && delivery.getReceiverPhone() != null && delivery.getReceiverAddress() != null) {
                return delivery;
            }
        }

        return null;
    }
}
