package com.zhixingyuebo.dao;

import java.sql.SQLException;
import java.util.List;

/**
 * 通用泛型 DAO 基础接口（定义 CRUD 规范契约）
 *
 * @param <T>  实体类型
 * @param <ID> 主键类型
 */
public interface BaseDao<T, ID> {

    int insert(T entity) throws SQLException;

    int update(T entity) throws SQLException;

    int deleteById(ID id) throws SQLException;

    T findById(ID id) throws SQLException;

    List<T> findAll() throws SQLException;
}
