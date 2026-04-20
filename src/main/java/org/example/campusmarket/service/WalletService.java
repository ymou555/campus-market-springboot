package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.example.campusmarket.entity.TransactionRecord;
import org.example.campusmarket.entity.Wallet;
import org.example.campusmarket.mapper.TransactionRecordMapper;
import org.example.campusmarket.mapper.WalletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class WalletService {
    @Autowired
    private WalletMapper walletMapper;
    @Autowired
    private TransactionRecordMapper transactionRecordMapper;

    // 获取用户钱包
    public Wallet getWallet(Integer userId) {
        LambdaQueryWrapper<Wallet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Wallet::getUserId, userId);
        Wallet wallet = walletMapper.selectOne(wrapper);
        if (wallet == null) {
            // 初始化钱包
            wallet = new Wallet();
            wallet.setUserId(userId);
            wallet.setBalance(0.0);
            wallet.setLastUpdateTime(new Date());
            walletMapper.insert(wallet);
        }
        return wallet;
    }

    // 充值
    @Transactional
    public void deposit(Integer userId, Double amount, String remark) {
        // 更新钱包余额
        LambdaUpdateWrapper<Wallet> walletWrapper = new LambdaUpdateWrapper<>();
        walletWrapper.eq(Wallet::getUserId, userId);
        walletWrapper.setSql("balance = balance + " + amount);
        walletWrapper.set(Wallet::getLastUpdateTime, new Date());
        walletMapper.update(null, walletWrapper);

        // 记录交易流水
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType("deposit");
        record.setStatus("success");
        record.setTransactionTime(new Date());
        record.setRemark(remark);
        transactionRecordMapper.insert(record);
    }

    // 扣款
    @Transactional
    public void withdraw(Integer userId, Double amount, String remark) {
        // 检查余额
        Wallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("余额不足");
        }

        // 更新钱包余额
        LambdaUpdateWrapper<Wallet> walletWrapper = new LambdaUpdateWrapper<>();
        walletWrapper.eq(Wallet::getUserId, userId);
        walletWrapper.setSql("balance = balance - " + amount);
        walletWrapper.set(Wallet::getLastUpdateTime, new Date());
        walletMapper.update(null, walletWrapper);

        // 记录交易流水
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType("withdraw");
        record.setStatus("success");
        record.setTransactionTime(new Date());
        record.setRemark(remark);
        transactionRecordMapper.insert(record);
    }

    // 支付
    @Transactional
    public void pay(Integer userId, Double amount, String remark) {
        // 检查余额
        Wallet wallet = getWallet(userId);
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("余额不足");
        }

        // 更新钱包余额
        LambdaUpdateWrapper<Wallet> walletWrapper = new LambdaUpdateWrapper<>();
        walletWrapper.eq(Wallet::getUserId, userId);
        walletWrapper.setSql("balance = balance - " + amount);
        walletWrapper.set(Wallet::getLastUpdateTime, new Date());
        walletMapper.update(null, walletWrapper);

        // 记录交易流水
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType("payment");
        record.setStatus("success");
        record.setTransactionTime(new Date());
        record.setRemark(remark);
        transactionRecordMapper.insert(record);
    }

    // 退款
    @Transactional
    public void refund(Integer userId, Double amount, String remark) {
        // 更新钱包余额
        LambdaUpdateWrapper<Wallet> walletWrapper = new LambdaUpdateWrapper<>();
        walletWrapper.eq(Wallet::getUserId, userId);
        walletWrapper.setSql("balance = balance + " + amount);
        walletWrapper.set(Wallet::getLastUpdateTime, new Date());
        walletMapper.update(null, walletWrapper);

        // 记录交易流水
        TransactionRecord record = new TransactionRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setType("refund");
        record.setStatus("success");
        record.setTransactionTime(new Date());
        record.setRemark(remark);
        transactionRecordMapper.insert(record);
    }

    // 获取交易流水列表
    public List<TransactionRecord> getTransactionRecords(Integer userId, String type) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionRecord::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(TransactionRecord::getType, type);
        }
        wrapper.orderByDesc(TransactionRecord::getTransactionTime);
        return transactionRecordMapper.selectList(wrapper);
    }
}