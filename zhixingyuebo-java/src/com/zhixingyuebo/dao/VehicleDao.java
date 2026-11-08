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

import com.zhixingyuebo.entity.Vehicle;

import java.sql.SQLException;
import java.util.List;

/**
 * 机动车档案增删改查与车牌唯一性检索接口
 */
public interface VehicleDao extends BaseDao<Vehicle, Integer> {

    /**
     * 根据车牌号精确检索车辆
     */
    Vehicle findByPlateNumber(String plateNumber) throws SQLException;
    Vehicle findByPlateNumberForUpdate(String plateNumber) throws SQLException;

    /**
     * 根据车主ID查询名下所有已绑定的车辆档案（1:n）
     */
    List<Vehicle> findByUserId(Integer userId) throws SQLException;
}
