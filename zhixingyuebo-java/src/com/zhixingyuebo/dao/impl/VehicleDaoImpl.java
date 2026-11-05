package com.zhixingyuebo.dao.impl;

import com.zhixingyuebo.dao.VehicleDao;
import com.zhixingyuebo.entity.Vehicle;
import com.zhixingyuebo.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 机动车档案数据访问层实现类
 */
public class VehicleDaoImpl implements VehicleDao {

    @Override
    public Vehicle findByPlateNumber(String plateNumber) throws SQLException {
        return findByPlateNumber(plateNumber, false);
    }

    @Override
    public Vehicle findByPlateNumberForUpdate(String plateNumber) throws SQLException {
        if (!DBUtil.isTransactionActive()) throw new SQLException("行锁查询必须在事务内执行");
        return findByPlateNumber(plateNumber, true);
    }

    private Vehicle findByPlateNumber(String plateNumber, boolean lock) throws SQLException {
        String sql = "SELECT vehicle_id, user_id, plate_number, vehicle_type, brand, is_deleted FROM tb_vehicle WHERE plate_number = ? AND is_deleted = 0"
                + (lock ? " FOR UPDATE" : "");
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, plateNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToVehicle(rs);
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
    public List<Vehicle> findByUserId(Integer userId) throws SQLException {
        String sql = "SELECT vehicle_id, user_id, plate_number, vehicle_type, brand, is_deleted FROM tb_vehicle WHERE user_id = ? AND is_deleted = 0";
        List<Vehicle> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToVehicle(rs));
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
    public Vehicle findById(Integer id) throws SQLException {
        String sql = "SELECT vehicle_id, user_id, plate_number, vehicle_type, brand, is_deleted FROM tb_vehicle WHERE vehicle_id = ? AND is_deleted = 0";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToVehicle(rs);
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
    public List<Vehicle> findAll() throws SQLException {
        String sql = "SELECT vehicle_id, user_id, plate_number, vehicle_type, brand, is_deleted FROM tb_vehicle WHERE is_deleted = 0";
        List<Vehicle> list = new ArrayList<>();
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToVehicle(rs));
            }
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
        return list;
    }

    @Override
    public int insert(Vehicle v) throws SQLException {
        String sql = "INSERT INTO tb_vehicle (user_id, plate_number, vehicle_type, brand, is_deleted) VALUES (?, ?, ?, ?, 0)";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getUserId());
            ps.setString(2, v.getPlateNumber());
            ps.setInt(3, v.getVehicleType());
            ps.setString(4, v.getBrand());
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        v.setVehicleId(rs.getInt(1));
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
    public int update(Vehicle v) throws SQLException {
        String sql = "UPDATE tb_vehicle SET plate_number = ?, vehicle_type = ?, brand = ? WHERE vehicle_id = ?";
        Connection conn = DBUtil.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, v.getPlateNumber());
            ps.setInt(2, v.getVehicleType());
            ps.setString(3, v.getBrand());
            ps.setInt(4, v.getVehicleId());
            return ps.executeUpdate();
        } finally {
            if (!DBUtil.isTransactionActive()) {
                DBUtil.close(conn);
            }
        }
    }

    @Override
    public int deleteById(Integer id) throws SQLException {
        String sql = "UPDATE tb_vehicle SET is_deleted = 1 WHERE vehicle_id = ?";
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

    private Vehicle mapRowToVehicle(ResultSet rs) throws SQLException {
        return new Vehicle(
                rs.getInt("vehicle_id"),
                rs.getInt("user_id"),
                rs.getString("plate_number"),
                rs.getInt("vehicle_type"),
                rs.getString("brand"),
                rs.getInt("is_deleted")
        );
    }
}
