package com.zhixingyuebo.entity;

import com.zhixingyuebo.util.FormatUtil;

import java.time.LocalDateTime;

/**
 * 物业运营管理员实体类（继承 User，多态实现全局运营监控控制台）
 */
public class Admin extends User {

    public Admin() {
        super();
        setUserType(3);
    }

    public Admin(Integer userId, String username, String phone, LocalDateTime createdAt, Integer isDeleted) {
        super(userId, username, phone, 3, createdAt, isDeleted);
    }

    @Override
    public void showMenu() {
        System.out.println(FormatUtil.BLUE + "┌────────────────────────────────────────────────────────┐" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.CYAN + "│          【智行悦泊 • 智慧园区物业综合运营管控中心】       │" + FormatUtil.RESET);
        System.out.println(FormatUtil.BLUE + "├────────────────────────────────────────────────────────┤" + FormatUtil.RESET);
        System.out.println("│  1. 全园区车位与充电桩综合运营全景监控看板            │");
        System.out.println("│  2. 查询全园区所有在场车辆与泊车记录流水              │");
        System.out.println("│  3. 资金黑匣子防篡改审计流水明细总台账                │");
        System.out.println("│  4. 【创新 B】导出每日车流吞吐与财务营收标准 CSV 报表 │");
        System.out.println("│  5. 快速新增/维护车位或充电桩硬件资产                │");
        System.out.println("│  6. 登记车主月卡有效期                                │");
        System.out.println("│  0. 安全注销并返回登录首页                            │");
        System.out.println(FormatUtil.BLUE + "└────────────────────────────────────────────────────────┘" + FormatUtil.RESET);
    }
}
