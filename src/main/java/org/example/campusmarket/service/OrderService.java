package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
    public void receiveOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "completed");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, "shipped", "completed", "用户");
    }

    // 申请退货
    @Transactional
    public void applyRefund(Integer orderId, String reason) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        if (!"paid".equals(order.getStatus()) && !"shipped".equals(order.getStatus())) {
            throw new RuntimeException("当前订单状态不允许申请退货");
        }

        List<OrderItem> items = getOrderItems(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            if (product != null) {
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, item.getProductId());
                productWrapper.set(Product::getStock, product.getStock() + item.getQuantity());
                productWrapper.set(Product::getSalesCount, Math.max(0, product.getSalesCount() - item.getQuantity()));
                if ("sold_out".equals(product.getStatus()) && product.getStock() + item.getQuantity() > 0) {
                    productWrapper.set(Product::getStatus, "on_sale");
                }
                productMapper.update(null, productWrapper);
            }
        }

        if (order.getPointsDeducted() != null && order.getPointsDeducted() > 0) {
            pointsService.addPoints(order.getUserId(), order.getPointsDeducted(), "退货退还积分，订单号：" + order.getOrderNo());
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "refunded");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, order.getStatus(), "refunded", "用户");
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
            pointsService.addPoints(order.getUserId(), order.getPointsDeducted(), "取消订单退还积分，订单号：" + order.getOrderNo());
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

    // 面交订单确认完成
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

        if (!"confirmed".equals(delivery.getMeetStatus())) {
            throw new RuntimeException("面交信息尚未确认");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "completed");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        recordOrderStatusChange(orderId, order.getStatus(), "completed", "用户");
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

        List<OrderListVO> result = new ArrayList<>();
        for (OrderInfo order : orders) {
            OrderListVO vo = new OrderListVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setStatus(order.getStatus());
            vo.setCreateTime(order.getCreateTime());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setActualAmount(order.getActualAmount());

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
            order.setStatus("completed");
            order.setUpdateTime(new Date());
            orderInfoMapper.updateById(order);

            recordOrderStatusChange(order.getId(), "shipped", "completed", "system");
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
}
