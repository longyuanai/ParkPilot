package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.ParkingSpotDao;
import com.zhixingyuebo.entity.ParkingSpot;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 车位数据访问层实现类
 */
public class ParkingSpotDaoImpl implements ParkingSpotDao {
    private static final String COLUMNS = "spot_id, lot_id, spot_number, spot_type, spot_status, is_deleted, reserved_by, reserved_until";

    @Override
    public ParkingSpot findBySpotNumber(String spotNumber) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM tb_parking_spot WHERE spot_number = ? AND is_deleted = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, spotNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSpot(rs);
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
    public List<ParkingSpot> findAvailableSpots(Integer spotType, Integer lotId, LocalDateTime now) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT " + COLUMNS + " FROM tb_parking_spot WHERE is_deleted = 0"
                + " AND (spot_status = 0 OR (spot_status = 1 AND (reserved_by IS NULL OR reserved_until IS NULL OR reserved_until <= ?)))");
        List<Object> params = new ArrayList<>();
        params.add(Timestamp.valueOf(now));

        if (spotType != null && spotType > 0) {
            sql.append(" AND spot_type = ?");
            params.add(spotType);
        }
        if (lotId != null && lotId > 0) {
            sql.append(" AND lot_id = ?");
            params.add(lotId);
        }
        sql.append(" ORDER BY spot_number ASC");

        List<ParkingSpot> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ParkingSpot spot = mapRowToSpot(rs);
                    spot.setSpotStatus(0);
                    spot.setReservation(null, null);
                    list.add(spot);
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
    public int updateStatus(Integer spotId, int newStatus) throws SQLException {
        String sql = "UPDATE tb_parking_spot SET spot_status = ?, reserved_by = NULL, reserved_until = NULL WHERE spot_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStatus);
            ps.setInt(2, spotId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public ParkingSpot findById(Integer id) throws SQLException {
        return findById(id, false);
    }

    @Override
    public ParkingSpot findByIdForUpdate(Integer id) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        return findById(id, true);
    }

    @Override
    public void reserve(Integer spotId, Integer userId, LocalDateTime until) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tb_parking_spot SET spot_status=1, reserved_by=?, reserved_until=? WHERE spot_id=?")) {
            ps.setInt(1, userId);
            ps.setTimestamp(2, Timestamp.valueOf(until));
            ps.setInt(3, spotId);
            ps.executeUpdate();
        } finally {
            DBUtil.close(conn);
        }
    }

    private ParkingSpot findById(Integer id, boolean lock) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM tb_parking_spot WHERE spot_id = ? AND is_deleted = 0"
                + (lock ? " FOR UPDATE" : "");
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToSpot(rs);
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
    public List<ParkingSpot> findAll() throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM tb_parking_spot WHERE is_deleted = 0 ORDER BY spot_id ASC";
        List<ParkingSpot> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToSpot(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(ParkingSpot spot) throws SQLException {
        String sql = "INSERT INTO tb_parking_spot (lot_id, spot_number, spot_type, spot_status, is_deleted) VALUES (?, ?, ?, ?, 0)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, spot.getLotId());
            ps.setString(2, spot.getSpotNumber());
            ps.setInt(3, spot.getSpotType());
            ps.setInt(4, spot.getSpotStatus());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        spot.setSpotId(rs.getInt(1));
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
    public int update(ParkingSpot spot) throws SQLException {
        String sql = "UPDATE tb_parking_spot SET lot_id = ?, spot_number = ?, spot_type = ?, spot_status = ? WHERE spot_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, spot.getLotId());
            ps.setString(2, spot.getSpotNumber());
            ps.setInt(3, spot.getSpotType());
            ps.setInt(4, spot.getSpotStatus());
            ps.setInt(5, spot.getSpotId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "UPDATE tb_parking_spot SET is_deleted = 1 WHERE spot_id = ?";
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

    private ParkingSpot mapRowToSpot(ResultSet rs) throws SQLException {
        ParkingSpot spot = new ParkingSpot(
                rs.getInt("spot_id"),
                rs.getInt("lot_id"),
                rs.getString("spot_number"),
                rs.getInt("spot_type"),
                rs.getInt("spot_status"),
                rs.getInt("is_deleted")
        );
        Timestamp until = rs.getTimestamp("reserved_until");
        int userId = rs.getInt("reserved_by");
        spot.setReservation(rs.wasNull() ? null : userId, until == null ? null : until.toLocalDateTime());
        return spot;
    }
}
