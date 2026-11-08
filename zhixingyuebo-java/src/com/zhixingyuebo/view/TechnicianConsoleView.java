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

import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.entity.PileFaultLog;
import com.zhixingyuebo.entity.Technician;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.ChargingService;
import com.zhixingyuebo.service.FaultService;
import com.zhixingyuebo.util.DateUtil;
import com.zhixingyuebo.util.FormatUtil;
import com.zhixingyuebo.util.ScannerUtil;

import java.util.List;

/**
 * 充电桩运维技师专属交互视图控制器（解耦自 ConsoleMenu）
 */
public class TechnicianConsoleView {

    private final FaultService faultService;
    private final ChargingService chargingService;

    public TechnicianConsoleView(FaultService faultService, ChargingService chargingService) {
        this.faultService = faultService;
        this.chargingService = chargingService;
    }

    public void handleTechnicianConsole(Technician tech) {
        while (true) {
            tech.showMenu();
            int choice = ScannerUtil.readInt(0, 5, "请输入运维菜单编号 [0-5]: ");
            if (choice == 0) {
                System.out.println(FormatUtil.cyan("已退出技师工单控制台。"));
                break;
            }

            try {
                switch (choice) {
                    case 1:
                        showPendingFaults();
                        break;
                    case 2:
                        doClaimFault(tech);
                        break;
                    case 3:
                        doResolveFault(tech);
                        break;
                    case 4:
                        doSimulateFaultTrigger();
                        break;
                    case 5:
                        showPilesHealth();
                        break;
                }
            } catch (ZhiXingYueBoException e) {
                System.out.println(FormatUtil.red("【运维处理提示】: " + e.getMessage()));
            } catch (Exception e) {
                System.out.println(FormatUtil.red("【系统异常】: " + e.getMessage()));
            }
            ScannerUtil.pressEnterToContinue();
        }
    }

    public void showPendingFaults() throws ZhiXingYueBoException {
        FormatUtil.printSection("待处理突发故障告警工单列表");
        List<PileFaultLog> list = faultService.getPendingFaults();
        if (list.isEmpty()) {
            System.out.println(FormatUtil.green("当前全园区充电桩运转良好，暂无待接单的故障工单！"));
            return;
        }

        System.out.println("----------------------------------------------------------------------------------------");
        System.out.printf("%-8s %-8s %-10s %-32s %-20s%n", "工单ID", "桩ID", "错误码", "故障现象描述", "触发时刻");
        System.out.println("----------------------------------------------------------------------------------------");
        for (PileFaultLog f : list) {
            System.out.printf("%-8d %-8d %-10s %-32s %-20s%n",
                    f.getFaultId(), f.getPileId(), FormatUtil.red(f.getErrorCode()), f.getFaultDesc(), DateUtil.format(f.getReportedAt()));
        }
        System.out.println("----------------------------------------------------------------------------------------");
    }

    public void doClaimFault(Technician tech) throws ZhiXingYueBoException {
        FormatUtil.printSection("一键认领抢单并进入排查状态");
        showPendingFaults();
        long faultId = ScannerUtil.readInt(1, 999999, "请输入要认领的故障工单ID (如 TC-05 推荐工单 1): ");
        faultService.claimFault(faultId, tech.getUserId());
        System.out.println(FormatUtil.green(">>> 工单认领成功！工单状态已翻转为“排查维修中”，接单技师: " + tech.getUsername()));
    }

    public void doResolveFault(Technician tech) throws ZhiXingYueBoException {
        FormatUtil.printSection("录入排障记录并消警上线");
        long faultId = ScannerUtil.readInt(1, 999999, "请输入已修复的故障工单ID: ");
        String note = ScannerUtil.readString("请输入技师检修排障处置措施 (如: 更换主板温控传感器并完成耐压绝缘测试): ");

        faultService.resolveFault(faultId, tech.getUserId(), note);
        System.out.println(FormatUtil.green(">>> 工单修复完成。设备状态已按现有车位占用和充电工单同步。"));
    }

    public void doSimulateFaultTrigger() throws ZhiXingYueBoException {
        FormatUtil.printSection("模拟充电桩突发过温/短路硬件报警");
        int pileId = ScannerUtil.readInt(1, 9999, "请输入发生突发故障的充电桩ID: ");
        System.out.println("故障类型错误代码:");
        System.out.println("  1. E01 (过温过载告警)");
        System.out.println("  2. E02 (短路接地熔断)");
        System.out.println("  3. E03 (通信网关中断)");
        System.out.println("  4. E04 (枪头电子锁死)");
        int codeOpt = ScannerUtil.readInt(1, 4, "选择错误代码 [1-4]: ");
        String code = "E0" + codeOpt;
        String desc = ScannerUtil.readString("请输入故障告警描述: ");

        PileFaultLog fault = faultService.reportFault(pileId, code, desc);
        System.out.println(FormatUtil.red(">>> [硬件告警] 充电桩 " + pileId + " 已触发自动熔断保护，状态置为“故障报警”，已生成告警工单号: " + fault.getFaultId()));
    }

    public void showPilesHealth() throws ZhiXingYueBoException {
        FormatUtil.printSection("全园区充电桩健康运行看板");
        List<ChargingPile> piles = chargingService.getAllPiles();
        System.out.println("------------------------------------------------------------------------");
        System.out.printf("%-8s %-20s %-12s %-12s %-12s%n", "桩ID", "出厂唯一条码", "额定功率", "综合电价", "硬件运行状态");
        System.out.println("------------------------------------------------------------------------");
        for (ChargingPile p : piles) {
            String statusStr;
            switch (p.getPileStatus()) {
                case 1: statusStr = FormatUtil.green("空闲就绪"); break;
                case 2: statusStr = FormatUtil.cyan("正在充电"); break;
                case 3: statusStr = FormatUtil.red("故障报警"); break;
                default: statusStr = FormatUtil.yellow("离线检修"); break;
            }
            System.out.printf("%-8d %-20s %-12s %-12s %-12s%n",
                    p.getPileId(), p.getPileCode(), p.getPowerKw() + " kW", "¥" + p.getPricePerKwh(), statusStr);
        }
        System.out.println("------------------------------------------------------------------------");
    }
}
