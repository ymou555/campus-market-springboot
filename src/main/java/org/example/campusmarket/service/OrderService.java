package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.*;
import org.example.campusmarket.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    // 创建订单
    @Transactional
    public OrderInfo createOrder(Integer userId, List<Cart> cartItems, Integer pointsDeducted) {
        return createOrder(userId, cartItems, pointsDeducted, null);
    }
    
    // 创建订单（支持议价）
    @Transactional
    public OrderInfo createOrder(Integer userId, List<Cart> cartItems, Integer pointsDeducted, Double buyerOfferPrice) {
        // 生成订单号
        String orderNo = "ORD" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        
        // 计算总金额
        double totalAmount = 0;
        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            totalAmount += product.getDiscountPrice() * cart.getQuantity();
        }

        // 判断是否为议价订单
        boolean isBargaining = buyerOfferPrice != null && buyerOfferPrice > 0;
        
        // 计算实际支付金额
        double actualAmount;
        String orderStatus;
        
        if (isBargaining) {
            // 议价订单：实际金额暂时为总价，等待商家确认
            actualAmount = totalAmount;
            orderStatus = "bargaining";
        } else {
            // 正常订单：计算积分抵扣
            actualAmount = totalAmount - (pointsDeducted / 100.0);
            if (actualAmount < 0) actualAmount = 0;
            orderStatus = "pending";
        }

        // 创建订单主表
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setMerchantId(cartItems.get(0).getProductId()); // 假设购物车中商品来自同一商家
        order.setTotalAmount(totalAmount);
        order.setActualAmount(actualAmount);
        order.setPointsDeducted(isBargaining ? 0 : pointsDeducted);
        order.setBuyerOfferPrice(buyerOfferPrice);
        order.setStatus(orderStatus);
        order.setCreateTime(new Date());
        order.setUpdateTime(new Date());
        orderInfoMapper.insert(order);

        // 创建订单明细
        for (Cart cart : cartItems) {
            Product product = productMapper.selectById(cart.getProductId());
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(cart.getProductId());
            item.setQuantity(cart.getQuantity());
            item.setPrice(product.getDiscountPrice());
            orderItemMapper.insert(item);

            // 议价订单不扣减库存，等待商家确认后再扣减
            if (!isBargaining) {
                // 扣减库存
                LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
                productWrapper.eq(Product::getId, cart.getProductId());
                productWrapper.set(Product::getStock, product.getStock() - cart.getQuantity());
                productWrapper.set(Product::getSalesCount, product.getSalesCount() + cart.getQuantity());
                if (product.getStock() - cart.getQuantity() <= 0) {
                    productWrapper.set(Product::getStatus, "sold_out");
                }
                productMapper.update(null, productWrapper);
            }
        }

        // 清空购物车中已购买的商品
        for (Cart cart : cartItems) {
            cartMapper.deleteById(cart.getId());
        }

        // 记录订单状态变更
        recordOrderStatusChange(order.getId(), null, orderStatus, "系统");

        return order;
    }

    // 支付订单
    public void payOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "paid");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        // 记录订单状态变更
        recordOrderStatusChange(orderId, "pending", "paid", "用户");
    }

    // 商家发货
    public void shipOrder(Integer orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "shipped");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        // 记录订单状态变更
        recordOrderStatusChange(orderId, "paid", "shipped", "商家");
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

        // 记录订单状态变更
        recordOrderStatusChange(orderId, "shipped", "completed", "用户");
    }

    // 申请退货
    public void applyRefund(Integer orderId, String reason) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 检查是否在可退货时间内
        // 这里可以添加时间检查逻辑

        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "refunded");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);

        // 记录订单状态变更
        recordOrderStatusChange(orderId, order.getStatus(), "refunded", "用户");
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
        
        // 更新订单：实际金额改为买家出价，状态改为待付款
        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getActualAmount, order.getBuyerOfferPrice());
        wrapper.set(OrderInfo::getStatus, "pending");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);
        
        // 扣减库存
        List<OrderItem> items = getOrderItems(orderId);
        for (OrderItem item : items) {
            Product product = productMapper.selectById(item.getProductId());
            LambdaUpdateWrapper<Product> productWrapper = new LambdaUpdateWrapper<>();
            productWrapper.eq(Product::getId, item.getProductId());
            productWrapper.set(Product::getStock, product.getStock() - item.getQuantity());
            productWrapper.set(Product::getSalesCount, product.getSalesCount() + item.getQuantity());
            if (product.getStock() - item.getQuantity() <= 0) {
                productWrapper.set(Product::getStatus, "sold_out");
            }
            productMapper.update(null, productWrapper);
        }
        
        // 记录订单状态变更
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
        
        // 更新订单状态为商家取消
        LambdaUpdateWrapper<OrderInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OrderInfo::getId, orderId);
        wrapper.set(OrderInfo::getStatus, "cancelled");
        wrapper.set(OrderInfo::getUpdateTime, new Date());
        orderInfoMapper.update(null, wrapper);
        
        // 记录订单状态变更
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

    // 获取用户订单列表
    public Page<OrderInfo> getUserOrders(int page, int size, Integer userId, String status) {
        Page<OrderInfo> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getUserId, userId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return orderInfoMapper.selectPage(orderPage, wrapper);
    }

    // 获取商家订单列表
    public Page<OrderInfo> getMerchantOrders(int page, int size, Integer merchantId, String status) {
        Page<OrderInfo> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getMerchantId, merchantId);
        if (status != null && !status.isEmpty()) {
            wrapper.eq(OrderInfo::getStatus, status);
        }
        wrapper.orderByDesc(OrderInfo::getCreateTime);
        return orderInfoMapper.selectPage(orderPage, wrapper);
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
        // 计算7天前的日期
        Date sevenDaysAgo = new Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000);
        
        // 查询超过7天未确认收货的订单
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getStatus, "shipped");
        wrapper.lt(OrderInfo::getUpdateTime, sevenDaysAgo);
        
        List<OrderInfo> orders = orderInfoMapper.selectList(wrapper);
        
        for (OrderInfo order : orders) {
            // 更新订单状态为已完成
            order.setStatus("completed");
            order.setUpdateTime(new Date());
            orderInfoMapper.updateById(order);
            
            // 记录状态变更日志
            recordOrderStatusChange(order.getId(), "shipped", "completed", "system");
            
            // 处理资金结算
            // 这里可以实现将资金从托管账户转移到商家账户的逻辑
        }
    }
}