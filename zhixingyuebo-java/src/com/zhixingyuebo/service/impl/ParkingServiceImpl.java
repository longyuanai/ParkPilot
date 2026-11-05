package com.zhixingyuebo.service.impl;

import com.zhixingyuebo.dao.*;
import com.zhixingyuebo.dao.impl.*;
import com.zhixingyuebo.entity.*;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.SpotNotAvailableException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.ParkingService;
import com.zhixingyuebo.util.DBUtil;
import com.zhixingyuebo.util.DateUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 泊车核心业务服务实现类（管控核心事务一：离场阶梯算费与自动扣款事务）
 */
public class ParkingServiceImpl implements ParkingService {
    private static final int RESERVATION_MINUTES = 30;
    private final Clock clock;

    private final ParkingSpotDao parkingSpotDao = new ParkingSpotDaoImpl();
    private final ParkingLotDao parkingLotDao = new ParkingLotDaoImpl();
    private final ParkingRecordDao parkingRecordDao = new ParkingRecordDaoImpl();
    private final VehicleDao vehicleDao = new VehicleDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final AccountFlowDao accountFlowDao = new AccountFlowDaoImpl();
    private final ChargingOrderDao chargingOrderDao = new ChargingOrderDaoImpl();
    private final ChargingPileDao chargingPileDao = new ChargingPileDaoImpl();
    private final UserDao userDao = new UserDaoImpl();

    public ParkingServiceImpl() {
        this(Clock.systemDefaultZone());
    }

