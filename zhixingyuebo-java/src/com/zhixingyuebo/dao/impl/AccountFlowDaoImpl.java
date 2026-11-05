package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.AccountFlowDao;
import com.zhixingyuebo.entity.AccountFlow;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金变动只读审计流水数据访问层实现类（金融级防篡改，严禁 UPDATE/DELETE）
 */
public class AccountFlowDaoImpl implements AccountFlowDao {

    @Override
    public int insert(AccountFlow flow) throws SQLException {
        String sql = "INSERT INTO tb_account_flow (account_id, before_balance, after_balance, diff_amount, biz_type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, flow.getAccountId());
            ps.setBigDecimal(2, flow.getBeforeBalance());
            ps.setBigDecimal(3, flow.getAfterBalance());
            ps.setBigDecimal(4, flow.getDiffAmount());
            ps.setString(5, flow.getBizType());
            ps.setTimestamp(6, flow.getCreatedAt() != null ? Timestamp.valueOf(flow.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        flow.setFlowId(rs.getLong(1));
                    }
                }
            }
            return affected;
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public List<AccountFlow> findByAccountId(Integer accountId) throws SQLException {
        String sql = "SELECT flow_id, account_id, before_balance, after_balance, diff_amount, biz_type, created_at FROM tb_account_flow WHERE account_id = ? ORDER BY flow_id DESC";
        List<AccountFlow> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToFlow(rs));
                }
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public AccountFlow findById(Long id) throws SQLException {
        String sql = "SELECT flow_id, account_id, before_balance, after_balance, diff_amount, biz_type, created_at FROM tb_account_flow WHERE flow_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFlow(rs);
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
    public List<AccountFlow> findAll() throws SQLException {
        String sql = "SELECT flow_id, account_id, before_balance, after_balance, diff_amount, biz_type, created_at FROM tb_account_flow ORDER BY flow_id DESC";
        List<AccountFlow> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToFlow(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int update(AccountFlow entity) throws SQLException {
        throw new UnsupportedOperationException("金融级只读审计流水表禁止任何 UPDATE 操作！");
    }

    @Override
    public int deleteById(Long id) throws SQLException {
        throw new UnsupportedOperationException("金融级只读审计流水表禁止任何 DELETE 操作！");
    }

    private AccountFlow mapRowToFlow(ResultSet rs) throws SQLException {
        return new AccountFlow(
                rs.getLong("flow_id"),
                rs.getInt("account_id"),
                rs.getBigDecimal("before_balance"),
                rs.getBigDecimal("after_balance"),
                rs.getBigDecimal("diff_amount"),
                rs.getString("biz_type"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
