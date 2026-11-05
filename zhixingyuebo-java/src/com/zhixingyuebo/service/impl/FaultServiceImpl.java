package com.zhixingyuebo.service.impl;

import com.zhixingyuebo.dao.ChargingPileDao;
import com.zhixingyuebo.dao.ChargingOrderDao;
import com.zhixingyuebo.dao.ParkingRecordDao;
import com.zhixingyuebo.dao.ParkingSpotDao;
import com.zhixingyuebo.dao.PileFaultLogDao;
import com.zhixingyuebo.dao.impl.ChargingPileDaoImpl;
import com.zhixingyuebo.dao.impl.ChargingOrderDaoImpl;
import com.zhixingyuebo.dao.impl.ParkingRecordDaoImpl;
import com.zhixingyuebo.dao.impl.UserDaoImpl;
import com.zhixingyuebo.dao.impl.ParkingSpotDaoImpl;
import com.zhixingyuebo.dao.impl.PileFaultLogDaoImpl;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.entity.PileFaultLog;
import com.zhixingyuebo.entity.ParkingSpot;
import com.zhixingyuebo.entity.Technician;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.FaultService;
import com.zhixingyuebo.util.DBUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 充电桩故障与报修运维服务实现类（工单状态机闭环流转）
 */
public class FaultServiceImpl implements FaultService {

    private final PileFaultLogDao pileFaultLogDao = new PileFaultLogDaoImpl();
    private final ChargingPileDao chargingPileDao = new ChargingPileDaoImpl();
    private final ParkingSpotDao parkingSpotDao = new ParkingSpotDaoImpl();
    private final ParkingRecordDao parkingRecordDao = new ParkingRecordDaoImpl();
    private final ChargingOrderDao chargingOrderDao = new ChargingOrderDaoImpl();

    private ChargingPile lockPileAndSpot(Integer pileId) throws SQLException, ZhiXingYueBoException {
        ChargingPile location = chargingPileDao.findById(pileId);
        if (location == null) throw new ZhiXingYueBoException("充电桩设备不存在！");
        ParkingSpot spot = parkingSpotDao.findByIdForUpdate(location.getSpotId());
        ChargingPile pile = chargingPileDao.findByIdForUpdate(pileId);
        if (spot == null || pile == null || !spot.getSpotId().equals(pile.getSpotId())) {
            throw new ZhiXingYueBoException("设备绑定车位已变更，请重新处理！");
        }
        return pile;
    }

    @Override
    public PileFaultLog reportFault(Integer pileId, String errorCode, String faultDesc) throws ZhiXingYueBoException {
        try {
            DBUtil.beginTransaction();
            ChargingPile pile = lockPileAndSpot(pileId);
            if (pileFaultLogDao.hasOpenFault(pileId)) {
                throw new ZhiXingYueBoException("该充电桩已有未处理故障工单！");
            }

            chargingPileDao.updateStatus(pileId, 3);
            chargingOrderDao.interruptActiveOrder(pileId);

            boolean parked = parkingRecordDao.findActiveRecordBySpotId(pile.getSpotId()) != null;
            parkingSpotDao.updateStatus(pile.getSpotId(), parked ? 2 : 3);

            // 3. 产生待处理报警工单
            PileFaultLog fault = new PileFaultLog(null, pileId, errorCode, faultDesc, LocalDateTime.now(), null, null, 0);
            pileFaultLogDao.insert(fault);

            DBUtil.commitTransaction();
            return fault;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("上报设备故障失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PileFaultLog> getPendingFaults() throws ZhiXingYueBoException {
        try {
            return pileFaultLogDao.findPendingFaults();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询待处理告警工单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void claimFault(Long faultId, Integer technicianId) throws ZhiXingYueBoException {
        try {
            if (technicianId == null || !(new UserDaoImpl().findById(technicianId) instanceof Technician)) {
                throw new ZhiXingYueBoException("只有运维技师可以接单！");
            }
            PileFaultLog fault = pileFaultLogDao.findById(faultId);
            if (fault == null) {
                throw new ZhiXingYueBoException("故障工单不存在！");
            }
            if (fault.getFaultStatus() != 0) {
                throw new ZhiXingYueBoException("该工单已被认领接单或已修复，无法重复接单！");
            }

            int rows = pileFaultLogDao.claimFault(faultId, technicianId);
            if (rows <= 0) {
                throw new ZhiXingYueBoException("认领工单失败，可能已被其他技师接单！");
            }
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("技师接单失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void resolveFault(Long faultId, Integer technicianId, String resolveNote) throws ZhiXingYueBoException {
        if (resolveNote == null || resolveNote.trim().isEmpty()) {
            throw new ZhiXingYueBoException("请填写排障处置措施说明！");
        }

        try {
            PileFaultLog fault = pileFaultLogDao.findById(faultId);
            if (fault == null) {
                throw new ZhiXingYueBoException("故障工单不存在！");
            }
            DBUtil.beginTransaction();
            ChargingPile pile = lockPileAndSpot(fault.getPileId());
            if (technicianId == null || pileFaultLogDao.resolveFault(faultId, technicianId, resolveNote.trim()) != 1) {
                throw new ZhiXingYueBoException("只能处理本人已认领的在修工单！");
            }

            boolean parked = parkingRecordDao.findActiveRecordBySpotId(pile.getSpotId()) != null;
            boolean unsettled = chargingOrderDao.findActiveOrderByPileId(pile.getPileId()) != null;
            boolean otherFault = pileFaultLogDao.hasOpenFault(pile.getPileId());
            chargingPileDao.updateStatus(pile.getPileId(), otherFault ? 3 : unsettled ? 2 : 1);
            parkingSpotDao.updateStatus(pile.getSpotId(), parked ? 2 : otherFault || unsettled ? 3 : 0);
            DBUtil.commitTransaction();
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("消警上线失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PileFaultLog> getAllFaults() throws ZhiXingYueBoException {
        try {
            return pileFaultLogDao.findAll();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询所有故障记录失败: " + e.getMessage(), e);
        }
    }
}
