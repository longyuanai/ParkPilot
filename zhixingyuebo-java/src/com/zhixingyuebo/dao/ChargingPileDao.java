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

import com.zhixingyuebo.entity.ChargingPile;

import java.sql.SQLException;

/**
 * 充电桩硬件设备状态流转与电价检索接口
 */
public interface ChargingPileDao extends BaseDao<ChargingPile, Integer> {
    ChargingPile findByIdForUpdate(Integer pileId) throws SQLException;
    int compareAndSetStatus(Integer pileId, int expected, int next) throws SQLException;


    /**
     * 根据搭载车位ID查询绑定的充电桩（1:1）
     */
    ChargingPile findBySpotId(Integer spotId) throws SQLException;
    ChargingPile findBySpotIdForUpdate(Integer spotId) throws SQLException;

    /**
     * 根据充电桩资产条码查询
     */
    ChargingPile findByPileCode(String pileCode) throws SQLException;

    /**
     * 更新充电桩硬件工作状态（1空闲就绪, 2正在充电, 3故障报警, 4离线检修）
     */
    int updateStatus(Integer pileId, int newStatus) throws SQLException;
}
