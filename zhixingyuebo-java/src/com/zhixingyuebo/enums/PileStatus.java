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
 * 充电桩硬件运行状态枚举（对应 tb_charging_pile.pile_status）
 */
public enum PileStatus {
    READY(1, "空闲就绪 (Online)"),
    CHARGING(2, "正在充电"),
    FAULT(3, "故障报警"),
    OFFLINE(4, "离线检修");

    private final int code;
    private final String description;

    PileStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static PileStatus fromCode(Integer code) {
        if (code == null) return null;
        for (PileStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
