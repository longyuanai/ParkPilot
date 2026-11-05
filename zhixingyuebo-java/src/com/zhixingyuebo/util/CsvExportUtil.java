package com.zhixingyuebo.util;

import com.zhixingyuebo.dao.AccountFlowDao;
import com.zhixingyuebo.dao.ChargingOrderDao;
import com.zhixingyuebo.dao.ParkingRecordDao;
import com.zhixingyuebo.dao.impl.AccountFlowDaoImpl;
import com.zhixingyuebo.dao.impl.ChargingOrderDaoImpl;
import com.zhixingyuebo.dao.impl.ParkingRecordDaoImpl;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.entity.ParkingRecord;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 【创新 B】园区车流吞吐与财务营收标准 CSV 报表导出工具
 * 基于 Java 缓冲 I/O 流（BufferedWriter），内置 UTF-8 BOM 编码，彻底杜绝 Excel 中文乱码
 */
public class CsvExportUtil {

    private static final ParkingRecordDao parkingRecordDao = new ParkingRecordDaoImpl();
    private static final ChargingOrderDao chargingOrderDao = new ChargingOrderDaoImpl();
    private static final AccountFlowDao accountFlowDao = new AccountFlowDaoImpl();

    /**
     * 导出全园区综合运营财务对账台账 CSV 报表
     */
    public static String exportDailyReport(String targetDir) throws Exception {
        return exportDailyReport(targetDir, LocalDate.now());
    }

