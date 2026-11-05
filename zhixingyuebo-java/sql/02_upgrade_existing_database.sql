-- 仅供旧版十表库升级一次使用。请先停用程序并保存可恢复的数据库备份。
-- 以下查询均应返回空结果。如有重复，须先人工确认有效记录再升级。
SELECT vehicle_id, COUNT(*) FROM tb_parking_record WHERE exit_time IS NULL GROUP BY vehicle_id HAVING COUNT(*) > 1;
SELECT spot_id, COUNT(*) FROM tb_parking_record WHERE exit_time IS NULL GROUP BY spot_id HAVING COUNT(*) > 1;
SELECT pile_id, COUNT(*) FROM tb_charging_order WHERE order_status IN (0, 2) GROUP BY pile_id HAVING COUNT(*) > 1;
SELECT user_id, COUNT(*) FROM tb_charging_order WHERE order_status IN (0, 2) GROUP BY user_id HAVING COUNT(*) > 1;

-- 在确认连接的是要升级的数据库后执行。DDL 逐条提交，失败后按备份恢复。
ALTER TABLE tb_parking_record
    ADD COLUMN active_vehicle_id INT UNSIGNED GENERATED ALWAYS AS (IF(exit_time IS NULL, vehicle_id, NULL)) VIRTUAL,
    ADD COLUMN active_spot_id INT UNSIGNED GENERATED ALWAYS AS (IF(exit_time IS NULL, spot_id, NULL)) VIRTUAL,
    ADD UNIQUE KEY uk_active_parking_vehicle (active_vehicle_id),
    ADD UNIQUE KEY uk_active_parking_spot (active_spot_id);
ALTER TABLE tb_charging_order
    ADD COLUMN active_pile_id INT UNSIGNED GENERATED ALWAYS AS (IF(order_status IN (0, 2), pile_id, NULL)) VIRTUAL,
    ADD COLUMN active_user_id INT UNSIGNED GENERATED ALWAYS AS (IF(order_status IN (0, 2), user_id, NULL)) VIRTUAL,
    ADD UNIQUE KEY uk_active_charging_pile (active_pile_id),
    ADD UNIQUE KEY uk_active_charging_user (active_user_id);
ALTER TABLE tb_user ADD COLUMN monthly_card_until DATETIME DEFAULT NULL;
ALTER TABLE tb_parking_spot
    ADD COLUMN reserved_by INT UNSIGNED DEFAULT NULL,
    ADD COLUMN reserved_until DATETIME DEFAULT NULL,
    ADD CONSTRAINT fk_spot_reserver FOREIGN KEY (reserved_by) REFERENCES tb_user (user_id) ON DELETE SET NULL;
