# 智行悦泊智慧停车与充电管理系统

黄家威，学号 20233130174，独立完成。项目采用 Java、JDBC、MySQL 和控制台交互，包含多角色、停车预约与进出场、充电、故障处理、资金审计及 CSV 日报。

## 答辩演示

1. 将 resources/db.properties 复制为 resources/db.local.properties，填写本机 MySQL 的连接信息。演示账号须能创建和清理随机数据库，JDBC URL 可以保留原库名。
2. 双击 `demo.bat`。它创建独立样例库，停车计费固定为园区当天 10:00。每次启动都加载新样例。
3. 登录 `13800000001`，在车主菜单 5 为川A88888 办理离场，实扣 4.00 元。登录 `13800000002` 为川A66666 办理离场，可复现应收 80.00 元、差 30.00 元的拦截。
4. 从主菜单输入 0 退出时，程序清理本次演示库。验收记录存于交付包内。

## 安装与运行

1. 准备 JDK 和本地 MySQL。Java 源码以 Java 8 为编译目标，本轮运行验收使用 JDK 17。项目自带实际版本为 8.1.0 的 MySQL Connector/J。
2. 新环境通过 MySQL 客户端执行 [初始化 SQL](sql/01_create_zhixingyuebo_database_and_tables.sql)。脚本创建 zhixingyuebo_db 及十一张核心物理表（含独立月卡实体台账表 `tb_monthly_card`），附带模拟账号。已有库请用下文的升级脚本。
3. 复制 resources/db.properties 为 resources/db.local.properties，在本地文件填写 JDBC 用户和密码。也可用 ZXYB_DB_CONFIG 指定私有配置文件，或用 ZXYB_JDBC_URL、ZXYB_JDBC_USERNAME、ZXYB_JDBC_PASSWORD 提供连接参数。示例配置里的凭据保留空值。
4. 双击 run.bat。首次运行会编译代码，也可先执行 build.bat。IDEA 用户直接打开工程目录，在 Project Structure 中选择本机 JDK，然后运行「智行悦泊」配置。

已有旧数据库时，停用旧进程并确认备份可恢复。连接到预期数据库后执行 [升级 SQL](sql/02_upgrade_existing_database.sql) 开头四项查重，均为空时再执行 ALTER 段。升级增加预约、月卡字段和有效订单唯一索引。若已有冲突记录，先人工确认应保留的真实记录。DDL 分表提交，升级失败时用原备份恢复后再处理。新库已经包含这些结构。

## 演示账号

| 手机号 | 角色 | 常用演示 |
|---|---|---|
| 13800000001 | 车主张先生 | 川A88888 离场、川AF99999 入场、菜单 8 办理月卡 |
| 13800000002 | 车主李女士 | 余额不足拦截 |
| 13800000003 | 运维技师王强 | 故障上报、接单和消警 |
| 13800000004 | 物业管理员 | 审计台账、日报、登记月卡 |
| 13800000005 | 车主陈志强 | 川AD12345 停入 B-0201，随后开桩并结算 |

手机号作为课程演示登录凭据。账号、车牌和钱包余额均为初始化样例。

## 业务口径

