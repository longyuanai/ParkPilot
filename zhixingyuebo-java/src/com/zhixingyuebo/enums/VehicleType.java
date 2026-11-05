package com.zhixingyuebo.enums;

/**
 * 车型分类枚举（对应 tb_vehicle.vehicle_type）
 */
public enum VehicleType {
    FUEL(1, "燃油轿车", false),
    EV(2, "新能源纯电 (EV)", true),
    PHEV(3, "新能源插混 (PHEV)", true),
    TRUCK(4, "大型货车", false);

    private final int code;
    private final String description;
    private final boolean newEnergy;

    VehicleType(int code, String description, boolean newEnergy) {
        this.code = code;
        this.description = description;
        this.newEnergy = newEnergy;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean isNewEnergy() {
        return newEnergy;
    }

    public static VehicleType fromCode(Integer code) {
        if (code == null) return null;
        for (VehicleType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
