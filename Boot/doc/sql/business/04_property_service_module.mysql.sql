-- 企业服务-物业服务迁移脚本
-- 来源库：ry_ics
-- 目标库：bladex_boot
-- 说明：保留 RuoYi 业务表结构，不引入 BladeX 标准审计/租户字段。

CREATE TABLE IF NOT EXISTS `biz_property_service` (
  `service_id` bigint NOT NULL AUTO_INCREMENT COMMENT '服务ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `service_name` varchar(100) NOT NULL COMMENT '服务名称',
  `service_type` varchar(50) DEFAULT NULL COMMENT '服务类型(repair维修/clean保洁/security安保/green绿化/parking车位/utility水电/other其他)',
  `service_desc` text COMMENT '服务说明',
  `required_materials` varchar(500) DEFAULT NULL COMMENT '申请所需材料(JSON)',
  `service_flow` varchar(500) DEFAULT NULL COMMENT '服务流程说明',
  `charge_standard` varchar(200) DEFAULT NULL COMMENT '收费标准',
  `time_limit` int DEFAULT NULL COMMENT '服务时限(小时)',
  `status` char(1) DEFAULT '0' COMMENT '状态(0正常 1停用)',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物业服务项配置';

CREATE TABLE IF NOT EXISTS `biz_service_workorder` (
  `order_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工单ID',
  `order_no` varchar(64) NOT NULL COMMENT '工单编号',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `service_id` bigint DEFAULT NULL COMMENT '关联服务项ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `customer_name` varchar(200) DEFAULT NULL COMMENT '客户名称',
  `contact_name` varchar(100) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
  `room_ids` varchar(500) DEFAULT NULL COMMENT '房源ID，多个逗号分隔',
  `room_info` varchar(200) DEFAULT NULL COMMENT '房间信息',
  `demand_desc` text COMMENT '需求描述',
  `demand_images` varchar(1000) DEFAULT NULL COMMENT '需求图片(多张逗号分隔)',
  `order_status` char(1) DEFAULT '0' COMMENT '工单状态(0待受理 1处理中 2已完成 3已评价 4已关闭)',
  `priority` char(1) DEFAULT '1' COMMENT '优先级(0紧急 1普通 2低)',
  `assign_to` varchar(100) DEFAULT NULL COMMENT '指派人/服务商',
  `assign_time` datetime DEFAULT NULL COMMENT '指派时间',
  `disposal_content` text COMMENT '处置内容',
  `finish_time` datetime DEFAULT NULL COMMENT '完成时间',
  `rating` int DEFAULT NULL COMMENT '评分(1-5)',
  `rating_content` varchar(500) DEFAULT NULL COMMENT '评价内容',
  `rating_time` datetime DEFAULT NULL COMMENT '评价时间',
  `processor` varchar(100) DEFAULT NULL COMMENT '处理人',
  `process_remark` text COMMENT '处理说明',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0存在 1删除)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`order_id`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_service_id` (`service_id`),
  KEY `idx_park_status` (`park_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物业服务工单';

CREATE TABLE IF NOT EXISTS `biz_workorder_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `order_id` bigint NOT NULL COMMENT '工单ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型',
  `action_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `operator` varchar(100) DEFAULT NULL COMMENT '操作人',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单操作日志';

INSERT INTO `biz_property_service` (
  `service_id`, `park_id`, `service_name`, `service_type`, `service_desc`, `required_materials`,
  `service_flow`, `charge_standard`, `time_limit`, `status`, `sort_order`, `del_flag`,
  `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT `service_id`, `park_id`, `service_name`, `service_type`, `service_desc`, `required_materials`,
       `service_flow`, `charge_standard`, `time_limit`, `status`, `sort_order`, `del_flag`,
       `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_property_service`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `service_name` = VALUES(`service_name`),
  `service_type` = VALUES(`service_type`),
  `service_desc` = VALUES(`service_desc`),
  `required_materials` = VALUES(`required_materials`),
  `service_flow` = VALUES(`service_flow`),
  `charge_standard` = VALUES(`charge_standard`),
  `time_limit` = VALUES(`time_limit`),
  `status` = VALUES(`status`),
  `sort_order` = VALUES(`sort_order`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_service_workorder` (
  `order_id`, `order_no`, `park_id`, `service_id`, `customer_id`, `customer_name`,
  `contact_name`, `contact_phone`, `room_ids`, `room_info`, `demand_desc`, `demand_images`,
  `order_status`, `priority`, `assign_to`, `assign_time`, `disposal_content`, `finish_time`,
  `rating`, `rating_content`, `rating_time`, `processor`, `process_remark`, `next_follow_time`,
  `remark`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT `order_id`, `order_no`, `park_id`, `service_id`, `customer_id`, `customer_name`,
       `contact_name`, `contact_phone`, `room_ids`, `room_info`, `demand_desc`, `demand_images`,
       `order_status`, `priority`, `assign_to`, `assign_time`, `disposal_content`, `finish_time`,
       `rating`, `rating_content`, `rating_time`, `processor`, `process_remark`, `next_follow_time`,
       `remark`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_service_workorder`
ON DUPLICATE KEY UPDATE
  `order_no` = VALUES(`order_no`),
  `park_id` = VALUES(`park_id`),
  `service_id` = VALUES(`service_id`),
  `customer_id` = VALUES(`customer_id`),
  `customer_name` = VALUES(`customer_name`),
  `contact_name` = VALUES(`contact_name`),
  `contact_phone` = VALUES(`contact_phone`),
  `room_ids` = VALUES(`room_ids`),
  `room_info` = VALUES(`room_info`),
  `demand_desc` = VALUES(`demand_desc`),
  `demand_images` = VALUES(`demand_images`),
  `order_status` = VALUES(`order_status`),
  `priority` = VALUES(`priority`),
  `assign_to` = VALUES(`assign_to`),
  `assign_time` = VALUES(`assign_time`),
  `disposal_content` = VALUES(`disposal_content`),
  `finish_time` = VALUES(`finish_time`),
  `rating` = VALUES(`rating`),
  `rating_content` = VALUES(`rating_content`),
  `rating_time` = VALUES(`rating_time`),
  `processor` = VALUES(`processor`),
  `process_remark` = VALUES(`process_remark`),
  `next_follow_time` = VALUES(`next_follow_time`),
  `remark` = VALUES(`remark`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_workorder_log` (`log_id`, `order_id`, `action`, `action_desc`, `operator`, `operate_time`)
SELECT `log_id`, `order_id`, `action`, `action_desc`, `operator`, `operate_time`
FROM `ry_ics`.`biz_workorder_log`
ON DUPLICATE KEY UPDATE
  `order_id` = VALUES(`order_id`),
  `action` = VALUES(`action`),
  `action_desc` = VALUES(`action_desc`),
  `operator` = VALUES(`operator`),
  `operate_time` = VALUES(`operate_time`);

-- 源端历史物业数据存在 park_id=2，但当前园区档案中“园区二号”为 id=1。
-- 仅在 id=2 不存在且 id=1 存在时做映射，避免引入无效园区引用。
UPDATE `biz_property_service`
SET `park_id` = 1
WHERE `park_id` = 2
  AND NOT EXISTS (SELECT 1 FROM `ics_park` WHERE `id` = 2)
  AND EXISTS (SELECT 1 FROM `ics_park` WHERE `id` = 1);

UPDATE `biz_service_workorder`
SET `park_id` = 1
WHERE `park_id` = 2
  AND NOT EXISTS (SELECT 1 FROM `ics_park` WHERE `id` = 2)
  AND EXISTS (SELECT 1 FROM `ics_park` WHERE `id` = 1);

-- 菜单：企业服务 / 物业服务 / 物业服务配置、工单处理
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000000, 0, 'enterprise_service', '企业服务', 'menu', '/enterprise', 'iconfont iconicon_subordinate', '', 40, 1, 0, 1, '企业服务', 0),
  (1890000000007000100, 1890000000007000000, 'property_service', '物业服务', 'menu', '/enterprise/property-service', 'iconfont iconicon_work', 'views/enterprise/property-service', 1, 1, 0, 1, '物业服务', 0),
  (1890000000007000150, 1890000000007000100, 'property_service_config', '物业服务配置', 'menu', '/enterprise/property-service-config', 'iconfont iconicon_setting', 'views/enterprise/property-service-config', 1, 1, 0, 1, '物业服务配置', 0),
  (1890000000007000101, 1890000000007000150, 'property_service_list', '服务列表', 'list', '/api/blade-ics/property-service/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000007000102, 1890000000007000150, 'property_service_add', '新增服务', 'add', '/api/blade-ics/property-service/submit', 'plus', '', 2, 2, 1, 1, NULL, 0),
  (1890000000007000103, 1890000000007000150, 'property_service_edit', '修改服务', 'edit', '/api/blade-ics/property-service/update', 'form', '', 3, 2, 2, 1, NULL, 0),
  (1890000000007000104, 1890000000007000150, 'property_service_delete', '删除服务', 'delete', '/api/blade-ics/property-service/remove', 'delete', '', 4, 2, 3, 1, NULL, 0),
  (1890000000007000105, 1890000000007000150, 'property_service_view', '查看服务', 'view', '/api/blade-ics/property-service/detail', 'file-text', '', 5, 2, 2, 1, NULL, 0),
  (1890000000007000160, 1890000000007000100, 'property_workorder_process', '工单处理', 'menu', '/enterprise/property-workorder', 'iconfont iconicon_task', 'views/enterprise/property-workorder', 2, 1, 0, 1, '工单处理', 0),
  (1890000000007000106, 1890000000007000160, 'property_workorder_list', '工单列表', 'list', '/api/blade-ics/property-workorder/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000007000107, 1890000000007000160, 'property_workorder_add', '新增工单', 'add', '/api/blade-ics/property-workorder/submit', 'plus', '', 2, 2, 1, 1, NULL, 0),
  (1890000000007000108, 1890000000007000160, 'property_workorder_edit', '处置工单', 'edit', '/api/blade-ics/property-workorder/update', 'form', '', 3, 2, 2, 1, NULL, 0),
  (1890000000007000109, 1890000000007000160, 'property_workorder_delete', '删除工单', 'delete', '/api/blade-ics/property-workorder/remove', 'delete', '', 4, 2, 3, 1, NULL, 0),
  (1890000000007000110, 1890000000007000160, 'property_workorder_view', '查看工单', 'view', '/api/blade-ics/property-workorder/detail', 'file-text', '', 5, 2, 2, 1, NULL, 0),
  (1890000000007000111, 1890000000007000160, 'property_workorder_assign', '指派工单', 'assign', '/api/blade-ics/property-workorder/assign', 'user', '', 6, 2, 2, 1, NULL, 0),
  (1890000000007000112, 1890000000007000160, 'property_workorder_finish', '完成工单', 'finish', '/api/blade-ics/property-workorder/finish', 'circle-check', '', 7, 2, 2, 1, NULL, 0),
  (1890000000007000113, 1890000000007000160, 'property_workorder_close', '关闭工单', 'close', '/api/blade-ics/property-workorder/close', 'circle-close', '', 8, 2, 2, 1, NULL, 0),
  (1890000000007000114, 1890000000007000160, 'property_workorder_rate', '评价工单', 'rate', '/api/blade-ics/property-workorder/rate', 'star', '', 9, 2, 2, 1, NULL, 0),
  (1890000000007000115, 1890000000007000150, 'property_service_miniapp_list', '小程序服务列表', 'list', '/api/blade-ics/property-service/miniapp/list', 'list', '', 10, 2, 0, 1, NULL, 0),
  (1890000000007000116, 1890000000007000150, 'property_service_miniapp_detail', '小程序服务详情', 'view', '/api/blade-ics/property-service/miniapp/detail', 'file-text', '', 11, 2, 0, 1, NULL, 0),
  (1890000000007000117, 1890000000007000160, 'property_workorder_miniapp_apply', '小程序申请工单', 'add', '/api/blade-ics/property-workorder/miniapp/apply', 'plus', '', 10, 2, 1, 1, NULL, 0),
  (1890000000007000118, 1890000000007000160, 'property_workorder_miniapp_my_page', '小程序我的工单', 'list', '/api/blade-ics/property-workorder/miniapp/my-page', 'list', '', 11, 2, 0, 1, NULL, 0),
  (1890000000007000119, 1890000000007000160, 'property_workorder_miniapp_admin_page', '小程序管理员工单', 'list', '/api/blade-ics/property-workorder/miniapp/admin/page', 'list', '', 12, 2, 0, 1, NULL, 0),
  (1890000000007000120, 1890000000007000160, 'property_workorder_miniapp_admin_handle', '小程序处理工单', 'edit', '/api/blade-ics/property-workorder/miniapp/admin/handle', 'form', '', 13, 2, 2, 1, NULL, 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `name` = VALUES(`name`),
  `alias` = VALUES(`alias`),
  `path` = VALUES(`path`),
  `source` = VALUES(`source`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `is_open` = VALUES(`is_open`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT IGNORE INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000008000001, 1123598816738675201, 1890000000007000000),
  (1890000000008000002, 1123598816738675201, 1890000000007000100),
  (1890000000008000017, 1123598816738675201, 1890000000007000150),
  (1890000000008000018, 1123598816738675201, 1890000000007000160),
  (1890000000008000003, 1123598816738675201, 1890000000007000101),
  (1890000000008000004, 1123598816738675201, 1890000000007000102),
  (1890000000008000005, 1123598816738675201, 1890000000007000103),
  (1890000000008000006, 1123598816738675201, 1890000000007000104),
  (1890000000008000007, 1123598816738675201, 1890000000007000105),
  (1890000000008000008, 1123598816738675201, 1890000000007000106),
  (1890000000008000009, 1123598816738675201, 1890000000007000107),
  (1890000000008000010, 1123598816738675201, 1890000000007000108),
  (1890000000008000011, 1123598816738675201, 1890000000007000109),
  (1890000000008000012, 1123598816738675201, 1890000000007000110),
  (1890000000008000013, 1123598816738675201, 1890000000007000111),
  (1890000000008000014, 1123598816738675201, 1890000000007000112),
  (1890000000008000015, 1123598816738675201, 1890000000007000113),
  (1890000000008000016, 1123598816738675201, 1890000000007000114),
  (1890000000008000019, 1123598816738675201, 1890000000007000115),
  (1890000000008000020, 1123598816738675201, 1890000000007000116),
  (1890000000008000021, 1123598816738675201, 1890000000007000117),
  (1890000000008000022, 1123598816738675201, 1890000000007000118),
  (1890000000008000023, 1123598816738675201, 1890000000007000119),
  (1890000000008000024, 1123598816738675201, 1890000000007000120);

-- 清理此前误落到「审批中心」下面的物业工单菜单
UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` IN (
  'property_workorder_edit',
  'property_workorder_delete',
  'property_workorder_view',
  'property_workorder_assign',
  'property_workorder_finish',
  'property_workorder_close',
  'property_workorder_rate'
)
  AND `parent_id` = 1890000000005000100;

-- 校验 SQL：以下结果均应为 0
SELECT COUNT(*) AS invalid_service_count
FROM `biz_service_workorder` w
LEFT JOIN `biz_property_service` s ON s.service_id = w.service_id
WHERE w.del_flag = '0'
  AND w.service_id IS NOT NULL
  AND s.service_id IS NULL;

SELECT COUNT(*) AS invalid_park_count
FROM `biz_service_workorder` w
LEFT JOIN `ics_park` p ON p.id = w.park_id
WHERE w.del_flag = '0'
  AND w.park_id IS NOT NULL
  AND p.id IS NULL;

SELECT COUNT(*) AS invalid_log_order_count
FROM `biz_workorder_log` l
LEFT JOIN `biz_service_workorder` w ON w.order_id = l.order_id
WHERE w.order_id IS NULL;

SELECT COUNT(*) AS invalid_status_count
FROM `biz_service_workorder`
WHERE del_flag = '0'
  AND (order_status IS NULL OR order_status NOT IN ('0', '1', '2', '3', '4'));

SELECT COUNT(*) AS service_workorder_park_mismatch
FROM `biz_service_workorder` w
JOIN `biz_property_service` s ON s.service_id = w.service_id
WHERE w.del_flag = '0'
  AND s.del_flag = '0'
  AND w.park_id <> s.park_id;
