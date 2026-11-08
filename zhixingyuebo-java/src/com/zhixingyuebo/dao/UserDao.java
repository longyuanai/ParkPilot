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

import com.zhixingyuebo.entity.User;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 用户认证与信息数据访问接口
 */
public interface UserDao extends BaseDao<User, Integer> {

    /**
     * 根据手机号查询用户（登录核心凭据）
     */
    User findByPhone(String phone) throws SQLException;
    LocalDateTime findMonthlyCardUntil(Integer userId) throws SQLException;
    LocalDateTime findMonthlyCardUntilForUpdate(Integer userId) throws SQLException;
    int updateMonthlyCardUntil(Integer userId, LocalDateTime until) throws SQLException;
}
