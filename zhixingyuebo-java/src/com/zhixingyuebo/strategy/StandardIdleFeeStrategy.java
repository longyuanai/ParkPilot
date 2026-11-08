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
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 充电桩超时占位滞纳金（Idle Fee）策略接口与实现：
 * 解决电车充完电后长期霸占充电车位的行业痛点。
 * 业务规则：
 * 1. 拔枪结束后给予 15 分钟免费移车宽限期；
 * 2. 超过 15 分钟后，每超期 1 分钟加收 0.50 元滞纳金；
 * 3. 单次离场滞纳金封顶 50.00 元。
 */
public class StandardIdleFeeStrategy {

    public static final int GRACE_PERIOD_MINUTES = 15;
    public static final BigDecimal RATE_PER_MINUTE = new BigDecimal("0.50");
    public static final BigDecimal MAX_IDLE_FEE = new BigDecimal("50.00");

    /**
     * 计算超时占位滞留费（违规滞纳金）
     *
     * @param chargeEndTime 充电拔枪完成时刻
     * @param exitTime      车辆道闸离场时刻
     * @return 滞留滞纳金（元）
     */
    public BigDecimal calculateIdleFee(LocalDateTime chargeEndTime, LocalDateTime exitTime) {
        if (chargeEndTime == null || exitTime == null || !exitTime.isAfter(chargeEndTime)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long minutesSinceCharge = Duration.between(chargeEndTime, exitTime).toMinutes();
        if (minutesSinceCharge <= GRACE_PERIOD_MINUTES) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long overtimeMinutes = minutesSinceCharge - GRACE_PERIOD_MINUTES;
        BigDecimal fee = RATE_PER_MINUTE.multiply(BigDecimal.valueOf(overtimeMinutes));
        return fee.min(MAX_IDLE_FEE).setScale(2, RoundingMode.HALF_UP);
    }
}
