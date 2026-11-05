package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.ParkingSpot;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 车位大盘查询、状态翻转与锁定接口
 */
public interface ParkingSpotDao extends BaseDao<ParkingSpot, Integer> {

    /**
     * 根据车位物理编号检索
     */
    ParkingSpot findBySpotNumber(String spotNumber) throws SQLException;

    /**
     * 多条件筛选空闲车位（按车位属性/区域过滤）
     */
    List<ParkingSpot> findAvailableSpots(Integer spotType, Integer lotId, LocalDateTime now) throws SQLException;

    ParkingSpot findByIdForUpdate(Integer spotId) throws SQLException;

    void reserve(Integer spotId, Integer userId, LocalDateTime until) throws SQLException;

    /**
     * 原子翻转车位状态（0空闲, 1已被预约, 2车辆已泊入, 3维护禁用）
     */
    int updateStatus(Integer spotId, int newStatus) throws SQLException;
}
