package org.example.campusmarket.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.campusmarket.entity.PointsRecord;
import org.example.campusmarket.mapper.PointsRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class PointsService {
    @Autowired
    private PointsRecordMapper pointsRecordMapper;

    // 获取用户总积分
    public int getTotalPoints(Integer userId) {
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId);
        // 使用QueryWrapper来支持SQL聚合函数
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PointsRecord> queryWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.select("SUM(points) as total_points");
        Object result = pointsRecordMapper.selectObjs(queryWrapper).stream().findFirst().orElse(0);
        return result != null ? Integer.parseInt(result.toString()) : 0;
    }

    // 增加积分
    @Transactional
    public void addPoints(Integer userId, Integer points, String remark) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(points);
        record.setType("earn");
        record.setCreateTime(new Date());
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }

    // 扣减积分
    @Transactional
    public void deductPoints(Integer userId, Integer points, String remark) {
        int totalPoints = getTotalPoints(userId);
        if (totalPoints < points) {
            throw new RuntimeException("积分不足");
        }

        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(-points);
        record.setType("deduct");
        record.setCreateTime(new Date());
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }

    // 积分过期
    @Transactional
    public void expirePoints(Integer userId, Integer points, String remark) {
        PointsRecord record = new PointsRecord();
        record.setUserId(userId);
        record.setPoints(-points);
        record.setType("expire");
        record.setCreateTime(new Date());
        record.setRemark(remark);
        pointsRecordMapper.insert(record);
    }

    // 获取积分流水记录
    public Page<PointsRecord> getPointsRecords(int page, int size, Integer userId, String type) {
        Page<PointsRecord> recordPage = new Page<>(page, size);
        LambdaQueryWrapper<PointsRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PointsRecord::getUserId, userId);
        if (type != null && !type.isEmpty()) {
            wrapper.eq(PointsRecord::getType, type);
        }
        wrapper.orderByDesc(PointsRecord::getCreateTime);
        return pointsRecordMapper.selectPage(recordPage, wrapper);
    }

    // 消费获得积分（1元=1积分）
    public void earnPointsByConsumption(Integer userId, Double amount) {
        int points = (int) Math.round(amount);
        if (points > 0) {
            addPoints(userId, points, "消费获得积分");
        }
    }

    // 积分抵扣（100积分=1元）
    public double deductPointsForPayment(Integer userId, Integer points) {
        deductPoints(userId, points, "积分抵扣");
        return points / 100.0;
    }
}