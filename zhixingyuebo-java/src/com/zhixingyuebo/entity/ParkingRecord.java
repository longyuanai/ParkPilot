package com.zhixingyuebo.entity;

import com.zhixingyuebo.enums.PayStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车进出场与计费记录实体（对应 tb_parking_record）
 */
public class ParkingRecord {

    private Long recordId;
    private Integer vehicleId;
    private Integer spotId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Integer parkMinutes;
    private BigDecimal totalFee;
    private Integer payStatus; // 0未结算/在场, 1月卡全免, 2钱包成功扣款, 3已补缴出场

    public ParkingRecord() {
    }

    public ParkingRecord(Long recordId, Integer vehicleId, Integer spotId, LocalDateTime entryTime, LocalDateTime exitTime, Integer parkMinutes, BigDecimal totalFee, Integer payStatus) {
        this.recordId = recordId;
        this.vehicleId = vehicleId;
        this.spotId = spotId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
        this.parkMinutes = parkMinutes;
        this.totalFee = totalFee;
        this.payStatus = payStatus;
    }

    public String getPayStatusName() {
        PayStatus status = getPayStatusEnum();
        return status != null ? status.getDescription() : "未知状态";
    }

    public PayStatus getPayStatusEnum() {
        return PayStatus.fromCode(payStatus);
    }

    public void setPayStatusEnum(PayStatus status) {
        this.payStatus = status != null ? status.getCode() : null;
    }

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getSpotId() {
        return spotId;
    }

    public void setSpotId(Integer spotId) {
        this.spotId = spotId;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public Integer getParkMinutes() {
        return parkMinutes;
    }

    public void setParkMinutes(Integer parkMinutes) {
        this.parkMinutes = parkMinutes;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public Integer getPayStatus() {
        return payStatus;
    }

    public void setPayStatus(Integer payStatus) {
        this.payStatus = payStatus;
    }

    @Override
    public String toString() {
        return "ParkingRecord{" +
                "recordId=" + recordId +
                ", vehicleId=" + vehicleId +
                ", spotId=" + spotId +
                ", entryTime=" + entryTime +
                ", exitTime=" + exitTime +
                ", parkMinutes=" + parkMinutes +
                ", totalFee=" + totalFee +
                ", payStatus=" + getPayStatusName() +
                '}';
    }
}
