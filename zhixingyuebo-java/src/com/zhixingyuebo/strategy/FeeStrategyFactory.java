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

/**
 * 计费策略工厂（管理停车与充电策略实例）
 */
public class FeeStrategyFactory {

    private static volatile ParkingFeeStrategy parkingFeeStrategy = new StandardParkingFeeStrategy();
    private static volatile ChargingFeeStrategy chargingFeeStrategy = new FlatChargingFeeStrategy();

    private FeeStrategyFactory() {
    }

    public static ParkingFeeStrategy getParkingFeeStrategy() {
        return parkingFeeStrategy;
    }

    public static void setParkingFeeStrategy(ParkingFeeStrategy strategy) {
        if (strategy != null) {
            parkingFeeStrategy = strategy;
        }
    }

    public static ChargingFeeStrategy getChargingFeeStrategy() {
        return chargingFeeStrategy;
    }

    public static void setChargingFeeStrategy(ChargingFeeStrategy strategy) {
        if (strategy != null) {
            chargingFeeStrategy = strategy;
        }
    }
}
