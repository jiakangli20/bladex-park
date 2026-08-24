-- 补齐园区活动审批留痕。可重复执行。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `add_park_activity_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_park_activity_column_if_missing`(IN p_name varchar(64), IN p_definition text)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='biz_park_activity' AND COLUMN_NAME=p_name) THEN
    SET @sql_text = CONCAT('ALTER TABLE `biz_park_activity` ADD COLUMN `', p_name, '` ', p_definition);
    PREPARE stmt FROM @sql_text; EXECUTE stmt; DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;
CALL `add_park_activity_column_if_missing`('audit_user_id', 'bigint DEFAULT NULL COMMENT ''审核用户ID'' AFTER `audit_status`');
CALL `add_park_activity_column_if_missing`('audit_user_name', 'varchar(100) DEFAULT NULL COMMENT ''审核人'' AFTER `audit_user_id`');
CALL `add_park_activity_column_if_missing`('audit_time', 'datetime DEFAULT NULL COMMENT ''审核时间'' AFTER `audit_user_name`');
DROP PROCEDURE IF EXISTS `add_park_activity_column_if_missing`;

CREATE TABLE IF NOT EXISTS `biz_park_activity_audit_log` (
  `id` bigint NOT NULL, `tenant_id` varchar(12) NOT NULL DEFAULT '000000', `activity_id` bigint NOT NULL,
  `park_id` bigint NOT NULL, `customer_id` bigint DEFAULT NULL, `action_type` varchar(32) NOT NULL,
  `before_audit_status` varchar(20) DEFAULT NULL, `after_audit_status` varchar(20) DEFAULT NULL,
  `before_publish_status` int DEFAULT NULL, `after_publish_status` int DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL, `operator_name` varchar(100) DEFAULT NULL,
  `opinion` varchar(500) DEFAULT NULL, `operate_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1, `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_activity_log_activity` (`tenant_id`,`activity_id`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区活动审核发布日志';
