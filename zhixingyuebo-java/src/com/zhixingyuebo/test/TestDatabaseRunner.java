package com.zhixingyuebo.test;

import com.zhixingyuebo.service.impl.ParkingServiceImpl;
import com.zhixingyuebo.util.DBUtil;
import com.zhixingyuebo.view.ConsoleMenu;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 创建随机验收或演示库。清理限定为本进程创建的独立库。 */
public final class TestDatabaseRunner {
    static final ZoneId FIXTURE_ZONE = ZoneId.of("Asia/Shanghai");
    static final LocalDateTime FIXTURE_TIME = LocalDate.now(FIXTURE_ZONE).atTime(10, 0);
    private static Connection admin;
    private static String ownedSchema;
    private static String seed;
    private TestDatabaseRunner() { }

    public static void main(String[] args) throws Exception {
        boolean demo = Arrays.asList(args).contains("--demo");
        boolean evidence = Arrays.asList(args).contains("--evidence");
        Path output = Paths.get(System.getProperty("zhixing.acceptance.dir", "build/acceptance"));
        if (!demo) Files.createDirectories(output);
        Path local = Paths.get(System.getProperty("zhixing.db.config",
            System.getenv("ZXYB_DB_CONFIG") == null ? "resources/db.local.properties" : System.getenv("ZXYB_DB_CONFIG")));
        Properties config = new Properties();
        try (InputStream in = Files.newInputStream(local)) { config.load(in); }
        Class.forName(config.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
        String originalUrl = config.getProperty("jdbc.url");
        Matcher endpoint = Pattern.compile("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1)(?::[0-9]+)?/([A-Za-z0-9_]+)(?:\\?.*)?")
                .matcher(originalUrl == null ? "" : originalUrl);
        if (!endpoint.matches()) {
            throw new IllegalArgumentException("隔离启动器只接受本机单一数据库 URL");
        }
        Properties options = new Properties();
        options.setProperty("user", config.getProperty("jdbc.username"));
        options.setProperty("password", config.getProperty("jdbc.password", ""));
        options.setProperty("connectTimeout", "5000");
        options.setProperty("socketTimeout", "30000");
        String baseUrl = originalUrl.replaceFirst("/([A-Za-z0-9_]+)(\\?|$)", "/$2");
        String originalSchema = endpoint.group(1);
        String before = null;
        if (!demo) {
            try (Connection original = DriverManager.getConnection(originalUrl, options)) {
                original.setReadOnly(true);
                before = fingerprint(original);
            }
        }
        String mode = demo ? "demo" : evidence ? "evidence" : "test";
        ownedSchema = "zhixingyuebo_" + mode + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        if (ownedSchema.equals(originalSchema)) throw new IllegalStateException("独立库名与原配置重合");
        seed = new String(Files.readAllBytes(Paths.get("sql", "01_create_zhixingyuebo_database_and_tables.sql")), StandardCharsets.UTF_8);
        seed = seed.replaceAll("(?m)--[^\\r\\n]*", "")
            .replaceAll("(?is)DROP DATABASE[^;]*;", "")
            .replaceAll("(?is)CREATE DATABASE[^;]*;", "")
            .replaceAll("(?is)USE\\s+`?[A-Za-z0-9_]+`?\\s*;", "");
        Properties summary = new Properties();
        boolean created = false;
        boolean passed = false;
        try (Connection control = DriverManager.getConnection(baseUrl, options)) {
            admin = control;
            try (PreparedStatement ps = admin.prepareStatement("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?")) {
                ps.setString(1, ownedSchema);
                try (ResultSet rs = ps.executeQuery()) { rs.next(); if (rs.getInt(1) != 0) throw new SQLException("测试库重名，拒绝覆盖"); }
            }
            try (Statement statement = admin.createStatement()) {
                statement.executeUpdate("CREATE DATABASE `" + ownedSchema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");
            }
            created = true;
            admin.setCatalog(ownedSchema);
            System.setProperty("zhixing.jdbc.url", originalUrl.replaceFirst("/([A-Za-z0-9_]+)(\\?|$)", "/" + ownedSchema + "$2"));
            System.setProperty("zhixing.jdbc.username", config.getProperty("jdbc.username"));
            System.setProperty("zhixing.jdbc.password", config.getProperty("jdbc.password", ""));
            System.setProperty("zhixing.db.config", local.toAbsolutePath().toString());
            System.setProperty("zhixing.testMode", "true");
            if (demo) {
                resetFixture();
                try (Connection application = DBUtil.getConnection()) {
                    DBUtil.verifyApplicationSchema(application);
                }
                System.out.println("DEMO_DATABASE=" + ownedSchema);
                System.out.println("答辩演示库已就绪，停车计费固定在 " + FIXTURE_TIME.toLocalDate() + " 10:00。");
                Clock clock = Clock.fixed(FIXTURE_TIME.atZone(FIXTURE_ZONE).toInstant(), FIXTURE_ZONE);
                new ConsoleMenu(new ParkingServiceImpl(clock)).start();
                return;
            }
            System.out.println("ISOLATED_DATABASE=" + ownedSchema);
            summary.setProperty("database.server", admin.getMetaData().getDatabaseProductVersion());
            summary.setProperty("database.driver", admin.getMetaData().getDriverVersion());
            passed = TestCaseVerifier.runAll(TestDatabaseRunner::resetFixture, output);
            if (passed && Arrays.asList(args).contains("--export-evidence")) {
                EvidenceWindow.export(TestCaseVerifier.RESULTS, ownedSchema, output.resolve("screenshots"));
                summary.setProperty("evidence.schema", ownedSchema);
                System.out.println("EVIDENCE_EXPORTED");
            }
            if (evidence && passed) {
                TestCaseVerifier.replayParkingExit(TestDatabaseRunner::resetFixture);
                System.out.println("TC02_QUERY_READY=" + ownedSchema);
                EvidenceWindow.showAndWait(TestCaseVerifier.RESULTS, ownedSchema);
            }
        } finally {
            if (created) {
                if (!ownedSchema.matches("zhixingyuebo_(test|evidence|demo)_[a-f0-9]{12}") || ownedSchema.equals(originalSchema)) throw new IllegalStateException("拒绝清理非本次独立库");
                try (Connection cleanup = DriverManager.getConnection(baseUrl, options); Statement statement = cleanup.createStatement()) {
                    statement.executeUpdate("DROP DATABASE `" + ownedSchema + "`");
                }
            }
            if (demo) {
                System.out.println("DEMO_DATABASE_DELETED=" + created);
            } else {
                try (Connection original = DriverManager.getConnection(originalUrl, options)) {
                    original.setReadOnly(true);
                    String after = fingerprint(original);
                    summary.setProperty("production.unchanged", Boolean.toString(before.equals(after)));
                    summary.setProperty("production.before.sha256", before);
                    summary.setProperty("production.after.sha256", after);
                    if (!before.equals(after)) passed = false;
                }
                summary.setProperty("test.schema", ownedSchema);
                summary.setProperty("test.schema.deleted", Boolean.toString(created));
                summary.setProperty("test.passed", Boolean.toString(passed));
                summary.setProperty("test.count", Integer.toString(TestCaseVerifier.RESULTS.size()));
                try (OutputStream out = Files.newOutputStream(output.resolve("summary.properties"))) { summary.store(out, "Acceptance result without credentials"); }
                System.out.println("PRODUCTION_UNCHANGED=" + summary.getProperty("production.unchanged"));
                System.out.println("TEST_DATABASE_DELETED=" + created);
            }
        }
        if (!passed) System.exit(1);
    }

    private static void resetFixture() throws Exception {
        DBUtil.assertIsolatedTestDatabase();
        if (!ownedSchema.equals(admin.getCatalog())) throw new SQLException("测试连接 catalog 已改变");
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = admin.getMetaData().getTables(ownedSchema, null, "tb_%", new String[]{"TABLE"})) {
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
        }
        try (Statement statement = admin.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS=0");
            for (String table : tables) {
                if (!table.matches("tb_[a-z_]+")) throw new SQLException("非预期表名");
                statement.execute("DROP TABLE `" + table + "`");
            }
            statement.execute("SET FOREIGN_KEY_CHECKS=1");
            statement.execute("SET time_zone='+08:00'");
            statement.execute("SET timestamp=" + FIXTURE_TIME.atZone(FIXTURE_ZONE).toEpochSecond());
            try {
                for (String sql : seed.split(";")) {
                    sql = sql.trim();
                    if (sql.isEmpty()) continue;
                    if (!sql.matches("(?is)^(SET |DROP TABLE |CREATE TABLE |INSERT INTO ).*")) throw new SQLException("测试初始化出现非预期 SQL");
                    statement.execute(sql);
                }
            } finally {
                statement.execute("SET timestamp=DEFAULT");
            }
        }
    }

    private static String fingerprint(Connection connection) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<String> tables = new ArrayList<>();
        try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, "tb_%", new String[]{"TABLE"})) {
            while (rs.next()) tables.add(rs.getString("TABLE_NAME"));
        }
        Collections.sort(tables);
        if (tables.size() != 10) throw new SQLException("业务库指纹必须覆盖十张核心表");
        for (String table : tables) {
            if (!table.matches("tb_[a-z_]+")) throw new SQLException("非预期表名");
            digest.update(table.getBytes(StandardCharsets.UTF_8));
            try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT * FROM `" + table + "` ORDER BY 1")) {
                int columns = rs.getMetaData().getColumnCount();
                while (rs.next()) for (int i=1; i<=columns; i++) {
                    String value = rs.getString(i);
                    byte[] bytes = (value == null ? "<NULL>" : value).getBytes(StandardCharsets.UTF_8);
                    digest.update(Integer.toString(bytes.length).getBytes(StandardCharsets.UTF_8));
                    digest.update((byte)':'); digest.update(bytes);
                }
            }
        }
        StringBuilder hash = new StringBuilder();
        for (byte value : digest.digest()) hash.append(String.format("%02x", value & 255));
        return hash.toString();
    }
}
