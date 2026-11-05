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
