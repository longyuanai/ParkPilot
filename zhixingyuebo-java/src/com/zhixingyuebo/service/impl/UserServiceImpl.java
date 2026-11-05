package com.zhixingyuebo.service.impl;

import com.zhixingyuebo.dao.AccountDao;
import com.zhixingyuebo.dao.AccountFlowDao;
import com.zhixingyuebo.dao.UserDao;
import com.zhixingyuebo.dao.VehicleDao;
import com.zhixingyuebo.dao.impl.AccountDaoImpl;
import com.zhixingyuebo.dao.impl.AccountFlowDaoImpl;
import com.zhixingyuebo.dao.impl.UserDaoImpl;
import com.zhixingyuebo.dao.impl.VehicleDaoImpl;
import com.zhixingyuebo.entity.Account;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.entity.Admin;
import com.zhixingyuebo.entity.OwnerDriver;
import com.zhixingyuebo.entity.User;
import com.zhixingyuebo.entity.Vehicle;
import com.zhixingyuebo.exception.InsufficientBalanceException;
import com.zhixingyuebo.exception.InvalidPlateNumberException;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.UserService;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户服务实现类
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();
    private final VehicleDao vehicleDao = new VehicleDaoImpl();
    private final AccountDao accountDao = new AccountDaoImpl();
    private final AccountFlowDao accountFlowDao = new AccountFlowDaoImpl();
    private static final BigDecimal DEFAULT_MONTHLY_CARD_PRICE = new BigDecimal("100.00");
    private static final BigDecimal MAX_WALLET_AMOUNT = new BigDecimal("99999999.99");

    // 中国机动车号牌标准正则表达式（涵盖普通蓝/黄牌 7 位及新能源绿牌 8 位）
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-HJ-NP-Z][A-HJ-NP-Z0-9]{4,6}[A-HJ-NP-Z0-9挂学警港澳]$");

    @Override
    public User login(String phone) throws ZhiXingYueBoException {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            throw new ZhiXingYueBoException("手机号码格式不合法，请输入标准的 11 位中国大陆手机号！");
        }
        try {
            User user = userDao.findByPhone(phone.trim());
            if (user == null) {
                throw new ZhiXingYueBoException("用户未找到或账号已被注销，请核对手机号码！");
            }
            return user;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("数据库访问异常: " + e.getMessage(), e);
        }
    }

    @Override
    public Vehicle bindVehicle(Integer userId, String plateNumber, int vehicleType, String brand) throws ZhiXingYueBoException {
        if (plateNumber == null || !PLATE_PATTERN.matcher(plateNumber.trim().toUpperCase()).matches()) {
            throw new InvalidPlateNumberException(plateNumber, "不符合中国机动车号牌国家标准格式（如：川A88888、川AD12345）！");
        }
        String cleanPlate = plateNumber.trim().toUpperCase();

        try {
            Vehicle exist = vehicleDao.findByPlateNumber(cleanPlate);
            if (exist != null) {
                throw new ZhiXingYueBoException("车牌号 [" + cleanPlate + "] 已被绑定在册，不可重复绑定！");
            }

            Vehicle vehicle = new Vehicle(null, userId, cleanPlate, vehicleType, brand, 0);
            vehicleDao.insert(vehicle);
            return vehicle;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("绑定车辆失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Vehicle> getUserVehicles(Integer userId) throws ZhiXingYueBoException {
        try {
            return vehicleDao.findByUserId(userId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询名下车辆失败: " + e.getMessage(), e);
        }
    }

    @Override
    public User getUserById(Integer userId) throws ZhiXingYueBoException {
        try {
            return userDao.findById(userId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询用户信息失败: " + e.getMessage(), e);
        }
    }

    @Override
    public LocalDateTime getMonthlyCardUntil(Integer userId) throws ZhiXingYueBoException {
        try {
            return userDao.findMonthlyCardUntil(userId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询月卡失败: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal getMonthlyCardPrice() throws ZhiXingYueBoException {
        String setting = System.getProperty("zhixing.monthlyCard.price", System.getenv("ZXYB_MONTHLY_CARD_PRICE"));
        if (setting == null || setting.trim().isEmpty()) return DEFAULT_MONTHLY_CARD_PRICE;
        String invalidPrice = "月卡价格须为 0.01 至 99999999.99 元，最多两位小数，请检查 ZXYB_MONTHLY_CARD_PRICE。";
        try {
            BigDecimal price = new BigDecimal(setting.trim());
            if (price.signum() <= 0 || price.compareTo(MAX_WALLET_AMOUNT) > 0) {
                throw new ZhiXingYueBoException(invalidPrice);
            }
            return price.setScale(2, RoundingMode.UNNECESSARY);
        } catch (IllegalArgumentException | ArithmeticException e) {
            throw new ZhiXingYueBoException(invalidPrice);
        }
    }

    @Override
    public LocalDateTime buyOrRenewMonthlyCard(Integer ownerId) throws ZhiXingYueBoException {
        if (ownerId == null) throw new ZhiXingYueBoException("仅车主可办理或续费月卡。");
        BigDecimal price = getMonthlyCardPrice();
        try {
            DBUtil.beginTransaction();
            // 与离场结算使用相同锁顺序，钱包与月卡有效期按同一笔业务核对。
            Account account = accountDao.findByIdForUpdate(ownerId);
            LocalDateTime currentUntil = userDao.findMonthlyCardUntilForUpdate(ownerId);
            if (!(userDao.findById(ownerId) instanceof OwnerDriver)) {
                throw new ZhiXingYueBoException("仅车主可办理或续费月卡。");
            }
            if (account == null) throw new ZhiXingYueBoException("请先在资产菜单初始化钱包并充值。");
            BigDecimal beforeBalance = account.getBalance();
            if (beforeBalance.compareTo(price) < 0) throw new InsufficientBalanceException(beforeBalance, price);

            LocalDateTime now = LocalDateTime.now().withNano(0);
            LocalDateTime start = currentUntil != null && currentUntil.isAfter(now) ? currentUntil : now;
            LocalDateTime until = start.plusDays(MONTHLY_CARD_DAYS);
            if (until.getYear() > 9999) throw new ZhiXingYueBoException("续费后的到期时间超出数据库范围。");
            if (accountDao.deductBalance(ownerId, price) != 1 || userDao.updateMonthlyCardUntil(ownerId, until) != 1) {
                throw new ZhiXingYueBoException("月卡办理失败，请重新查询钱包与有效期。");
            }
            AccountFlow flow = new AccountFlow(null, ownerId, beforeBalance, beforeBalance.subtract(price),
                    price.negate(), AccountFlow.MONTHLY_CARD_PAYMENT, now);
            if (accountFlowDao.insert(flow) != 1) throw new ZhiXingYueBoException("月卡流水写入失败。");
            DBUtil.commitTransaction();
            return until;
        } catch (ZhiXingYueBoException e) {
            DBUtil.rollbackTransaction();
            throw e;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("月卡办理失败，事务已回滚: " + e.getMessage(), e);
        }
    }

    @Override
    public void setMonthlyCardUntil(Integer adminId, Integer ownerId, LocalDateTime until) throws ZhiXingYueBoException {
        if (adminId == null || !(getUserById(adminId) instanceof Admin)) {
            throw new ZhiXingYueBoException("只有管理员可以登记月卡！");
        }
        if (ownerId == null || until == null || !until.isAfter(LocalDateTime.now())) {
            throw new ZhiXingYueBoException("请输入有效车主与未来的月卡到期时刻！");
        }
        try {
            if (userDao.updateMonthlyCardUntil(ownerId, until) != 1) {
                throw new ZhiXingYueBoException("找不到该车主或到期时刻未变化！");
            }
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("登记月卡失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Vehicle getVehicleByPlate(String plateNumber) throws ZhiXingYueBoException {
        if (plateNumber == null || plateNumber.trim().isEmpty()) {
            return null;
        }
        try {
            return vehicleDao.findByPlateNumber(plateNumber.trim().toUpperCase());
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询车辆失败: " + e.getMessage(), e);
        }
    }
}
