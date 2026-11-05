package com.zhixingyuebo.service.impl;

import com.zhixingyuebo.dao.AccountDao;
import com.zhixingyuebo.dao.AccountFlowDao;
import com.zhixingyuebo.dao.impl.AccountDaoImpl;
import com.zhixingyuebo.dao.impl.AccountFlowDaoImpl;
import com.zhixingyuebo.entity.Account;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.exception.ZhiXingYueBoException;
import com.zhixingyuebo.service.AccountService;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 钱包账户业务服务实现类
 */
public class AccountServiceImpl implements AccountService {

    private final AccountDao accountDao = new AccountDaoImpl();
    private final AccountFlowDao accountFlowDao = new AccountFlowDaoImpl();

    @Override
    public Account getAccount(Integer userId) throws ZhiXingYueBoException {
        try {
            Account account = accountDao.findById(userId);
            if (account == null) {
                // 若用户尚未初始化钱包，则自动初始化
                account = new Account(userId, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), LocalDateTime.now());
                accountDao.insert(account);
            }
            return account;
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询钱包账户失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Account recharge(Integer userId, BigDecimal amount) throws ZhiXingYueBoException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ZhiXingYueBoException("充值金额必须大于 0.00 元！");
        }
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.signum() <= 0) throw new ZhiXingYueBoException("充值金额四舍五入后必须至少为 0.01 元");

        try {
            DBUtil.beginTransaction();

            Account account = accountDao.findByIdForUpdate(userId);
            if (account == null) {
                account = new Account(userId, BigDecimal.ZERO, BigDecimal.ZERO, LocalDateTime.now());
                accountDao.insert(account);
            }

            BigDecimal beforeBalance = account.getBalance();
            BigDecimal afterBalance = beforeBalance.add(amount);

            // 1. 累加钱包余额
            if (accountDao.recharge(userId, amount) != 1) throw new ZhiXingYueBoException("账户不存在，充值失败");

            // 2. 写入审计黑匣子流水
            AccountFlow flow = new AccountFlow(null, userId, beforeBalance, afterBalance, amount, "账户充值", LocalDateTime.now());
            accountFlowDao.insert(flow);

            DBUtil.commitTransaction();

            account.setBalance(afterBalance);
            return account;
        } catch (Exception e) {
            DBUtil.rollbackTransaction();
            throw new ZhiXingYueBoException("账户充值失败，已安全回滚: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AccountFlow> getFlowsByUserId(Integer userId) throws ZhiXingYueBoException {
        try {
            return accountFlowDao.findByAccountId(userId);
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询资金审计流水失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AccountFlow> getAllFlows() throws ZhiXingYueBoException {
        try {
            return accountFlowDao.findAll();
        } catch (SQLException e) {
            throw new ZhiXingYueBoException("查询全局流水台账失败: " + e.getMessage(), e);
        }
    }
}
