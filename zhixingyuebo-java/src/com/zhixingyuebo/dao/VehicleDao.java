package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.Vehicle;

import java.sql.SQLException;
import java.util.List;

/**
 * 机动车档案增删改查与车牌唯一性检索接口
 */
public interface VehicleDao extends BaseDao<Vehicle, Integer> {

    /**
     * 根据车牌号精确检索车辆
     */
    Vehicle findByPlateNumber(String plateNumber) throws SQLException;
    Vehicle findByPlateNumberForUpdate(String plateNumber) throws SQLException;

    /**
     * 根据车主ID查询名下所有已绑定的车辆档案（1:n）
     */
    List<Vehicle> findByUserId(Integer userId) throws SQLException;
}
