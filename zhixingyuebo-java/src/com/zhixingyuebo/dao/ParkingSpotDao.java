/*
 * Copyright (c) 2026 Jiawei Huang (黄家威). All Rights Reserved.
 *
 * Project: ParkPilot (智泊领航) - Smart Parking & EV Charging Core Engine
 * Licensed under the PolyForm Noncommercial License 1.0.0 (the "License").
 * You may not use this file except in compliance with the License.
 *
 * Commercial Use strictly requires a separate commercial license from the author.
 * For commercial licensing, please refer to COMMERCIAL.md or contact: 1507205727@qq.com
 */
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
