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
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.*;
import com.zhixingyuebo.thread.ChargingSimulatorThread;
import com.zhixingyuebo.util.DateUtil;
import com.zhixingyuebo.util.FormatUtil;
import com.zhixingyuebo.util.ScannerUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 园区业主与私家车主专属交互视图控制器（解耦自 ConsoleMenu）
 */
public class OwnerConsoleView {

    private final UserService userService;
    private final ParkingService parkingService;
    private final ChargingService chargingService;
    private final AccountService accountService;

    public OwnerConsoleView(UserService userService, ParkingService parkingService,
                            ChargingService chargingService, AccountService accountService) {
        this.userService = userService;
        this.parkingService = parkingService;
        this.chargingService = chargingService;
        this.accountService = accountService;
    }

    public void handleOwnerConsole(OwnerDriver owner) {
        while (true) {
            owner.showMenu();
            int choice = ScannerUtil.readInt(0, 8, "请输入菜单指令编号 [0-8]: ");
            if (choice == 0) {
                System.out.println(FormatUtil.cyan("已退出车主工作台。"));
                break;
            }

            try {
                switch (choice) {
                    case 1:
                        showOwnerAssets(owner);
                        break;
                    case 2:
                        doBindVehicle(owner);
                        break;
                    case 3:
                        doSearchAndReserveSpot(owner);
                        break;
                    case 4:
                        doSimulateEntry(owner);
                        break;
                    case 5:
                        doProcessExit(owner); // 核心事务一
                        break;
                    case 6:
                        doChargingFlow(owner); // 核心事务二
                        break;
                    case 7:
                        doChargingSimulator(owner); // 创新 A
                        break;
                    case 8:
                        doBuyMonthlyCard(owner);
                        break;
                }
            } catch (InsufficientBalanceException e) {
                System.out.println(FormatUtil.red("\n════════════════════════════════════════════════════════════════"));
                System.out.println(FormatUtil.bold(FormatUtil.red("【系统拦截】" + e.getMessage())));
                if (choice == 5) {
                    System.out.println(FormatUtil.bold(FormatUtil.red("【道闸状态】钱包可用余额不足，拒绝抬杆放行！事务已全盘原子回滚！")));
                }
                System.out.println(FormatUtil.red("════════════════════════════════════════════════════════════════"));
            } catch (ZhiXingYueBoException e) {
                System.out.println(FormatUtil.red("【业务提示】: " + e.getMessage()));
            } catch (Exception e) {
                System.out.println(FormatUtil.red("【系统异常】: " + e.getMessage()));
            }
            ScannerUtil.pressEnterToContinue();
        }
    }

    public void doBuyMonthlyCard(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("停车月卡");
        LocalDateTime currentUntil = userService.getMonthlyCardUntil(owner.getUserId());
        System.out.println(currentUntil != null && currentUntil.isAfter(LocalDateTime.now())
                ? "月卡生效至 " + DateUtil.format(currentUntil) : "暂无生效月卡");
        BigDecimal price = userService.getMonthlyCardPrice();
        System.out.println("本次月卡费用 " + FormatUtil.formatMoney(price)
                + "，有效期增加 " + UserService.MONTHLY_CARD_DAYS + " 天。");
        if (ScannerUtil.readInt(0, 1, "确认从钱包扣款？(1: 确认, 0: 返回): ") == 0) {
            System.out.println("已取消月卡办理。");
            return;
        }
        LocalDateTime until = userService.buyOrRenewMonthlyCard(owner.getUserId());
        System.out.println(FormatUtil.green("月卡办理成功，有效至 " + DateUtil.format(until)));
    }

