/*
 * Copyright (c) 2026 Jiawei Huang (黄家威). All Rights Reserved.
 *
 * Project: ParkPilot (智泊领航) - Smart Parking & EV Charging Core Engine
 * Licensed under the PolyForm Noncommercial License 1.0.0 (the "License").
 * You may not use this file except in compliance with the License.
 *
 * Commercial Use strictly requires a separate commercial license from the author.
 * For commercial licensing, please refer to COMMERCIAL.md or contact: 1507205727@qq.com
 */
package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.PileFaultLogDao;
import com.zhixingyuebo.entity.PileFaultLog;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 充电桩故障告警数据访问层实现类
 */
public class PileFaultLogDaoImpl implements PileFaultLogDao {

    @Override
    public List<PileFaultLog> findPendingFaults() throws SQLException {
        String sql = "SELECT fault_id, pile_id, error_code, fault_desc, reported_at, technician_id, resolve_note, fault_status FROM tb_pile_fault_log WHERE fault_status = 0 ORDER BY reported_at ASC";
        List<PileFaultLog> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToFault(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int claimFault(Long faultId, Integer technicianId) throws SQLException {
        String sql = "UPDATE tb_pile_fault_log SET technician_id = ?, fault_status = 1 WHERE fault_id = ? AND fault_status = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, technicianId);
            ps.setLong(2, faultId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int resolveFault(Long faultId, Integer technicianId, String resolveNote) throws SQLException {
        String sql = "UPDATE tb_pile_fault_log SET resolve_note = ?, fault_status = 2 WHERE fault_id = ? AND fault_status = 1 AND technician_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, resolveNote);
            ps.setLong(2, faultId);
            ps.setInt(3, technicianId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public boolean hasOpenFault(Integer pileId) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM tb_pile_fault_log WHERE pile_id=? AND fault_status IN (0, 1) LIMIT 1")) {
            ps.setInt(1, pileId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } finally {
            DBUtil.close(conn);
        }
    }

    @Override
    public PileFaultLog findById(Long id) throws SQLException {
        String sql = "SELECT fault_id, pile_id, error_code, fault_desc, reported_at, technician_id, resolve_note, fault_status FROM tb_pile_fault_log WHERE fault_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToFault(rs);
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
    public List<PileFaultLog> findAll() throws SQLException {
        String sql = "SELECT fault_id, pile_id, error_code, fault_desc, reported_at, technician_id, resolve_note, fault_status FROM tb_pile_fault_log ORDER BY fault_id DESC";
        List<PileFaultLog> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToFault(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(PileFaultLog log) throws SQLException {
        String sql = "INSERT INTO tb_pile_fault_log (pile_id, error_code, fault_desc, reported_at, fault_status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, log.getPileId());
            ps.setString(2, log.getErrorCode());
            ps.setString(3, log.getFaultDesc());
            ps.setTimestamp(4, log.getReportedAt() != null ? Timestamp.valueOf(log.getReportedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, log.getFaultStatus() != null ? log.getFaultStatus() : 0);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        log.setFaultId(rs.getLong(1));
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
    public int update(PileFaultLog log) throws SQLException {
        String sql = "UPDATE tb_pile_fault_log SET pile_id = ?, error_code = ?, fault_desc = ?, reported_at = ?, technician_id = ?, resolve_note = ?, fault_status = ? WHERE fault_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getPileId());
            ps.setString(2, log.getErrorCode());
            ps.setString(3, log.getFaultDesc());
            ps.setTimestamp(4, Timestamp.valueOf(log.getReportedAt()));
            if (log.getTechnicianId() != null) {
                ps.setInt(5, log.getTechnicianId());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, log.getResolveNote());
            ps.setInt(7, log.getFaultStatus());
            ps.setLong(8, log.getFaultId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM tb_pile_fault_log WHERE fault_id = ?";
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

    private PileFaultLog mapRowToFault(ResultSet rs) throws SQLException {
        int techId = rs.getInt("technician_id");
        Integer technicianId = rs.wasNull() ? null : techId;
        return new PileFaultLog(
                rs.getLong("fault_id"),
                rs.getInt("pile_id"),
                rs.getString("error_code"),
                rs.getString("fault_desc"),
                rs.getTimestamp("reported_at").toLocalDateTime(),
                technicianId,
                rs.getString("resolve_note"),
                rs.getInt("fault_status")
        );
    }
}
