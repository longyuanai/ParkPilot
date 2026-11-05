package com.zhixingyuebo.enums;

/**
 * 车位当前占用状态枚举（对应 tb_parking_spot.spot_status）
 */
public enum SpotStatus {
    AVAILABLE(0, "空闲"),
    RESERVED(1, "已被预约"),
    OCCUPIED(2, "车辆已泊入"),
    MAINTENANCE(3, "维护禁用");

    private final int code;
    private final String description;

    SpotStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SpotStatus fromCode(Integer code) {
        if (code == null) return null;
        for (SpotStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
