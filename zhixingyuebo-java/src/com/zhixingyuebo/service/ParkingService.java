package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.ParkingLot;
import com.zhixingyuebo.entity.ParkingRecord;
import com.zhixingyuebo.entity.ParkingSpot;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.SpotNotAvailableException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.util.List;

/**
 * 车位检索预约、道闸入场、离场阶梯算费与扣款事务接口（核心事务一）
 */
public interface ParkingService {

    /**
     * 多条件筛选空闲车位
     */
    List<ParkingSpot> getAvailableSpots(Integer spotType, Integer lotId) throws ZhiXingYueBoException;

    /**
     * 获取全园区所有车位大盘
     */
    List<ParkingSpot> getAllSpots() throws ZhiXingYueBoException;

    /**
     * 车位预约锁定（车位状态翻转为 1: 已预约）
     */
    ParkingSpot reserveSpot(Integer spotId, Integer userId) throws ZhiXingYueBoException;

    /**
     * 车辆模拟道闸驶入登记（车位状态翻转为 2: 已泊入，生成入场单据）
     */
    ParkingRecord simulateEntry(String plateNumber, Integer spotId, Integer userId) throws ZhiXingYueBoException;

    /**
     * 车辆离场阶梯算费与原子扣款（核心事务一）
     * 包含：核算停泊时长与阶梯金额 -> 检查钱包余额 -> 开启事务 -> 扣减钱包 -> 释放车位 -> 回填单据 -> 写入审计流水 -> 提交
     */
    ParkingRecord processExit(String plateNumber, Integer userId) throws ZhiXingYueBoException;

    /**
     * 查询指定车辆当前在场泊车记录
     */
    ParkingRecord getActiveRecordByPlate(String plateNumber) throws ZhiXingYueBoException;

    /**
     * 查询全场所有当前停泊在场的车辆流水
     */
    List<ParkingRecord> getAllActiveRecords() throws ZhiXingYueBoException;

    /**
     * 获取所有停车场区域
     */
    List<ParkingLot> getAllLots() throws ZhiXingYueBoException;

    /**
     * 智能车位匹配与就近推荐算法（借鉴 parking-lot-problem 最近距离堆排序与新能源车型匹配策略）
     * 1. 新能源汽车（EV/PHEV）：优先就近推荐空闲充电专用位；若满位则推荐就近普通车位；
     * 2. 传统燃油汽车：推荐就近普通车位，严格禁入充电专用位；
     */
    ParkingSpot recommendOptimalSpot(String plateNumber) throws ZhiXingYueBoException;

    /**
     * 根据主键查询车位信息
     */
    ParkingSpot getSpotById(Integer spotId) throws ZhiXingYueBoException;

    /**
     * 新增规划车位
     */
    void addSpot(ParkingSpot spot) throws ZhiXingYueBoException;

    /**
     * 修改车位状态（空闲、禁用等）
     */
    void updateSpotStatus(Integer spotId, int status) throws ZhiXingYueBoException;
}
