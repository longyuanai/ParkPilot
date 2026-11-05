package com.zhixingyuebo.thread;

import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.service.ChargingService;
import com.zhixingyuebo.util.FormatUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 【创新 A】IoT 智能充电桩多线程功率与电量采集模拟器
 * 后台守护线程，模拟充电桩硬件上报电流、功率与动态电量，控制台动态刷新 ASCII 进度条
 */
public class ChargingSimulatorThread extends Thread {

    private final Long orderId;
    private final Integer userId;
    private final BigDecimal initialKwh;
    private final BigDecimal targetKwh;
    private final BigDecimal powerKw;
    private final ChargingService chargingService;
    private volatile boolean running = true;
    private ChargingOrder resultOrder = null;
    private Exception error = null;

    public ChargingSimulatorThread(ChargingOrder order, BigDecimal targetKwh, BigDecimal powerKw, ChargingService chargingService) {
        this.orderId = order.getOrderId();
        this.userId = order.getUserId();
        this.initialKwh = order.getKwhAmount();
        if (targetKwh == null || targetKwh.compareTo(BigDecimal.ZERO) <= 0) {
            this.targetKwh = new BigDecimal("30.00").setScale(2, RoundingMode.HALF_UP);
        } else {
            this.targetKwh = targetKwh;
        }
        this.powerKw = powerKw != null ? powerKw.setScale(1, RoundingMode.HALF_UP) : new BigDecimal("7.0");
        this.chargingService = chargingService;
        setDaemon(true);
        this.setName("IoT-Charging-Simulator-" + orderId);
    }

    @Override
    public void run() {
        System.out.println(FormatUtil.green("\n[IoT 智能充电网关] 充电枪头物理连接成功，高压回路闭合，功率: " + powerKw + " kW"));
        System.out.println(FormatUtil.cyan("[IoT 智能充电网关] 目标充入电量: " + targetKwh + " kWh，开始脉冲上报...\n"));

        BigDecimal currentKwh = initialKwh;
        BigDecimal stepKwh = new BigDecimal("2.50"); // 每次递增 2.5 度电
        int barLength = 30; // 进度条总长度字符数

        try {
            BigDecimal target = ChargingService.exactKwh(targetKwh);
            while (running && currentKwh.compareTo(target) < 0) {
                Thread.sleep(2000);
                if (!running) break;
                BigDecimal nextKwh = currentKwh.add(stepKwh).min(target);
                if (!chargingService.recordMeterProgress(orderId, userId, currentKwh, nextKwh)) {
                    ChargingOrder liveOrder = chargingService.getActiveOrderByUserId(userId);
                    if (liveOrder != null && orderId.equals(liveOrder.getOrderId()) && liveOrder.getOrderStatus() == 2) {
                        System.out.println("\n设备告警，中止采集，按已保存电量结算。");
                        resultOrder = chargingService.finishCharging(orderId, userId, null);
                    } else {
                        System.out.println("\n订单状态或计量已更新，请通过充电菜单查看。");
                    }
                    return;
                }
                currentKwh = nextKwh;

                // 计算百分比
                double percent = currentKwh.divide(target, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
                int filledLength = (int) (barLength * (percent / 100.0));
                if (filledLength > barLength) filledLength = barLength;

                StringBuilder bar = new StringBuilder();
                for (int i = 0; i < filledLength; i++) {
                    bar.append("=");
                }
                if (filledLength < barLength) {
                    bar.append(">");
                    for (int i = filledLength + 1; i < barLength; i++) {
                        bar.append(" ");
                    }
                }

                // 控制台 \r 动态回车覆盖刷新进度条
                String line = String.format("\r%s[%s] %5.1f%% %s(已充: %6s / %6s kWh | 实时功率: %5.1f kW)",
                        FormatUtil.CYAN,
                        bar.toString(),
                        percent,
                        FormatUtil.GREEN,
                        currentKwh.toPlainString(),
                        target.toPlainString(),
                        powerKw.doubleValue());
                System.out.print(line);
                System.out.flush();
            }

            System.out.println(running ? "\n已达到目标电量，按订单计量自动结算。"
                    : "\n已停止充电，按订单计量结算。");

            this.resultOrder = chargingService.finishCharging(orderId, userId, null);

        } catch (InterruptedException e) {
            System.out.println("\n" + FormatUtil.yellow(">>> [IoT 充电网关] 用户手动中止拔枪，正在按当前实际充入电量结算..."));
            try {
                this.resultOrder = chargingService.finishCharging(orderId, userId, null);
            } catch (Exception ex) {
                this.error = ex;
            }
        } catch (Exception e) {
            this.error = e;
        }
    }

    public void stopChargingEarly() {
        this.running = false;
        this.interrupt();
    }

    public ChargingOrder getResultOrder() {
        return resultOrder;
    }

    public Exception getError() {
        return error;
    }
}
