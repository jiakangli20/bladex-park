-- 第 3 批入驻管理加固：背景调查园区归属、企业并发去重、查询索引和保存权限
--
-- 生产执行规则：
-- 1. 必须先备份目标数据库。
-- 2. 本脚本先完成全部结构、数据和固定 ID 预检；任何预检失败时，不修改业务表数据和结构。
-- 3. 历史 customer_id=0/-1、失效客户关联等数据不会被脚本静默修改，需核对后单独清洗再执行。
-- 4. park_id=0 的标签为全局标签，属于合法数据，不按跨园区异常处理。
-- 5. 脚本可重复执行；执行结束必须看到 postcheck_status=PASS。

SET NAMES utf8mb4;

-- 第一阶段：只校验必需对象、固定菜单/授权 ID 和既有结构，不修改业务对象。
-- 使用临时表 CHECK 作为硬阻断，失败会话结束后不会在数据库残留校验过程。
SELECT COUNT(*) INTO @required_table_count
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'biz_business_opportunity', 'biz_customer', 'biz_background_investigation',
    'biz_tag', 'biz_customer_tag', 'biz_business_opportunity_tag',
    'blade_menu', 'blade_role', 'blade_role_menu'
  );

SELECT COUNT(*) INTO @parent_menu_count
FROM `blade_menu`
WHERE `id` = 1890000000003000400
  AND `code` = 'settlement_background_investigation'
  AND `is_deleted` = 0;

SELECT COUNT(*) INTO @target_role_count
FROM `blade_role`
WHERE `id` IN (1123598816738675201, 1123598816738675203, 1123598816738675204, 1123598816738675205)
  AND `is_deleted` = 0;

SELECT COUNT(*) INTO @parent_grant_count
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000003000400
  AND `role_id` IN (1123598816738675201, 1123598816738675203, 1123598816738675204, 1123598816738675205);

SELECT COUNT(*) INTO @menu_id_collision_count
FROM `blade_menu`
WHERE (`id` = 1890000000003000401 AND `code` <> 'settlement_background_investigation_save')
   OR (`code` = 'settlement_background_investigation_save' AND `id` <> 1890000000003000401);

SELECT COUNT(*) INTO @grant_id_collision_count
FROM `blade_role_menu`
WHERE (`id` = 2072000000000000061 AND (`role_id` <> 1123598816738675201 OR `menu_id` <> 1890000000003000401))
   OR (`id` = 2072000000000000062 AND (`role_id` <> 1123598816738675203 OR `menu_id` <> 1890000000003000401))
   OR (`id` = 2072000000000000063 AND (`role_id` <> 1123598816738675204 OR `menu_id` <> 1890000000003000401))
   OR (`id` = 2072000000000000064 AND (`role_id` <> 1123598816738675205 OR `menu_id` <> 1890000000003000401));

SELECT COUNT(*) INTO @incompatible_background_park_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_background_investigation'
  AND column_name = 'park_id'
  AND data_type <> 'bigint';

SELECT COUNT(*) INTO @incompatible_generated_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN ('biz_business_opportunity', 'biz_customer')
  AND column_name = 'active_enterprise_name'
  AND (column_type <> 'varchar(200)' OR extra NOT LIKE '%STORED GENERATED%');

SELECT COUNT(*) INTO @incompatible_index_count
FROM (
  SELECT table_name, index_name, non_unique,
         GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_used
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND index_name IN (
      'idx_background_park_enterprise',
      'uk_opportunity_active_enterprise',
      'uk_customer_park_active_enterprise'
    )
  GROUP BY table_name, index_name, non_unique
) idx
WHERE (idx.index_name = 'idx_background_park_enterprise'
       AND (idx.table_name <> 'biz_background_investigation' OR idx.non_unique <> 1 OR idx.columns_used <> 'park_id,enterprise_name,create_time'))
   OR (idx.index_name = 'uk_opportunity_active_enterprise'
       AND (idx.table_name <> 'biz_business_opportunity' OR idx.non_unique <> 0 OR idx.columns_used <> 'active_enterprise_name'))
   OR (idx.index_name = 'uk_customer_park_active_enterprise'
       AND (idx.table_name <> 'biz_customer' OR idx.non_unique <> 0 OR idx.columns_used <> 'park_id,active_enterprise_name'));

