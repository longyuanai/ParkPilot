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
package com.zhixingyuebo.util;

import com.zhixingyuebo.pool.ConnectionPool;
import com.zhixingyuebo.pool.SimpleConnectionPool;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/** JDBC 连接和当前线程事务。真实凭据只从外部本地配置或环境读取。集成轻量级原生连接池。 */
public final class DBUtil {
    private static final Properties CONFIG = loadConfiguration();
    private static final ThreadLocal<Connection> CONNECTION_HOLDER = new ThreadLocal<>();
    private static volatile SimpleConnectionPool pool;
    private DBUtil() { }
    private static Properties loadConfiguration() {
        Properties config = new Properties();
        try {
            try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in != null) config.load(in);
            }
            String configured = System.getProperty("zhixing.db.config", System.getenv("ZXYB_DB_CONFIG"));
            Path local = Paths.get(configured != null ? configured : "resources/db.local.properties");
            if (Files.isRegularFile(local)) {
                try (InputStream in = Files.newInputStream(local)) { config.load(in); }
            } else if (configured != null) throw new IOException("指定的本地配置文件不存在");
            override(config, "jdbc.url", "ZXYB_JDBC_URL");
            override(config, "jdbc.username", "ZXYB_JDBC_USERNAME");
            override(config, "jdbc.password", "ZXYB_JDBC_PASSWORD");
            Class.forName(config.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
            return config;
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static void override(Properties config, String key, String env) {
        String value = System.getProperty("zhixing." + key);
        if (value == null) value = System.getenv(env);
        if (value != null) config.setProperty(key, value);
    }
    private static String getEffectiveUrl() {
        String url = System.getProperty("zhixing.jdbc.url");
        if (url == null) url = System.getenv("ZXYB_JDBC_URL");
        if (url == null) url = CONFIG.getProperty("jdbc.url");
        return url;
    }
    private static String getEffectiveUsername() {
        String user = System.getProperty("zhixing.jdbc.username");
        if (user == null) user = System.getenv("ZXYB_JDBC_USERNAME");
        if (user == null) user = CONFIG.getProperty("jdbc.username", "");
        return user;
    }
    private static String getEffectivePassword() {
        String pwd = System.getProperty("zhixing.jdbc.password");
        if (pwd == null) pwd = System.getenv("ZXYB_JDBC_PASSWORD");
        if (pwd == null) pwd = CONFIG.getProperty("jdbc.password", "");
        return pwd;
    }
    private static Connection connect() throws SQLException {
        String url = getEffectiveUrl();
        String user = getEffectiveUsername();
        if (url == null || user.trim().isEmpty()) throw new SQLException("请设置 db.local.properties 或 ZXYB_JDBC_* 环境变量");
        Properties options = new Properties();
        options.setProperty("user", user);
        options.setProperty("password", getEffectivePassword());
        options.setProperty("connectTimeout", "5000");
        options.setProperty("socketTimeout", "30000");
        return DriverManager.getConnection(url, options);
    }
    public static ConnectionPool getPool() throws SQLException {
        String currentUrl = getEffectiveUrl();
        if (pool == null || !pool.getUrl().equals(currentUrl)) {
            synchronized (DBUtil.class) {
                if (pool == null || !pool.getUrl().equals(currentUrl)) {
                    if (pool != null) {
                        pool.close();
                    }
                    String user = getEffectiveUsername();
                    if (currentUrl == null || user.trim().isEmpty()) {
                        throw new SQLException("请设置 db.local.properties 或 ZXYB_JDBC_* 环境变量");
                    }
                    Properties options = new Properties();
                    options.setProperty("user", user);
                    options.setProperty("password", getEffectivePassword());
                    options.setProperty("connectTimeout", "5000");
                    options.setProperty("socketTimeout", "30000");
                    pool = new SimpleConnectionPool(currentUrl, options, 2, 32, 8000);
                }
            }
        }
        return pool;
    }
    public static Connection getConnection() throws SQLException {
        Connection connection = CONNECTION_HOLDER.get();
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        if ("false".equalsIgnoreCase(System.getProperty("zhixing.pool.enabled"))) {
            return connect();
        }
        return getPool().getConnection();
    }
    public static void verifyApplicationSchema(Connection connection) throws SQLException {
        String[][] required = {
                {"tb_user", "monthly_card_until"}, {"tb_parking_spot", "reserved_by"}, {"tb_parking_spot", "reserved_until"},
                {"tb_parking_record", "active_vehicle_id"}, {"tb_parking_record", "active_spot_id"},
                {"tb_charging_order", "active_pile_id"}, {"tb_charging_order", "active_user_id"}
        };
        for (String[] column : required) {
            try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), null, column[0], column[1])) {
                if (!rs.next()) throw new SQLException("数据库结构尚未升级。请参照 README 备份后执行 02_upgrade_existing_database.sql。");
            }
        }
    }
    public static Connection beginTransaction() throws SQLException {
        if (isTransactionActive()) throw new SQLException("不允许嵌套事务");
        Connection connection = connect();
        try {
            // 业务状态用显式行锁保护，普通查询读取已提交状态。
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            connection.setAutoCommit(false);
            CONNECTION_HOLDER.set(connection);
            return connection;
        } catch (SQLException e) { connection.close(); throw e; }
    }
    public static void commitTransaction() throws SQLException {
        Connection connection = CONNECTION_HOLDER.get();
        if (connection == null) throw new SQLException("没有可提交的事务");
        try { connection.commit(); }
        catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
            throw e;
        } finally {
            CONNECTION_HOLDER.remove();
            try { connection.close(); } catch (SQLException ignored) { }
        }
    }
    public static void rollbackTransaction() {
        Connection connection = CONNECTION_HOLDER.get();
        CONNECTION_HOLDER.remove();
        if (connection == null) return;
        try { connection.rollback(); }
        catch (SQLException e) { System.err.println("事务回滚失败，请核对数据库连接状态"); }
        finally { try { connection.close(); } catch (SQLException ignored) { } }
    }
    public static boolean isTransactionActive() {
        Connection connection = CONNECTION_HOLDER.get();
        try { return connection != null && !connection.isClosed() && !connection.getAutoCommit(); }
        catch (SQLException e) { return false; }
    }
    /** 测试夹具必须核验实际 catalog，不能仅信任命令行的模式开关。 */
    public static void assertIsolatedTestDatabase() throws SQLException {
        if (!Boolean.getBoolean("zhixing.testMode")) throw new SQLException("拒绝在业务模式运行测试夹具");
        try (Connection connection = connect()) {
            String catalog = connection.getCatalog();
            if (catalog == null || !catalog.matches("zhixingyuebo_(test|evidence|demo)_[a-f0-9]{12}")) throw new SQLException("拒绝修改非独立测试库");
        }
    }
    public static void close(Connection connection) {
        if (connection == null || connection == CONNECTION_HOLDER.get()) return;
        try { connection.close(); } catch (SQLException ignored) { }
    }
    public static void close(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable resource : resources) {
            if (resource instanceof Connection) close((Connection) resource);
            else if (resource != null) { try { resource.close(); } catch (Exception ignored) { } }
        }
    }
}
