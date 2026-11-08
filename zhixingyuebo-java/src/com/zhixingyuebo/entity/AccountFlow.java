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
 * 资金变动只读审计流水实体（对应 tb_account_flow）
 */
public class AccountFlow {
    public static final String MONTHLY_CARD_PAYMENT = "月卡扣费";


    private Long flowId;
    private Integer accountId;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private BigDecimal diffAmount;
    private String bizType; // 停车扣费/充电扣款/账户充值/违规滞纳金
    private LocalDateTime createdAt;

    public AccountFlow() {
    }

    public AccountFlow(Long flowId, Integer accountId, BigDecimal beforeBalance, BigDecimal afterBalance, BigDecimal diffAmount, String bizType, LocalDateTime createdAt) {
        this.flowId = flowId;
        this.accountId = accountId;
        this.beforeBalance = beforeBalance;
        this.afterBalance = afterBalance;
        this.diffAmount = diffAmount;
        this.bizType = bizType;
        this.createdAt = createdAt;
    }

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getBeforeBalance() {
        return beforeBalance;
    }

    public void setBeforeBalance(BigDecimal beforeBalance) {
        this.beforeBalance = beforeBalance;
    }

    public BigDecimal getAfterBalance() {
        return afterBalance;
    }

    public void setAfterBalance(BigDecimal afterBalance) {
        this.afterBalance = afterBalance;
    }

    public BigDecimal getDiffAmount() {
        return diffAmount;
    }

    public void setDiffAmount(BigDecimal diffAmount) {
        this.diffAmount = diffAmount;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "AccountFlow{" +
                "flowId=" + flowId +
                ", accountId=" + accountId +
                ", beforeBalance=" + beforeBalance +
                ", afterBalance=" + afterBalance +
                ", diffAmount=" + diffAmount +
                ", bizType='" + bizType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
