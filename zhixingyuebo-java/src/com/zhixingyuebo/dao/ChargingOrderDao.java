package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.ChargingOrder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电工单创建、计量更新与结算归档接口
 */
public interface ChargingOrderDao extends BaseDao<ChargingOrder, Long> {

    ChargingOrder findByIdForUpdate(Long orderId) throws SQLException;

    /**
     * 查询指定充电桩当前正在进行的充电工单（order_status = 0）
     */
    ChargingOrder findActiveOrderByPileId(Integer pileId) throws SQLException;
    boolean hasUnsettledOrderForPlate(String plateNumber) throws SQLException;
    void interruptActiveOrder(Integer pileId) throws SQLException;

    /**
     * 查询指定用户当前进行中的充电工单
     */
    ChargingOrder findActiveOrderByUserId(Integer userId) throws SQLException;

    int advanceMeter(Long orderId, Integer userId, BigDecimal previousKwh, BigDecimal nextKwh) throws SQLException;

    /**
     * 回填结束充电拔枪数据并完成工单归档
     */
    int finishChargingOrder(Long orderId, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal totalCost, int orderStatus) throws SQLException;

    /**
     * 查询指定用户的所有历史充电订单
     */
    List<ChargingOrder> findByUserId(Integer userId) throws SQLException;
}