SELECT
  @required_table_count AS `required_table_count_expected_9`,
  @parent_menu_count AS `parent_menu_count_expected_1`,
  @target_role_count AS `target_role_count_expected_4`,
  @parent_grant_count AS `parent_grant_count_expected_4`,
  @menu_id_collision_count AS `menu_id_collision_count_expected_0`,
  @grant_id_collision_count AS `grant_id_collision_count_expected_0`,
  @incompatible_background_park_count AS `incompatible_background_park_count_expected_0`,
  @incompatible_generated_column_count AS `incompatible_generated_column_count_expected_0`,
  @incompatible_index_count AS `incompatible_index_count_expected_0`;

DROP TEMPORARY TABLE IF EXISTS `migration_50_prerequisite_guard`;
CREATE TEMPORARY TABLE `migration_50_prerequisite_guard` (
  `failed_count` int NOT NULL,
  CONSTRAINT `chk_migration_50_prerequisite` CHECK (`failed_count` = 0)
);
INSERT INTO `migration_50_prerequisite_guard` (`failed_count`) VALUES (
  (@required_table_count <> 9)
  + (@parent_menu_count <> 1)
  + (@target_role_count <> 4)
  + (@parent_grant_count <> 4)
  + @menu_id_collision_count
  + @grant_id_collision_count
  + @incompatible_background_park_count
  + @incompatible_generated_column_count
  + @incompatible_index_count
);
DROP TEMPORARY TABLE `migration_50_prerequisite_guard`;

-- 背景调查 park_id 可能尚未创建，使用动态 SQL 计算可否从现有关系解析园区。
SET @has_background_park_id := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_background_investigation'
    AND column_name = 'park_id'
);
SET @background_unresolved_sql := IF(
  @has_background_park_id = 1,
  'SELECT COUNT(*) INTO @background_unresolved_count FROM `biz_background_investigation` bi LEFT JOIN `biz_business_opportunity` o ON o.`opportunity_id` = bi.`opportunity_id` AND o.`del_flag` = ''0'' LEFT JOIN `biz_customer` c ON c.`customer_id` = bi.`customer_id` AND c.`del_flag` = ''0'' WHERE COALESCE(bi.`park_id`, o.`park_id`, c.`park_id`) IS NULL',
  'SELECT COUNT(*) INTO @background_unresolved_count FROM `biz_background_investigation` bi LEFT JOIN `biz_business_opportunity` o ON o.`opportunity_id` = bi.`opportunity_id` AND o.`del_flag` = ''0'' LEFT JOIN `biz_customer` c ON c.`customer_id` = bi.`customer_id` AND c.`del_flag` = ''0'' WHERE COALESCE(o.`park_id`, c.`park_id`) IS NULL'
);
PREPARE background_unresolved_stmt FROM @background_unresolved_sql;
EXECUTE background_unresolved_stmt;
DEALLOCATE PREPARE background_unresolved_stmt;