    public ParkingServiceImpl(Clock clock) {
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    private Vehicle lockOwnedVehicle(String plateNumber, Integer userId) throws SQLException, ZhiXingYueBoException {
        Vehicle vehicle = plateNumber == null ? null : vehicleDao.findByPlateNumberForUpdate(plateNumber.trim().toUpperCase(Locale.ROOT));
        if (vehicle == null || !vehicle.getUserId().equals(userId)) {
            throw new ZhiXingYueBoException("只能操作本人名下车辆！");
        }
        return vehicle;
    }

    private ParkingSpot lockSpot(Integer spotId) throws SQLException, ZhiXingYueBoException {
        ParkingSpot spot = parkingSpotDao.findByIdForUpdate(spotId);
        if (spot == null) throw new ZhiXingYueBoException("车位不存在！");
        return spot;
    }

    private boolean hasOccupancy(ParkingSpot spot) throws SQLException {
        ChargingPile pile = chargingPileDao.findBySpotIdForUpdate(spot.getSpotId());
        return parkingRecordDao.findActiveRecordBySpotId(spot.getSpotId()) != null
                || (pile != null && (pile.getPileStatus() != 1
                || chargingOrderDao.findActiveOrderByPileId(pile.getPileId()) != null));
    }

    @Override
    public List<ParkingSpot> getAvailableSpots(Integer spotType, Integer lotId) throws ZhiXingYueBoException {
        try {
            return parkingSpotDao.findAvailableSpots(spotType, lotId, now());
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询可用车位失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ParkingSpot> getAllSpots() throws ZhiXingYueBoException {
        try {
            List<ParkingSpot> spots = parkingSpotDao.findAll();
            LocalDateTime at = now();
            for (ParkingSpot spot : spots) {
                if (spot.getSpotStatus() == 1 && !spot.hasActiveReservation(at)) {
                    spot.setSpotStatus(0);
                    spot.setReservation(null, null);
                }
            }
            return spots;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询车位列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingSpot reserveSpot(Integer spotId, Integer userId) throws ZhiXingYueBoException {
        try {
            DBUtil.beginTransaction();
            ParkingSpot spot = lockSpot(spotId);
            LocalDateTime at = now();
            if (userId == null || !(userDao.findById(userId) instanceof OwnerDriver)) {
                throw new ZhiXingYueBoException("只有车主可以预约车位！");
            }
            boolean unavailable = spot.getSpotStatus() >= 2 || spot.hasActiveReservation(at);
            if (unavailable || hasOccupancy(spot)) {
                throw new SpotNotAvailableException(spot.getSpotNumber(), spot.getSpotStatus(), "车位当前处于非空闲状态，无法预约！");
            }

            LocalDateTime until = at.plusMinutes(RESERVATION_MINUTES);
            parkingSpotDao.reserve(spotId, userId, until);
            DBUtil.commitTransaction();
            spot.setSpotStatus(1);
            spot.setReservation(userId, until);
            return spot;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("预约车位异常: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingRecord simulateEntry(String plateNumber, Integer spotId, Integer userId) throws ZhiXingYueBoException {
        try {
            DBUtil.beginTransaction();
            Vehicle vehicle = lockOwnedVehicle(plateNumber, userId);
            ParkingSpot spot = lockSpot(spotId);

            ParkingRecord active = parkingRecordDao.findActiveRecordByVehicleId(vehicle.getVehicleId());
            if (active != null) {
                throw new ZhiXingYueBoException("车辆 [" + plateNumber + "] 已经在场泊车中，无需重复入场！");
            }

            // 借鉴 archttp/parking-lot-management 工业级规范：油车占用新能源专用车位拦截防范
            if (!vehicle.isNewEnergy() && spot.getSpotType() == 2) {
                throw new ZhiXingYueBoException("【绿色通道保护】车牌 [" + plateNumber + "] 登记为燃油车，严禁占用新能源充电专用车位！请停泊至普通车位。");
            }

            if (spot.getSpotStatus() >= 2 || hasOccupancy(spot)) {
                throw new SpotNotAvailableException(spot.getSpotNumber(), spot.getSpotStatus(), "车位已被占用或维护禁用！");
            }
            if (spot.hasActiveReservation(now()) && !userId.equals(spot.getReservedBy())) {
                throw new SpotNotAvailableException(spot.getSpotNumber(), spot.getSpotStatus(), "车位已被其他车主预约！");
            }

            parkingSpotDao.updateStatus(spotId, 2);
            ParkingRecord record = new ParkingRecord(null, vehicle.getVehicleId(), spotId, now(), null, 0, BigDecimal.ZERO, 0);
            parkingRecordDao.insert(record);

            DBUtil.commitTransaction();
            return record;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("车辆入场登记失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingRecord processExit(String plateNumber, Integer userId) throws ZhiXingYueBoException {
        try {
            DBUtil.beginTransaction();
            Vehicle vehicle = lockOwnedVehicle(plateNumber, userId);

            ParkingRecord record = parkingRecordDao.findActiveRecordByVehicleId(vehicle.getVehicleId());
            if (record == null) {
                throw new ZhiXingYueBoException("未查询到车辆 [" + plateNumber + "] 的在场泊车记录，车辆可能已离场或尚未入场！");
            }

            ParkingSpot spot = lockSpot(record.getSpotId());
            ChargingPile pile = chargingPileDao.findBySpotIdForUpdate(spot.getSpotId());
            if (chargingOrderDao.hasUnsettledOrderForPlate(vehicle.getPlateNumber())
                    || (pile != null && chargingOrderDao.findActiveOrderByPileId(pile.getPileId()) != null)) {
                throw new ZhiXingYueBoException("车位仍有未结算充电工单，请先拔枪结算！");
            }
            record = parkingRecordDao.findActiveRecordByVehicleIdForUpdate(vehicle.getVehicleId());
            if (record == null || !spot.getSpotId().equals(record.getSpotId())) {
                throw new ZhiXingYueBoException("在场记录已变更，请重新办理离场！");
            }
            ParkingLot lot = parkingLotDao.findById(spot.getLotId());
            BigDecimal hourlyRate = lot != null ? lot.getHourlyRate() : new BigDecimal("4.00");

            // 计算时间与阶梯停车费
            LocalDateTime exitTime = now();
            long minutes = DateUtil.calculateMinutes(record.getEntryTime(), exitTime);
            LocalDateTime monthlyCardUntil = userDao.findMonthlyCardUntil(userId);
            boolean monthlyFree = monthlyCardUntil != null && monthlyCardUntil.isAfter(exitTime);
            BigDecimal totalFee = monthlyFree ? new BigDecimal("0.00")
                    : DateUtil.calculateParkingFee(record.getEntryTime(), exitTime, hourlyRate);
            int payStatus = monthlyFree ? 1 : 2;

            // 检查车主钱包可用余额
            Account account = accountDao.findByIdForUpdate(vehicle.getUserId());
            if (account == null) {
                throw new ZhiXingYueBoException("用户钱包未初始化");
            }

            // 若可用余额不足，立即抛出受检异常，阻断抬杆放行（绝不产生单边账）
            if (account.getBalance().compareTo(totalFee) < 0) {
                throw new InsufficientBalanceException(account.getBalance(), totalFee);
            }

            // =====================================================================
            // 核心事务一：原子化离场结算闭环（JDBC 手动事务）
            // =====================================================================
            BigDecimal beforeBalance = account.getBalance();
            BigDecimal afterBalance = beforeBalance.subtract(totalFee);

            // 1. 扣减车主钱包可用余额
            if (totalFee.signum() > 0 && accountDao.deductBalance(vehicle.getUserId(), totalFee) != 1) {
                throw new ZhiXingYueBoException("扣减钱包余额失败，可能余额已被其他并发事务划扣！");
            }

            // 2. 写入只读防篡改资金变动审计流水
            AccountFlow flow = new AccountFlow(null, vehicle.getUserId(), beforeBalance, afterBalance,
                    totalFee.negate(), monthlyFree ? "月卡免停" : "停车扣费", exitTime);
            accountFlowDao.insert(flow);

            // 3. 将对应车位状态安全释放为 0: 空闲
            boolean maintenance = spot.getSpotStatus() == 3 || (pile != null && pile.getPileStatus() >= 3);
            parkingSpotDao.updateStatus(spot.getSpotId(), maintenance ? 3 : 0);

            // 4. 回填停车流水的出场时刻、停泊分钟数、实收总额与支付状态 (2: 钱包成功扣款)
            if (parkingRecordDao.finishParkingRecord(record.getRecordId(), exitTime, (int) minutes, totalFee, payStatus) != 1) {
                throw new ZhiXingYueBoException("停车单已结算，拒绝重复扣款");
            }

            // 5. 全链路无误，统一步骤提交事务
            DBUtil.commitTransaction();

            // 更新实体状态用于表现层渲染与打印小票
            record.setExitTime(exitTime);
            record.setParkMinutes((int) minutes);
            record.setTotalFee(totalFee);
            record.setPayStatus(payStatus);

            return record;
        } catch (InsufficientBalanceException e) {
            // 余额不足：事务零变更，重新向外抛出
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            // 任何未知异常：原子回滚并绝不抬杆
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("离场结算失败，已安全回滚数据库事务: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingRecord getActiveRecordByPlate(String plateNumber) throws ZhiXingYueBoException {
        try {
            Vehicle vehicle = vehicleDao.findByPlateNumber(plateNumber);
            if (vehicle == null) {
                return null;
            }
            return parkingRecordDao.findActiveRecordByVehicleId(vehicle.getVehicleId());
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询泊车记录失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ParkingRecord> getAllActiveRecords() throws ZhiXingYueBoException {
        try {
            return parkingRecordDao.findActiveRecords();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询在场车辆流水失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ParkingLot> getAllLots() throws ZhiXingYueBoException {
        try {
            return parkingLotDao.findAll();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询区域列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingSpot getSpotById(Integer spotId) throws ZhiXingYueBoException {
        try {
            return parkingSpotDao.findById(spotId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询车位详情失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void addSpot(ParkingSpot spot) throws ZhiXingYueBoException {
        if (spot == null || spot.getSpotNumber() == null || spot.getSpotNumber().trim().isEmpty()) {
            throw new ZhiXingYueBoException("车位编号不能为空！");
        }
        try {
            parkingSpotDao.insert(spot);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("新增规划车位失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ParkingSpot recommendOptimalSpot(String plateNumber) throws ZhiXingYueBoException {
        try {
            Vehicle vehicle = vehicleDao.findByPlateNumber(plateNumber);
            boolean isEv = vehicle != null && vehicle.isNewEnergy();

            List<ParkingSpot> availableSpots = parkingSpotDao.findAvailableSpots(null, null, now());
            if (availableSpots == null || availableSpots.isEmpty()) {
                return null;
            }

            // 排序规则：按物理车位编号自然顺序就近匹配（模拟物理入口最近距离）
            availableSpots.sort((a, b) -> a.getSpotNumber().compareToIgnoreCase(b.getSpotNumber()));

            if (isEv) {
                // 新能源车：优先匹配空闲充电车位 (spotType = 2)
                for (ParkingSpot s : availableSpots) {
                    if (s.getSpotType() == 2) {
                        return s;
                    }
                }
                // 充电车位全满时，回退到就近普通车位 (spotType = 1)
                for (ParkingSpot s : availableSpots) {
                    if (s.getSpotType() == 1) {
                        return s;
                    }
                }
            } else {
                // 燃油车：只推荐普通车位 (spotType = 1)，严禁推荐充电车位
                for (ParkingSpot s : availableSpots) {
                    if (s.getSpotType() == 1) {
                        return s;
                    }
                }
                // 普通车位全满时，备选 VIP 车位 (spotType = 3)
                for (ParkingSpot s : availableSpots) {
                    if (s.getSpotType() == 3) {
                        return s;
                    }
                }
            }

            return null;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("智能推荐车位失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateSpotStatus(Integer spotId, int status) throws ZhiXingYueBoException {
        if (status != 0 && status != 3) {
            throw new ZhiXingYueBoException("只能手动设置空闲或维护状态！");
        }
        try {
            DBUtil.beginTransaction();
            ParkingSpot spot = lockSpot(spotId);
            ChargingPile pile = chargingPileDao.findBySpotIdForUpdate(spotId);
            if (spot.hasActiveReservation(now()) || parkingRecordDao.findActiveRecordBySpotId(spotId) != null
                    || (pile != null && chargingOrderDao.findActiveOrderByPileId(pile.getPileId()) != null)) {
                throw new ZhiXingYueBoException("车位仍有预约、在场车辆或充电工单！");
            }
            if (status == 0 && pile != null && pile.getPileStatus() >= 3) {
                throw new ZhiXingYueBoException("充电桩故障或离线，请先处理设备状态！");
            }
            parkingSpotDao.updateStatus(spotId, status);
            DBUtil.commitTransaction();
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("修改车位状态异常: " + e.getMessage(), e);
        }
    }
}
