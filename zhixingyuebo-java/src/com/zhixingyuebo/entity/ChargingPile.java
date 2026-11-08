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

import com.zhixingyuebo.enums.PileStatus;

import java.math.BigDecimal;

/**
 * 智能充电桩设备实体（对应 tb_charging_pile）
 */
public class ChargingPile {

    private Integer pileId;
    private Integer spotId;
    private String pileCode;
    private BigDecimal powerKw;
    private BigDecimal pricePerKwh;
    private Integer pileStatus; // 1空闲就绪, 2正在充电, 3故障报警, 4离线检修

    public ChargingPile() {
    }

    public ChargingPile(Integer pileId, Integer spotId, String pileCode, BigDecimal powerKw, BigDecimal pricePerKwh, Integer pileStatus) {
        this.pileId = pileId;
        this.spotId = spotId;
        this.pileCode = pileCode;
        this.powerKw = powerKw;
        this.pricePerKwh = pricePerKwh;
        this.pileStatus = pileStatus;
    }

    public String getPileStatusName() {
        PileStatus status = getPileStatusEnum();
        return status != null ? status.getDescription() : "异常状态";
    }

    public boolean isReady() {
        return pileStatus != null && pileStatus == 1;
    }

    public PileStatus getPileStatusEnum() {
        return PileStatus.fromCode(pileStatus);
    }

    public void setPileStatusEnum(PileStatus status) {
        this.pileStatus = status != null ? status.getCode() : null;
    }

    public Integer getPileId() {
        return pileId;
    }

    public void setPileId(Integer pileId) {
        this.pileId = pileId;
    }

    public Integer getSpotId() {
        return spotId;
    }

    public void setSpotId(Integer spotId) {
        this.spotId = spotId;
    }

    public String getPileCode() {
        return pileCode;
    }

    public void setPileCode(String pileCode) {
        this.pileCode = pileCode;
    }

    public BigDecimal getPowerKw() {
        return powerKw;
    }

    public void setPowerKw(BigDecimal powerKw) {
        this.powerKw = powerKw;
    }

    public BigDecimal getPricePerKwh() {
        return pricePerKwh;
    }

    public void setPricePerKwh(BigDecimal pricePerKwh) {
        this.pricePerKwh = pricePerKwh;
    }

    public Integer getPileStatus() {
        return pileStatus;
    }

    public void setPileStatus(Integer pileStatus) {
        this.pileStatus = pileStatus;
    }

    @Override
    public String toString() {
        return "ChargingPile{" +
                "pileId=" + pileId +
                ", spotId=" + spotId +
                ", pileCode='" + pileCode + '\'' +
                ", powerKw=" + powerKw + " kW" +
                ", pricePerKwh=¥" + pricePerKwh + "/度" +
                ", pileStatus=" + getPileStatusName() +
                '}';
    }
}
