package com.zhixingyuebo.service.impl;

import com.zhixingyuebo.dao.AccountDao;
import com.zhixingyuebo.dao.AccountFlowDao;
import com.zhixingyuebo.dao.ChargingOrderDao;
import com.zhixingyuebo.dao.ChargingPileDao;
import com.zhixingyuebo.dao.ParkingRecordDao;
import com.zhixingyuebo.dao.ParkingSpotDao;
import com.zhixingyuebo.dao.VehicleDao;
import com.zhixingyuebo.dao.impl.AccountDaoImpl;
import com.zhixingyuebo.dao.impl.AccountFlowDaoImpl;
import com.zhixingyuebo.dao.impl.ChargingOrderDaoImpl;
import com.zhixingyuebo.dao.impl.ChargingPileDaoImpl;
import com.zhixingyuebo.dao.impl.ParkingRecordDaoImpl;
import com.zhixingyuebo.dao.impl.ParkingSpotDaoImpl;
import com.zhixingyuebo.dao.impl.VehicleDaoImpl;
import com.zhixingyuebo.entity.Account;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.entity.ParkingRecord;
import com.zhixingyuebo.entity.ParkingSpot;
import com.zhixingyuebo.entity.Vehicle;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.PileFaultException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.ChargingService;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 新能源充电业务服务实现类（管控核心事务二：拔枪分时电量核算与钱包结算事务）
 */
public class ChargingServiceImpl implements ChargingService {

    private final ChargingPileDao chargingPileDao = new ChargingPileDaoImpl();
    private final ChargingOrderDao chargingOrderDao = new ChargingOrderDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final AccountFlowDao accountFlowDao = new AccountFlowDaoImpl();
    private final VehicleDao vehicleDao = new VehicleDaoImpl();
    private final ParkingRecordDao parkingRecordDao = new ParkingRecordDaoImpl();
    private final ParkingSpotDao parkingSpotDao = new ParkingSpotDaoImpl();

    /** 充电、离场和故障处理都按车位、设备的顺序加锁。 */
    private ParkingSpot lockSpot(ChargingPile pile) throws SQLException, ZhiXingYueBoException {
        if (pile == null) throw new ZhiXingYueBoException("充电桩设备不存在！");
        ParkingSpot spot = parkingSpotDao.findByIdForUpdate(pile.getSpotId());
        if (spot == null) throw new ZhiXingYueBoException("充电桩绑定的车位不存在！");
        return spot;
    }

