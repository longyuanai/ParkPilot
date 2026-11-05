package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.PileFaultException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.math.BigDecimal;
import java.util.List;

/**
 * 充电桩插枪启动、工单流转、拔枪分时计费与电费结算事务接口（核心事务二）
 */
public interface ChargingService {

    /**
     * 获取所有处于空闲就绪状态的充电桩列表（pile_status = 1）
     */
    List<ChargingPile> getReadyPiles() throws ZhiXingYueBoException;

    /**
     * 获取全园区所有充电桩列表
     */
    List<ChargingPile> getAllPiles() throws ZhiXingYueBoException;

    /**
     * 根据ID获取充电桩设备
     */
    ChargingPile getPileById(Integer pileId) throws ZhiXingYueBoException;

    /**
     * 新能源车辆插枪启动充电
     */
    ChargingOrder startCharging(Integer pileId, Integer userId, String plateNumber) throws ZhiXingYueBoException;

    /**
     * 拔枪分时计量与电费结算（核心事务二）
     * 包含：核算充电度数与总电费 -> 校验钱包余额 -> 开启事务 -> 扣减钱包 -> 归档工单 -> 复位充电桩 -> 写入审计流水 -> 提交
     */
    ChargingOrder finishCharging(Long orderId, Integer userId, BigDecimal kwhAmount) throws ZhiXingYueBoException;

    /**
     * 获取当前用户进行中的充电工单
     */
    ChargingOrder getActiveOrderByUserId(Integer userId) throws ZhiXingYueBoException;
}
