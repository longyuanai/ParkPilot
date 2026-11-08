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
package com.zhixingyuebo.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 原生时间折算与停车分钟数/小时阶梯核算工具
 */
public class DateUtil {

    public static final DateTimeFormatter STANDARD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 格式化 LocalDateTime
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(STANDARD_FORMATTER);
    }

    /**
     * 解析字符串为 LocalDateTime
     */
    public static LocalDateTime parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(text.trim(), STANDARD_FORMATTER);
    }

    /**
     * 计算两时刻之间的分钟差（入场到离场）
     */
    public static long calculateMinutes(LocalDateTime entryTime, LocalDateTime exitTime) {
        if (entryTime == null || exitTime == null) {
            return 0;
        }
        Duration duration = Duration.between(entryTime, exitTime);
        long minutes = duration.toMinutes();
        return Math.max(minutes, 0);
    }

    /** 免收前半小时，之后每个园区自然日向上取整到小时，单日最高 50 元（委托计费策略引擎）。 */
    public static BigDecimal calculateParkingFee(LocalDateTime entry, LocalDateTime exit, BigDecimal hourlyRate) {
        return com.zhixingyuebo.strategy.FeeStrategyFactory.getParkingFeeStrategy()
                .calculateFee(entry, exit, hourlyRate, false);
    }
}