    public void showOwnerAssets(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("我的资产与钱包概览");
        Account account = accountService.getAccount(owner.getUserId());
        System.out.println("车主姓名: " + owner.getUsername() + " | 绑定手机: " + owner.getPhone());
        System.out.println("钱包可用余额: " + FormatUtil.green(FormatUtil.formatMoney(account.getBalance())) + " | 冻结金额: " + FormatUtil.formatMoney(account.getFrozenMoney()));
        LocalDateTime cardUntil = userService.getMonthlyCardUntil(owner.getUserId());
        System.out.println("月卡: " + (cardUntil != null && cardUntil.isAfter(LocalDateTime.now())
                ? "生效至 " + DateUtil.format(cardUntil) : "暂无生效月卡"));

        List<Vehicle> vehicles = userService.getUserVehicles(owner.getUserId());
        System.out.println("\n名下绑定车辆档案 (" + vehicles.size() + " 辆):");
        if (vehicles.isEmpty()) {
            System.out.println("  (暂未绑定车辆，请使用菜单 2 绑定)");
        } else {
            for (Vehicle v : vehicles) {
                System.out.printf("  • 车牌: %-10s | 车型: %-10s | 品牌: %s%n",
                        FormatUtil.bold(v.getPlateNumber()), v.getVehicleTypeName(), v.getBrand());
            }
        }

        // 检测是否存在已出场待补缴的欠费记录（闭环 pay_status=3）
        List<ParkingRecord> arrears = parkingService.getArrearsByUserId(owner.getUserId());
        if (arrears != null && !arrears.isEmpty()) {
            System.out.println(FormatUtil.yellow("\n【待补缴停车单】检测到您名下有 " + arrears.size() + " 笔已离场欠费挂账记录："));
            for (ParkingRecord r : arrears) {
                System.out.printf("  • 停车单号: %d | 车位ID: %d | 离场时刻: %s | 应补金额: %s%n",
                        r.getRecordId(), r.getSpotId(), DateUtil.format(r.getExitTime()), FormatUtil.red(FormatUtil.formatMoney(r.getTotalFee())));
            }
            int payOpt = ScannerUtil.readInt(0, 1, "是否立即补缴欠费？(1: 立即补缴, 0: 暂不补缴): ");
            if (payOpt == 1) {
                long targetId = ScannerUtil.readInt(1, 99999999, "请输入要补缴的停车单号: ");
                ParkingRecord paid = parkingService.payArrears(targetId, owner.getUserId());
                System.out.println(FormatUtil.green(">>> 欠费补缴成功！状态已翻转为【已补缴出场】，实扣金额: " + FormatUtil.formatMoney(paid.getTotalFee())));
            }
        }

        System.out.print("\n是否需要充值钱包？(1: 立即充值, 0: 返回): ");
        int opt = ScannerUtil.readInt(0, 1, "");
        if (opt == 1) {
            BigDecimal amount = ScannerUtil.readPositiveBigDecimal("请输入充值金额（元）: ");
            account = accountService.recharge(owner.getUserId(), amount);
            System.out.println(FormatUtil.green(">>> 充值成功！当前钱包最新可用余额: " + FormatUtil.formatMoney(account.getBalance())));
        }
    }

    public void doBindVehicle(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("绑定新私家机动车档案");
        String plate = ScannerUtil.readString("请输入车牌号码（如：川A88888、川AD12345）: ");
        System.out.println("请选择车型属性:");
        System.out.println("  1. 燃油轿车");
        System.out.println("  2. 新能源纯电 (EV)");
        System.out.println("  3. 新能源插混 (PHEV)");
        System.out.println("  4. 大型货车");
        int type = ScannerUtil.readInt(1, 4, "请选择车型 [1-4]: ");
        String brand = ScannerUtil.readString("请输入车辆品牌型号（如：比亚迪汉、特斯拉Model Y）: ");

        Vehicle v = userService.bindVehicle(owner.getUserId(), plate, type, brand);
        System.out.println(FormatUtil.green(">>> 车辆绑定成功！内部档案编号: " + v.getVehicleId() + "，车牌: " + v.getPlateNumber()));
    }

