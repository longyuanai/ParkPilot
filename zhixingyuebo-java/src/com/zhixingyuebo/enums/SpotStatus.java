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
