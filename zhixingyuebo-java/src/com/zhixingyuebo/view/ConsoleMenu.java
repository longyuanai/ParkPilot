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

import com.zhixingyuebo.entity.Admin;
import com.zhixingyuebo.entity.OwnerDriver;
import com.zhixingyuebo.entity.Technician;
import com.zhixingyuebo.entity.User;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.*;
import com.zhixingyuebo.service.impl.*;
import com.zhixingyuebo.util.FormatUtil;
import com.zhixingyuebo.util.ScannerUtil;

/**
 * 交互菜单分发路由与多角色工作台前端控制器（Front Controller 架构核心）
 * 遵循职责单一原则（SRP），将具体交互解耦委托给各角色专有视图：
 * - OwnerConsoleView: 车主工作台
 * - TechnicianConsoleView: 运维技师工作台
 * - AdminConsoleView: 物业管理员工作台
 */
public class ConsoleMenu {

    private final UserService userService = new UserServiceImpl();
    private final ParkingService parkingService;
    private final ChargingService chargingService = new ChargingServiceImpl();
    private final FaultService faultService = new FaultServiceImpl();
    private final AccountService accountService = new AccountServiceImpl();

    private final OwnerConsoleView ownerConsoleView;
    private final TechnicianConsoleView technicianConsoleView;
    private final AdminConsoleView adminConsoleView;

    public ConsoleMenu() {
        this(new ParkingServiceImpl());
    }

    public ConsoleMenu(ParkingService parkingService) {
        this.parkingService = parkingService;
        this.ownerConsoleView = new OwnerConsoleView(userService, parkingService, chargingService, accountService);
        this.technicianConsoleView = new TechnicianConsoleView(faultService, chargingService);
        this.adminConsoleView = new AdminConsoleView(parkingService, chargingService, accountService, userService);
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
                ownerConsoleView.handleOwnerConsole((OwnerDriver) user);
            } else if (user instanceof Technician) {
                technicianConsoleView.handleTechnicianConsole((Technician) user);
            } else if (user instanceof Admin) {
                adminConsoleView.handleAdminConsole((Admin) user);
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

    private String getRoleName(int userType) {
        switch (userType) {
            case 1: return "园区车主/业主";
            case 2: return "充电桩运维技师";
            case 3: return "物业综合管理员";
            default: return "普通访客";
        }
    }
}
