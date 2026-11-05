package com.zhixingyuebo.entity;

import com.zhixingyuebo.util.FormatUtil;

import java.time.LocalDateTime;

/**
 * 充电桩运维技师实体类（继承 User，多态实现抢单报修控制台）
 */
public class Technician extends User {

    public Technician() {
        super();
        setUserType(2);
    }

    public Technician(Integer userId, String username, String phone, LocalDateTime createdAt, Integer isDeleted) {
        super(userId, username, phone, 2, createdAt, isDeleted);
    }

    @Override
    public void showMenu() {
        System.out.println(FormatUtil.YELLOW + "┌────────────────────────────────────────────────────────┐" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.YELLOW + "│          【智行悦泊 • 充电桩运维技师专属工单控制台】       │" + FormatUtil.RESET);
        System.out.println(FormatUtil.YELLOW + "├────────────────────────────────────────────────────────┤" + FormatUtil.RESET);
        System.out.println("│  1. 查看待处理突发故障告警工单列表 (待接单)            │");
        System.out.println("│  2. 一键认领抢单并进入现场排障检修状态                │");
        System.out.println("│  3. 录入排障处理措施并消警 (恢复充电桩上线就绪)        │");
        System.out.println("│  4. 模拟充电桩硬件突发过温/短路熔断报警 (E01~E04)      │");
        System.out.println("│  5. 查看全园区充电桩硬件运行健康状态大盘              │");
        System.out.println("│  0. 安全注销并返回登录首页                            │");
        System.out.println(FormatUtil.YELLOW + "└────────────────────────────────────────────────────────┘" + FormatUtil.RESET);
    }
}
