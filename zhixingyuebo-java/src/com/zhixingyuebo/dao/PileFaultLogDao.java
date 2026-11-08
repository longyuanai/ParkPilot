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
package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.PileFaultLog;

import java.sql.SQLException;
import java.util.List;

/**
 * 故障告警流水录入、技师接单与消警记录回填接口
 */
public interface PileFaultLogDao extends BaseDao<PileFaultLog, Long> {

    /**
     * 查询所有待接单的报警故障工单（fault_status = 0）
     */
    List<PileFaultLog> findPendingFaults() throws SQLException;

    /**
     * 运维技师认领接单（状态更新为 1: 排查维修中）
     */
    int claimFault(Long faultId, Integer technicianId) throws SQLException;

    /**
     * 技师录入排障措施并消警修复（状态更新为 2: 已修复上线）
     */
    int resolveFault(Long faultId, Integer technicianId, String resolveNote) throws SQLException;
    boolean hasOpenFault(Integer pileId) throws SQLException;
}
