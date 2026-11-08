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
package com.zhixingyuebo;

/**
 * 根包快捷主启动类（委托调用 com.zhixingyuebo.view.MainApp）
 */
public class MainApp {
    public static void main(String[] args) {
        com.zhixingyuebo.view.MainApp.main(args);
    }
}
