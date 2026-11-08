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

import java.sql.SQLException;
import java.util.List;

/**
 * 通用泛型 DAO 基础接口（定义 CRUD 规范契约）
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface BaseDao<T, ID> {

    int insert(T entity) throws SQLException;

    int update(T entity) throws SQLException;

    int deleteById(ID id) throws SQLException;

    T findById(ID id) throws SQLException;

    List<T> findAll() throws SQLException;
}
