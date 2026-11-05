package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.ChargingOrderDao;
import com.zhixingyuebo.entity.ChargingOrder;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 充电工单数据访问层实现类
 */
public class ChargingOrderDaoImpl implements ChargingOrderDao {

    @Override
    public ChargingOrder findByIdForUpdate(Long id) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order WHERE order_id = ? FOR UPDATE";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
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
    public ChargingOrder findActiveOrderByPileId(Integer pileId) throws SQLException {
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order WHERE active_pile_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
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
    public ChargingOrder findActiveOrderByUserId(Integer userId) throws SQLException {
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order WHERE active_user_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
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
    public int finishChargingOrder(Long orderId, LocalDateTime endTime, BigDecimal kwhAmount, BigDecimal totalCost, int orderStatus) throws SQLException {
        String sql = "UPDATE tb_charging_order SET end_time = ?, kwh_amount = ?, total_cost = ?, order_status = ? WHERE order_id = ? AND order_status IN (0, 2)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(endTime));
            ps.setBigDecimal(2, kwhAmount);
            ps.setBigDecimal(3, totalCost);
            ps.setInt(4, orderStatus);
            ps.setLong(5, orderId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public boolean hasUnsettledOrderForPlate(String plateNumber) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM tb_charging_order WHERE plate_number=? AND order_status IN (0, 2) LIMIT 1")) {
            ps.setString(1, plateNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } finally {
            DBUtil.close(conn);
        }
    }

    @Override
    public void interruptActiveOrder(Integer pileId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tb_charging_order SET order_status=2 WHERE active_pile_id=? AND order_status=0")) {
            ps.setInt(1, pileId);
            ps.executeUpdate();
        } finally {
            DBUtil.close(conn);
        }
    }

    @Override
    public List<ChargingOrder> findByUserId(Integer userId) throws SQLException {
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order WHERE user_id = ? ORDER BY order_id DESC";
        List<ChargingOrder> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToOrder(rs));
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
    public ChargingOrder findById(Long id) throws SQLException {
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order WHERE order_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToOrder(rs);
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
    public List<ChargingOrder> findAll() throws SQLException {
        String sql = "SELECT order_id, pile_id, user_id, plate_number, start_time, end_time, kwh_amount, total_cost, order_status FROM tb_charging_order ORDER BY order_id DESC";
        List<ChargingOrder> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToOrder(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(ChargingOrder order) throws SQLException {
        String sql = "INSERT INTO tb_charging_order (pile_id, user_id, plate_number, start_time, kwh_amount, total_cost, order_status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, order.getPileId());
            ps.setInt(2, order.getUserId());
            ps.setString(3, order.getPlateNumber());
            ps.setTimestamp(4, order.getStartTime() != null ? Timestamp.valueOf(order.getStartTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setBigDecimal(5, order.getKwhAmount() != null ? order.getKwhAmount() : BigDecimal.ZERO);
            ps.setBigDecimal(6, order.getTotalCost() != null ? order.getTotalCost() : BigDecimal.ZERO);
            ps.setInt(7, order.getOrderStatus() != null ? order.getOrderStatus() : 0);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        order.setOrderId(rs.getLong(1));
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
    public int update(ChargingOrder order) throws SQLException {
        String sql = "UPDATE tb_charging_order SET pile_id = ?, user_id = ?, plate_number = ?, start_time = ?, end_time = ?, kwh_amount = ?, total_cost = ?, order_status = ? WHERE order_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, order.getPileId());
            ps.setInt(2, order.getUserId());
            ps.setString(3, order.getPlateNumber());
            ps.setTimestamp(4, Timestamp.valueOf(order.getStartTime()));
            ps.setTimestamp(5, order.getEndTime() != null ? Timestamp.valueOf(order.getEndTime()) : null);
            ps.setBigDecimal(6, order.getKwhAmount());
            ps.setBigDecimal(7, order.getTotalCost());
            ps.setInt(8, order.getOrderStatus());
            ps.setLong(9, order.getOrderId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM tb_charging_order WHERE order_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    private ChargingOrder mapRowToOrder(ResultSet rs) throws SQLException {
        Timestamp endTs = rs.getTimestamp("end_time");
        return new ChargingOrder(
                rs.getLong("order_id"),
                rs.getInt("pile_id"),
                rs.getInt("user_id"),
                rs.getString("plate_number"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                endTs != null ? endTs.toLocalDateTime() : null,
                rs.getBigDecimal("kwh_amount"),
                rs.getBigDecimal("total_cost"),
                rs.getInt("order_status")
        );
    }
}