    public void doSearchAndReserveSpot(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("智慧车位实时检索与在线预约");
        System.out.println("请选择筛选条件:");
        System.out.println("  0. 查看全部空闲车位");
        System.out.println("  1. 仅限普通车位");
        System.out.println("  2. 仅限新能源充电专用位");
        int typeChoice = ScannerUtil.readInt(0, 2, "请选择车位类型过滤 [0-2]: ");
        Integer type = typeChoice == 0 ? null : typeChoice;

        List<ParkingLot> lots = parkingService.getAllLots();
        System.out.println("  0. 全部区域");
        for (int i = 0; i < lots.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + lots.get(i).getLotName());
        }
        int area = ScannerUtil.readInt(0, lots.size(), "请选择车位区域 [0-" + lots.size() + "]: ");
        Integer lotId = area == 0 ? null : lots.get(area - 1).getLotId();
        List<ParkingSpot> spots = parkingService.getAvailableSpots(type, lotId);
        if (spots.isEmpty()) {
            System.out.println(FormatUtil.yellow("当前园区暂无符合条件的空闲车位！"));
            return;
        }

        System.out.println("\n当前空闲车位列表:");
        System.out.println("------------------------------------------------------------");
        System.out.printf("%-8s %-12s %-18s %-10s%n", "车位ID", "物理编号", "车位属性", "状态");
        System.out.println("------------------------------------------------------------");
        for (ParkingSpot s : spots) {
            System.out.printf("%-8d %-12s %-18s %-10s%n", s.getSpotId(), s.getSpotNumber(), s.getSpotTypeName(), FormatUtil.green(s.getSpotStatusName()));
        }
        System.out.println("------------------------------------------------------------");

