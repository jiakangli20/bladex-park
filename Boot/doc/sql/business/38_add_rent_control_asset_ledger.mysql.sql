-- 租控管理 - 资产登记台账
-- 说明：新增资产登记表及租控页签按钮权限。脚本可重复执行。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_asset_record` (
  `asset_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资产ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `building_id` bigint NOT NULL COMMENT '楼宇ID',
  `floor_no` int DEFAULT NULL COMMENT '楼层',
  `room_id` bigint DEFAULT NULL COMMENT '房间ID',
  `asset_code` varchar(100) NOT NULL COMMENT '资产编号',
  `asset_name` varchar(100) NOT NULL COMMENT '资产名称',
  `asset_category` varchar(32) NOT NULL COMMENT '资产分类',
  `brand_model` varchar(150) DEFAULT NULL COMMENT '品牌型号',
  `quantity` int NOT NULL DEFAULT 1 COMMENT '数量',
  `unit` varchar(20) NOT NULL DEFAULT '件' COMMENT '计量单位',
  `purchase_date` date DEFAULT NULL COMMENT '购置日期',
  `original_value` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '资产原值',
  `asset_status` varchar(20) NOT NULL DEFAULT 'in_use' COMMENT '资产状态：in_use在用、idle闲置、repair维修、scrapped报废',
  `responsible_person` varchar(100) DEFAULT NULL COMMENT '责任人',
  `location_note` varchar(200) DEFAULT NULL COMMENT '安装位置补充',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常、1删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`asset_id`),
  UNIQUE KEY `uk_asset_record_code` (`asset_code`),
  KEY `idx_asset_record_location` (`park_id`, `building_id`, `floor_no`, `room_id`),
  KEY `idx_asset_record_category_status` (`asset_category`, `asset_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产登记台账';

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1963598815738675521, 1963598815738675501, 'rent_control_asset_list', '资产台账查询', 'list', '/api/blade-park/asset-record/page', 'search', '', 15, 2, 1, 1, '资产登记台账查询和详情权限', 0),
  (1963598815738675522, 1963598815738675501, 'rent_control_asset_add', '登记资产', 'add', '/api/blade-park/asset-record/submit', 'plus', '', 16, 2, 1, 1, '新增资产登记', 0),
  (1963598815738675523, 1963598815738675501, 'rent_control_asset_edit', '编辑资产', 'edit', '/api/blade-park/asset-record/submit', 'form', '', 17, 2, 2, 1, '编辑资产登记', 0),
  (1963598815738675524, 1963598815738675501, 'rent_control_asset_delete', '删除资产', 'delete', '/api/blade-park/asset-record/remove', 'delete', '', 18, 2, 3, 1, '删除资产登记', 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
  `name` = VALUES(`name`),
  `alias` = VALUES(`alias`),
  `path` = VALUES(`path`),
  `source` = VALUES(`source`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT grants.`id`, grants.`role_id`, grants.`menu_id`
FROM (
  SELECT 2072000000000000021 AS `id`, 1123598816738675201 AS `role_id`, 1963598815738675521 AS `menu_id`
  UNION ALL SELECT 2072000000000000022, 1123598816738675201, 1963598815738675522
  UNION ALL SELECT 2072000000000000023, 1123598816738675201, 1963598815738675523
  UNION ALL SELECT 2072000000000000024, 1123598816738675201, 1963598815738675524
  UNION ALL SELECT 2072000000000000025, 1123598816738675203, 1963598815738675521
  UNION ALL SELECT 2072000000000000026, 1123598816738675203, 1963598815738675522
  UNION ALL SELECT 2072000000000000027, 1123598816738675203, 1963598815738675523
  UNION ALL SELECT 2072000000000000028, 1123598816738675203, 1963598815738675524
  UNION ALL SELECT 2072000000000000029, 1123598816738675204, 1963598815738675521
  UNION ALL SELECT 2072000000000000030, 1123598816738675204, 1963598815738675522
  UNION ALL SELECT 2072000000000000031, 1123598816738675204, 1963598815738675523
  UNION ALL SELECT 2072000000000000032, 1123598816738675204, 1963598815738675524
  UNION ALL SELECT 2072000000000000033, 1123598816738675205, 1963598815738675521
  UNION ALL SELECT 2072000000000000034, 1123598816738675205, 1963598815738675522
  UNION ALL SELECT 2072000000000000035, 1123598816738675205, 1963598815738675523
  UNION ALL SELECT 2072000000000000036, 1123598816738675205, 1963598815738675524
) grants
INNER JOIN `blade_role_menu` parent_grant
  ON parent_grant.`role_id` = grants.`role_id`
 AND parent_grant.`menu_id` = 1963598815738675501
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_role_menu` existing_grant
  WHERE existing_grant.`role_id` = grants.`role_id`
    AND existing_grant.`menu_id` = grants.`menu_id`
)
  AND NOT EXISTS (
    SELECT 1 FROM `blade_role_menu` existing_id
    WHERE existing_id.`id` = grants.`id`
  );

SELECT COUNT(*) AS `asset_record_table_count`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'biz_asset_record';

SELECT `parent_id`, `code`, `name`, `path`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` LIKE 'rent_control_asset_%'
ORDER BY `sort`;

SELECT `menu_id`, COUNT(DISTINCT `role_id`) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` IN (1963598815738675521, 1963598815738675522, 1963598815738675523, 1963598815738675524)
GROUP BY `menu_id`
ORDER BY `menu_id`;
