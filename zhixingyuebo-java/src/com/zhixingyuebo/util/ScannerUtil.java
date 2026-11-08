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
import java.util.Scanner;

/**
 * 控制台用户输入安全读取工具类（TC-06 健壮性防御）
 * 彻底防止 InputMismatchException 与非法字符导致的系统崩溃闪退
 */
public class ScannerUtil {

    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * 读取指定范围内的整数序号（如菜单项：1~6）
     */
    public static int readInt(int min, int max, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println(FormatUtil.yellow("输入不能为空，请重新输入！"));
                continue;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < min || value > max) {
                    System.out.println(FormatUtil.yellow("输入指令有误，请输入有效菜单数字序号（" + min + " - " + max + "）！"));
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println(FormatUtil.yellow("输入指令有误，请输入有效菜单数字序号！"));
            }
        }
    }

    /**
     * 读取非空字符串
     */
    public static String readString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println(FormatUtil.yellow("输入内容不能为空，请重新输入！"));
        }
    }

    /**
     * 读取合法金额（BigDecimal，必须大于 0）
     */
    public static BigDecimal readPositiveBigDecimal(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = SCANNER.nextLine().trim();
            try {
                BigDecimal val = new BigDecimal(input);
                if (val.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.println(FormatUtil.yellow("数值必须大于 0，请重新输入！"));
                    continue;
                }
                return val;
            } catch (Exception e) {
                System.out.println(FormatUtil.yellow("请输入有效的数值格式（如 50.00）！"));
            }
        }
    }

    /**
     * 等待回车继续
     */
    public static void pressEnterToContinue() {
        System.out.print(FormatUtil.cyan("\n[ 按回车键继续... ]"));
        SCANNER.nextLine();
    }
}
