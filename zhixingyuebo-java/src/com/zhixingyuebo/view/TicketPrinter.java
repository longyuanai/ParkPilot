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

import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.entity.ParkingRecord;
import com.zhixingyuebo.entity.Vehicle;
import com.zhixingyuebo.util.DateUtil;
import com.zhixingyuebo.util.FormatUtil;

import java.math.BigDecimal;

/**
 * 停车离场小票与新能源充电消费结算电子发票终端格式化打印组件
 */
public class TicketPrinter {

    /**
     * 打印停车离场放行小票
     */
    public static void printParkingTicket(ParkingRecord record, Vehicle vehicle, String spotNumber, BigDecimal beforeBalance, BigDecimal afterBalance) {
        System.out.println(FormatUtil.GREEN + "╔════════════════════════════════════════════════════════════╗" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.GREEN + "║             【智行悦泊 • 智慧停车离场结算收银小票】           ║" + FormatUtil.RESET);
        System.out.println(FormatUtil.GREEN + "╠════════════════════════════════════════════════════════════╣" + FormatUtil.RESET);
        System.out.printf("║ 停车单流水号: %-44d ║%n", record.getRecordId());
        System.out.printf("║ 离场车牌号码: %-44s ║%n", vehicle != null ? vehicle.getPlateNumber() : "未知车牌");
        System.out.printf("║ 停泊物理车位: %-44s ║%n", spotNumber != null ? spotNumber : ("Spot#" + record.getSpotId()));
        System.out.printf("║ 道闸入场时刻: %-44s ║%n", DateUtil.format(record.getEntryTime()));
        System.out.printf("║ 离场放行时刻: %-44s ║%n", DateUtil.format(record.getExitTime()));
        System.out.printf("║ 累计计费时长: %-42s ║%n", (record.getParkMinutes() != null ? record.getParkMinutes() : 0) + " 分钟 (前30分钟免费)");
        System.out.println(FormatUtil.GREEN + "╟────────────────────────────────────────────────────────────╢" + FormatUtil.RESET);
        System.out.printf("║ 应收实扣金额: %-44s ║%n", FormatUtil.yellow(FormatUtil.formatMoney(record.getTotalFee())));
        if (beforeBalance != null && afterBalance != null) {
            System.out.printf("║ 钱包变动明细: %-44s ║%n", String.format("¥%s -> ¥%s", beforeBalance.setScale(2), afterBalance.setScale(2)));
        }
        System.out.printf("║ 结算状态: %-46s ║%n", FormatUtil.green(record.getPayStatusName()));
        System.out.println(FormatUtil.GREEN + "╠════════════════════════════════════════════════════════════╣" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.CYAN + "║ >>> 提示: 扣款成功，道闸已自动抬杆，祝您一路平安！            ║" + FormatUtil.RESET);
        System.out.println(FormatUtil.GREEN + "╚════════════════════════════════════════════════════════════╝" + FormatUtil.RESET);
    }

    /**
     * 打印新能源汽车充电消费电子结算发票
     */
    public static void printChargingInvoice(ChargingOrder order, ChargingPile pile, BigDecimal beforeBalance, BigDecimal afterBalance) {
        System.out.println(FormatUtil.CYAN + "╔════════════════════════════════════════════════════════════╗" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.CYAN + "║          【智行悦泊 • 新能源汽车充电消费电子结算单】          ║" + FormatUtil.RESET);
        System.out.println(FormatUtil.CYAN + "╠════════════════════════════════════════════════════════════╣" + FormatUtil.RESET);
        System.out.printf("║ 充电订单编号: %-44d ║%n", order.getOrderId());
        System.out.printf("║ 服务车辆车牌: %-44s ║%n", order.getPlateNumber());
        System.out.printf("║ 充电设备资产: %-44s ║%n", pile != null ? (pile.getPileCode() + " (" + pile.getPowerKw() + "kW)") : ("Pile#" + order.getPileId()));
        System.out.printf("║ 启动充电时刻: %-44s ║%n", DateUtil.format(order.getStartTime()));
        System.out.printf("║ 拔枪结束时刻: %-44s ║%n", DateUtil.format(order.getEndTime()));
        System.out.println(FormatUtil.CYAN + "╟────────────────────────────────────────────────────────────╢" + FormatUtil.RESET);
        System.out.printf("║ 累计充入电量: %-44s ║%n", (order.getKwhAmount() != null ? order.getKwhAmount().setScale(2) : "0.00") + " 度 (kWh)");
        System.out.printf("║ 综合电价费率: %-44s ║%n", (pile != null ? ("¥" + pile.getPricePerKwh().setScale(2)) : "¥1.20") + " 元/度");
        System.out.printf("║ 扣除电费总计: %-44s ║%n", FormatUtil.yellow(FormatUtil.formatMoney(order.getTotalCost())));
        if (beforeBalance != null && afterBalance != null) {
            System.out.printf("║ 车主钱包余额: %-44s ║%n", String.format("¥%s -> ¥%s", beforeBalance.setScale(2), afterBalance.setScale(2)));
        }
        System.out.printf("║ 充电桩现状态: %-44s ║%n", FormatUtil.green("拔枪复位，已恢复空闲就绪 (Online)"));
        System.out.println(FormatUtil.CYAN + "╠════════════════════════════════════════════════════════════╣" + FormatUtil.RESET);
        System.out.println(FormatUtil.BOLD + FormatUtil.GREEN + "║ >>> 绿色低碳，智行悦泊！本次充电结算完成，感谢您的使用！       ║" + FormatUtil.RESET);
        System.out.println(FormatUtil.CYAN + "╚════════════════════════════════════════════════════════════╝" + FormatUtil.RESET);
    }
}
