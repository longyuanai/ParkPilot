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
package com.zhixingyuebo.view;

import com.zhixingyuebo.entity.*;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.AccountService;
import com.zhixingyuebo.service.ChargingService;
import com.zhixingyuebo.service.ParkingService;
import com.zhixingyuebo.service.UserService;
import com.zhixingyuebo.util.CsvExportUtil;
import com.zhixingyuebo.util.DateUtil;
import com.zhixingyuebo.util.FormatUtil;
import com.zhixingyuebo.util.ScannerUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 物业运营综合管理员专属交互视图控制器（解耦自 ConsoleMenu）
 */
public class AdminConsoleView {

    private final ParkingService parkingService;
    private final ChargingService chargingService;
    private final AccountService accountService;
    private final UserService userService;

    public AdminConsoleView(ParkingService parkingService, ChargingService chargingService,
                            AccountService accountService, UserService userService) {
        this.parkingService = parkingService;
        this.chargingService = chargingService;
        this.accountService = accountService;
        this.userService = userService;
    }

    public void handleAdminConsole(Admin admin) {
        while (true) {
            admin.showMenu();
            int choice = ScannerUtil.readInt(0, 6, "请输入物业管理员菜单编号 [0-6]: ");
            if (choice == 0) {
                System.out.println(FormatUtil.cyan("已退出管理员工作台。"));
                break;
            }

            try {
                switch (choice) {
                    case 1:
                        showPanoramaBoard();
                        break;
                    case 2:
                        showActiveParkingRecords();
                        break;
                    case 3:
                        showAuditFlows();
                        break;
                    case 4:
                        doExportDailyCsvReport(); // 创新 B
                        break;
                    case 5:
                        doManageSpots();
                        break;
                    case 6:
                        doSetMonthlyCard(admin);
                        break;
                }
            } catch (ZhiXingYueBoException e) {
                System.out.println(FormatUtil.red("【运营管理提示】: " + e.getMessage()));
            } catch (Exception e) {
                System.out.println(FormatUtil.red("【系统异常】: " + e.getMessage()));
            }
            ScannerUtil.pressEnterToContinue();
        }
    }

    public void showPanoramaBoard() throws ZhiXingYueBoException {
        FormatUtil.printSection("全园区车位与充电桩运营全景大盘 (借鉴 smartparking 可视化看板)");
        List<ParkingSpot> spots = parkingService.getAllSpots();
        int totalSpots = spots.size();
        int freeSpots = 0, reservedSpots = 0, occupiedSpots = 0, maintenanceSpots = 0;
        for (ParkingSpot s : spots) {
            if (s.getSpotStatus() == 0) freeSpots++;
            else if (s.getSpotStatus() == 1) reservedSpots++;
            else if (s.getSpotStatus() == 2) occupiedSpots++;
            else if (s.getSpotStatus() == 3) maintenanceSpots++;
        }

        List<ChargingPile> piles = chargingService.getAllPiles();
        int totalPiles = piles.size();
        int readyPiles = 0, chargingPiles = 0, faultPiles = 0;
        for (ChargingPile p : piles) {
            if (p.getPileStatus() == 1) readyPiles++;
            else if (p.getPileStatus() == 2) chargingPiles++;
            else if (p.getPileStatus() == 3) faultPiles++;
        }

        // ASCII 仪表盘渲染
        System.out.println("【核心资产运行指标与占用监控】:");
        int activeOccupied = occupiedSpots + reservedSpots;
        System.out.print("  • 园区车位综合占用率: ");
        System.out.println(FormatUtil.getProgressBar(activeOccupied, totalSpots, 25, activeOccupied > 0 ? FormatUtil.YELLOW : FormatUtil.GREEN));
        System.out.print("  • 充电设备健康就绪率: ");
        System.out.println(FormatUtil.getProgressBar(readyPiles, totalPiles, 25, readyPiles == totalPiles ? FormatUtil.GREEN : FormatUtil.CYAN));

        System.out.printf("%n车位容量总计: %d ｜ 空闲: %s ｜ 已预约: %s ｜ 已泊入: %s ｜ 维护禁用: %s%n",
                totalSpots, FormatUtil.green(String.valueOf(freeSpots)), FormatUtil.yellow(String.valueOf(reservedSpots)), FormatUtil.red(String.valueOf(occupiedSpots)), FormatUtil.red(String.valueOf(maintenanceSpots)));
        System.out.printf("充电桩总计: %d ｜ 空闲就绪: %s ｜ 正在充电: %s ｜ 故障告警: %s%n",
                totalPiles, FormatUtil.green(String.valueOf(readyPiles)), FormatUtil.cyan(String.valueOf(chargingPiles)), FormatUtil.red(String.valueOf(faultPiles)));

        System.out.println("\n车位单元实时状态分布明细:");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-8s %-12s %-18s %-12s%n", "车位ID", "物理编号", "车位属性", "当前状态");
        System.out.println("------------------------------------------------------------");
        for (ParkingSpot s : spots) {
            String statusColor;
            switch (s.getSpotStatus()) {
                case 0: statusColor = FormatUtil.green(s.getSpotStatusName()); break;
                case 1: statusColor = FormatUtil.yellow(s.getSpotStatusName()); break;
                case 2: statusColor = FormatUtil.red(s.getSpotStatusName()); break;
                default: statusColor = FormatUtil.red(s.getSpotStatusName()); break;
            }
            System.out.printf("%-8d %-12s %-18s %-12s%n", s.getSpotId(), s.getSpotNumber(), s.getSpotTypeName(), statusColor);
        }
        System.out.println("------------------------------------------------------------");
    }

