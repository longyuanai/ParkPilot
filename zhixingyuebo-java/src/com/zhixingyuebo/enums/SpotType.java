package com.zhixingyuebo.enums;

/**
 * 车位类型枚举（对应 tb_parking_spot.spot_type）
 * 借鉴 parking-lot-problem 消除魔法数字，提供类型安全保障
 */
public enum SpotType {
    STANDARD(1, "普通车位"),
    EV_CHARGING(2, "新能源充电专用位"),
    VIP(3, "无障碍VIP位");

    private final int code;
    private final String description;

    SpotType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static SpotType fromCode(Integer code) {
        if (code == null) return null;
        for (SpotType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
