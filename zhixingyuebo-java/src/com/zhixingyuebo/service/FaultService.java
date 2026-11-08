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
package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.PileFaultLog;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.util.List;

/**
 * 突发故障报警触发、技师认领排障、消警恢复上线业务接口
 */
public interface FaultService {

    /**
     * 触发硬件故障报警熔断（硬件状态置为 3，生成待处理告警工单）
     */
    PileFaultLog reportFault(Integer pileId, String errorCode, String faultDesc) throws ZhiXingYueBoException;

    /**
     * 查询全场所有待接单的故障工单
     */
    List<PileFaultLog> getPendingFaults() throws ZhiXingYueBoException;

    /**
     * 运维技师认领接单
     */
    void claimFault(Long faultId, Integer technicianId) throws ZhiXingYueBoException;

    /**
     * 录入检修处置记录并消警，恢复充电桩上线就绪
     */
    void resolveFault(Long faultId, Integer technicianId, String resolveNote) throws ZhiXingYueBoException;

    /**
     * 获取全量故障报警工单记录
     */
    List<PileFaultLog> getAllFaults() throws ZhiXingYueBoException;
}
