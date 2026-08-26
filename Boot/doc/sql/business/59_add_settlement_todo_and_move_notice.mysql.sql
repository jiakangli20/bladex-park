-- 小程序入驻意向独立为“招商待办”，并将相关入口归入“企业服务”（可重复执行）。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_settlement_todo` (
  `todo_id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办ID',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `todo_no` varchar(40) NOT NULL COMMENT '待办编号',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '企业客户ID',
  `member_id` bigint DEFAULT NULL COMMENT '小程序成员ID',
  `source_room_id` bigint DEFAULT NULL COMMENT '来源房源ID',
  `source_opportunity_id` bigint DEFAULT NULL COMMENT '历史商机ID',
  `enterprise_name` varchar(200) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(50) NOT NULL COMMENT '统一社会信用代码',
  `industry_type` varchar(100) DEFAULT NULL COMMENT '行业类型',
  `enterprise_scale` varchar(64) DEFAULT NULL COMMENT '企业规模',
  `intent_area` decimal(18,2) DEFAULT NULL COMMENT '意向面积',
  `expected_entry_date` date DEFAULT NULL COMMENT '预计入驻日期',
  `contact_name` varchar(100) NOT NULL COMMENT '联系人',
  `contact_phone` varchar(50) NOT NULL COMMENT '联系电话',
  `demand_desc` varchar(1000) DEFAULT NULL COMMENT '需求说明',
  `todo_status` char(1) NOT NULL DEFAULT '0' COMMENT '状态：0待受理1已受理2跟进中3已完成4已驳回',
  `assignee_user_id` bigint DEFAULT NULL COMMENT '处理人用户ID',
  `assignee_name` varchar(100) DEFAULT NULL COMMENT '处理人',
  `process_remark` varchar(1000) DEFAULT NULL COMMENT '处理说明',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '驳回原因',
  `processed_time` datetime DEFAULT NULL COMMENT '最后处理时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  PRIMARY KEY (`todo_id`),
  UNIQUE KEY `uk_settlement_todo_no` (`todo_no`),
  UNIQUE KEY `uk_settlement_source_opportunity` (`source_opportunity_id`),
  KEY `idx_settlement_todo_scope` (`tenant_id`,`park_id`,`customer_id`,`create_time`),
  KEY `idx_settlement_todo_status` (`tenant_id`,`park_id`,`todo_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招商待办';

-- 兼容已上线版本：历史小程序商机迁入招商待办，随后从商机管理中逻辑移除。
INSERT IGNORE INTO `biz_settlement_todo` (
  `tenant_id`, `todo_no`, `park_id`, `customer_id`, `source_room_id`, `source_opportunity_id`,
  `enterprise_name`, `credit_code`, `industry_type`, `enterprise_scale`, `intent_area`,
  `expected_entry_date`, `contact_name`, `contact_phone`, `demand_desc`, `todo_status`,
  `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`
)
SELECT
  '000000', CONCAT('ZSDM', opportunity_id), park_id, customer_id, source_room_id, opportunity_id,
  enterprise_name, credit_code, industry_type, enterprise_scale, intent_area,
  expected_entry_date, contact_name, contact_phone, remark,
  CASE
    WHEN opportunity_status IN ('1') THEN '1'
    WHEN opportunity_status IN ('2') THEN '2'
    WHEN opportunity_status IN ('3', 'DEAL') THEN '3'
    WHEN opportunity_status IN ('4') THEN '4'
    ELSE '0'
  END,
  create_by, create_time, update_by, update_time, '0'
FROM `biz_business_opportunity`
WHERE UPPER(COALESCE(channel, '')) = 'MINIAPP' AND COALESCE(del_flag, '0') = '0';

UPDATE `biz_business_opportunity`
SET del_flag = '1', update_time = NOW(), update_by = 'miniapp-migration'
WHERE UPPER(COALESCE(channel, '')) = 'MINIAPP' AND COALESCE(del_flag, '0') = '0';

-- 企业服务下：物业服务、商户管理、政策服务、在园企业数据、招商待办、通知公告。
UPDATE `blade_menu`
SET parent_id = 1890000000007000000, sort = 6
WHERE id = 1123598815738675202;

-- 通知公告前端入口统一迁移到企业服务。
UPDATE `blade_menu`
SET path = '/enterprise/notice', component = 'views/enterprise/notice'
WHERE id = 1123598815738675202;

UPDATE `blade_menu`
SET path = REPLACE(path, '/desk/notice', '/enterprise/notice')
WHERE parent_id = 1123598815738675202 AND path LIKE '/desk/notice%';

-- 页面已取消删除入口，同步隐藏删除权限菜单。
UPDATE `blade_menu`
SET is_deleted = 1
WHERE id = 1123598815738675221;

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000500, 1890000000007000000, 'settlement_todo', '招商待办', 'menu', '/enterprise/settlement-todo', 'iconfont iconicon_task', 'views/enterprise/settlement-todo', 5, 1, 0, 1, '小程序入驻意向处理', 0),
  (1890000000007000501, 1890000000007000500, 'settlement_todo_view', '查看', 'view', '/enterprise/settlement-todo/view', 'file-text', '', 1, 2, 2, 1, NULL, 0),
  (1890000000007000502, 1890000000007000500, 'settlement_todo_process', '处理', 'process', '/enterprise/settlement-todo/process', 'finished', '', 2, 2, 2, 1, NULL, 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`), `name` = VALUES(`name`), `path` = VALUES(`path`),
  `component` = VALUES(`component`), `sort` = VALUES(`sort`), `is_deleted` = 0;

