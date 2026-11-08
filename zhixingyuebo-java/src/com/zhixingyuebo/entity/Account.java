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
package com.zhixingyuebo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车主虚拟钱包账户实体（对应 tb_account）
 */
public class Account {

    private Integer accountId; // 与 user_id 严格 1:1 对应
    private BigDecimal balance;
    private BigDecimal frozenMoney;
    private LocalDateTime updatedAt;

    public Account() {
    }

    public Account(Integer accountId, BigDecimal balance, BigDecimal frozenMoney, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.balance = balance;
        this.frozenMoney = frozenMoney;
        this.updatedAt = updatedAt;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getFrozenMoney() {
        return frozenMoney;
    }

    public void setFrozenMoney(BigDecimal frozenMoney) {
        this.frozenMoney = frozenMoney;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Account{" +
                "accountId=" + accountId +
                ", balance=" + balance +
                ", frozenMoney=" + frozenMoney +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
