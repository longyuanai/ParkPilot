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
package com.zhixingyuebo.entity;

import com.zhixingyuebo.enums.VehicleType;

/**
 * 机动车车辆档案实体（对应 tb_vehicle）
 * 融合面向对象状态与新能源类型判定
 */
public class Vehicle {

    private Integer vehicleId;
    private Integer userId;
    private String plateNumber;
    private Integer vehicleType; // 1燃油轿车, 2新能源纯电, 3新能源插混, 4大型货车
    private String brand;
    private Integer isDeleted;

    public Vehicle() {
    }

    public Vehicle(Integer vehicleId, Integer userId, String plateNumber, Integer vehicleType, String brand, Integer isDeleted) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.brand = brand;
        this.isDeleted = isDeleted;
    }

    public String getVehicleTypeName() {
        VehicleType type = getVehicleTypeEnum();
        return type != null ? type.getDescription() : "未知车型";
    }

    public boolean isNewEnergy() {
        VehicleType type = getVehicleTypeEnum();
        return type != null && type.isNewEnergy();
    }

    public VehicleType getVehicleTypeEnum() {
        return VehicleType.fromCode(vehicleType);
    }

    public void setVehicleTypeEnum(VehicleType type) {
        this.vehicleType = type != null ? type.getCode() : null;
    }

    public Integer getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Integer vehicleId) {
        this.vehicleId = vehicleId;
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

    public Integer getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(Integer vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "vehicleId=" + vehicleId +
                ", userId=" + userId +
                ", plateNumber='" + plateNumber + '\'' +
                ", vehicleType=" + getVehicleTypeName() +
                ", brand='" + brand + '\'' +
                '}';
    }
}
