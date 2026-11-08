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
package com.zhixingyuebo.exception;

/**
 * 车位已被预约/已被占用/维护禁用冲突异常
 */
public class SpotNotAvailableException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String spotNumber;
    private final int currentStatus;

    public SpotNotAvailableException(String spotNumber, int currentStatus, String message) {
        super(String.format("车位 [%s] 当前不可用（状态码: %d）: %s", spotNumber, currentStatus, message));
        this.spotNumber = spotNumber;
        this.currentStatus = currentStatus;
    }

    public String getSpotNumber() {
        return spotNumber;
    }

    public int getCurrentStatus() {
        return currentStatus;
    }
}
