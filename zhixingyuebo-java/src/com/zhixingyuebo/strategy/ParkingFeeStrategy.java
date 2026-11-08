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
package com.zhixingyuebo.strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 停车计费策略接口（面向对象开闭原则 OCP 扩展）
 */
public interface ParkingFeeStrategy {

    /**
     * 计算停车总费用
     *
     * @param entry              入场时刻
     * @param exit               离场时刻
     * @param hourlyRate         区域标准小时费率
     * @param hasValidMonthlyCard 是否拥有在有效期内的月卡
     * @return 应收停车费用（两位小数精度）
     */
    BigDecimal calculateFee(LocalDateTime entry, LocalDateTime exit, BigDecimal hourlyRate, boolean hasValidMonthlyCard);

    /**
     * 策略名称描述
     */
    String getStrategyName();
}