    public static String exportDailyReport(String targetDir, LocalDate today) throws Exception {
        if (today == null) throw new IllegalArgumentException("报表日期不能为空");
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File dir = new File(targetDir != null && !targetDir.isEmpty() ? targetDir : ".");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File csvFile = new File(dir, "智行悦泊园区运营与财务对账报表_" + dateStr + ".csv");

        List<ParkingRecord> parkingRecords = parkingRecordDao.findAll();
        List<ChargingOrder> chargingOrders = chargingOrderDao.findAll();
        List<AccountFlow> accountFlows = accountFlowDao.findAll();
        parkingRecords.removeIf(record -> record.getExitTime() == null || !record.getExitTime().toLocalDate().equals(today));
        chargingOrders.removeIf(order -> order.getOrderStatus() != 1
                || order.getEndTime() == null || !order.getEndTime().toLocalDate().equals(today));
        accountFlows.removeIf(flow -> flow.getCreatedAt() == null || !flow.getCreatedAt().toLocalDate().equals(today));

        // 汇总指标
        BigDecimal totalParkingIncome = BigDecimal.ZERO;
        int totalParkingCount = parkingRecords.size();
        for (ParkingRecord pr : parkingRecords) {
            if (pr.getTotalFee() != null && pr.getTotalFee().compareTo(BigDecimal.ZERO) > 0) {
                totalParkingIncome = totalParkingIncome.add(pr.getTotalFee());
            }
        }

        BigDecimal totalChargingIncome = BigDecimal.ZERO;
        BigDecimal totalKwh = BigDecimal.ZERO;
        int totalChargingCount = chargingOrders.size();
        for (ChargingOrder co : chargingOrders) {
            if (co.getTotalCost() != null && co.getTotalCost().compareTo(BigDecimal.ZERO) > 0) {
                totalChargingIncome = totalChargingIncome.add(co.getTotalCost());
            }
            if (co.getKwhAmount() != null) {
                totalKwh = totalKwh.add(co.getKwhAmount());
            }
        }

        BigDecimal totalMonthlyCardIncome = BigDecimal.ZERO;
        for (AccountFlow flow : accountFlows) {
            if (AccountFlow.MONTHLY_CARD_PAYMENT.equals(flow.getBizType())) {
                totalMonthlyCardIncome = totalMonthlyCardIncome.subtract(flow.getDiffAmount());
            }
        }
        BigDecimal totalGrossIncome = totalParkingIncome.add(totalChargingIncome).add(totalMonthlyCardIncome);

        // 使用 BufferedWriter 写入标准 CSV
        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // 写入 UTF-8 BOM（﻿），确保 Windows Excel 打开 100% 不乱码
            writer.write("﻿");

            // 1. 报表表头概述
            writer.write("【“智行悦泊”智慧园区运营与财务综合对账日报表】");
            writer.newLine();
            writer.write("导出生成日期," + DateUtil.format(java.time.LocalDateTime.now()));
            writer.newLine();
            writer.write(String.format("全日总营收（元）,¥%s,停车总收费,¥%s (已结%d车次),充电总收费,¥%s (累计%s度 / %d次),月卡总收费,¥%s",
                    totalGrossIncome.setScale(2).toPlainString(),
                    totalParkingIncome.setScale(2).toPlainString(),
                    totalParkingCount,
                    totalChargingIncome.setScale(2).toPlainString(),
                    totalKwh.setScale(2).toPlainString(),
                    totalChargingCount,
                    totalMonthlyCardIncome.setScale(2).toPlainString()));
            writer.newLine();
            writer.newLine();

            // 2. 停车进出场与计费明细
            writer.write(">>> 一、车辆泊车进出场与阶梯计费明细");
            writer.newLine();
            writer.write("流水单号,车辆ID,车位ID,道闸入场时刻,离场放行时刻,停泊分钟数,实收费用(元),结算状态");
            writer.newLine();
            for (ParkingRecord pr : parkingRecords) {
                writer.write(String.format("%d,%d,%d,%s,%s,%d,%.2f,%s",
                        pr.getRecordId(),
                        pr.getVehicleId(),
                        pr.getSpotId(),
                        DateUtil.format(pr.getEntryTime()),
                        DateUtil.format(pr.getExitTime()),
                        pr.getParkMinutes() != null ? pr.getParkMinutes() : 0,
                        pr.getTotalFee() != null ? pr.getTotalFee() : BigDecimal.ZERO,
                        pr.getPayStatusName()));
                writer.newLine();
            }
            writer.newLine();

            // 3. 新能源充电计量与结算明细
            writer.write(">>> 二、新能源汽车充电电量与电费结算明细");
            writer.newLine();
            writer.write("充电订单号,充电桩ID,车主ID,车牌号码,插枪启动时刻,拔枪结束时刻,充入电量(kWh),实付电费(元),工单状态");
            writer.newLine();
            for (ChargingOrder co : chargingOrders) {
                writer.write(String.format("%d,%d,%d,%s,%s,%s,%.2f,%.2f,%s",
                        co.getOrderId(),
                        co.getPileId(),
                        co.getUserId(),
                        co.getPlateNumber(),
                        DateUtil.format(co.getStartTime()),
                        DateUtil.format(co.getEndTime()),
                        co.getKwhAmount() != null ? co.getKwhAmount() : BigDecimal.ZERO,
                        co.getTotalCost() != null ? co.getTotalCost() : BigDecimal.ZERO,
                        co.getOrderStatusName()));
                writer.newLine();
            }
            writer.newLine();

            // 4. 资金变动只读审计流水
            writer.write(">>> 三、资金黑匣子防篡改只读审计流水");
            writer.newLine();
            writer.write("审计流水号,钱包账户ID,变动前原余额(元),变动后新余额(元),本次差额(元),业务类型,产生时刻");
            writer.newLine();
            for (AccountFlow af : accountFlows) {
                writer.write(String.format("%d,%d,%.2f,%.2f,%.2f,%s,%s",
                        af.getFlowId(),
                        af.getAccountId(),
                        af.getBeforeBalance(),
                        af.getAfterBalance(),
                        af.getDiffAmount(),
                        af.getBizType(),
                        DateUtil.format(af.getCreatedAt())));
                writer.newLine();
            }

            writer.flush();
        }

        return csvFile.getAbsolutePath();
    }
}
