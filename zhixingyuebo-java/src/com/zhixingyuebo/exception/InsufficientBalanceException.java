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
package com.zhixingyuebo.exception;

import java.math.BigDecimal;

/**
 * 钱包可用余额不足受检异常（阻断抬杆/拔枪结算，触发事务回滚）
 */
public class InsufficientBalanceException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final BigDecimal currentBalance;
    private final BigDecimal requiredAmount;

    public InsufficientBalanceException(BigDecimal currentBalance, BigDecimal requiredAmount) {
        super(String.format("钱包可用余额不足！当前可用余额: ¥%s，应付金额: ¥%s，尚差: ¥%s",
                currentBalance.setScale(2).toPlainString(),
                requiredAmount.setScale(2).toPlainString(),
                requiredAmount.subtract(currentBalance).setScale(2).toPlainString()));
        this.currentBalance = currentBalance;
        this.requiredAmount = requiredAmount;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }
}
