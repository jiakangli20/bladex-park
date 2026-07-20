-- 租控管理 - 内嵌智能水电表与表计业务字段
-- 说明：智能水电表保留在租控管理页签内，补齐水表/电表参数与预警配置。脚本可重复执行。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `add_smart_meter_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_smart_meter_column_if_missing`(
  IN p_column_name varchar(64),
  IN p_column_definition varchar(1000)
)
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'biz_smart_device'
      AND column_name = p_column_name
  ) THEN
    SET @column_sql = CONCAT('ALTER TABLE `biz_smart_device` ADD COLUMN `', p_column_name, '` ', p_column_definition);
    PREPARE column_stmt FROM @column_sql;
    EXECUTE column_stmt;
    DEALLOCATE PREPARE column_stmt;
  END IF;
END$$
DELIMITER ;

CALL `add_smart_meter_column_if_missing`('payment_type', 'varchar(20) NOT NULL DEFAULT ''postpaid'' COMMENT ''付费类型：prepaid预付费、postpaid后付费'' AFTER `device_model`');
CALL `add_smart_meter_column_if_missing`('meter_type', 'varchar(20) NOT NULL DEFAULT ''branch'' COMMENT ''表类型：branch分表、total总表、public公摊表'' AFTER `payment_type`');
CALL `add_smart_meter_column_if_missing`('purpose', 'varchar(100) DEFAULT NULL COMMENT ''用途'' AFTER `meter_type`');
CALL `add_smart_meter_column_if_missing`('current_reading', 'decimal(18,4) NOT NULL DEFAULT 0 COMMENT ''当前读数'' AFTER `purpose`');
CALL `add_smart_meter_column_if_missing`('current_balance', 'decimal(18,2) NOT NULL DEFAULT 0 COMMENT ''当前余额'' AFTER `current_reading`');
CALL `add_smart_meter_column_if_missing`('multiplier', 'decimal(10,4) NOT NULL DEFAULT 1 COMMENT ''倍率'' AFTER `current_balance`');
CALL `add_smart_meter_column_if_missing`('max_reading', 'decimal(18,4) DEFAULT NULL COMMENT ''最大读数'' AFTER `multiplier`');
CALL `add_smart_meter_column_if_missing`('warning_unit', 'varchar(20) DEFAULT NULL COMMENT ''预警单位：yuan/kwh/m3'' AFTER `max_reading`');
CALL `add_smart_meter_column_if_missing`('warning_rules', 'text COMMENT ''预警规则JSON'' AFTER `warning_unit`');

DROP PROCEDURE `add_smart_meter_column_if_missing`;

SET @smart_meter_index_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_smart_device'
     AND index_name = 'idx_smart_device_meter') = 0,
  'ALTER TABLE `biz_smart_device` ADD INDEX `idx_smart_device_meter` (`device_type`, `payment_type`, `meter_type`)',
  'SELECT 1'
);
PREPARE smart_meter_index_stmt FROM @smart_meter_index_sql;
EXECUTE smart_meter_index_stmt;
DEALLOCATE PREPARE smart_meter_index_stmt;

DELETE FROM `blade_top_menu_setting`
WHERE `menu_id` = 1963598815738675515;

DELETE FROM `blade_role_menu`
WHERE `menu_id` = 1963598815738675515;

DELETE FROM `blade_menu`
WHERE `id` = 1963598815738675515 OR `code` = 'smart_device';

UPDATE `blade_menu`
SET `sort` = CASE `code`
  WHEN 'rent_control' THEN 1
  WHEN 'park' THEN 2
  WHEN 'building' THEN 3
  WHEN 'floor' THEN 4
  ELSE `sort`
END
WHERE `parent_id` = 1963598815738675401
  AND `code` IN ('rent_control', 'park', 'building', 'floor');

UPDATE `blade_menu`
SET `parent_id` = 1963598815738675501,
    `sort` = CASE `code`
      WHEN 'rent_control_device_list' THEN 11
      WHEN 'rent_control_device_add' THEN 12
      WHEN 'rent_control_device_edit' THEN 13
      WHEN 'rent_control_device_delete' THEN 14
      ELSE `sort`
    END
WHERE `code` IN (
  'rent_control_device_list',
  'rent_control_device_add',
  'rent_control_device_edit',
  'rent_control_device_delete'
);

SELECT COUNT(*) AS `smart_meter_column_count`
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_smart_device'
  AND column_name IN (
    'payment_type', 'meter_type', 'purpose', 'current_reading', 'current_balance',
    'multiplier', 'max_reading', 'warning_unit', 'warning_rules'
  );

SELECT COUNT(*) AS `standalone_smart_meter_menu_count`
FROM `blade_menu`
WHERE `code` = 'smart_device';

SELECT `parent_id`, `code`, `sort`
FROM `blade_menu`
WHERE `code` LIKE 'rent_control_device_%'
ORDER BY `sort`;

SELECT COUNT(*) AS `rent_control_device_button_count`
FROM `blade_menu`
WHERE `parent_id` = 1963598815738675501
  AND `code` LIKE 'rent_control_device_%';
