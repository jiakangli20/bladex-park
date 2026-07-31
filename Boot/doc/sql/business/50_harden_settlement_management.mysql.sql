-- 第 3 批入驻管理加固：背景调查园区归属、企业并发去重、查询索引和保存权限
-- 说明：脚本可重复执行；执行前应先确认现有有效企业名称不存在重复数据。

SET NAMES utf8mb4;

SET @has_background_park_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_background_investigation' AND column_name = 'park_id'
);
SET @add_background_park_id_sql := IF(
  @has_background_park_id = 0,
  'ALTER TABLE `biz_background_investigation` ADD COLUMN `park_id` bigint DEFAULT NULL COMMENT ''园区ID'' AFTER `customer_id`',
  'SELECT 1'
);
PREPARE add_background_park_id_stmt FROM @add_background_park_id_sql;
EXECUTE add_background_park_id_stmt;
DEALLOCATE PREPARE add_background_park_id_stmt;

UPDATE `biz_background_investigation` bi
LEFT JOIN `biz_business_opportunity` o ON o.`opportunity_id` = bi.`opportunity_id`
LEFT JOIN `biz_customer` c ON c.`customer_id` = bi.`customer_id`
SET bi.`park_id` = COALESCE(o.`park_id`, c.`park_id`)
WHERE bi.`park_id` IS NULL;

-- 唯一索引和园区索引创建前先阻断脏数据，避免脚本执行到一半才因 DDL 失败。
DROP PROCEDURE IF EXISTS `assert_settlement_hardening_ready`;
DELIMITER $$
CREATE PROCEDURE `assert_settlement_hardening_ready`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM `biz_business_opportunity`
    WHERE `del_flag` = '0'
    GROUP BY TRIM(`enterprise_name`)
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '商机存在重复企业名称，请先清洗后再执行 50 号脚本';
  END IF;

  IF EXISTS (
    SELECT 1
    FROM `biz_customer`
    WHERE `del_flag` = '0'
    GROUP BY `park_id`, TRIM(`enterprise_name`)
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '同一园区存在重复客户企业名称，请先清洗后再执行 50 号脚本';
  END IF;

  IF EXISTS (
    SELECT 1 FROM `biz_background_investigation` WHERE `park_id` IS NULL
  ) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '背景调查存在无法归属园区的数据，请先补齐后再执行 50 号脚本';
  END IF;
END$$
DELIMITER ;
CALL `assert_settlement_hardening_ready`();
DROP PROCEDURE `assert_settlement_hardening_ready`;

SET @has_background_park_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_background_investigation' AND index_name = 'idx_background_park_enterprise'
);
SET @add_background_park_index_sql := IF(
  @has_background_park_index = 0,
  'ALTER TABLE `biz_background_investigation` ADD INDEX `idx_background_park_enterprise` (`park_id`, `enterprise_name`, `create_time`)',
  'SELECT 1'
);
PREPARE add_background_park_index_stmt FROM @add_background_park_index_sql;
EXECUTE add_background_park_index_stmt;
DEALLOCATE PREPARE add_background_park_index_stmt;

SET @has_opportunity_active_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_business_opportunity' AND column_name = 'active_enterprise_name'
);
SET @add_opportunity_active_name_sql := IF(
  @has_opportunity_active_name = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `active_enterprise_name` varchar(200) GENERATED ALWAYS AS (CASE WHEN `del_flag` = ''0'' THEN TRIM(`enterprise_name`) ELSE NULL END) STORED',
  'SELECT 1'
);
PREPARE add_opportunity_active_name_stmt FROM @add_opportunity_active_name_sql;
EXECUTE add_opportunity_active_name_stmt;
DEALLOCATE PREPARE add_opportunity_active_name_stmt;

SET @has_opportunity_active_name_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_business_opportunity' AND index_name = 'uk_opportunity_active_enterprise'
);
SET @add_opportunity_active_name_uk_sql := IF(
  @has_opportunity_active_name_uk = 0,
  'ALTER TABLE `biz_business_opportunity` ADD UNIQUE INDEX `uk_opportunity_active_enterprise` (`active_enterprise_name`)',
  'SELECT 1'
);
PREPARE add_opportunity_active_name_uk_stmt FROM @add_opportunity_active_name_uk_sql;
EXECUTE add_opportunity_active_name_uk_stmt;
DEALLOCATE PREPARE add_opportunity_active_name_uk_stmt;

