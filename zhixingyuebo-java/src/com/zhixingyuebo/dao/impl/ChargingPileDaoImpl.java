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

import com.zhixingyuebo.dao.ChargingPileDao;
import com.zhixingyuebo.entity.ChargingPile;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 充电桩数据访问层实现类
 */
public class ChargingPileDaoImpl implements ChargingPileDao {

    @Override
    public int compareAndSetStatus(Integer pileId, int expected, int next) throws SQLException {
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE tb_charging_pile SET pile_status = ? WHERE pile_id = ? AND pile_status = ?")) {
            ps.setInt(1, next); ps.setInt(2, pileId); ps.setInt(3, expected);
            return ps.executeUpdate();
        } finally { DBUtil.close(conn); }
    }

    @Override
    public ChargingPile findByIdForUpdate(Integer id) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        String sql = "SELECT pile_id, spot_id, pile_code, power_kw, price_per_kwh, pile_status FROM tb_charging_pile WHERE pile_id = ? FOR UPDATE";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPile(rs);
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
    public ChargingPile findBySpotId(Integer spotId) throws SQLException {
        return findBySpotId(spotId, false);
    }

    @Override
    public ChargingPile findBySpotIdForUpdate(Integer spotId) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        return findBySpotId(spotId, true);
    }

    private ChargingPile findBySpotId(Integer spotId, boolean lock) throws SQLException {
        String sql = "SELECT pile_id, spot_id, pile_code, power_kw, price_per_kwh, pile_status FROM tb_charging_pile WHERE spot_id = ?"
                + (lock ? " FOR UPDATE" : "");
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, spotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPile(rs);
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
    public ChargingPile findByPileCode(String pileCode) throws SQLException {
        String sql = "SELECT pile_id, spot_id, pile_code, power_kw, price_per_kwh, pile_status FROM tb_charging_pile WHERE pile_code = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pileCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPile(rs);
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
    public int updateStatus(Integer pileId, int newStatus) throws SQLException {
        String sql = "UPDATE tb_charging_pile SET pile_status = ? WHERE pile_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newStatus);
            ps.setInt(2, pileId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public ChargingPile findById(Integer id) throws SQLException {
        String sql = "SELECT pile_id, spot_id, pile_code, power_kw, price_per_kwh, pile_status FROM tb_charging_pile WHERE pile_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPile(rs);
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
    public List<ChargingPile> findAll() throws SQLException {
        String sql = "SELECT pile_id, spot_id, pile_code, power_kw, price_per_kwh, pile_status FROM tb_charging_pile ORDER BY pile_id ASC";
        List<ChargingPile> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToPile(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(ChargingPile pile) throws SQLException {
        String sql = "INSERT INTO tb_charging_pile (spot_id, pile_code, power_kw, price_per_kwh, pile_status) VALUES (?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, pile.getSpotId());
            ps.setString(2, pile.getPileCode());
            ps.setBigDecimal(3, pile.getPowerKw());
            ps.setBigDecimal(4, pile.getPricePerKwh());
            ps.setInt(5, pile.getPileStatus());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        pile.setPileId(rs.getInt(1));
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
    public int update(ChargingPile pile) throws SQLException {
        String sql = "UPDATE tb_charging_pile SET spot_id = ?, pile_code = ?, power_kw = ?, price_per_kwh = ?, pile_status = ? WHERE pile_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pile.getSpotId());
            ps.setString(2, pile.getPileCode());
            ps.setBigDecimal(3, pile.getPowerKw());
            ps.setBigDecimal(4, pile.getPricePerKwh());
            ps.setInt(5, pile.getPileStatus());
            ps.setInt(6, pile.getPileId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM tb_charging_pile WHERE pile_id = ?";
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

    private ChargingPile mapRowToPile(ResultSet rs) throws SQLException {
        return new ChargingPile(
                rs.getInt("pile_id"),
                rs.getInt("spot_id"),
                rs.getString("pile_code"),
                rs.getBigDecimal("power_kw"),
                rs.getBigDecimal("price_per_kwh"),
                rs.getInt("pile_status")
        );
    }
}
