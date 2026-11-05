package com.zhixingyuebo.view;

import com.zhixingyuebo.entity.*;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.*;
import com.zhixingyuebo.service.impl.*;
import com.zhixingyuebo.thread.ChargingSimulatorThread;
import com.zhixingyuebo.util.CsvExportUtil;
import com.zhixingyuebo.util.DateUtil;
import com.zhixingyuebo.util.FormatUtil;
import com.zhixingyuebo.util.ScannerUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 交互菜单分发路由、多角色控制台与异常输入兜底处理（View 展现交互层核心）
 */
public class ConsoleMenu {

    private final UserService userService = new UserServiceImpl();
    private final ParkingService parkingService;
    private final ChargingService chargingService = new ChargingServiceImpl();
    private final FaultService faultService = new FaultServiceImpl();
    private final AccountService accountService = new AccountServiceImpl();

    public ConsoleMenu() {
        this(new ParkingServiceImpl());
    }

    public ConsoleMenu(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    /**
     * 启动控制台主循环
     */
    public void start() {
        while (true) {
            FormatUtil.printBanner("“智行悦泊”智慧园区停车与新能源充电综合管理系统");
            System.out.println("  1. 手机号码快捷认证登录 (车主 / 运维技师 / 物业管理员)");
            System.out.println("  2. 【快捷评测导航】查看预置考核账号与自测用例说明 (TC-01 ~ TC-06)");
            System.out.println("  0. 退出系统");
            System.out.println(FormatUtil.CYAN + "------------------------------------------------------------------------" + FormatUtil.RESET);

            int choice = ScannerUtil.readInt(0, 2, "请选择操作菜单编号 [0-2]: ");
            if (choice == 0) {
                System.out.println(FormatUtil.green("\n感谢使用“智行悦泊”综合管理系统，系统已安全退出！"));
                break;
            } else if (choice == 1) {
                doLogin();
            } else if (choice == 2) {
                showQuickGuide();
            }
        }
    }

    /**
     * 执行用户登录并多态分发工作台
     */
    private void doLogin() {
        FormatUtil.printSection("用户手机号认证登录");
        System.out.println("提示：可用测试手机号 - 张先生车主(13800000001)、李女士车主(13800000002)、王强技师(13800000003)、刘建国管理员(13800000004)");
        String phone = ScannerUtil.readString("请输入 11 位手机号码: ");

        try {
            User user = userService.login(phone);
            System.out.println(FormatUtil.green("\n>>> 登录成功！欢迎 " + user.getUsername() + "（身份: " + getRoleName(user.getUserType()) + "）进入系统。"));

            // 根据用户类型进入多态菜单控制台
            if (user instanceof OwnerDriver) {
                handleOwnerConsole((OwnerDriver) user);
            } else if (user instanceof Technician) {
                handleTechnicianConsole((Technician) user);
            } else if (user instanceof Admin) {
                handleAdminConsole((Admin) user);
            }
        } catch (ZhiXingYueBoException e) {
            System.out.println(FormatUtil.red("【登录失败】: " + e.getMessage()));
            ScannerUtil.pressEnterToContinue();
        }
    }

    /**
     * 快捷评测指南
     */
    private void showQuickGuide() {
        FormatUtil.printSection("任务书 6 组核心自测用例快捷对照表");
        System.out.println("使用 demo.bat 可按固定时点复现以下样例。重新启动演示会加载新样例。");
        System.out.println("• TC-01: 车主登录 (13800000001) -> 欢迎张先生，名下川A88888，钱包500元，调出专属菜单");
        System.out.println("• TC-02: 离场阶梯算费与扣费 (川A88888停泊90分) -> 扣4.00元，钱包剩496元，释放车位并打印小票");
        System.out.println("• TC-03: 离场余额不足回滚测试 (李女士 13800000002，川A66666应收80元，余额50元) -> 红字拦截拒抬杆，事务零变更");
        System.out.println("• TC-04: 新能源充电启动与结算 (川AD12345驶入B-0201快充) -> 充30度扣45元，拔枪复位打印发票");
        System.out.println("• TC-05: 充电桩故障报警与技师消警 (王强 13800000003) -> 认领E01工单，录入排障日志消警复位");
        System.out.println("• TC-06: 异常输入健壮性防御 -> 输入任意字母(如 xyz)、符号(&&)或负数(-9)，系统不崩溃闪退！");
        ScannerUtil.pressEnterToContinue();
    }

    // =========================================================================
    // 角色控制台：车主 / 业主工作台（OwnerDriver）
    // =========================================================================
    private void handleOwnerConsole(OwnerDriver owner) {
        while (true) {
            // 面向对象多态展示车主专属菜单
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

    private void doBuyMonthlyCard(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void showOwnerAssets(OwnerDriver owner) throws ZhiXingYueBoException {
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

        System.out.print("\n是否需要充值钱包？(1: 立即充值, 0: 返回): ");
        int opt = ScannerUtil.readInt(0, 1, "");
        if (opt == 1) {
            BigDecimal amount = ScannerUtil.readPositiveBigDecimal("请输入充值金额（元）: ");
            account = accountService.recharge(owner.getUserId(), amount);
            System.out.println(FormatUtil.green(">>> 充值成功！当前钱包最新可用余额: " + FormatUtil.formatMoney(account.getBalance())));
        }
    }

    private void doBindVehicle(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void doSearchAndReserveSpot(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void doSimulateEntry(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void doProcessExit(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void doChargingFlow(OwnerDriver owner) throws ZhiXingYueBoException {
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

    private void doChargingSimulator(OwnerDriver owner) throws ZhiXingYueBoException {
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

    // =========================================================================
    // 角色控制台：充电桩运维技师工作台（Technician）
    // =========================================================================
    private void handleTechnicianConsole(Technician tech) {
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

    private void showPendingFaults() throws ZhiXingYueBoException {
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

    private void doClaimFault(Technician tech) throws ZhiXingYueBoException {
        FormatUtil.printSection("一键认领抢单并进入排查状态");
        showPendingFaults();
        long faultId = ScannerUtil.readInt(1, 999999, "请输入要认领的故障工单ID (如 TC-05 推荐工单 1): ");
        faultService.claimFault(faultId, tech.getUserId());
        System.out.println(FormatUtil.green(">>> 工单认领成功！工单状态已翻转为“排查维修中”，接单技师: " + tech.getUsername()));
    }

    private void doResolveFault(Technician tech) throws ZhiXingYueBoException {
        FormatUtil.printSection("录入排障记录并消警上线");
        long faultId = ScannerUtil.readInt(1, 999999, "请输入已修复的故障工单ID: ");
        String note = ScannerUtil.readString("请输入技师检修排障处置措施 (如: 更换主板温控传感器并完成耐压绝缘测试): ");

        faultService.resolveFault(faultId, tech.getUserId(), note);
        System.out.println(FormatUtil.green(">>> 工单修复完成。设备状态已按现有车位占用和充电工单同步。"));
    }

    private void doSimulateFaultTrigger() throws ZhiXingYueBoException {
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

    private void showPilesHealth() throws ZhiXingYueBoException {
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

    // =========================================================================
    // 角色控制台：物业运营管理员工作台（Admin）
    // =========================================================================
    private void handleAdminConsole(Admin admin) {
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

    private void showPanoramaBoard() throws ZhiXingYueBoException {
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

    private void showActiveParkingRecords() throws ZhiXingYueBoException {
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

    private void showAuditFlows() throws ZhiXingYueBoException {
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

    private void doExportDailyCsvReport() throws Exception {
        FormatUtil.printSection("【创新 B】导出每日车流吞吐高峰与财务营收 CSV 报表");
        System.out.println("正在通过 Java 缓冲流（BufferedWriter）与 UTF-8 BOM 编码聚合生成报表...");

        String path = CsvExportUtil.exportDailyReport("reports");
        System.out.println(FormatUtil.green("\n>>> 【导出成功】！标准 CSV 报表已成功生成："));
        System.out.println(FormatUtil.bold(FormatUtil.cyan(path)));
        System.out.println("提示：已内置 UTF-8 BOM 签名，支持直接双击使用 Microsoft Excel 或 WPS 无中文乱码打开！");
    }

    private void doManageSpots() throws ZhiXingYueBoException {
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

    private void doSetMonthlyCard(Admin admin) throws ZhiXingYueBoException {
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

    private String getRoleName(int userType) {
        switch (userType) {
            case 1: return "园区车主/业主";
            case 2: return "充电桩运维技师";
            case 3: return "物业综合管理员";
            default: return "普通访客";
        }
    }
}