SET @invalid_background_relation_sql := IF(
  @has_background_park_id = 1,
  'SELECT COUNT(*) INTO @invalid_background_relation_count FROM `biz_background_investigation` bi LEFT JOIN `biz_business_opportunity` o ON o.`opportunity_id` = bi.`opportunity_id` AND o.`del_flag` = ''0'' LEFT JOIN `biz_customer` c ON c.`customer_id` = bi.`customer_id` AND c.`del_flag` = ''0'' WHERE (bi.`opportunity_id` IS NOT NULL AND (o.`opportunity_id` IS NULL OR (bi.`park_id` IS NOT NULL AND o.`park_id` <> bi.`park_id`))) OR (bi.`customer_id` IS NOT NULL AND (bi.`customer_id` <= 0 OR c.`customer_id` IS NULL OR (bi.`park_id` IS NOT NULL AND c.`park_id` <> bi.`park_id`) OR (o.`opportunity_id` IS NOT NULL AND c.`park_id` <> o.`park_id`)))',
  'SELECT COUNT(*) INTO @invalid_background_relation_count FROM `biz_background_investigation` bi LEFT JOIN `biz_business_opportunity` o ON o.`opportunity_id` = bi.`opportunity_id` AND o.`del_flag` = ''0'' LEFT JOIN `biz_customer` c ON c.`customer_id` = bi.`customer_id` AND c.`del_flag` = ''0'' WHERE (bi.`opportunity_id` IS NOT NULL AND o.`opportunity_id` IS NULL) OR (bi.`customer_id` IS NOT NULL AND (bi.`customer_id` <= 0 OR c.`customer_id` IS NULL OR (o.`opportunity_id` IS NOT NULL AND c.`park_id` <> o.`park_id`)))'
);
PREPARE invalid_background_relation_stmt FROM @invalid_background_relation_sql;
EXECUTE invalid_background_relation_stmt;
DEALLOCATE PREPARE invalid_background_relation_stmt;

SELECT COUNT(*) INTO @invalid_opportunity_customer_count
FROM `biz_business_opportunity` o
LEFT JOIN `biz_customer` c
  ON c.`customer_id` = o.`customer_id`
 AND c.`del_flag` = '0'
WHERE o.`customer_id` IS NOT NULL
  AND (o.`customer_id` <= 0 OR c.`customer_id` IS NULL OR c.`park_id` <> o.`park_id`);

SELECT COUNT(*) INTO @blank_opportunity_name_count
FROM `biz_business_opportunity`
WHERE `del_flag` = '0' AND (`enterprise_name` IS NULL OR TRIM(`enterprise_name`) = '');

SELECT COUNT(*) INTO @blank_customer_name_count
FROM `biz_customer`
WHERE `del_flag` = '0' AND (`enterprise_name` IS NULL OR TRIM(`enterprise_name`) = '');

SELECT COUNT(*) INTO @duplicate_opportunity_name_count
FROM (
  SELECT TRIM(`enterprise_name`)
  FROM `biz_business_opportunity`
  WHERE `del_flag` = '0'
  GROUP BY TRIM(`enterprise_name`)
  HAVING COUNT(*) > 1
) duplicate_opportunity;

SELECT COUNT(*) INTO @duplicate_customer_name_count
FROM (
  SELECT `park_id`, TRIM(`enterprise_name`)
  FROM `biz_customer`
  WHERE `del_flag` = '0'
  GROUP BY `park_id`, TRIM(`enterprise_name`)
  HAVING COUNT(*) > 1
) duplicate_customer;

SELECT COUNT(*) INTO @invalid_customer_tag_count
FROM `biz_customer_tag` ct
INNER JOIN `biz_customer` c ON c.`customer_id` = ct.`customer_id` AND c.`del_flag` = '0'
INNER JOIN `biz_tag` t ON t.`tag_id` = ct.`tag_id` AND t.`del_flag` = '0'
WHERE t.`park_id` <> 0 AND c.`park_id` <> t.`park_id`;

SELECT COUNT(*) INTO @invalid_opportunity_tag_count
FROM `biz_business_opportunity_tag` ot
INNER JOIN `biz_business_opportunity` o ON o.`opportunity_id` = ot.`opportunity_id` AND o.`del_flag` = '0'
INNER JOIN `biz_tag` t ON t.`tag_id` = ot.`tag_id` AND t.`del_flag` = '0'
WHERE t.`park_id` <> 0 AND o.`park_id` <> t.`park_id`;

