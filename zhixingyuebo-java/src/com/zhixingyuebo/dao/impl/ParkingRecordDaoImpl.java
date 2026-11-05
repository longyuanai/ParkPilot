package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.ParkingRecordDao;
import com.zhixingyuebo.entity.ParkingRecord;
import com.zhixingyuebo.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 泊车记录数据访问层实现类
 */
public class ParkingRecordDaoImpl implements ParkingRecordDao {

    @Override
    public ParkingRecord findActiveRecordByVehicleIdForUpdate(Integer vehicleId) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record WHERE vehicle_id = ? AND exit_time IS NULL ORDER BY record_id DESC LIMIT 1 FOR UPDATE";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRecord(rs);
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
    public ParkingRecord findActiveRecordByVehicleId(Integer vehicleId) throws SQLException {
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record WHERE vehicle_id = ? AND exit_time IS NULL ORDER BY record_id DESC LIMIT 1";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRecord(rs);
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
    public ParkingRecord findActiveRecordBySpotId(Integer spotId) throws SQLException {
        return findActiveRecordBySpotId(spotId, false);
    }

    @Override
    public ParkingRecord findActiveRecordBySpotIdForUpdate(Integer spotId) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        return findActiveRecordBySpotId(spotId, true);
    }

    private ParkingRecord findActiveRecordBySpotId(Integer spotId, boolean lock) throws SQLException {
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record WHERE active_spot_id = ?"
                + (lock ? " FOR UPDATE" : "");
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, spotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRecord(rs);
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
    public int finishParkingRecord(Long recordId, LocalDateTime exitTime, int parkMinutes, BigDecimal totalFee, int payStatus) throws SQLException {
        String sql = "UPDATE tb_parking_record SET exit_time = ?, park_minutes = ?, total_fee = ?, pay_status = ? WHERE record_id = ? AND exit_time IS NULL AND pay_status = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(exitTime));
            ps.setInt(2, parkMinutes);
            ps.setBigDecimal(3, totalFee);
            ps.setInt(4, payStatus);
            ps.setLong(5, recordId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public List<ParkingRecord> findActiveRecords() throws SQLException {
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record WHERE exit_time IS NULL ORDER BY entry_time ASC";
        List<ParkingRecord> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRecord(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public ParkingRecord findById(Long id) throws SQLException {
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record WHERE record_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRecord(rs);
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
    public List<ParkingRecord> findAll() throws SQLException {
        String sql = "SELECT record_id, vehicle_id, spot_id, entry_time, exit_time, park_minutes, total_fee, pay_status FROM tb_parking_record ORDER BY record_id DESC";
        List<ParkingRecord> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRecord(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(ParkingRecord record) throws SQLException {
        String sql = "INSERT INTO tb_parking_record (vehicle_id, spot_id, entry_time, total_fee, pay_status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getVehicleId());
            ps.setInt(2, record.getSpotId());
            ps.setTimestamp(3, record.getEntryTime() != null ? Timestamp.valueOf(record.getEntryTime()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setBigDecimal(4, record.getTotalFee() != null ? record.getTotalFee() : BigDecimal.ZERO);
            ps.setInt(5, record.getPayStatus() != null ? record.getPayStatus() : 0);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        record.setRecordId(rs.getLong(1));
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
    public int update(ParkingRecord record) throws SQLException {
        String sql = "UPDATE tb_parking_record SET vehicle_id = ?, spot_id = ?, entry_time = ?, exit_time = ?, park_minutes = ?, total_fee = ?, pay_status = ? WHERE record_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, record.getVehicleId());
            ps.setInt(2, record.getSpotId());
            ps.setTimestamp(3, Timestamp.valueOf(record.getEntryTime()));
            ps.setTimestamp(4, record.getExitTime() != null ? Timestamp.valueOf(record.getExitTime()) : null);
            ps.setInt(5, record.getParkMinutes());
            ps.setBigDecimal(6, record.getTotalFee());
            ps.setInt(7, record.getPayStatus());
            ps.setLong(8, record.getRecordId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM tb_parking_record WHERE record_id = ?";
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

    private ParkingRecord mapRowToRecord(ResultSet rs) throws SQLException {
        Timestamp exitTs = rs.getTimestamp("exit_time");
        return new ParkingRecord(
                rs.getLong("record_id"),
                rs.getInt("vehicle_id"),
                rs.getInt("spot_id"),
                rs.getTimestamp("entry_time").toLocalDateTime(),
                exitTs != null ? exitTs.toLocalDateTime() : null,
                rs.getInt("park_minutes"),
                rs.getBigDecimal("total_fee"),
                rs.getInt("pay_status")
        );
    }
}
