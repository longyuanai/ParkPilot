package com.zhixingyuebo.dao;

import com.zhixingyuebo.entity.User;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 用户认证与信息数据访问接口
 */
public interface UserDao extends BaseDao<User, Integer> {

    /**
     * 根据手机号查询用户（登录核心凭据）
     */
    User findByPhone(String phone) throws SQLException;
    LocalDateTime findMonthlyCardUntil(Integer userId) throws SQLException;
    LocalDateTime findMonthlyCardUntilForUpdate(Integer userId) throws SQLException;
    int updateMonthlyCardUntil(Integer userId, LocalDateTime until) throws SQLException;
}
