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
import java.time.temporal.ChronoUnit;

/**
 * 园区标准阶梯停车计费策略：
 * 1. 生效月卡直接全免（0.00 元）；
 * 2. 入场前 30 分钟免费；
 * 3. 超过 30 分钟按小时向上取整（不足 1 小时按 1 小时计费）；
 * 4. 自然日内 50.00 元封顶，跨日累加。
 */
public class StandardParkingFeeStrategy implements ParkingFeeStrategy {

    public static final BigDecimal DAILY_CAP = new BigDecimal("50.00");
    public static final BigDecimal DEFAULT_HOURLY_RATE = new BigDecimal("4.00");
    public static final int FREE_MINUTES = 30;

    @Override
    public BigDecimal calculateFee(LocalDateTime entry, LocalDateTime exit, BigDecimal hourlyRate, boolean hasValidMonthlyCard) {
        if (hasValidMonthlyCard) {
            return new BigDecimal("0.00");
        }
        if (entry == null || exit == null || !exit.isAfter(entry)) {
            return new BigDecimal("0.00");
        }
        LocalDateTime billableStart = entry.plusMinutes(FREE_MINUTES);
        if (!exit.isAfter(billableStart)) {
            return new BigDecimal("0.00");
        }
        BigDecimal rate = (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0)
                ? DEFAULT_HOURLY_RATE : hourlyRate;

        LocalDateTime nextMidnight = billableStart.toLocalDate().plusDays(1).atStartOfDay();
        BigDecimal total = calculateDailyIntervalFee(billableStart, exit.isBefore(nextMidnight) ? exit : nextMidnight, rate);

        if (exit.isAfter(nextMidnight)) {
            LocalDateTime lastMidnight = exit.toLocalDate().atStartOfDay();
            long fullDays = ChronoUnit.DAYS.between(nextMidnight.toLocalDate(), lastMidnight.toLocalDate());
            BigDecimal fullDayFee = rate.multiply(BigDecimal.valueOf(24)).min(DAILY_CAP);
            total = total.add(fullDayFee.multiply(BigDecimal.valueOf(fullDays)))
                    .add(calculateDailyIntervalFee(lastMidnight, exit, rate));
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyIntervalFee(LocalDateTime start, LocalDateTime end, BigDecimal rate) {
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        if (!duration.minusHours(hours).isZero()) {
            hours++;
        }
        return rate.multiply(BigDecimal.valueOf(hours)).min(DAILY_CAP);
    }

    @Override
    public String getStrategyName() {
        return "园区标准阶梯计费（前30分免费/50元日封顶）";
    }
}
