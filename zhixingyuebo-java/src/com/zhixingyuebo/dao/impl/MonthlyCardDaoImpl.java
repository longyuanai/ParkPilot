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

import com.zhixingyuebo.dao.MonthlyCardDao;
import com.zhixingyuebo.entity.MonthlyCard;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 车主停车月卡数据访问实现类
 */
public class MonthlyCardDaoImpl implements MonthlyCardDao {

    @Override
    public int insert(MonthlyCard card) throws SQLException {
        String sql = "INSERT INTO tb_monthly_card (user_id, card_type, price, start_time, end_time, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, card.getUserId());
            ps.setString(2, card.getCardType() != null ? card.getCardType() : "STANDARD");
            ps.setBigDecimal(3, card.getPrice());
            ps.setTimestamp(4, Timestamp.valueOf(card.getStartTime()));
            ps.setTimestamp(5, Timestamp.valueOf(card.getEndTime()));
            ps.setTimestamp(6, card.getCreatedAt() != null ? Timestamp.valueOf(card.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        card.setCardId(rs.getLong(1));
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
    public int update(MonthlyCard card) throws SQLException {
        String sql = "UPDATE tb_monthly_card SET card_type = ?, price = ?, start_time = ?, end_time = ? WHERE card_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, card.getCardType());
            ps.setBigDecimal(2, card.getPrice());
            ps.setTimestamp(3, Timestamp.valueOf(card.getStartTime()));
            ps.setTimestamp(4, Timestamp.valueOf(card.getEndTime()));
            ps.setLong(5, card.getCardId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Long cardId) throws SQLException {
        String sql = "DELETE FROM tb_monthly_card WHERE card_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public MonthlyCard findById(Long cardId) throws SQLException {
        String sql = "SELECT card_id, user_id, card_type, price, start_time, end_time, created_at FROM tb_monthly_card WHERE card_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, cardId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public List<MonthlyCard> findAll() throws SQLException {
        String sql = "SELECT card_id, user_id, card_type, price, start_time, end_time, created_at FROM tb_monthly_card ORDER BY card_id DESC";
        List<MonthlyCard> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public List<MonthlyCard> findByUserId(Integer userId) throws SQLException {
        String sql = "SELECT card_id, user_id, card_type, price, start_time, end_time, created_at FROM tb_monthly_card WHERE user_id = ? ORDER BY card_id DESC";
        List<MonthlyCard> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public MonthlyCard findLatestByUserId(Integer userId) throws SQLException {
        String sql = "SELECT card_id, user_id, card_type, price, start_time, end_time, created_at FROM tb_monthly_card WHERE user_id = ? ORDER BY card_id DESC LIMIT 1";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    private MonthlyCard mapRow(ResultSet rs) throws SQLException {
        Timestamp start = rs.getTimestamp("start_time");
        Timestamp end = rs.getTimestamp("end_time");
        Timestamp created = rs.getTimestamp("created_at");
        return new MonthlyCard(
                rs.getLong("card_id"),
                rs.getInt("user_id"),
                rs.getString("card_type"),
                rs.getBigDecimal("price"),
                start != null ? start.toLocalDateTime() : null,
                end != null ? end.toLocalDateTime() : null,
                created != null ? created.toLocalDateTime() : null
        );
    }
}