SET @has_customer_active_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'biz_customer' AND column_name = 'active_enterprise_name'
);
SET @add_customer_active_name_sql := IF(
  @has_customer_active_name = 0,
  'ALTER TABLE `biz_customer` ADD COLUMN `active_enterprise_name` varchar(200) GENERATED ALWAYS AS (CASE WHEN `del_flag` = ''0'' THEN TRIM(`enterprise_name`) ELSE NULL END) STORED',
  'SELECT 1'
);
PREPARE add_customer_active_name_stmt FROM @add_customer_active_name_sql;
EXECUTE add_customer_active_name_stmt;
DEALLOCATE PREPARE add_customer_active_name_stmt;

SET @has_customer_active_name_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE() AND table_name = 'biz_customer' AND index_name = 'uk_customer_park_active_enterprise'
);
SET @add_customer_active_name_uk_sql := IF(
  @has_customer_active_name_uk = 0,
  'ALTER TABLE `biz_customer` ADD UNIQUE INDEX `uk_customer_park_active_enterprise` (`park_id`, `active_enterprise_name`)',
  'SELECT 1'
);
PREPARE add_customer_active_name_uk_stmt FROM @add_customer_active_name_uk_sql;
EXECUTE add_customer_active_name_uk_stmt;
DEALLOCATE PREPARE add_customer_active_name_uk_stmt;

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000003000401, 1890000000003000400, 'settlement_background_investigation_save', '保存核验结果', 'save', '/api/blade-park/background-investigation/save', 'check', '', 1, 2, 2, 1, '背景调查人工核验保存权限', 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
  `name` = VALUES(`name`),
  `path` = VALUES(`path`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT grants.`id`, grants.`role_id`, grants.`menu_id`
FROM (
  SELECT 2072000000000000061 AS `id`, 1123598816738675201 AS `role_id`, 1890000000003000401 AS `menu_id`
  UNION ALL SELECT 2072000000000000062, 1123598816738675203, 1890000000003000401
  UNION ALL SELECT 2072000000000000063, 1123598816738675204, 1890000000003000401
  UNION ALL SELECT 2072000000000000064, 1123598816738675205, 1890000000003000401
) grants
INNER JOIN `blade_role_menu` parent_grant
  ON parent_grant.`role_id` = grants.`role_id`
 AND parent_grant.`menu_id` = 1890000000003000400
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_role_menu` existing_grant
  WHERE existing_grant.`role_id` = grants.`role_id` AND existing_grant.`menu_id` = grants.`menu_id`
)
AND NOT EXISTS (
  SELECT 1 FROM `blade_role_menu` existing_id WHERE existing_id.`id` = grants.`id`
);

SELECT `park_id`, `enterprise_name`, COUNT(*) AS `duplicate_count`
FROM `biz_customer`
WHERE `del_flag` = '0'
GROUP BY `park_id`, TRIM(`enterprise_name`)
HAVING COUNT(*) > 1;

SELECT `enterprise_name`, COUNT(*) AS `duplicate_count`
FROM `biz_business_opportunity`
WHERE `del_flag` = '0'
GROUP BY TRIM(`enterprise_name`)
HAVING COUNT(*) > 1;

SELECT COUNT(*) AS `cross_park_customer_tag_count`
FROM `biz_customer_tag` ct
INNER JOIN `biz_customer` c ON c.`customer_id` = ct.`customer_id` AND c.`del_flag` = '0'
INNER JOIN `biz_tag` t ON t.`tag_id` = ct.`tag_id` AND t.`del_flag` = '0'
WHERE c.`park_id` <> t.`park_id`;

SELECT COUNT(*) AS `cross_park_opportunity_tag_count`
FROM `biz_business_opportunity_tag` ot
INNER JOIN `biz_business_opportunity` o ON o.`opportunity_id` = ot.`opportunity_id` AND o.`del_flag` = '0'
INNER JOIN `biz_tag` t ON t.`tag_id` = ot.`tag_id` AND t.`del_flag` = '0'
WHERE o.`park_id` <> t.`park_id`;

SELECT COUNT(*) AS `background_missing_park_count`
FROM `biz_background_investigation`
WHERE `park_id` IS NULL;

SELECT COUNT(*) AS `background_save_authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000003000401;
