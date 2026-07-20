-- 租控管理 - 智能设备台账与真实空置开始时间
-- 说明：新增设备台账、接口按钮权限，并由房间状态流转维护 vacant_since。脚本可重复执行。

SET NAMES utf8mb4;

SET @vacant_since_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'ics_room'
     AND column_name = 'vacant_since') = 0,
  'ALTER TABLE `ics_room` ADD COLUMN `vacant_since` datetime DEFAULT NULL COMMENT ''空置开始时间'' AFTER `status`',
  'SELECT 1'
);
PREPARE vacant_since_stmt FROM @vacant_since_sql;
EXECUTE vacant_since_stmt;
DEALLOCATE PREPARE vacant_since_stmt;

UPDATE `ics_room`
SET `vacant_since` = CASE
  WHEN `status` = '0' THEN COALESCE(`vacant_since`, `update_time`, `create_time`, NOW())
  ELSE NULL
END;

SET @vacant_since_index_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.statistics
   WHERE table_schema = DATABASE()
     AND table_name = 'ics_room'
     AND index_name = 'idx_room_status_vacant_since') = 0,
  'ALTER TABLE `ics_room` ADD INDEX `idx_room_status_vacant_since` (`status`, `vacant_since`)',
  'SELECT 1'
);
PREPARE vacant_since_index_stmt FROM @vacant_since_index_sql;
EXECUTE vacant_since_index_stmt;
DEALLOCATE PREPARE vacant_since_index_stmt;

CREATE TABLE IF NOT EXISTS `biz_smart_device` (
  `device_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `building_id` bigint DEFAULT NULL COMMENT '楼宇ID',
  `floor_no` int DEFAULT NULL COMMENT '楼层',
  `room_id` bigint DEFAULT NULL COMMENT '房间ID',
  `device_name` varchar(100) NOT NULL COMMENT '设备名称',
  `device_code` varchar(100) NOT NULL COMMENT '设备编码',
  `device_type` varchar(32) NOT NULL COMMENT '设备类型：electric/water/camera/lock/access',
  `brand` varchar(100) DEFAULT NULL COMMENT '品牌',
  `device_model` varchar(100) DEFAULT NULL COMMENT '型号',
  `install_location` varchar(200) DEFAULT NULL COMMENT '安装位置',
  `online_status` char(1) NOT NULL DEFAULT '1' COMMENT '在线状态：0在线、1离线',
  `enabled_status` char(1) NOT NULL DEFAULT '0' COMMENT '启用状态：0启用、1停用',
  `last_online_time` datetime DEFAULT NULL COMMENT '最后在线时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常、1删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`device_id`),
  UNIQUE KEY `uk_smart_device_code` (`device_code`),
  KEY `idx_smart_device_location` (`park_id`, `building_id`, `floor_no`),
  KEY `idx_smart_device_type_status` (`device_type`, `enabled_status`, `online_status`),
  KEY `idx_smart_device_room` (`room_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='智能设备台账';

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1963598815738675511, 1963598815738675501, 'rent_control_device_list', '设备台账查询', 'list', '/api/blade-park/smart-device/page', 'search', '', 11, 2, 1, 1, '智能设备台账查询、详情和统计权限', 0),
  (1963598815738675512, 1963598815738675501, 'rent_control_device_add', '新增设备', 'add', '/api/blade-park/smart-device/submit', 'plus', '', 12, 2, 1, 1, '新增智能设备', 0),
  (1963598815738675513, 1963598815738675501, 'rent_control_device_edit', '编辑设备', 'edit', '/api/blade-park/smart-device/submit', 'form', '', 13, 2, 2, 1, '编辑智能设备', 0),
  (1963598815738675514, 1963598815738675501, 'rent_control_device_delete', '删除设备', 'delete', '/api/blade-park/smart-device/remove', 'delete', '', 14, 2, 3, 1, '删除智能设备', 0)
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
  SELECT 2072000000000000001 AS `id`, 1123598816738675201 AS `role_id`, 1963598815738675511 AS `menu_id`
  UNION ALL SELECT 2072000000000000002, 1123598816738675201, 1963598815738675512
  UNION ALL SELECT 2072000000000000003, 1123598816738675201, 1963598815738675513
  UNION ALL SELECT 2072000000000000004, 1123598816738675201, 1963598815738675514
  UNION ALL SELECT 2072000000000000005, 1123598816738675203, 1963598815738675511
  UNION ALL SELECT 2072000000000000006, 1123598816738675203, 1963598815738675512
  UNION ALL SELECT 2072000000000000007, 1123598816738675203, 1963598815738675513
  UNION ALL SELECT 2072000000000000008, 1123598816738675203, 1963598815738675514
  UNION ALL SELECT 2072000000000000009, 1123598816738675204, 1963598815738675511
  UNION ALL SELECT 2072000000000000010, 1123598816738675204, 1963598815738675512
  UNION ALL SELECT 2072000000000000011, 1123598816738675204, 1963598815738675513
  UNION ALL SELECT 2072000000000000012, 1123598816738675204, 1963598815738675514
  UNION ALL SELECT 2072000000000000013, 1123598816738675205, 1963598815738675511
  UNION ALL SELECT 2072000000000000014, 1123598816738675205, 1963598815738675512
  UNION ALL SELECT 2072000000000000015, 1123598816738675205, 1963598815738675513
  UNION ALL SELECT 2072000000000000016, 1123598816738675205, 1963598815738675514
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

SELECT COUNT(*) AS `vacant_since_column_count`
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'ics_room' AND column_name = 'vacant_since';

SELECT COUNT(*) AS `smart_device_table_count`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'biz_smart_device';

SELECT `code`, `name`, `path`, `action`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('rent_control_device_list', 'rent_control_device_add', 'rent_control_device_edit', 'rent_control_device_delete')
ORDER BY `sort`;

SELECT `menu_id`, COUNT(*) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` IN (1963598815738675511, 1963598815738675512, 1963598815738675513, 1963598815738675514)
GROUP BY `menu_id`
ORDER BY `menu_id`;
