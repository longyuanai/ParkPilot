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
package com.zhixingyuebo.pool;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 原生轻量级 JDBC 连接池规范接口（零第三方依赖，纯 Java SE 实现）
 */
public interface ConnectionPool extends AutoCloseable {

    /**
     * 从连接池获取可用数据库连接（若繁忙则阻塞等待超时）
     */
    Connection getConnection() throws SQLException;

    /**
     * 获取当前处于使用借出状态的连接数
     */
    int getActiveCount();

    /**
     * 获取当前空闲可用的连接数
     */
    int getIdleCount();

    /**
     * 获取当前连接池物理维护的连接总数（空闲+借出）
     */
    int getTotalCount();

    /**
     * 获取连接池绑定的数据库 JDBC URL
     */
    String getUrl();

    /**
     * 优雅关闭连接池，释放所有物理底层连接
     */
    @Override
    void close();
}
