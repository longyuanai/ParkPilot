package com.zhixingyuebo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 新能源汽车充电工单实体（对应 tb_charging_order）
 */
public class ChargingOrder {

    private Long orderId;
    private Integer pileId;
    private Integer userId;
    private String plateNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal kwhAmount;
    private BigDecimal totalCost;
    private Integer orderStatus; // 0充电中, 1已结算完成, 2异常中断待处理

    public ChargingOrder() {
    }

    public ChargingOrder(Long orderId, Integer pileId, Integer userId, String plateNumber, LocalDateTime startTime, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal totalCost, Integer orderStatus) {
        this.orderId = orderId;
        this.pileId = pileId;
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.kwhAmount = kwhAmount;
        this.totalCost = totalCost;
        this.orderStatus = orderStatus;
    }

    public String getOrderStatusName() {
        if (orderStatus == null) return "未知状态";
        switch (orderStatus) {
            case 0: return "正在充电";
            case 1: return "已结算完成";
            case 2: return "异常中断";
            default: return "其他";
        }
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getPileId() {
        return pileId;
    }

    public void setPileId(Integer pileId) {
        this.pileId = pileId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getKwhAmount() {
        return kwhAmount;
    }

    public void setKwhAmount(BigDecimal kwhAmount) {
        this.kwhAmount = kwhAmount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    @Override
    public String toString() {
        return "ChargingOrder{" +
                "orderId=" + orderId +
                ", pileId=" + pileId +
                ", userId=" + userId +
                ", plateNumber='" + plateNumber + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", kwhAmount=" + kwhAmount + " kWh" +
                ", totalCost=¥" + totalCost +
                ", orderStatus=" + getOrderStatusName() +
                '}';
    }
}
