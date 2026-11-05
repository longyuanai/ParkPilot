package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.UserDao;
import com.zhixingyuebo.entity.Admin;
import com.zhixingyuebo.entity.OwnerDriver;
import com.zhixingyuebo.entity.Technician;
import com.zhixingyuebo.entity.User;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户数据访问层实现类（纯 JDBC 实现，参数化占位符防注入）
 */
public class UserDaoImpl implements UserDao {

    @Override
    public LocalDateTime findMonthlyCardUntil(Integer userId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT monthly_card_until FROM tb_user WHERE user_id=? AND is_deleted=0")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                Timestamp until = rs.next() ? rs.getTimestamp(1) : null;
                return until == null ? null : until.toLocalDateTime();
            }
        } finally {
            DBUtil.close(conn);
        }
    }

    @Override
    public int updateMonthlyCardUntil(Integer userId, LocalDateTime until) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tb_user SET monthly_card_until=? WHERE user_id=? AND user_type=1 AND is_deleted=0")) {
            ps.setTimestamp(1, Timestamp.valueOf(until));
            ps.setInt(2, userId);
            return ps.executeUpdate();
        } finally {
            DBUtil.close(conn);
        }
    }

    @Override
    public User findByPhone(String phone) throws SQLException {
        String sql = "SELECT user_id, username, phone, user_type, created_at, is_deleted FROM tb_user WHERE phone = ? AND is_deleted = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
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
    public User findById(Integer id) throws SQLException {
        String sql = "SELECT user_id, username, phone, user_type, created_at, is_deleted FROM tb_user WHERE user_id = ? AND is_deleted = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToUser(rs);
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
    public List<User> findAll() throws SQLException {
        String sql = "SELECT user_id, username, phone, user_type, created_at, is_deleted FROM tb_user WHERE is_deleted = 0";
        List<User> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToUser(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(User user) throws SQLException {
        String sql = "INSERT INTO tb_user (username, phone, user_type, is_deleted) VALUES (?, ?, ?, 0)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPhone());
            ps.setInt(3, user.getUserType());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
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
    public int update(User user) throws SQLException {
        String sql = "UPDATE tb_user SET username = ?, phone = ?, user_type = ? WHERE user_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPhone());
            ps.setInt(3, user.getUserType());
            ps.setInt(4, user.getUserId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "UPDATE tb_user SET is_deleted = 1 WHERE user_id = ?";
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

    /**
     * 将 ResultSet 行映射为具体的 User 多态子类
     */
    private User mapRowToUser(ResultSet rs) throws SQLException {
        int userId = rs.getInt("user_id");
        String username = rs.getString("username");
        String phone = rs.getString("phone");
        int userType = rs.getInt("user_type");
        Timestamp createdAtTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdAtTs != null ? createdAtTs.toLocalDateTime() : LocalDateTime.now();
        int isDeleted = rs.getInt("is_deleted");

        User user;
        switch (userType) {
            case 2:
                user = new Technician(userId, username, phone, createdAt, isDeleted);
                break;
            case 3:
                user = new Admin(userId, username, phone, createdAt, isDeleted);
                break;
            case 1:
            default:
                user = new OwnerDriver(userId, username, phone, createdAt, isDeleted);
                break;
        }
        return user;
    }
}
