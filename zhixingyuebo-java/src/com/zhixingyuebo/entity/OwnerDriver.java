package com.zhixingyuebo.entity;

import com.zhixingyuebo.util.FormatUtil;

import java.time.LocalDateTime;

/**
 * 园区业主与常驻车主实体类（继承 User，多态实现车主控制台菜单）
 */
public class OwnerDriver extends User {

    public OwnerDriver() {
        super();
        setUserType(1);
    }

    public OwnerDriver(Integer userId, String username, String phone, LocalDateTime createdAt, Integer isDeleted) {
        super(userId, username, phone, 1, createdAt, isDeleted);
    }

    @Override
    public void showMenu() {
        System.out.println(FormatUtil.CYAN + "┌────────────────────────────────────────────────────────┐" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.GREEN + "│           【智行悦泊 • 业主与车主服务专属控制台】          │" + FormatUtil.RESET);
        System.out.println(FormatUtil.CYAN + "├────────────────────────────────────────────────────────┤" + FormatUtil.RESET);
        System.out.println("│  1. 查看名下绑定车辆与钱包账户资产 (含在线充值)        │");
        System.out.println("│  2. 绑定新机动车档案 (支持燃油车/纯电/插混)            │");
        System.out.println("│  3. 智慧车位实时检索与空闲车位预约锁定                │");
        System.out.println("│  4. 模拟道闸车辆驶入登记 (车位状态更新为已泊入)        │");
        System.out.println("│  5. 车辆离场阶梯算费与自动扣款 (核心事务一)            │");
        System.out.println("│  6. 新能源充电桩插枪充电与拔枪结算 (核心事务二)        │");
        System.out.println("│  7. 【创新 A】启动 IoT 智能充电桩多线程进度模拟器      │");
        System.out.println("│  8. 查询、办理或续费停车月卡                          │");
        System.out.println("│  0. 安全注销并返回登录首页                            │");
        System.out.println(FormatUtil.CYAN + "└────────────────────────────────────────────────────────┘" + FormatUtil.RESET);
    }
}
