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

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车主停车月卡办理与续费历史台账实体类（对应 tb_monthly_card，完善任务书 1:n 办理月卡设计）
 */
public class MonthlyCard implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long cardId;
    private Integer userId;
    private String cardType;
    private BigDecimal price;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;

    public MonthlyCard() {
    }

    public MonthlyCard(Long cardId, Integer userId, String cardType, BigDecimal price,
                       LocalDateTime startTime, LocalDateTime endTime, LocalDateTime createdAt) {
        this.cardId = cardId;
        this.userId = userId;
        this.cardType = cardType;
        this.price = price;
        this.startTime = startTime;
        this.endTime = endTime;
        this.createdAt = createdAt;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "MonthlyCard{" +
                "cardId=" + cardId +
                ", userId=" + userId +
                ", cardType='" + cardType + '\'' +
                ", price=" + price +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", createdAt=" + createdAt +
                '}';
    }
}