- 预约保留 30 分钟。有效期内仅预约人可入场，到期按可用展示并可重新预约，成功入场会清除预约。
- 车主菜单 3 可组合选择车位类型和园区区域。
- 离场需校验车辆归属。普通停车前 30 分钟免费，之后按园区自然日分段，不足一小时按一小时，每日最高 50 元。TC-03 用固定的跨日场景验证 80 元应收与 50 元余额。
- 车主在菜单 8 查询、办理或续费月卡。确认后从钱包扣款，初次办理或过期重办从当前时刻起算，未过期续费从原到期时间增加 30 天。余额、有效期和月卡扣费流水一起提交。管理员仍可登记有效期。有效月卡免停车费，离场时保留零金额流水。
- 课程演示价为每 30 天 100.00 元。可在启动前设置环境变量 `ZXYB_MONTHLY_CARD_PRICE`，例如 PowerShell 中执行 `$env:ZXYB_MONTHLY_CARD_PRICE = '120.00'`。价格须为正数，精确到分。日报单列当日月卡实收，并计入总营收。
- 开桩前车辆须停在对应充电位。模拟充电每次上报先保存累计电量。余额不足后可充值，并在菜单 6 按原计量补结算。电量精确到 0.01 kWh。故障中断时保留原工单和停车占用，结算和消警分别决定设备是否可用。
- 日报分别统计完成车次和实收金额，免费时段和月卡免停的离场也计入车次。
- 按车辆、车位、充电桩、工单和钱包的顺序加锁。月卡办理与离场都在钱包加锁后核对有效期。数据库唯一索引限制一车一条在场记录、一位一辆在场车、每桩和每车主各一张未结充电单。
- **架构扩展与设计模式（O-01~O-06）**：
  - 计费策略体系（`com.zhixingyuebo.strategy`）：遵循开闭原则（OCP），实现基础停车阶梯算费、分时峰谷充电计费（`PeakValleyChargingFeeStrategy`）及充电位超时占位滞纳金（`StandardIdleFeeStrategy`，拔枪超15分钟免费宽限期后按0.5元/分加收，封顶50元）。
  - 欠费挂账与历史补缴：车辆离场时若余额不足可选择应急放行（`pay_status=0: 挂账待补缴`），后续充值后支持在车主控制台在线原子补缴（`pay_status=3: 已补缴出场`）。
  - 原生轻量连接池（`SimpleConnectionPool`）：基于 JDK 动态代理 `Proxy` 拦截 `close()` 实现连接复用，支持 `isValid(1)` 探活与 CAS 原子并发管控，默认启用。
  - 表现层单一职责重构（SRP）：`ConsoleMenu` 作为前端控制器（Front Controller）分发路由，具体业务视图解耦为 `OwnerConsoleView`、`TechnicianConsoleView`、`AdminConsoleView`。

## 验收与打包

Windows 下运行 verify_tests.bat，执行 43 项全量自动化回归（涵盖核心 TC-01~09、高并发 F-01~08、安全 S-01~08、月卡 M-01~05、隔离演示 D-01~05、扩展优化 O-01~06）。测试配置应指向已初始化的本机数据库，账号应有创建和清理随机测试库及创建触发器的权限。工具对业务库取前后指纹，所有用例在新建测试库运行，结束后清理。业务运行账号与验收配置可分开设置。

在开发工程执行 `powershell -NoProfile -ExecutionPolicy Bypass -File verify_tests.ps1 -ExportEvidence -NoPause` 可导出本轮截图与结果，路径为 build/acceptance。重新生成 Word 报告前先备份原文件，再用带有 python-docx 和 Pillow 的 Python 运行 `python docs/build_report.py .`。开发工程的 package.bat 核对源码指纹、验收结果与 Word 内嵌截图，再将黄家威.zip 生成到项目外层的 delivery 目录。

打包默认使用同轮验收的 TC-02 Navicat 截图。复拍时在上述验收命令中加 `-Evidence`。出现 `TC02_QUERY_READY` 后，在 Navicat 选择输出所示的随机验收库，执行下面的只读查询。验收窗口关闭或等待 15 分钟后，工具会清理该库。

```sql
SELECT p.record_id, p.exit_time, p.total_fee,
       f.flow_id, f.before_balance, f.after_balance, f.diff_amount
FROM tb_parking_record AS p
JOIN tb_account_flow AS f
  ON f.account_id = 1 AND f.biz_type = '停车扣费' AND f.created_at = p.exit_time
WHERE p.record_id = 1;
```

将该查询截图保存为 `build/acceptance/screenshots/TC-02-Navicat.png`，把同轮验收库名写入 `build/acceptance/navicat.schema`，再生成报告。报告会并排展示小票和查询结果。

复用已验收的 TC-02 原图时，将旧包中同一验收批次的 `TC-02.png`、`TC-02-Navicat.png` 与记录该库名的 `navicat.schema` 放入 `build/acceptance/tc02-reference`。生成报告时加 `--use-archived-tc02`，打包时加 `-UseArchivedTc02`。报告与验收摘要会标明图片出处，本轮仍须完整重跑验收。
