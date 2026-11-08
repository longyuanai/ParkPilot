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

import com.zhixingyuebo.entity.AccountFlow;

import java.sql.SQLException;
import java.util.List;

/**
 * 资金变动防篡改流水单向插入与审计检索接口（金融级黑匣子）
 */
public interface AccountFlowDao extends BaseDao<AccountFlow, Long> {

    /**
     * 根据账户ID检索其全量审计资金变动流水
     */
    List<AccountFlow> findByAccountId(Integer accountId) throws SQLException;
}
