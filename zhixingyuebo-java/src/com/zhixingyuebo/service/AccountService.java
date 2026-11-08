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
package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.Account;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.math.BigDecimal;
import java.util.List;

/**
 * 钱包账户查询、模拟充值与资金对账流水业务接口
 */
public interface AccountService {

    /**
     * 获取指定用户的钱包账户信息
     */
    Account getAccount(Integer userId) throws ZhiXingYueBoException;

    /**
     * 钱包模拟充值（原子写入审计流水）
     */
    Account recharge(Integer userId, BigDecimal amount) throws ZhiXingYueBoException;

    /**
     * 查询指定车主名下的所有变动流水明细
     */
    List<AccountFlow> getFlowsByUserId(Integer userId) throws ZhiXingYueBoException;

    /**
     * 查询全园区资金变动流水总台账（物业管理员审计视角）
     */
    List<AccountFlow> getAllFlows() throws ZhiXingYueBoException;
}
