package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.ParkingRecord;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 泊车进场登记、离场计费与出场回填接口
 */
public interface ParkingRecordDao extends BaseDao<ParkingRecord, Long> {

    ParkingRecord findActiveRecordByVehicleIdForUpdate(Integer vehicleId) throws SQLException;

    /**
     * 查询指定车辆当前在场且尚未结算的停车单（exit_time 为 NULL）
     */
    ParkingRecord findActiveRecordByVehicleId(Integer vehicleId) throws SQLException;

    /**
     * 查询指定车位当前停泊的未结单据
     */
    ParkingRecord findActiveRecordBySpotId(Integer spotId) throws SQLException;
    ParkingRecord findActiveRecordBySpotIdForUpdate(Integer spotId) throws SQLException;

    /**
     * 回填出场时刻、总停泊分钟数、应收实收金额及支付结算状态（事务核心）
     */
    int finishParkingRecord(Long recordId, LocalDateTime exitTime, int parkMinutes, BigDecimal totalFee, int payStatus) throws SQLException;

    /**
     * 查询所有在场未出场的记录
     */
    List<ParkingRecord> findActiveRecords() throws SQLException;
}
