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

import com.zhixingyuebo.dao.ParkingLotDao;
import com.zhixingyuebo.entity.ParkingLot;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 停车场区域信息数据访问层实现类
 */
public class ParkingLotDaoImpl implements ParkingLotDao {

    @Override
    public ParkingLot findById(Integer id) throws SQLException {
        String sql = "SELECT lot_id, lot_name, total_capacity, hourly_rate FROM tb_parking_lot WHERE lot_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLot(rs);
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
    public List<ParkingLot> findAll() throws SQLException {
        String sql = "SELECT lot_id, lot_name, total_capacity, hourly_rate FROM tb_parking_lot ORDER BY lot_id ASC";
        List<ParkingLot> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToLot(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(ParkingLot lot) throws SQLException {
        String sql = "INSERT INTO tb_parking_lot (lot_name, total_capacity, hourly_rate) VALUES (?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lot.getLotName());
            ps.setInt(2, lot.getTotalCapacity());
            ps.setBigDecimal(3, lot.getHourlyRate());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        lot.setLotId(rs.getInt(1));
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
    public int update(ParkingLot lot) throws SQLException {
        String sql = "UPDATE tb_parking_lot SET lot_name = ?, total_capacity = ?, hourly_rate = ? WHERE lot_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, lot.getLotName());
            ps.setInt(2, lot.getTotalCapacity());
            ps.setBigDecimal(3, lot.getHourlyRate());
            ps.setInt(4, lot.getLotId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM tb_parking_lot WHERE lot_id = ?";
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

    private ParkingLot mapRowToLot(ResultSet rs) throws SQLException {
        return new ParkingLot(
                rs.getInt("lot_id"),
                rs.getString("lot_name"),
                rs.getInt("total_capacity"),
                rs.getBigDecimal("hourly_rate")
        );
    }
}
