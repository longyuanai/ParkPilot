package com.zhixingyuebo.view;

import com.zhixingyuebo.util.DBUtil;
import com.zhixingyuebo.util.FormatUtil;

import java.sql.Connection;

/**
 * “智行悦泊”智慧园区停车与新能源充电综合管理系统 - 应用程序主运行入口
 */
public class MainApp {

    public static void main(String[] args) {
        // 打印系统启动欢迎横幅
        FormatUtil.printBanner("智行悦泊 • 智慧园区停车与新能源充电综合管理系统 v1.0.0");
        System.out.println("  [系统初始化] 正在检测本地 MySQL 数据库服务与 JDBC 驱动链路...");

        // 自检数据库连接
        try (Connection conn = DBUtil.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                DBUtil.verifyApplicationSchema(conn);
                System.out.println(FormatUtil.green("  [系统初始化] MySQL 数据库已就绪 " + conn.getCatalog()));
            }
        } catch (Exception e) {
            System.err.println(FormatUtil.red("  [警告] 数据库初次握手异常: " + e.getMessage()));
            System.err.println("  请核对本地数据库配置和 README 中的初始化或升级步骤。");
            System.err.println("  答辩演示可运行 demo.bat，启动独立样例库。");
            System.exit(1);
            return;
        }

        System.out.println(FormatUtil.cyan("  [系统初始化] 核心三层架构、线程模拟器与报表引擎装载完毕。\n"));

        // 启动交互菜单分发主循环
        ConsoleMenu menu = new ConsoleMenu();
        menu.start();
    }
}
