package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.AccountDao;
import com.zhixingyuebo.entity.Account;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 钱包账户数据访问层实现类
 */
public class AccountDaoImpl implements AccountDao {

    @Override
    public Account findByIdForUpdate(Integer id) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        String sql = "SELECT account_id, balance, frozen_money, updated_at FROM tb_account WHERE account_id = ? FOR UPDATE";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs);
                }
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return null;
    }

    @Override
    public int recharge(Integer accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE tb_account SET balance = balance + ? WHERE account_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deductBalance(Integer accountId, BigDecimal amount) throws SQLException {
        String sql = "UPDATE tb_account SET balance = balance - ? WHERE account_id = ? AND balance >= ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, accountId);
            ps.setBigDecimal(3, amount);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int updateBalance(Integer accountId, BigDecimal newBalance) throws SQLException {
        String sql = "UPDATE tb_account SET balance = ? WHERE account_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setInt(2, accountId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public Account findById(Integer id) throws SQLException {
        String sql = "SELECT account_id, balance, frozen_money, updated_at FROM tb_account WHERE account_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToAccount(rs);
                }
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return null;
    }

    @Override
    public List<Account> findAll() throws SQLException {
        String sql = "SELECT account_id, balance, frozen_money, updated_at FROM tb_account ORDER BY account_id ASC";
        List<Account> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToAccount(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(Account account) throws SQLException {
        String sql = "INSERT INTO tb_account (account_id, balance, frozen_money) VALUES (?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, account.getAccountId());
            ps.setBigDecimal(2, account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
            ps.setBigDecimal(3, account.getFrozenMoney() != null ? account.getFrozenMoney() : BigDecimal.ZERO);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int update(Account account) throws SQLException {
        String sql = "UPDATE tb_account SET balance = ?, frozen_money = ? WHERE account_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, account.getBalance());
            ps.setBigDecimal(2, account.getFrozenMoney());
            ps.setInt(3, account.getAccountId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM tb_account WHERE account_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    private Account mapRowToAccount(ResultSet rs) throws SQLException {
        Timestamp updatedTs = rs.getTimestamp("updated_at");
        return new Account(
                rs.getInt("account_id"),
                rs.getBigDecimal("balance"),
                rs.getBigDecimal("frozen_money"),
                updatedTs != null ? updatedTs.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
