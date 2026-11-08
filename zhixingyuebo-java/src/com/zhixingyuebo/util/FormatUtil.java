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

/**
 * 控制台终端美化、表格对齐与字符颜色工具
 */
public class FormatUtil {

    // ANSI 颜色转义字符（Windows 10/11 Terminal 原生支持）
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String BOLD = "\u001B[1m";

    public static String red(String text) {
        return RED + text + RESET;
    }

    public static String green(String text) {
        return GREEN + text + RESET;
    }

    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }

    public static String blue(String text) {
        return BLUE + text + RESET;
    }

    public static String cyan(String text) {
        return CYAN + text + RESET;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    /**
     * 格式化金额为带符号和两位的字符串：¥12.50
     */
    public static String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "¥0.00";
        }
        return "¥" + amount.setScale(2).toPlainString();
    }

    /**
     * 打印主标题横幅
     */
    public static void printBanner(String title) {
        System.out.println(CYAN + "========================================================================" + RESET);
        System.out.println(BOLD + CYAN + "               " + title + RESET);
        System.out.println(CYAN + "========================================================================" + RESET);
    }

    /**
     * 打印分节标题
     */
    public static void printSection(String sectionName) {
        System.out.println(YELLOW + ">>> 【" + sectionName + "】 " + RESET);
    }

    /**
     * 生成 ASCII 可视化进度条（支持 ANSI 颜色高亮与百分比渲染）
     */
    public static String getProgressBar(int current, int total, int width, String color) {
        if (total <= 0) total = 1;
        double ratio = Math.min(1.0, Math.max(0.0, (double) current / total));
        int filled = (int) Math.round(ratio * width);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < width; i++) {
            if (i < filled) sb.append("=");
            else if (i == filled) sb.append(">");
            else sb.append(" ");
        }
        sb.append(String.format("] %5.1f%% (%d/%d)", ratio * 100.0, current, total));
        return color != null ? color + sb.toString() + RESET : sb.toString();
    }
}
