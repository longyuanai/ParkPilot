# ParkPilot (智泊领航) 🚀
### 工业级智慧园区停车与新能源充电综合管控引擎 (Smart Parking & EV Charging Core Engine)

[![License: PolyForm Noncommercial 1.0.0](https://img.shields.io/badge/License-PolyForm%20Noncommercial-blue.svg)](LICENSE)
[![Commercial License](https://img.shields.io/badge/Commercial%20License-Available%20%26%20Required-brightgreen.svg)](COMMERCIAL.md)
[![Java: 8 / 17](https://img.shields.io/badge/Java-8%20%7C%2017-orange.svg)](https://www.oracle.com/java/)
[![Tests: 43/43 Passed](https://img.shields.io/badge/Tests-43%2F43%20Passed%20(100%25)-success.svg)](zhixingyuebo-java/verify_tests.bat)
[![Architecture: Zero Framework](https://img.shields.io/badge/Architecture-Pure%20Native%20Java%20SE-purple.svg)](README.md)
[![Author: Jiawei Huang](https://img.shields.io/badge/Author-Jiawei%20Huang%20(%E9%BB%84%E5%AE%B6%E5%A8%81)-informational.svg)](COMMERCIAL.md)

---

## 📖 项目简介 (Overview)

**ParkPilot (智泊领航)** 是一套面向现代化智慧园区、商业综合体及新能源充电场站打造的**轻量级、高并发、工业级停车与充电综合管控核心引擎**。

系统**坚决拒绝 Spring / MyBatis 等重型框架黑盒**，完全回归 Java 底层面向对象设计（OOP）、多线程并发控制、网络与原生 JDBC 事务本质。包体积仅几百 KB，内存占用低于 100MB，既可部署在云端服务器，更可直接在 **200 元的树莓派、工控机、道闸边缘计算网关** 上毫秒级极速响应，支持断网离线安全结算。

> 💡 **项目商业模式**：本项目遵循 **PolyForm Noncommercial 1.0.0** 源码可用协议。个人学习与学术研究免费开放，**任何商业运营、企业生产部署、闭源集成或盈利性产品使用，均须获得原作者（黄家威）书面商业授权**。详见 [COMMERCIAL.md](COMMERCIAL.md)。

---

## ⚡ 核心架构与技术突破 (Key Features)

### 1. 纯原生动态代理轻量级连接池 (SimpleConnectionPool)
- 摆脱 Druid / HikariCP 依赖，纯手写原生高效连接池；
- 基于 JDK 动态代理 `Proxy` 深度拦截 `Connection.close()`，实现无感归还连接复用与脏事务状态自动复位；
- 内置 `BlockingQueue` 阻塞队列、CAS `AtomicInteger` 物理连接上限控制与 `isValid(1)` 自动心跳探活，彻底杜绝连接泄漏。

### 2. 金融级事务原子性与并发风控 (Financial-Grade Concurrency Control)
- **ThreadLocal 物理连接绑定**：业务层统一管理事务边界，多 DAO 间通过 `ThreadLocal<Connection>` 强绑定，失败时全盘安全回滚；
- **悲观排他行锁 (`SELECT ... FOR UPDATE`)**：离场结算与拔枪扣款严格串行化，经受住 **8 路并发离场、8 路并发拔枪、24 路并发充值** 抗重放压测，确保绝无单边账；
- **只读防篡改资金变动黑匣子**：全系统金额、电量强制使用 `BigDecimal` 字符串构造与高精度四舍五入（`RoundingMode.HALF_UP`），资金变动流水严格只读追加。

### 3. 停充一体化与创新计费策略 (Smart Strategy & OCP)
- **开闭原则计费策略工厂 (`FeeStrategyFactory`)**：动态切换阶梯停车计费与新能源电价核算；
- **超时占桩滞纳金策略 (`StandardIdleFeeStrategy`)**：新能源汽车拔枪后享受 15 分钟免费挪车宽限期，超时后按 0.50 元/分钟计收超时占桩滞纳金（单次 50 元封顶），与出场停车费合并扣缴并计入对账流水；
- **智能电网分时峰谷充电计费 (`PeakValleyChargingFeeStrategy`)**：按峰段（1.8元/度）、平段（1.2元/度）、谷段（0.6元/度）梯度电价动态核算；
- **燃油车绿色通道主动拦截**：毫秒级识别燃油车并拒绝驶入充电专用车位，守护新能源通道。

### 4. 欠费应急挂账与在线原子补缴状态机 (State Machine Closed-Loop)
- 支持车辆离场时余额不足选择应急放行（`pay_status=0` 挂账待补缴，道闸应急抬杆）；
- 车主充值后，在车主工作台支持一键发起原子补缴，安全扣款并将状态机翻转为 `pay_status=3`（已补缴出场），形成完整财务闭环。

### 5. 双创新特色模块 (Innovative Modules)
- **创新 A：IoT 智能充电网关模拟器 (`ChargingSimulatorThread`)**：
  多线程守护任务，利用终端单行 `\r` 平滑刷新 ASCII 动态彩色进度条与实时脉冲电量上报；
- **创新 B：防乱码离线财务对账日报 (`CsvExportUtil`)**：
  前置注入 UTF-8 BOM 字符，彻底解决 Windows Excel 打开乱码问题，精确汇总全日营收、分项收费、完成车次与只读资金审计流水。

---

## 🏛️ 系统架构设计 (System Architecture)

```
┌────────────────────────────────────────────────────────────────────────┐
│                   ParkPilot (智泊领航) 架构全景图                       │
├────────────────────────────────────────────────────────────────────────┤
│ 【表现层 (View)】 Front Controller 前端控制器统一路由                   │
│   ├── OwnerConsoleView (车主与业主服务工作台)                          │
│   ├── TechnicianConsoleView (充电设备运维技师工作台)                   │
│   └── AdminConsoleView (物业综合运营管理工作台)                        │
├────────────────────────────────────────────────────────────────────────┤
│ 【业务层 (Service)】 ThreadLocal 原子事务控制 + 策略工厂 (Strategy)     │
│   ├── ParkingService (车位预约、道闸联动、阶梯计费、欠费挂账出场)       │
│   ├── ChargingService (插枪启动、拔枪结算、超时占位滞纳金核算)         │
│   ├── FeeStrategyFactory (分时峰谷计费 / 超时占位费 / 阶梯停车费)      │
│   └── UserService / AccountService (用户资产、独立月卡台账审计)        │
├────────────────────────────────────────────────────────────────────────┤
│ 【持久层 (DAO)】 纯原生参数化 JDBC CRUD (100% 防 SQL 注入)             │
│   ├── SimpleConnectionPool (JDK Proxy 动态代理连接池、心跳探活)        │
│   └── 11 张物理核心表 (tb_parking_record, tb_monthly_card, tb_account) │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 🧪 质量工程：43/43 项全自动化回归测试

ParkPilot 拥有工业级的全自动化测试核验套件（`TestCaseVerifier`），包含**自毁式独立沙箱数据库机制（UUID 动态 schema）**，测试完成后自动自毁，保证生产数据库 `PRODUCTION_UNCHANGED=true` 干净零污染：

| 测试范围 | 用例标识 | 验证核心要点 | 状态 |
|---|---|---|---|
| **核心业务闭环** | `TC-01 ~ TC-09` | 多角色登录、4元阶梯离场、80元余额不足回滚、30度充电结算、消警恢复、非法输入重试、油车绿色通道拦截、充电位优先调度、金额边界枚举看板 | **PASS (100%)** |
| **双创新特色** | `A-01, B-01` | IoT 多线程动态进度模拟器、UTF-8 BOM CSV 财务日报跨日隔离 | **PASS (100%)** |
| **高并发与事务风控** | `F-01 ~ F-08` | 8路并发离场防重放、8路并发抢单、24路并发充值审计、SQL 触发器故障原子回滚、月卡免停 | **PASS (100%)** |
| **安全与边界防御** | `S-01 ~ S-08` | 生产库保护、越权离场拦截、预约30分钟到期自动回收、8路并发抢同一车位、充电泊位联动锁定 | **PASS (100%)** |
| **月卡与对账机制** | `M-01 ~ M-05` | 控制台自助办卡/续费、过期重办、并发续费串行与日报对账 | **PASS (100%)** |
| **隔离答辩与复核** | `D-01 ~ D-05` | 固定时点账单复现、计量补扣与精度断言、免费车次统计 | **PASS (100%)** |
| **架构扩展专项** | `O-01 ~ O-06` | 策略工厂动态切换、分时峰谷核算、独立月卡实体台账、超时占桩滞纳金、欠费挂账出场与在线补缴、动态代理连接池复用 | **PASS (100%)** |

---

## 🚀 快速开始 (Quick Start)

### 1. 环境准备
- **运行环境**：JDK 8 或 JDK 17+
- **数据库**：MySQL 8.0+（自带 Connector/J 8.1.0 驱动）

### 2. 数据库初始化
在本地 MySQL 客户端执行初始化脚本：
```bash
mysql -u root -p < zhixingyuebo-java/sql/01_create_zhixingyuebo_database_and_tables.sql
```

### 3. 配置与启动
将 `resources/db.properties` 复制为 `resources/db.local.properties` 并填写本地连接参数：
```properties
jdbc.url=jdbc:mysql://localhost:3306/zhixingyuebo_db?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
jdbc.username=your_username
jdbc.password=your_password
```

运行启动脚本：
```bash
# Windows 控制台一键编译并启动主程序
cd zhixingyuebo-java
run.bat

# 一键执行 43 项全量自动化测试验证
verify_tests.bat
```

### 4. 预置演示账号矩阵
| 手机号 (登录凭据) | 角色 | 典型演示链路 |
|---|---|---|
| `13800000001` | 高净值车主 (张先生) | 预约车位、川A88888 离场扣款 4 元、办理 100 元停车月卡 |
| `13800000002` | 欠费测试车主 (李女士) | 余额不足 80 元拦截、应急挂账出场 (`pay_status=0`)、充值后在线补缴 (`pay_status=3`) |
| `13800000003` | 运维技师 (王强) | 认领 E01 温控熔断工单、录入检修记录、设备消警上线 |
| `13800000004` | 物业综合管理员 | 全景 ASCII 动态彩色进度条看板、导出 UTF-8 BOM CSV 财务报表 |
| `13800000005` | 新能源车主 (陈志强) | 纯电川AD12345 停入充电位、启动 120kW 快充桩充入 30 度电扣款 45 元 |

---

## 💼 商业授权与企业支持 (Commercial Licensing)

ParkPilot (智泊领航) 采用**「源码可用 + 双重商业许可」**模式：

- **非商业用途**：遵循 `PolyForm Noncommercial License 1.0.0`，个人学习、高校科研免费使用；
- **商业用途**：企业生产运营、软件外包集成、商业闭源修改、SaaS 平台收费部署，**必须向原作者购买商业授权**。

### 商业授权方案概览
1. **企业源码买断商业授权**（¥5,999 / 一次性买断）：获取 100% 完整源代码，授予企业闭源二次修改、转售与无限次商业部署权，签署《软件商业授权合同》；
2. **单场站运营部署授权**（¥1,500 / 场站 / 年）：提供针对单一停车场的生产运行授权及技术维护；
3. **企业定制与硬件对接**（按需评估）：对接海康/大华车牌识别一体机、充电桩国标通信协议（GB/T 27930 或 OCPP）、微信/支付宝无感支付等。

> 📮 **商务咨询与商业授权联系方式**：  
> - **著作权所有人**：黄家威 (Jiawei Huang)  
> - **商业授权邮箱**：**[1507205727@qq.com](mailto:1507205727@qq.com)**  
> - **商业授权细则详见**：[COMMERCIAL.md](COMMERCIAL.md)

---

## 📄 版权与声明 (Copyright & Notice)

Copyright (c) 2026 **黄家威 (Jiawei Huang)**. All Rights Reserved.  
本项目全部源代码、架构设计与技术文档保留所有合法商业权利。未经书面授权擅自商用者，将依法追究侵权法律责任。