SELECT
  @invalid_opportunity_customer_count AS `invalid_opportunity_customer_count_expected_0`,
  @blank_opportunity_name_count AS `blank_opportunity_name_count_expected_0`,
  @blank_customer_name_count AS `blank_customer_name_count_expected_0`,
  @duplicate_opportunity_name_count AS `duplicate_opportunity_name_count_expected_0`,
  @duplicate_customer_name_count AS `duplicate_customer_name_count_expected_0`,
  @background_unresolved_count AS `background_unresolved_count_expected_0`,
  @invalid_background_relation_count AS `invalid_background_relation_count_expected_0`,
  @invalid_customer_tag_count AS `invalid_customer_tag_count_expected_0`,
  @invalid_opportunity_tag_count AS `invalid_opportunity_tag_count_expected_0`;

DROP TEMPORARY TABLE IF EXISTS `migration_50_data_guard`;
CREATE TEMPORARY TABLE `migration_50_data_guard` (
  `failed_count` int NOT NULL,
  CONSTRAINT `chk_migration_50_data` CHECK (`failed_count` = 0)
);
INSERT INTO `migration_50_data_guard` (`failed_count`) VALUES (
  @invalid_opportunity_customer_count
  + @blank_opportunity_name_count
  + @blank_customer_name_count
  + @duplicate_opportunity_name_count
  + @duplicate_customer_name_count
  + @background_unresolved_count
  + @invalid_background_relation_count
  + @invalid_customer_tag_count
  + @invalid_opportunity_tag_count
);
DROP TEMPORARY TABLE `migration_50_data_guard`;

-- 第二阶段：全部预检通过后，才开始业务结构和数据迁移。
SET @add_background_park_id_sql := IF(
  @has_background_park_id = 0,
  'ALTER TABLE `biz_background_investigation` ADD COLUMN `park_id` bigint DEFAULT NULL COMMENT ''园区ID'' AFTER `customer_id`',
  'DO 1'
);
PREPARE add_background_park_id_stmt FROM @add_background_park_id_sql;
EXECUTE add_background_park_id_stmt;
DEALLOCATE PREPARE add_background_park_id_stmt;

UPDATE `biz_background_investigation` bi
LEFT JOIN `biz_business_opportunity` o
  ON o.`opportunity_id` = bi.`opportunity_id` AND o.`del_flag` = '0'
LEFT JOIN `biz_customer` c
  ON c.`customer_id` = bi.`customer_id` AND c.`del_flag` = '0'
SET bi.`park_id` = COALESCE(bi.`park_id`, o.`park_id`, c.`park_id`)
WHERE bi.`park_id` IS NULL;

SET @has_background_park_index := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_background_investigation'
    AND index_name = 'idx_background_park_enterprise'
);
SET @add_background_park_index_sql := IF(
  @has_background_park_index = 0,
  'ALTER TABLE `biz_background_investigation` ADD INDEX `idx_background_park_enterprise` (`park_id`, `enterprise_name`, `create_time`)',
  'DO 1'
);
PREPARE add_background_park_index_stmt FROM @add_background_park_index_sql;
EXECUTE add_background_park_index_stmt;
DEALLOCATE PREPARE add_background_park_index_stmt;

SET @has_opportunity_active_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_business_opportunity'
    AND column_name = 'active_enterprise_name'
);
SET @add_opportunity_active_name_sql := IF(
  @has_opportunity_active_name = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `active_enterprise_name` varchar(200) GENERATED ALWAYS AS (CASE WHEN `del_flag` = ''0'' THEN TRIM(`enterprise_name`) ELSE NULL END) STORED',
  'DO 1'
);
PREPARE add_opportunity_active_name_stmt FROM @add_opportunity_active_name_sql;
EXECUTE add_opportunity_active_name_stmt;
DEALLOCATE PREPARE add_opportunity_active_name_stmt;

SET @has_opportunity_active_name_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_business_opportunity'
    AND index_name = 'uk_opportunity_active_enterprise'
);
SET @add_opportunity_active_name_uk_sql := IF(
  @has_opportunity_active_name_uk = 0,
  'ALTER TABLE `biz_business_opportunity` ADD UNIQUE INDEX `uk_opportunity_active_enterprise` (`active_enterprise_name`)',
  'DO 1'
);
PREPARE add_opportunity_active_name_uk_stmt FROM @add_opportunity_active_name_uk_sql;
EXECUTE add_opportunity_active_name_uk_stmt;
DEALLOCATE PREPARE add_opportunity_active_name_uk_stmt;

