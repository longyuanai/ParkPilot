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

    /** 免收前半小时，之后每个园区自然日向上取整到小时，单日最高 50 元。 */
    public static BigDecimal calculateParkingFee(LocalDateTime entry, LocalDateTime exit, BigDecimal hourlyRate) {
        LocalDateTime billableStart = entry.plusMinutes(30);
        if (!exit.isAfter(billableStart)) return new BigDecimal("0.00");
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            hourlyRate = new BigDecimal("4.00");
        }
        LocalDateTime nextMidnight = billableStart.toLocalDate().plusDays(1).atStartOfDay();
        BigDecimal total = dailyFee(billableStart, exit.isBefore(nextMidnight) ? exit : nextMidnight, hourlyRate);
        if (exit.isAfter(nextMidnight)) {
            LocalDateTime lastMidnight = exit.toLocalDate().atStartOfDay();
            long fullDays = ChronoUnit.DAYS.between(nextMidnight.toLocalDate(), lastMidnight.toLocalDate());
            BigDecimal fullDayFee = hourlyRate.multiply(BigDecimal.valueOf(24)).min(new BigDecimal("50.00"));
            total = total.add(fullDayFee.multiply(BigDecimal.valueOf(fullDays)))
                    .add(dailyFee(lastMidnight, exit, hourlyRate));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal dailyFee(LocalDateTime start, LocalDateTime end, BigDecimal hourlyRate) {
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        if (!duration.minusHours(hours).isZero()) hours++;
        return hourlyRate.multiply(BigDecimal.valueOf(hours)).min(new BigDecimal("50.00"));
    }
}