    @Override
    public List<ChargingPile> getReadyPiles() throws ZhiXingYueBoException {
        try {
            List<ChargingPile> all = chargingPileDao.findAll();
            List<ChargingPile> ready = new ArrayList<>();
            for (ChargingPile pile : all) {
                if (pile.getPileStatus() == 1) {
                    ready.add(pile);
                }
            }
            return ready;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询就绪充电桩失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChargingPile> getAllPiles() throws ZhiXingYueBoException {
        try {
            return chargingPileDao.findAll();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询充电桩列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChargingPile getPileById(Integer pileId) throws ZhiXingYueBoException {
        try {
            return chargingPileDao.findById(pileId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询充电桩详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChargingOrder startCharging(Integer pileId, Integer userId, String plateNumber) throws ZhiXingYueBoException {
        try {
            ChargingPile location = chargingPileDao.findById(pileId);
            DBUtil.beginTransaction();
            Vehicle vehicle = plateNumber == null ? null
                    : vehicleDao.findByPlateNumberForUpdate(plateNumber.trim().toUpperCase(Locale.ROOT));
            if (vehicle == null || !vehicle.getUserId().equals(userId) || !vehicle.isNewEnergy()) {
                throw new ZhiXingYueBoException("请使用本人已绑定的新能源车辆启动充电");
            }
            plateNumber = vehicle.getPlateNumber();
            ParkingSpot spot = lockSpot(location);
            ChargingPile pile = chargingPileDao.findByIdForUpdate(pileId);
            if (pile == null || !pile.getSpotId().equals(spot.getSpotId())) {
                throw new ZhiXingYueBoException("充电桩绑定已变更，请重新选择！");
            }
            if (pile.getPileStatus() != 1) {
                throw new PileFaultException(pile.getPileCode(), pile.getPileStatus(), "充电桩当前非空闲就绪状态（可能正在充电或故障报警）！");
            }
            ParkingRecord parked = parkingRecordDao.findActiveRecordBySpotId(spot.getSpotId());
            if (spot.getSpotType() != 2 || spot.getSpotStatus() != 2
                    || parked == null || !vehicle.getVehicleId().equals(parked.getVehicleId())) {
                throw new ZhiXingYueBoException("请先将本人车辆停入该充电桩对应的专用车位！");
            }
            if (accountDao.findByIdForUpdate(userId) == null) throw new ZhiXingYueBoException("用户钱包未初始化");
            if (chargingOrderDao.findActiveOrderByPileId(pileId) != null) {
                throw new ZhiXingYueBoException("此充电桩还有未结算工单！");
            }

            // 检查用户是否已有进行中的充电
            ChargingOrder active = chargingOrderDao.findActiveOrderByUserId(userId);
            if (active != null) {
                throw new ZhiXingYueBoException("您当前已有正在充电中的工单（订单号: " + active.getOrderId() + "），请先拔枪结算！");
            }

            // 订单创建和设备占用使用同一事务。

            // 1. 将充电桩状态翻转为 2: 正在充电
            if (chargingPileDao.compareAndSetStatus(pileId, 1, 2) != 1) throw new ZhiXingYueBoException("充电桩已被占用");

            // 2. 插入进行中的充电工单
            ChargingOrder order = new ChargingOrder(null, pileId, userId, plateNumber, LocalDateTime.now().withNano(0), null, BigDecimal.ZERO, BigDecimal.ZERO, 0);
            chargingOrderDao.insert(order);

            DBUtil.commitTransaction();
            return order;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("启动充电桩失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean recordMeterProgress(Long orderId, Integer userId, BigDecimal previousKwh, BigDecimal nextKwh)
            throws ZhiXingYueBoException {
        BigDecimal previous = ChargingService.exactKwh(previousKwh);
        BigDecimal next = ChargingService.exactKwh(nextKwh);
        if (orderId == null || userId == null || next.compareTo(previous) <= 0) {
            throw new ZhiXingYueBoException("充电计量须随有效工单递增。");
        }
        try {
            return chargingOrderDao.advanceMeter(orderId, userId, previous, next) == 1;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("保存充电计量失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ChargingOrder finishCharging(Long orderId, Integer userId, BigDecimal kwhAmount) throws ZhiXingYueBoException {
        BigDecimal manualKwh = kwhAmount == null ? null : ChargingService.exactKwh(kwhAmount);

        try {
            ChargingOrder location = chargingOrderDao.findById(orderId);
            if (location == null || !location.getUserId().equals(userId)) {
                throw new ZhiXingYueBoException("只能结算本人名下的充电工单！");
            }
            ChargingPile pileLocation = chargingPileDao.findById(location.getPileId());
            DBUtil.beginTransaction();
            ParkingSpot spot = lockSpot(pileLocation);
            ChargingPile pile = chargingPileDao.findByIdForUpdate(location.getPileId());
            if (pile == null || !pile.getSpotId().equals(spot.getSpotId())) {
                throw new ZhiXingYueBoException("充电桩绑定已变更，请重新结算！");
            }
            ChargingOrder order = chargingOrderDao.findByIdForUpdate(orderId);
            if (order == null || !order.getUserId().equals(userId) || !order.getPileId().equals(pile.getPileId())) {
                throw new ZhiXingYueBoException("充电工单已变更，请重新结算！");
            }
            if (order.getOrderStatus() != 0 && order.getOrderStatus() != 2) {
                throw new ZhiXingYueBoException("该充电工单已结算归档，无需重复结算！");
            }

            BigDecimal measuredKwh = order.getKwhAmount();
            BigDecimal settledKwh = manualKwh == null || measuredKwh.signum() > 0 ? measuredKwh : manualKwh;
            BigDecimal pricePerKwh = pile.getPricePerKwh();

            // 计算实付总金额: kwhAmount * pricePerKwh
            BigDecimal totalCost = settledKwh.multiply(pricePerKwh).setScale(2, RoundingMode.HALF_UP);

            // 检查车主钱包可用余额
            Account account = accountDao.findByIdForUpdate(order.getUserId());
            if (account == null) {
                throw new ZhiXingYueBoException("用户钱包未初始化");
            }

            if (account.getBalance().compareTo(totalCost) < 0) {
                throw new InsufficientBalanceException(account.getBalance(), totalCost);
            }

            // =====================================================================
            // 核心事务二：新能源汽车拔枪结算原子事务（JDBC 手动事务）
            // =====================================================================
            BigDecimal beforeBalance = account.getBalance();
            BigDecimal afterBalance = beforeBalance.subtract(totalCost);

            // 1. 扣减车主钱包可用余额
            if (totalCost.signum() > 0 && accountDao.deductBalance(order.getUserId(), totalCost) != 1) {
                throw new ZhiXingYueBoException("扣除充电钱包金额失败，可能余额已被并发划扣！");
            }

            // 2. 写入审计黑匣子流水
            LocalDateTime endTime = LocalDateTime.now().withNano(0);
            AccountFlow flow = new AccountFlow(null, order.getUserId(), beforeBalance, afterBalance, totalCost.negate(), "充电扣款", endTime);
            accountFlowDao.insert(flow);

            // 3. 归档充电工单状态（状态置为 1: 已结算完成）
            if (chargingOrderDao.finishChargingOrder(orderId, endTime, settledKwh, totalCost, 1) != 1) throw new ZhiXingYueBoException("订单已结算，拒绝重复扣款");

            // 4. 将充电桩硬件状态恢复为 1: 空闲就绪
            if (chargingOrderDao.findActiveOrderByPileId(pile.getPileId()) == null) {
                int released = chargingPileDao.compareAndSetStatus(pile.getPileId(), 2, 1);
                if (released == 1 && spot.getSpotStatus() == 3
                        && parkingRecordDao.findActiveRecordBySpotId(spot.getSpotId()) == null) {
                    parkingSpotDao.updateStatus(spot.getSpotId(), 0);
                }
            }

            // 5. 提交事务
            DBUtil.commitTransaction();

            // 更新实体供打印小票
            order.setEndTime(endTime);
            order.setKwhAmount(settledKwh);
            order.setTotalCost(totalCost);
            order.setOrderStatus(1);

            return order;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("充电结算失败，事务已安全回滚: " + e.getMessage(), e);
        }
    }

    @Override
    public ChargingOrder getActiveOrderByUserId(Integer userId) throws ZhiXingYueBoException {
        try {
            return chargingOrderDao.findActiveOrderByUserId(userId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询进行中的充电工单失败: " + e.getMessage(), e);
        }
    }
}