SET @has_customer_active_name := (
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_customer'
    AND column_name = 'active_enterprise_name'
);
SET @add_customer_active_name_sql := IF(
  @has_customer_active_name = 0,
  'ALTER TABLE `biz_customer` ADD COLUMN `active_enterprise_name` varchar(200) GENERATED ALWAYS AS (CASE WHEN `del_flag` = ''0'' THEN TRIM(`enterprise_name`) ELSE NULL END) STORED',
  'DO 1'
);
PREPARE add_customer_active_name_stmt FROM @add_customer_active_name_sql;
EXECUTE add_customer_active_name_stmt;
DEALLOCATE PREPARE add_customer_active_name_stmt;

SET @has_customer_active_name_uk := (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'biz_customer'
    AND index_name = 'uk_customer_park_active_enterprise'
);
SET @add_customer_active_name_uk_sql := IF(
  @has_customer_active_name_uk = 0,
  'ALTER TABLE `biz_customer` ADD UNIQUE INDEX `uk_customer_park_active_enterprise` (`park_id`, `active_enterprise_name`)',
  'DO 1'
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
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_role_menu` existing_grant
  WHERE existing_grant.`role_id` = grants.`role_id`
    AND existing_grant.`menu_id` = grants.`menu_id`
);

-- 第三阶段：硬性后置校验。任何缺项都以 SQL 异常结束，不能把“查询到异常”误判为成功。
SELECT COUNT(*) INTO @post_background_park_column_count
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_background_investigation'
  AND column_name = 'park_id';

SELECT COUNT(*) INTO @post_background_missing_park_count
FROM `biz_background_investigation`
WHERE `park_id` IS NULL;

SELECT COUNT(*) INTO @post_target_index_column_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND index_name IN ('idx_background_park_enterprise', 'uk_opportunity_active_enterprise', 'uk_customer_park_active_enterprise');

SELECT COUNT(*) INTO @post_permission_menu_count
FROM `blade_menu`
WHERE `id` = 1890000000003000401
  AND `code` = 'settlement_background_investigation_save'
  AND `is_deleted` = 0;

SELECT COUNT(*) INTO @post_permission_grant_count
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000003000401
  AND `role_id` IN (1123598816738675201, 1123598816738675203, 1123598816738675204, 1123598816738675205);

DROP TEMPORARY TABLE IF EXISTS `migration_50_postcondition_guard`;
CREATE TEMPORARY TABLE `migration_50_postcondition_guard` (
  `failed_count` int NOT NULL,
  CONSTRAINT `chk_migration_50_postcondition` CHECK (`failed_count` = 0)
);
INSERT INTO `migration_50_postcondition_guard` (`failed_count`) VALUES (
  (@post_background_park_column_count <> 1)
  + @post_background_missing_park_count
  + (@post_target_index_column_count <> 6)
  + (@post_permission_menu_count <> 1)
  + (@post_permission_grant_count <> 4)
);
DROP TEMPORARY TABLE `migration_50_postcondition_guard`;

SELECT
  'PASS' AS `postcheck_status`,
  @invalid_opportunity_customer_count AS `invalid_opportunity_customer_count`,
  @duplicate_opportunity_name_count AS `duplicate_opportunity_name_count`,
  @duplicate_customer_name_count AS `duplicate_customer_name_count`,
  @background_unresolved_count AS `background_unresolved_count`,
  @invalid_background_relation_count AS `invalid_background_relation_count`,
  @invalid_customer_tag_count AS `invalid_customer_tag_count`,
  @invalid_opportunity_tag_count AS `invalid_opportunity_tag_count`,
  (SELECT COUNT(*) FROM `blade_role_menu` WHERE `menu_id` = 1890000000003000401) AS `authorized_role_count`;