    public void showActiveParkingRecords() throws ZhiXingYueBoException {
        FormatUtil.printSection("全场当前在场停泊车辆台账");
        List<ParkingRecord> list = parkingService.getAllActiveRecords();
        if (list.isEmpty()) {
            System.out.println(FormatUtil.green("当前全园区场内无在场停泊车辆。"));
            return;
        }

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-10s %-10s %-10s %-22s %-12s%n", "单据号", "车辆ID", "车位ID", "入场时刻", "停泊时长估算");
        System.out.println("--------------------------------------------------------------------------------");
        for (ParkingRecord pr : list) {
            long mins = DateUtil.calculateMinutes(pr.getEntryTime(), java.time.LocalDateTime.now());
            System.out.printf("%-10d %-10d %-10d %-22s %-12s%n",
                    pr.getRecordId(), pr.getVehicleId(), pr.getSpotId(), DateUtil.format(pr.getEntryTime()), mins + " 分钟");
        }
        System.out.println("--------------------------------------------------------------------------------");
    }

    public void showAuditFlows() throws ZhiXingYueBoException {
        FormatUtil.printSection("资金变动只读审计黑匣子流水总台账");
        List<AccountFlow> list = accountService.getAllFlows();
        if (list.isEmpty()) {
            System.out.println("暂无资金变动流水。");
            return;
        }

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.printf("%-8s %-8s %-12s %-12s %-12s %-14s %-20s%n", "流水号", "账户ID", "变动前", "变动后", "发生差额", "业务类型", "产生时刻");
        System.out.println("----------------------------------------------------------------------------------------");
        for (AccountFlow af : list) {
            String diffStr = af.getDiffAmount().compareTo(BigDecimal.ZERO) >= 0 ?
                    FormatUtil.green("+" + af.getDiffAmount()) : FormatUtil.red(af.getDiffAmount().toString());
            System.out.printf("%-8d %-8d %-12s %-12s %-12s %-14s %-20s%n",
                    af.getFlowId(), af.getAccountId(), FormatUtil.formatMoney(af.getBeforeBalance()), FormatUtil.formatMoney(af.getAfterBalance()), diffStr, af.getBizType(), DateUtil.format(af.getCreatedAt()));
        }
        System.out.println("----------------------------------------------------------------------------------------");
    }

    public void doExportDailyCsvReport() throws Exception {
        FormatUtil.printSection("【创新 B】导出每日车流吞吐高峰与财务营收 CSV 报表");
        System.out.println("正在通过 Java 缓冲流（BufferedWriter）与 UTF-8 BOM 编码聚合生成报表...");

        String path = CsvExportUtil.exportDailyReport("reports");
        System.out.println(FormatUtil.green("\n>>> 【导出成功】！标准 CSV 报表已成功生成："));
        System.out.println(FormatUtil.bold(FormatUtil.cyan(path)));
        System.out.println("提示：已内置 UTF-8 BOM 签名，支持直接双击使用 Microsoft Excel 或 WPS 无中文乱码打开！");
    }

    public void doManageSpots() throws ZhiXingYueBoException {
        FormatUtil.printSection("车位与资产维护");
        System.out.println("  1. 新增规划车位");
        System.out.println("  2. 修改车位状态 (空闲 / 维护禁用)");
        System.out.println("  0. 返回");
        int opt = ScannerUtil.readInt(0, 2, "请选择操作 [0-2]: ");
        if (opt == 1) {
            String spotNumber = ScannerUtil.readString("请输入新车位编号（如：A-0106）: ");
            int type = ScannerUtil.readInt(1, 3, "请选择车位类型 (1普通, 2充电桩位, 3VIP位): ");
            ParkingSpot spot = new ParkingSpot(null, 1, spotNumber, type, 0, 0);
            try {
                parkingService.addSpot(spot);
                System.out.println(FormatUtil.green(">>> 新车位 [" + spotNumber + "] 录入成功！"));
            } catch (Exception e) {
                System.out.println(FormatUtil.red("录入失败: " + e.getMessage()));
            }
        } else if (opt == 2) {
            int spotId = ScannerUtil.readInt(1, 9999, "请输入要修改的车位ID: ");
            int status = ScannerUtil.readInt(0, 3, "请输入新状态 (0空闲, 3维护禁用): ");
            try {
                parkingService.updateSpotStatus(spotId, status);
                System.out.println(FormatUtil.green(">>> 车位状态更新成功！"));
            } catch (Exception e) {
                System.out.println(FormatUtil.red("修改失败: " + e.getMessage()));
            }
        }
    }

    public void doSetMonthlyCard(Admin admin) throws ZhiXingYueBoException {
        String phone = ScannerUtil.readString("请输入车主手机号: ");
        User owner = userService.login(phone);
        String value = ScannerUtil.readString("请输入月卡到期时间（yyyy-MM-dd HH:mm:ss）: ");
        try {
            LocalDateTime until = DateUtil.parse(value);
            userService.setMonthlyCardUntil(admin.getUserId(), owner.getUserId(), until);
            System.out.println(FormatUtil.green("月卡已登记，有效至 " + DateUtil.format(until)));
        } catch (DateTimeParseException e) {
            throw new ZhiXingYueBoException("时间格式无效，请按 yyyy-MM-dd HH:mm:ss 输入。");
        }
    }
}
