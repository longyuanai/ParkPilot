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
import java.time.LocalDateTime;

/**
 * 新能源汽车充电计费策略接口（开闭原则 OCP 扩展）
 */
public interface ChargingFeeStrategy {

    /**
     * 核算实际充电总电费
     *
     * @param startTime        插枪启动充电时刻
     * @param endTime          拔枪停止充电时刻
     * @param kwhAmount        充入总电量（度/kWh）
     * @param basePricePerKwh  设备综合基准单价（元/度）
     * @return 实收电费（两位小数精度）
     */
    BigDecimal calculateCost(LocalDateTime startTime, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal basePricePerKwh);

    /**
     * 策略名称
     */
    String getStrategyName();
}
