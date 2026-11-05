package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.Account;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * 钱包余额查询、可用余额扣减与充值接口
 */
public interface AccountDao extends BaseDao<Account, Integer> {

    Account findByIdForUpdate(Integer accountId) throws SQLException;

    /**
     * 账户充值（可用余额增加）
     */
    int recharge(Integer accountId, BigDecimal amount) throws SQLException;

    /**
     * 原子安全扣减可用余额（带有余额非负防穿透约束）
     */
    int deductBalance(Integer accountId, BigDecimal amount) throws SQLException;

    /**
     * 直接更新可用余额数值
     */
    int updateBalance(Integer accountId, BigDecimal newBalance) throws SQLException;
}
