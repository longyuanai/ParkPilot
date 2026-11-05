package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 充电桩插枪启动、工单流转、拔枪分时计费与电费结算事务接口（核心事务二）
 */
public interface ChargingService {

    BigDecimal MAX_KWH = new BigDecimal("9999.99");

    static BigDecimal exactKwh(BigDecimal amount) throws ZhiXingYueBoException {
        if (amount == null || amount.signum() < 0 || amount.compareTo(MAX_KWH) > 0) {
            throw new ZhiXingYueBoException("电量须在 0.00 至 9999.99 度之间。");
        }
        try {
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new ZhiXingYueBoException("电量最多保留两位小数。");
        }
    }

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

    /** 仅当进行中订单仍处于预期累计电量时接收一次计量。 */
    boolean recordMeterProgress(Long orderId, Integer userId, BigDecimal previousKwh, BigDecimal nextKwh)
            throws ZhiXingYueBoException;

    /**
     * 拔枪结算。已保存的正数计量优先。kwhAmount 为 null 时直接使用订单累计计量。
     */
    ChargingOrder finishCharging(Long orderId, Integer userId, BigDecimal kwhAmount) throws ZhiXingYueBoException;

    /**
     * 获取当前用户进行中的充电工单
     */
    ChargingOrder getActiveOrderByUserId(Integer userId) throws ZhiXingYueBoException;
}
