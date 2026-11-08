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
import java.time.LocalTime;

/**
 * 工业级峰谷平时段分时充电计费策略（借鉴 steve / 智能电网标准）：
 * - 峰时 (Peak): 10:00 - 12:00, 18:00 - 21:00 (浮动系数 1.30)
 * - 平时 (Flat): 08:00 - 10:00, 12:00 - 18:00, 21:00 - 23:00 (浮动系数 1.00)
 * - 谷时 (Valley): 23:00 - 次日 08:00 (浮动系数 0.60 惠民低碳)
 */
public class PeakValleyChargingFeeStrategy implements ChargingFeeStrategy {

    private static final BigDecimal PEAK_RATIO = new BigDecimal("1.30");
    private static final BigDecimal FLAT_RATIO = new BigDecimal("1.00");
    private static final BigDecimal VALLEY_RATIO = new BigDecimal("0.60");

    @Override
    public BigDecimal calculateCost(LocalDateTime startTime, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal basePricePerKwh) {
        if (kwhAmount == null || basePricePerKwh == null || kwhAmount.signum() <= 0) {
            return new BigDecimal("0.00");
        }
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            // 时间异常兜底按平时基准价
            return kwhAmount.multiply(basePricePerKwh).setScale(2, RoundingMode.HALF_UP);
        }

        // 以 15 分钟为一个计量离散片元（Slot），统计峰/平/谷各时段所占总片元数
        long totalMinutes = Math.max(1, Duration.between(startTime, endTime).toMinutes());
        long peakMinutes = 0;
        long flatMinutes = 0;
        long valleyMinutes = 0;

        LocalDateTime cursor = startTime;
        while (cursor.isBefore(endTime)) {
            LocalDateTime next = cursor.plusMinutes(1);
            if (next.isAfter(endTime)) {
                next = endTime;
            }
            long step = Duration.between(cursor, next).toMinutes();
            if (step <= 0) {
                step = 1;
            }
            LocalTime t = cursor.toLocalTime();
            if (isPeakTime(t)) {
                peakMinutes += step;
            } else if (isValleyTime(t)) {
                valleyMinutes += step;
            } else {
                flatMinutes += step;
            }
            cursor = cursor.plusMinutes(Math.max(1, step));
        }

        BigDecimal totalMinBd = BigDecimal.valueOf(totalMinutes);
        BigDecimal peakKwh = kwhAmount.multiply(BigDecimal.valueOf(peakMinutes)).divide(totalMinBd, 4, RoundingMode.HALF_UP);
        BigDecimal valleyKwh = kwhAmount.multiply(BigDecimal.valueOf(valleyMinutes)).divide(totalMinBd, 4, RoundingMode.HALF_UP);
        BigDecimal flatKwh = kwhAmount.subtract(peakKwh).subtract(valleyKwh);
        if (flatKwh.signum() < 0) {
            flatKwh = BigDecimal.ZERO;
        }

        BigDecimal peakCost = peakKwh.multiply(basePricePerKwh.multiply(PEAK_RATIO));
        BigDecimal flatCost = flatKwh.multiply(basePricePerKwh.multiply(FLAT_RATIO));
        BigDecimal valleyCost = valleyKwh.multiply(basePricePerKwh.multiply(VALLEY_RATIO));

        return peakCost.add(flatCost).add(valleyCost).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isPeakTime(LocalTime time) {
        // 10:00 - 12:00 或 18:00 - 21:00
        return (!time.isBefore(LocalTime.of(10, 0)) && time.isBefore(LocalTime.of(12, 0)))
                || (!time.isBefore(LocalTime.of(18, 0)) && time.isBefore(LocalTime.of(21, 0)));
    }

    private boolean isValleyTime(LocalTime time) {
        // 23:00 - 24:00 或 00:00 - 08:00
        return !time.isBefore(LocalTime.of(23, 0)) || time.isBefore(LocalTime.of(8, 0));
    }

    @Override
    public String getStrategyName() {
        return "分时峰谷平时段计费（峰时1.3x/平时1.0x/谷时0.6x）";
    }
}
