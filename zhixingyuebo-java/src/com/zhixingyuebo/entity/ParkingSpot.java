package com.zhixingyuebo.entity;

import com.zhixingyuebo.enums.SpotStatus;
import com.zhixingyuebo.enums.SpotType;
import java.time.LocalDateTime;

/**
 * 车位基础单元实体（对应 tb_parking_spot）
 * 兼容经典面向对象设计与强类型枚举映射
 */
public class ParkingSpot {

    private Integer spotId;
    private Integer lotId;
    private String spotNumber;
    private Integer spotType;   // 1普通车位, 2新能源充电专用位, 3无障碍VIP位
    private Integer spotStatus; // 0空闲, 1已被预约, 2车辆已泊入, 3维护禁用
    private Integer isDeleted;
    private Integer reservedBy;
    private LocalDateTime reservedUntil;

    public ParkingSpot() {
    }

    public ParkingSpot(Integer spotId, Integer lotId, String spotNumber, Integer spotType, Integer spotStatus, Integer isDeleted) {
        this.spotId = spotId;
        this.lotId = lotId;
        this.spotNumber = spotNumber;
        this.spotType = spotType;
        this.spotStatus = spotStatus;
        this.isDeleted = isDeleted;
    }

    public String getSpotTypeName() {
        SpotType type = getSpotTypeEnum();
        return type != null ? type.getDescription() : "未知车位";
    }

    public String getSpotStatusName() {
        SpotStatus status = getSpotStatusEnum();
        return status != null ? status.getDescription() : "未知状态";
    }

    public SpotType getSpotTypeEnum() {
        return SpotType.fromCode(spotType);
    }

    public void setSpotTypeEnum(SpotType type) {
        this.spotType = type != null ? type.getCode() : null;
    }

    public SpotStatus getSpotStatusEnum() {
        return SpotStatus.fromCode(spotStatus);
    }

    public void setSpotStatusEnum(SpotStatus status) {
        this.spotStatus = status != null ? status.getCode() : null;
    }

    public boolean isAvailable() {
        return spotStatus != null && spotStatus == 0 && (isDeleted == null || isDeleted == 0);
    }

    public Integer getReservedBy() { return reservedBy; }

    public LocalDateTime getReservedUntil() { return reservedUntil; }

    public void setReservation(Integer userId, LocalDateTime until) {
        this.reservedBy = userId;
        this.reservedUntil = until;
    }

    public boolean hasActiveReservation(LocalDateTime now) {
        return getSpotStatusEnum() == SpotStatus.RESERVED && reservedBy != null
                && reservedUntil != null && reservedUntil.isAfter(now);
    }

    public Integer getSpotId() {
        return spotId;
    }

    public void setSpotId(Integer spotId) {
        this.spotId = spotId;
    }

    public Integer getLotId() {
        return lotId;
    }

    public void setLotId(Integer lotId) {
        this.lotId = lotId;
    }

    public String getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(String spotNumber) {
        this.spotNumber = spotNumber;
    }

    public Integer getSpotType() {
        return spotType;
    }

    public void setSpotType(Integer spotType) {
        this.spotType = spotType;
    }

    public Integer getSpotStatus() {
        return spotStatus;
    }

    public void setSpotStatus(Integer spotStatus) {
        this.spotStatus = spotStatus;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "ParkingSpot{" +
                "spotId=" + spotId +
                ", spotNumber='" + spotNumber + '\'' +
                ", spotType=" + getSpotTypeName() +
                ", spotStatus=" + getSpotStatusName() +
                '}';
    }
}
