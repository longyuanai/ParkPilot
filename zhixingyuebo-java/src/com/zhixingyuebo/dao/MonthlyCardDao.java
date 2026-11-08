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

import com.zhixingyuebo.entity.MonthlyCard;

import java.sql.SQLException;
import java.util.List;

/**
 * 车主停车月卡数据访问接口（tb_monthly_card 数据契约）
 */
public interface MonthlyCardDao extends BaseDao<MonthlyCard, Long> {

    /**
     * 查询指定车主名下的全部月卡办理与续费历史
     */
    List<MonthlyCard> findByUserId(Integer userId) throws SQLException;

    /**
     * 查询指定车主最新的一笔月卡办理记录
     */
    MonthlyCard findLatestByUserId(Integer userId) throws SQLException;
}
