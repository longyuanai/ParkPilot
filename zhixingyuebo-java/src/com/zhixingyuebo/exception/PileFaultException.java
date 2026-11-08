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
 * 充电桩硬件故障/离线不可用异常
 */
public class PileFaultException extends ZhiXingYueBoException {
    private static final long serialVersionUID = 1L;

    private final String pileCode;
    private final int pileStatus;

    public PileFaultException(String pileCode, int pileStatus, String message) {
        super(String.format("充电桩 [%s] 无法启用（状态码: %d）: %s", pileCode, pileStatus, message));
        this.pileCode = pileCode;
        this.pileStatus = pileStatus;
    }

    public String getPileCode() {
        return pileCode;
    }

    public int getPileStatus() {
        return pileStatus;
    }
}
