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
package com.zhixingyuebo.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 基础固定单价充电计费策略（默认标准）：
 * 实付金额 = 充入电量 * 设备设定综合单价
 */
public class FlatChargingFeeStrategy implements ChargingFeeStrategy {

    @Override
    public BigDecimal calculateCost(LocalDateTime startTime, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal basePricePerKwh) {
        if (kwhAmount == null || basePricePerKwh == null || kwhAmount.signum() <= 0) {
            return new BigDecimal("0.00");
        }
        return kwhAmount.multiply(basePricePerKwh).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getStrategyName() {
        return "基准固定费率策略（电量 × 基准单价）";
    }
}
