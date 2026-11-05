package com.zhixingyuebo.service;

import com.zhixingyuebo.entity.User;
import com.zhixingyuebo.entity.Vehicle;
import com.zhixingyuebo.exception.ZhiXingYueBoException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 手机号登录认证、名下车辆绑定业务接口
 */
public interface UserService {

    int MONTHLY_CARD_DAYS = 30;

    /**
     * 手机号码快速认证登录
     */
    User login(String phone) throws ZhiXingYueBoException;

    /**
     * 车主绑定机动车档案（含严格车牌正则表达式校验）
     */
    Vehicle bindVehicle(Integer userId, String plateNumber, int vehicleType, String brand) throws ZhiXingYueBoException;

    /**
     * 查询车主名下绑定的所有机动车
     */
    List<Vehicle> getUserVehicles(Integer userId) throws ZhiXingYueBoException;

    /**
     * 根据主键查询用户
     */
    User getUserById(Integer userId) throws ZhiXingYueBoException;
    LocalDateTime getMonthlyCardUntil(Integer userId) throws ZhiXingYueBoException;
    BigDecimal getMonthlyCardPrice() throws ZhiXingYueBoException;
    LocalDateTime buyOrRenewMonthlyCard(Integer ownerId) throws ZhiXingYueBoException;
    void setMonthlyCardUntil(Integer adminId, Integer ownerId, LocalDateTime until) throws ZhiXingYueBoException;

    /**
     * 根据车牌号码查询车辆档案
     */
    Vehicle getVehicleByPlate(String plateNumber) throws ZhiXingYueBoException;
}
