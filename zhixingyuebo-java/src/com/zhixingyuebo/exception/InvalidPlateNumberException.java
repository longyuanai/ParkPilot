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
 * 车牌号码格式校验不合规异常
 */
public class InvalidPlateNumberException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String plateNumber;

    public InvalidPlateNumberException(String plateNumber, String message) {
        super(String.format("车牌号码 [%s] 不合规: %s", plateNumber, message));
        this.plateNumber = plateNumber;
    }

    public String getPlateNumber() {
        return plateNumber;
    }
}
