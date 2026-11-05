package com.zhixingyuebo.entity;

import java.math.BigDecimal;

/**
 * 停车场区域信息实体（对应 tb_parking_lot）
 */
public class ParkingLot {

    private Integer lotId;
    private String lotName;
    private Integer totalCapacity;
    private BigDecimal hourlyRate;

    public ParkingLot() {
    }

    public ParkingLot(Integer lotId, String lotName, Integer totalCapacity, BigDecimal hourlyRate) {
        this.lotId = lotId;
        this.lotName = lotName;
        this.totalCapacity = totalCapacity;
        this.hourlyRate = hourlyRate;
    }

    public Integer getLotId() {
        return lotId;
    }

    public void setLotId(Integer lotId) {
        this.lotId = lotId;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    @Override
    public String toString() {
        return "ParkingLot{" +
                "lotId=" + lotId +
                ", lotName='" + lotName + '\'' +
                ", totalCapacity=" + totalCapacity +
                ", hourlyRate=" + hourlyRate +
                '}';
    }
}
