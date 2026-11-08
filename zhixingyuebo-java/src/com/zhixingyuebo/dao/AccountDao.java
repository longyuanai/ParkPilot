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

import com.zhixingyuebo.entity.Account;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * 钱包余额查询、可用余额扣减与充值接口
 */
public interface AccountDao extends BaseDao<Account, Integer> {

    Account findByIdForUpdate(Integer accountId) throws SQLException;

    /**
     * 账户充值（可用余额增加）
     */
    int recharge(Integer accountId, BigDecimal amount) throws SQLException;

    /**
     * 原子安全扣减可用余额（带有余额非负防穿透约束）
     */
    int deductBalance(Integer accountId, BigDecimal amount) throws SQLException;

    /**
     * 直接更新可用余额数值
     */
    int updateBalance(Integer accountId, BigDecimal newBalance) throws SQLException;
}
