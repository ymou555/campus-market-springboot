package org.example.campusmarket.config;

import org.example.campusmarket.service.OrderService;
import org.example.campusmarket.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Date;

@Configuration
@EnableScheduling
public class ScheduledConfig {
    @Autowired
    private OrderService orderService;
    @Autowired
    private MerchantService merchantService;

    // 每5分钟执行一次，检查并取消超过30分钟未支付的订单
    @Scheduled(cron = "0 */5 * * * ?")
    public void autoCancelExpiredOrders() {
        System.out.println("开始执行订单超时自动取消任务: " + new Date());
        orderService.autoCancelExpiredOrders();
        System.out.println("订单超时自动取消任务执行完成: " + new Date());
    }

    // 每天凌晨1点执行一次
    // @Scheduled(cron = "0 0 1 * * ?")
    // public void autoCompleteOrders() {
    //     System.out.println("开始执行自动确认收货任务: " + new Date());
    //     orderService.autoCompleteOrders();
    //     System.out.println("自动确认收货任务执行完成: " + new Date());
    // }

    // 每月1号凌晨2点执行一次
    // @Scheduled(cron = "0 0 2 1 * ?")
    // public void adjustMerchantLevels() {
    //     System.out.println("开始执行商家等级调整任务: " + new Date());
    //     merchantService.adjustMerchantLevels();
    //     System.out.println("商家等级调整任务执行完成: " + new Date());
    // }
}