        System.out.print("\n是否锁定预约车位？(1: 立即预约, 0: 返回): ");
        int opt = ScannerUtil.readInt(0, 1, "");
        if (opt == 1) {
            int spotId = ScannerUtil.readInt(1, 9999, "请输入要预约的车位ID: ");
            if (spots.stream().noneMatch(spot -> spot.getSpotId().equals(spotId))) {
                throw new ZhiXingYueBoException("请从本次筛选结果中选择车位。");
            }
            ParkingSpot reserved = parkingService.reserveSpot(spotId, owner.getUserId());
            System.out.println(FormatUtil.green(">>> 车位 [" + reserved.getSpotNumber() + "] 已为您预约至 "
                    + DateUtil.format(reserved.getReservedUntil())));
        }
    }

    public void doSimulateEntry(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("模拟道闸车辆驶入登记");
        List<Vehicle> vehicles = userService.getUserVehicles(owner.getUserId());
        if (vehicles.isEmpty()) {
            System.out.println(FormatUtil.yellow("名下暂无绑定车辆，请先在菜单 2 中绑定私家车！"));
            return;
        }

        System.out.println("请选择入场车辆:");
        for (int i = 0; i < vehicles.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + vehicles.get(i).getPlateNumber() + " (" + vehicles.get(i).getBrand() + ")");
        }
        int vIndex = ScannerUtil.readInt(1, vehicles.size(), "选择车辆序号: ") - 1;
        String plate = vehicles.get(vIndex).getPlateNumber();

        ParkingSpot optimal = parkingService.recommendOptimalSpot(plate);
        if (optimal == null) {
            System.out.println(FormatUtil.yellow("暂无推荐空位，仍可输入已预约的车位编号。"));
        } else {
            System.out.println(">>> 为您的车辆推荐车位 " + optimal.getSpotNumber() + "，ID " + optimal.getSpotId());
        }

        int entryOpt = optimal == null
                ? ScannerUtil.readInt(0, 1, "1 输入已预约车位 ID，0 取消: ")
                : ScannerUtil.readInt(0, 2, "1 采纳推荐，2 手动输入车位 ID，0 取消: ");
        if (entryOpt == 0) return;
        int spotId = optimal != null && entryOpt == 1 ? optimal.getSpotId()
                : ScannerUtil.readInt(1, 9999, "请输入泊入的目标车位ID: ");

        ParkingRecord record = parkingService.simulateEntry(plate, spotId, owner.getUserId());
        System.out.println(FormatUtil.green(">>> 道闸识别成功！已抬杆放行！"));
        System.out.println("入场流水号: " + record.getRecordId() + " | 入场时刻: " + DateUtil.format(record.getEntryTime()));
    }

    public void doProcessExit(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("车辆离场阶梯算费与自动扣款 (核心事务一)");
        List<Vehicle> vehicles = userService.getUserVehicles(owner.getUserId());
        String plate;
        if (!vehicles.isEmpty()) {
            System.out.println("快捷选择名下车辆，或输入 0 手动输入本人车牌。");
            for (int i = 0; i < vehicles.size(); i++) {
                System.out.println("  " + (i + 1) + ". " + vehicles.get(i).getPlateNumber());
            }
            int opt = ScannerUtil.readInt(0, vehicles.size(), "选择序号 [0-" + vehicles.size() + "]: ");
            if (opt > 0) {
                plate = vehicles.get(opt - 1).getPlateNumber();
            } else {
                plate = ScannerUtil.readString("请输入要办理离场的车牌号: ");
            }
        } else {
            plate = ScannerUtil.readString("请输入要办理离场的车牌号: ");
        }

        Vehicle v = userService.getVehicleByPlate(plate);
        Integer billingUserId = owner.getUserId();
        if (v == null || !billingUserId.equals(v.getUserId())) {
            throw new ZhiXingYueBoException("只能操作本人名下车辆！");
        }

        Account beforeAcc = accountService.getAccount(billingUserId);
        BigDecimal beforeBalance = beforeAcc.getBalance();

        // 执行核心离场结算事务
        ParkingRecord finished = parkingService.processExit(plate, billingUserId);

        Account afterAcc = accountService.getAccount(billingUserId);
        BigDecimal afterBalance = afterAcc.getBalance();

        ParkingSpot spot = parkingService.getSpotById(finished.getSpotId());
        String spotNumber = spot == null ? "未知车位" : spot.getSpotNumber();
        TicketPrinter.printParkingTicket(finished, v, spotNumber, beforeBalance, afterBalance);
    }

    public void doChargingFlow(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("新能源充电桩插枪充电与拔枪结算 (核心事务二)");

        // 检查是否有正在进行中的工单
        ChargingOrder active = chargingService.getActiveOrderByUserId(owner.getUserId());
        if (active != null) {
            System.out.println(FormatUtil.yellow("未结工单 " + active.getOrderId() + " | " + active.getPlateNumber()
                    + " | " + active.getOrderStatusName()));
            System.out.println("启动时刻: " + DateUtil.format(active.getStartTime()));
            boolean hasMeter = active.getKwhAmount().signum() > 0;
            if (hasMeter) {
                System.out.println("订单累计计量 " + active.getKwhAmount().toPlainString() + " kWh，本次按计量结算。");
            }
            System.out.print("是否现在进行拔枪结算？(1: 立即拔枪结算, 0: 返回): ");
            int opt = ScannerUtil.readInt(0, 1, "");
            if (opt == 1) {
                BigDecimal kwh = hasMeter ? null
                        : ScannerUtil.readPositiveBigDecimal("请输入实际充入电量（度/kWh，如 30.00）: ");
                Account beforeAcc = accountService.getAccount(owner.getUserId());
                BigDecimal beforeBalance = beforeAcc.getBalance();

                ChargingOrder finished = chargingService.finishCharging(active.getOrderId(), owner.getUserId(), kwh);

                Account afterAcc = accountService.getAccount(owner.getUserId());
                BigDecimal afterBalance = afterAcc.getBalance();

                ChargingPile pile = chargingService.getPileById(finished.getPileId());
                TicketPrinter.printChargingInvoice(finished, pile, beforeBalance, afterBalance);
            }
            return;
        }

        // 启动新充电
        List<ChargingPile> readyPiles = chargingService.getReadyPiles();
        if (readyPiles.isEmpty()) {
            System.out.println(FormatUtil.yellow("当前全园区暂无可用的空闲就绪充电桩！"));
            return;
        }

        System.out.println("当前空闲就绪的充电桩设备:");
        System.out.println("------------------------------------------------------------------------");
        System.out.printf("%-8s %-20s %-12s %-12s %-10s%n", "桩ID", "设备资产编号", "额定功率", "综合电价", "硬件状态");
        System.out.println("------------------------------------------------------------------------");
        for (ChargingPile p : readyPiles) {
            System.out.printf("%-8d %-20s %-12s %-12s %-10s%n",
                    p.getPileId(), p.getPileCode(), p.getPowerKw() + " kW", "¥" + p.getPricePerKwh() + "/度", FormatUtil.green(p.getPileStatusName()));
        }
        System.out.println("------------------------------------------------------------------------");

        int pileId = ScannerUtil.readInt(1, 9999, "请输入选定的充电桩ID (如 TC-04 推荐快充桩 1): ");
        String plate = ScannerUtil.readString("请输入充电车牌号（如：川AD12345）: ");

        ChargingOrder order = chargingService.startCharging(pileId, owner.getUserId(), plate);
        System.out.println(FormatUtil.green(">>> 插枪启动成功！已生成充电工单号: " + order.getOrderId() + "，充电桩状态翻转为“正在充电”。"));
        System.out.println("您可随时再次进入本菜单进行拔枪结算，或进入菜单 7 使用多线程模拟器实时充入电量。");
    }

    public void doChargingSimulator(OwnerDriver owner) throws ZhiXingYueBoException {
        FormatUtil.printSection("【创新 A】IoT 智能充电桩多线程功率与电量采集模拟器");
        ChargingOrder active = chargingService.getActiveOrderByUserId(owner.getUserId());
        if (active == null) {
            System.out.println(FormatUtil.yellow("当前您没有正在充电中的工单！请先在菜单 6 中选择充电桩并插枪启动充电。"));
            return;
        }
        if (active.getOrderStatus() == 2) {
            throw new ZhiXingYueBoException("工单已因设备故障中断，请在菜单 6 按实际电量结算。");
        }

        ChargingPile pile = chargingService.getPileById(active.getPileId());
        System.out.println("服务工单号: " + active.getOrderId() + " | 服务车辆: " + active.getPlateNumber());
        System.out.println("设备资产: " + (pile != null ? pile.getPileCode() : "-") + " | 额定功率: " + (pile != null ? pile.getPowerKw() : "7.0") + " kW");

        BigDecimal targetKwh = ChargingService.exactKwh(
                ScannerUtil.readPositiveBigDecimal("请输入本订单累计目标电量（度/kWh，如 30.00）: "));

        Account beforeAcc = accountService.getAccount(owner.getUserId());
        BigDecimal beforeBalance = beforeAcc.getBalance();

        // 启动后台多线程模拟器
        ChargingSimulatorThread simulator = new ChargingSimulatorThread(active, targetKwh,
                pile != null ? pile.getPowerKw() : new BigDecimal("120.0"), chargingService);
        simulator.start();

        try {
            simulator.join(); // 主线程等待模拟器充电完毕
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (simulator.getError() != null) {
            throw new ZhiXingYueBoException("模拟器充电异常: " + simulator.getError().getMessage(), simulator.getError());
        }

        ChargingOrder finished = simulator.getResultOrder();
        if (finished != null) {
            Account afterAcc = accountService.getAccount(owner.getUserId());
            BigDecimal afterBalance = afterAcc.getBalance();
            TicketPrinter.printChargingInvoice(finished, pile, beforeBalance, afterBalance);
        }
    }
}
