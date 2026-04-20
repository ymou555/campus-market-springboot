package org.example.campusmarket.controller;

import org.example.campusmarket.entity.TransactionRecord;
import org.example.campusmarket.entity.Wallet;
import org.example.campusmarket.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {
    @Autowired
    private WalletService walletService;

    // 获取钱包信息
    @GetMapping("/info")
    public Map<String, Object> getWalletInfo(@RequestParam Integer userId) {
        Wallet wallet = walletService.getWallet(userId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("wallet", wallet);
        return result;
    }

    // 充值
    @PostMapping("/deposit")
    public Map<String, Object> deposit(
            @RequestParam Integer userId,
            @RequestParam Double amount,
            @RequestParam String remark) {
        walletService.deposit(userId, amount, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "充值成功");
        return result;
    }

    // 扣款
    @PostMapping("/withdraw")
    public Map<String, Object> withdraw(
            @RequestParam Integer userId,
            @RequestParam Double amount,
            @RequestParam String remark) {
        walletService.withdraw(userId, amount, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "扣款成功");
        return result;
    }

    // 支付
    @PostMapping("/pay")
    public Map<String, Object> pay(
            @RequestParam Integer userId,
            @RequestParam Double amount,
            @RequestParam String remark) {
        walletService.pay(userId, amount, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "支付成功");
        return result;
    }

    // 退款
    @PostMapping("/refund")
    public Map<String, Object> refund(
            @RequestParam Integer userId,
            @RequestParam Double amount,
            @RequestParam String remark) {
        walletService.refund(userId, amount, remark);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "退款成功");
        return result;
    }

    // 获取交易流水列表
    @GetMapping("/transactions")
    public Map<String, Object> getTransactionRecords(
            @RequestParam Integer userId,
            @RequestParam(required = false) String type) {
        List<TransactionRecord> records = walletService.getTransactionRecords(userId, type);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "获取成功");
        result.put("data", records);
        return result;
    }
}