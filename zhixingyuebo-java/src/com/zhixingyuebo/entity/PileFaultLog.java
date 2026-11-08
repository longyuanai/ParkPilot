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
package com.zhixingyuebo.entity;

import com.zhixingyuebo.enums.FaultErrorCode;

import java.time.LocalDateTime;

/**
 * 充电桩故障与告警工单实体（对应 tb_pile_fault_log）
 */
public class PileFaultLog {

    private Long faultId;
    private Integer pileId;
    private String errorCode; // E01过温过载, E02短路接地, E03通讯中断, E04枪头锁死
    private String faultDesc;
    private LocalDateTime reportedAt;
    private Integer technicianId;
    private String resolveNote;
    private Integer faultStatus; // 0待接单, 1排查维修中, 2已修复恢复上线

    public PileFaultLog() {
    }

    public PileFaultLog(Long faultId, Integer pileId, String errorCode, String faultDesc, LocalDateTime reportedAt, Integer technicianId, String resolveNote, Integer faultStatus) {
        this.faultId = faultId;
        this.pileId = pileId;
        this.errorCode = errorCode;
        this.faultDesc = faultDesc;
        this.reportedAt = reportedAt;
        this.technicianId = technicianId;
        this.resolveNote = resolveNote;
        this.faultStatus = faultStatus;
    }

    public String getFaultStatusName() {
        if (faultStatus == null) return "未知状态";
        switch (faultStatus) {
            case 0: return "待接单";
            case 1: return "排查维修中";
            case 2: return "已修复恢复上线";
            default: return "其他";
        }
    }

    public Long getFaultId() {
        return faultId;
    }

    public void setFaultId(Long faultId) {
        this.faultId = faultId;
    }

    public Integer getPileId() {
        return pileId;
    }

    public void setPileId(Integer pileId) {
        this.pileId = pileId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public FaultErrorCode getErrorCodeEnum() {
        return FaultErrorCode.fromCode(errorCode);
    }

    public void setErrorCodeEnum(FaultErrorCode code) {
        this.errorCode = code != null ? code.getCode() : null;
    }

    public String getErrorCodeName() {
        FaultErrorCode err = getErrorCodeEnum();
        return err != null ? err.getName() : (errorCode != null ? errorCode : "未知告警");
    }

    public String getFaultDesc() {
        return faultDesc;
    }

    public void setFaultDesc(String faultDesc) {
        this.faultDesc = faultDesc;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }

    public Integer getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Integer technicianId) {
        this.technicianId = technicianId;
    }

    public String getResolveNote() {
        return resolveNote;
    }

    public void setResolveNote(String resolveNote) {
        this.resolveNote = resolveNote;
    }

    public Integer getFaultStatus() {
        return faultStatus;
    }

    public void setFaultStatus(Integer faultStatus) {
        this.faultStatus = faultStatus;
    }

    @Override
    public String toString() {
        return "PileFaultLog{" +
                "faultId=" + faultId +
                ", pileId=" + pileId +
                ", errorCode='" + errorCode + '\'' +
                ", faultDesc='" + faultDesc + '\'' +
                ", reportedAt=" + reportedAt +
                ", technicianId=" + technicianId +
                ", resolveNote='" + resolveNote + '\'' +
                ", faultStatus=" + getFaultStatusName() +
                '}';
    }
}
