-- Fresh database initialization only. Existing database is never dropped.
-- ====================================================================
-- 《“智行悦泊”智慧园区停车与新能源充电综合管理系统》
-- 数据库初始化脚本：01_create_zhixingyuebo_database_and_tables.sql
-- 字符集：utf8mb4 / 排序规则：utf8mb4_general_ci
-- ====================================================================

CREATE DATABASE `zhixingyuebo_db`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `zhixingyuebo_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 表 1：用户基本信息表（tb_user）
-- --------------------------------------------------------------------
CREATE TABLE `tb_user` (
    `user_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户唯一编码',
    `username` VARCHAR(30) NOT NULL COMMENT '真实姓名/昵称',
    `phone` CHAR(11) NOT NULL COMMENT '11位手机号码（登录唯一凭据）',
    `user_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '角色（1普通车主/业主, 2运维技师, 3物业管理员）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号注册时间',
    `monthly_card_until` DATETIME DEFAULT NULL COMMENT '月卡到期时刻',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除标记（0正常, 1已注销）',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户基本信息表';

-- --------------------------------------------------------------------
-- 表 2：机动车车辆档案表（tb_vehicle）
-- --------------------------------------------------------------------
CREATE TABLE `tb_vehicle` (
    `vehicle_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车辆内部编号',
    `user_id` INT UNSIGNED NOT NULL COMMENT '归属车主ID，关联 tb_user.user_id',
    `plate_number` VARCHAR(10) NOT NULL COMMENT '车牌号码（如：川A88888、川AD12345）',
    `vehicle_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '车型（1燃油轿车, 2新能源纯电, 3新能源插混, 4大型货车）',
    `brand` VARCHAR(30) NOT NULL DEFAULT '' COMMENT '车辆品牌型号（如：比亚迪汉、特斯拉Model Y）',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '软删除（0在册, 1解绑）',
    PRIMARY KEY (`vehicle_id`),
    UNIQUE KEY `uk_plate_number` (`plate_number`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_vehicle_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='机动车车辆档案表';

-- --------------------------------------------------------------------
-- 表 3：园区停车场区域表（tb_parking_lot）
-- --------------------------------------------------------------------
CREATE TABLE `tb_parking_lot` (
    `lot_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '区域编号',
    `lot_name` VARCHAR(50) NOT NULL COMMENT '停车场名称（如：A区地下负一层、B区南门地面停车场）',
    `total_capacity` SMALLINT UNSIGNED NOT NULL DEFAULT 100 COMMENT '规划总车位数',
    `hourly_rate` DECIMAL(6,2) NOT NULL DEFAULT 4.00 COMMENT '临停标准费率（元/小时，前30分钟免费）',
    PRIMARY KEY (`lot_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='园区停车场区域表';

-- --------------------------------------------------------------------
-- 表 4：车位基础信息表（tb_parking_spot）
-- --------------------------------------------------------------------
CREATE TABLE `tb_parking_spot` (
    `spot_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '车位唯一编号',
    `lot_id` INT UNSIGNED NOT NULL COMMENT '所属区域ID，关联 tb_parking_lot.lot_id',
    `spot_number` VARCHAR(20) NOT NULL COMMENT '车位物理编码（如：A-0101、B-0205）',
    `spot_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '车位属性（1普通车位, 2新能源充电专用位, 3无障碍VIP位）',
    `spot_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前占用状态（0空闲, 1已被预约, 2车辆已泊入, 3维护禁用）',
    `reserved_by` INT UNSIGNED DEFAULT NULL COMMENT '预约车主',
    `reserved_until` DATETIME DEFAULT NULL COMMENT '预约到期时刻',
    `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '报废软删除',
    PRIMARY KEY (`spot_id`),
    UNIQUE KEY `uk_spot_number` (`spot_number`),
    KEY `idx_lot_id` (`lot_id`),
    CONSTRAINT `fk_spot_reserver` FOREIGN KEY (`reserved_by`) REFERENCES `tb_user` (`user_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_spot_lot` FOREIGN KEY (`lot_id`) REFERENCES `tb_parking_lot` (`lot_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车位基础信息表';

-- --------------------------------------------------------------------
-- 表 5：新能源智能充电桩表（tb_charging_pile）
-- --------------------------------------------------------------------
CREATE TABLE `tb_charging_pile` (
    `pile_id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '充电桩资产编号',
    `spot_id` INT UNSIGNED NOT NULL COMMENT '绑定车位ID，关联 tb_parking_spot.spot_id（1:1）',
    `pile_code` VARCHAR(32) NOT NULL COMMENT '充电桩设备唯一出厂条码编号',
    `power_kw` DECIMAL(5,1) NOT NULL DEFAULT 7.0 COMMENT '额定功率（kW，如：7.0慢充、120.0快充）',
    `price_per_kwh` DECIMAL(6,2) NOT NULL DEFAULT 1.20 COMMENT '综合电价费率（元/度，含电费+服务费）',
    `pile_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '硬件状态（1空闲就绪, 2正在充电, 3故障报警, 4离线检修）',
    PRIMARY KEY (`pile_id`),
    UNIQUE KEY `uk_spot_id` (`spot_id`),
    UNIQUE KEY `uk_pile_code` (`pile_code`),
    CONSTRAINT `fk_pile_spot` FOREIGN KEY (`spot_id`) REFERENCES `tb_parking_spot` (`spot_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='新能源智能充电桩表';

-- --------------------------------------------------------------------
-- 表 6：停车进出场与计费记录主表（tb_parking_record）
-- --------------------------------------------------------------------
CREATE TABLE `tb_parking_record` (
    `record_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '停车单流水号',
    `vehicle_id` INT UNSIGNED NOT NULL COMMENT '停泊车辆ID，关联 tb_vehicle.vehicle_id',
    `spot_id` INT UNSIGNED NOT NULL COMMENT '占用车位ID，关联 tb_parking_spot.spot_id',
    `entry_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '车辆道闸入场时刻',
    `exit_time` DATETIME DEFAULT NULL COMMENT '车辆离场道闸放行时刻（未离场为NULL）',
    `park_minutes` INT UNSIGNED DEFAULT 0 COMMENT '累计停泊总分钟数',
    `total_fee` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '应收停车总费（元）',
    `pay_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '结算状态（0未结算/在场, 1月卡全免, 2钱包成功扣款, 3已补缴出场）',
    `active_vehicle_id` INT UNSIGNED GENERATED ALWAYS AS (IF(`exit_time` IS NULL, `vehicle_id`, NULL)) VIRTUAL,
    `active_spot_id` INT UNSIGNED GENERATED ALWAYS AS (IF(`exit_time` IS NULL, `spot_id`, NULL)) VIRTUAL,
    PRIMARY KEY (`record_id`),
    UNIQUE KEY `uk_active_parking_vehicle` (`active_vehicle_id`),
    UNIQUE KEY `uk_active_parking_spot` (`active_spot_id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_spot_id` (`spot_id`),
    CONSTRAINT `fk_record_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `tb_vehicle` (`vehicle_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_record_spot` FOREIGN KEY (`spot_id`) REFERENCES `tb_parking_spot` (`spot_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='停车进出场与计费记录主表';

-- --------------------------------------------------------------------
-- 表 7：新能源汽车充电工单表（tb_charging_order）
-- --------------------------------------------------------------------
CREATE TABLE `tb_charging_order` (
    `order_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '充电订单号',
    `pile_id` INT UNSIGNED NOT NULL COMMENT '使用充电桩ID，关联 tb_charging_pile.pile_id',
    `user_id` INT UNSIGNED NOT NULL COMMENT '充电车主ID，关联 tb_user.user_id',
    `plate_number` VARCHAR(10) NOT NULL COMMENT '充电车牌号',
    `start_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始插枪启动充电时刻',
    `end_time` DATETIME DEFAULT NULL COMMENT '充电结束/拔枪时刻',
    `kwh_amount` DECIMAL(6,2) NOT NULL DEFAULT 0.00 COMMENT '累计充入电量（度/kWh）',
    `total_cost` DECIMAL(8,2) NOT NULL DEFAULT 0.00 COMMENT '充电实付总金额（kwh_amount * price_per_kwh）',
    `order_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态（0充电中, 1已结算完成, 2异常中断待处理）',
    `active_pile_id` INT UNSIGNED GENERATED ALWAYS AS (IF(`order_status` IN (0, 2), `pile_id`, NULL)) VIRTUAL,
    `active_user_id` INT UNSIGNED GENERATED ALWAYS AS (IF(`order_status` IN (0, 2), `user_id`, NULL)) VIRTUAL,
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_active_charging_pile` (`active_pile_id`),
    UNIQUE KEY `uk_active_charging_user` (`active_user_id`),
    KEY `idx_pile_id` (`pile_id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_charging_pile` FOREIGN KEY (`pile_id`) REFERENCES `tb_charging_pile` (`pile_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_charging_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='新能源汽车充电工单表';

-- --------------------------------------------------------------------
-- 表 8：充电桩故障与告警工单表（tb_pile_fault_log）
-- --------------------------------------------------------------------
CREATE TABLE `tb_pile_fault_log` (
    `fault_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '故障报警流水号',
    `pile_id` INT UNSIGNED NOT NULL COMMENT '故障设备ID，关联 tb_charging_pile.pile_id',
    `error_code` VARCHAR(20) NOT NULL DEFAULT 'E01' COMMENT '故障代码（E01过温过载, E02短路接地, E03通讯中断, E04枪头锁死）',
    `fault_desc` VARCHAR(200) NOT NULL COMMENT '故障现象描述',
    `reported_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '故障触发上报时刻',
    `technician_id` INT UNSIGNED DEFAULT NULL COMMENT '接单检修技师ID，关联 tb_user.user_id',
    `resolve_note` VARCHAR(200) DEFAULT NULL COMMENT '技师检修排除记录',
    `fault_status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态（0待接单, 1排查维修中, 2已修复恢复上线）',
    PRIMARY KEY (`fault_id`),
    KEY `idx_fault_pile` (`pile_id`),
    KEY `idx_fault_tech` (`technician_id`),
    CONSTRAINT `fk_fault_pile` FOREIGN KEY (`pile_id`) REFERENCES `tb_charging_pile` (`pile_id`) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_fault_tech` FOREIGN KEY (`technician_id`) REFERENCES `tb_user` (`user_id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='充电桩故障与告警工单表';

-- --------------------------------------------------------------------
-- 表 9：车主虚拟钱包账户主表（tb_account）
-- --------------------------------------------------------------------
CREATE TABLE `tb_account` (
    `account_id` INT UNSIGNED NOT NULL COMMENT '账户ID，取值与 user_id 严格对应',
    `balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '可用资金余额（元）',
    `frozen_money` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '预约车位/正在充电锁定押金（元）',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '资金变动入账时刻',
    PRIMARY KEY (`account_id`),
    CONSTRAINT `fk_account_user` FOREIGN KEY (`account_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车主虚拟钱包账户主表';

-- --------------------------------------------------------------------
-- 表 10：资金变动只读审计流水表（tb_account_flow）
-- --------------------------------------------------------------------
CREATE TABLE `tb_account_flow` (
    `flow_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '流水号',
    `account_id` INT UNSIGNED NOT NULL COMMENT '关联钱包账户ID',
    `before_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '变动前原余额',
    `after_balance` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '变动后最新余额',
    `diff_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '本次变动差额（正为充值，负为扣款）',
    `biz_type` VARCHAR(30) NOT NULL COMMENT '业务类型（停车扣费/充电扣款/账户充值/违规滞纳金）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '流水产生时刻',
    PRIMARY KEY (`flow_id`),
    KEY `idx_account_id` (`account_id`),
    CONSTRAINT `fk_flow_account` FOREIGN KEY (`account_id`) REFERENCES `tb_account` (`account_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资金变动只读审计流水表';

-- --------------------------------------------------------------------
-- 表 11：车主停车月卡办理与续费历史台账表（tb_monthly_card，完善任务书 1:n 办理月卡设计）
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_monthly_card` (
    `card_id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '月卡流水主键',
    `user_id` INT UNSIGNED NOT NULL COMMENT '车主用户ID，关联 tb_user.user_id',
    `card_type` VARCHAR(20) NOT NULL DEFAULT 'STANDARD' COMMENT '月卡卡种（STANDARD普通全通卡, VIP专享卡）',
    `price` DECIMAL(10,2) NOT NULL DEFAULT 100.00 COMMENT '实付办卡/续费金额',
    `start_time` DATETIME NOT NULL COMMENT '本期生效起始时刻',
    `end_time` DATETIME NOT NULL COMMENT '本期有效截止时刻',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '办理/续费交易时刻',
    PRIMARY KEY (`card_id`),
    KEY `idx_card_user` (`user_id`),
    CONSTRAINT `fk_card_user` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='车主停车月卡办理与续费历史台账表';

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- 初始化种子测试数据（完美覆盖 TC-01 ~ TC-06 考核用例）
-- ====================================================================

-- 1. 用户数据 (包含车主、访客、技师、管理员)
INSERT INTO `tb_user` (`user_id`, `username`, `phone`, `user_type`, `created_at`, `is_deleted`) VALUES
(1, '张先生', '13800000001', 1, '2026-10-01 08:00:00', 0),
(2, '李女士', '13800000002', 1, '2026-10-05 09:30:00', 0),
(3, '王强',   '13800000003', 2, '2026-10-01 08:00:00', 0),
(4, '刘建国', '13800000004', 3, '2026-10-01 08:00:00', 0),
(5, '陈志强', '13800000005', 1, '2026-10-10 11:20:00', 0);

-- 2. 钱包账户初始化
INSERT INTO `tb_account` (`account_id`, `balance`, `frozen_money`, `updated_at`) VALUES
(1, 500.00, 0.00, '2026-10-01 08:05:00'),
(2, 50.00,  0.00, '2026-10-05 09:35:00'),
(3, 0.00,   0.00, '2026-10-01 08:05:00'),
(4, 0.00,   0.00, '2026-10-01 08:05:00'),
(5, 300.00, 0.00, '2026-10-10 11:25:00');

-- 3. 初始充值审计流水
INSERT INTO `tb_account_flow` (`account_id`, `before_balance`, `after_balance`, `diff_amount`, `biz_type`, `created_at`) VALUES
(1, 0.00, 500.00, 500.00, '账户充值', '2026-10-01 08:05:00'),
(2, 0.00, 50.00,  50.00,  '账户充值', '2026-10-05 09:35:00'),
(5, 0.00, 300.00, 300.00, '账户充值', '2026-10-10 11:25:00');

-- 4. 车辆数据 (张先生名下多车绑定)
INSERT INTO `tb_vehicle` (`vehicle_id`, `user_id`, `plate_number`, `vehicle_type`, `brand`, `is_deleted`) VALUES
(1, 1, '川A88888', 1, '奥迪 A6L', 0),
(2, 2, '川A66666', 1, '大众迈腾', 0),
(3, 5, '川AD12345', 2, '比亚迪 汉EV', 0),
(4, 1, '川AF99999', 3, '特斯拉 Model Y', 0);

-- 5. 停车场区域划分
INSERT INTO `tb_parking_lot` (`lot_id`, `lot_name`, `total_capacity`, `hourly_rate`) VALUES
(1, 'A区地下负一层', 100, 4.00),
(2, 'B区南门地面停车场', 80, 5.00);

-- 6. 车位基础数据
INSERT INTO `tb_parking_spot` (`spot_id`, `lot_id`, `spot_number`, `spot_type`, `spot_status`, `is_deleted`) VALUES
-- A区地下车位
(1, 1, 'A-0101', 1, 2, 0), -- 状态2(已泊入): 供 TC-02 离场测试 (停泊川A88888已达90分钟)
(2, 1, 'A-0102', 1, 2, 0), -- 状态2(已泊入): 供 TC-03 余额不足回滚测试 (停泊川A66666总费80元，李女士余额50元)
(3, 1, 'A-0103', 1, 0, 0), -- 状态0(空闲)
(4, 1, 'A-0104', 1, 0, 0), -- 状态0(空闲)
(5, 1, 'A-0105', 3, 0, 0), -- 状态0(空闲 VIP位)
-- B区地面车位（带充电桩）
(6, 2, 'B-0201', 2, 0, 0), -- 状态0(空闲 充电位): 供 TC-04 充电结算测试 (快充 120kW, 1.50元/度)
(7, 2, 'B-0202', 2, 3, 0), -- 状态3(维护禁用): 供 TC-05 充电桩报警消警测试 (慢充 E01故障)
(8, 2, 'B-0203', 2, 0, 0), -- 状态0(空闲 充电位)
(9, 2, 'B-0204', 1, 0, 0); -- 状态0(空闲 普通位)

-- 7. 充电桩设备数据
INSERT INTO `tb_charging_pile` (`pile_id`, `spot_id`, `pile_code`, `power_kw`, `price_per_kwh`, `pile_status`) VALUES
(1, 6, 'PILE-B0201-FAST', 120.0, 1.50, 1), -- 1空闲就绪 (TC-04 使用)
(2, 7, 'PILE-B0202-SLOW', 7.0,   1.20, 3), -- 3故障报警 (TC-05 技师排障使用)
(3, 8, 'PILE-B0203-SLOW', 7.0,   1.20, 1); -- 1空闲就绪

-- 8. 停车记录主表（初始化在场车辆）
INSERT INTO `tb_parking_record` (`record_id`, `vehicle_id`, `spot_id`, `entry_time`, `exit_time`, `park_minutes`, `total_fee`, `pay_status`) VALUES
-- 记录 1：川A88888 停泊 90 分钟（前30分钟免费，超过按1小时算，标准费率4.00元，应收4.00元）
(1, 1, 1, DATE_SUB(NOW(), INTERVAL 90 MINUTE), NULL, 0, 0.00, 0),
-- 记录 2：川A66666 停泊 1230 分钟 (20.5小时，前30分钟免费，计20小时*4元=80元，李女士余额仅50元)
(2, 2, 2, DATE_SUB(NOW(), INTERVAL 1230 MINUTE), NULL, 0, 0.00, 0);

-- 9. 充电桩故障工单（供 TC-05 测试技师接单消警）
INSERT INTO `tb_pile_fault_log` (`fault_id`, `pile_id`, `error_code`, `fault_desc`, `reported_at`, `technician_id`, `resolve_note`, `fault_status`) VALUES
(1, 2, 'E01', 'B-0202慢充桩主板温控传感器触发过温熔断告警，设备自动停机保护', DATE_SUB(NOW(), INTERVAL 35 MINUTE), NULL, NULL, 0);