-- 继承企业服务菜单已有角色，保证新增二级菜单可见。
INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2090701000000000000 + ROW_NUMBER() OVER (ORDER BY role_id), 1890000000007000500, role_id
FROM `blade_role_menu` parent_role
WHERE parent_role.menu_id = 1890000000007000000
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` existing WHERE existing.role_id = parent_role.role_id AND existing.menu_id = 1890000000007000500);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2090702000000000000 + ROW_NUMBER() OVER (ORDER BY role_id), 1890000000007000501, role_id
FROM `blade_role_menu` parent_role
WHERE parent_role.menu_id = 1890000000007000000
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` existing WHERE existing.role_id = parent_role.role_id AND existing.menu_id = 1890000000007000501);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2090703000000000000 + ROW_NUMBER() OVER (ORDER BY role_id), 1890000000007000502, role_id
FROM `blade_role_menu` parent_role
WHERE parent_role.menu_id = 1890000000007000000
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` existing WHERE existing.role_id = parent_role.role_id AND existing.menu_id = 1890000000007000502);

-- 顶部“企业服务”只展示其自身业务菜单。新增、移动菜单必须同步加入顶部菜单配置。
INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 2090704000000000001, service_top.`id`, 1890000000007000500
FROM (
  SELECT `id`
  FROM `blade_top_menu`
  WHERE `code` = 'service' AND `is_deleted` = 0
  ORDER BY `id`
  LIMIT 1
) service_top
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_top_menu_setting` current_setting
  WHERE current_setting.`top_menu_id` = service_top.`id`
    AND current_setting.`menu_id` = 1890000000007000500
)
  AND NOT EXISTS (
    SELECT 1 FROM `blade_top_menu_setting` current_id WHERE current_id.`id` = 2090704000000000001
  );

INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 2090704000000000002, service_top.`id`, 1123598815738675202
FROM (
  SELECT `id`
  FROM `blade_top_menu`
  WHERE `code` = 'service' AND `is_deleted` = 0
  ORDER BY `id`
  LIMIT 1
) service_top
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_top_menu_setting` current_setting
  WHERE current_setting.`top_menu_id` = service_top.`id`
    AND current_setting.`menu_id` = 1123598815738675202
)
  AND NOT EXISTS (
    SELECT 1 FROM `blade_top_menu_setting` current_id WHERE current_id.`id` = 2090704000000000002
  );
