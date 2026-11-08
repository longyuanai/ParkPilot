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
 * 用户角色类型枚举（对应 tb_user.user_type）
 */
public enum UserRole {
    OWNER(1, "园区车主/业主"),
    TECHNICIAN(2, "充电桩运维技师"),
    ADMIN(3, "物业综合管理员");

    private final int code;
    private final String description;

    UserRole(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserRole fromCode(Integer code) {
        if (code == null) return null;
        for (UserRole role : values()) {
            if (role.code == code) {
                return role;
            }
        }
        return null;
    }
}
