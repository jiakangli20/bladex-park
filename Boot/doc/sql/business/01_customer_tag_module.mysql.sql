-- 客户标签模块迁移脚本（第一阶段）
-- 来源库：ry_ics
-- 目标库：bladex_boot
-- 说明：保留 RuoYi 业务表结构，不引入 BladeX 标准审计/租户字段。

CREATE TABLE IF NOT EXISTS `biz_tag_type` (
  `type_id` int NOT NULL AUTO_INCREMENT COMMENT '标签类型ID',
  `type_name` varchar(50) NOT NULL DEFAULT '' COMMENT '标签类型名称',
  `sort_order` int NOT NULL DEFAULT '1' COMMENT '排序',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`type_id`) USING BTREE,
  UNIQUE KEY `uk_biz_tag_type_name` (`type_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='客户标签类型表';

CREATE TABLE IF NOT EXISTS `biz_tag` (
  `tag_id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `tag_name` varchar(50) NOT NULL DEFAULT '' COMMENT '标签名称',
  `tag_type` int NOT NULL DEFAULT '1' COMMENT '标签类型ID',
  `tag_color` varchar(20) DEFAULT '#1059C6' COMMENT '标签颜色',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `park_id` bigint NOT NULL DEFAULT '1' COMMENT '园区ID',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`tag_id`) USING BTREE,
  KEY `idx_tag_type` (`tag_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='客户标签字典表';

CREATE TABLE IF NOT EXISTS `biz_customer_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_customer_tag` (`customer_id`,`tag_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_tag_id` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC COMMENT='客户标签关联表';

CREATE TABLE IF NOT EXISTS `biz_business_opportunity_tag` (
  `opportunity_id` bigint NOT NULL COMMENT '商机ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`opportunity_id`,`tag_id`),
  KEY `idx_opportunity_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商机客户标签关联表';

INSERT INTO `biz_tag_type` (`type_id`, `type_name`, `sort_order`, `status`, `del_flag`, `create_by`, `update_by`, `create_time`, `update_time`)
SELECT `type_id`, `type_name`, `sort_order`, `status`, `del_flag`, `create_by`, `update_by`, `create_time`, `update_time`
FROM `ry_ics`.`biz_tag_type`
ON DUPLICATE KEY UPDATE
  `type_name` = VALUES(`type_name`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_tag_type` (`type_id`, `type_name`, `sort_order`, `status`, `del_flag`, `create_by`, `create_time`)
VALUES (4, '客户来源', 4, '0', '0', 'system', now())
ON DUPLICATE KEY UPDATE
  `type_name` = VALUES(`type_name`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`),
  `del_flag` = VALUES(`del_flag`);

INSERT INTO `biz_tag` (`tag_id`, `tag_name`, `tag_type`, `tag_color`, `sort_order`, `status`, `del_flag`, `park_id`, `create_by`, `create_time`, `update_time`)
SELECT `tag_id`, `tag_name`, `tag_type`, `tag_color`, `sort_order`, `status`, `del_flag`, `park_id`, `create_by`, `create_time`, `update_time`
FROM `ry_ics`.`biz_tag`
ON DUPLICATE KEY UPDATE
  `tag_name` = VALUES(`tag_name`),
  `tag_type` = VALUES(`tag_type`),
  `tag_color` = VALUES(`tag_color`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`),
  `del_flag` = VALUES(`del_flag`),
  `park_id` = VALUES(`park_id`),
  `update_time` = VALUES(`update_time`);

-- 客户主表尚未迁移前，关系表只建结构不导入数据，避免悬空客户关系影响后续校验。
-- 客户模块完成后再执行：
-- INSERT IGNORE INTO `biz_customer_tag` (`id`, `customer_id`, `tag_id`, `create_time`)
-- SELECT `id`, `customer_id`, `tag_id`, `create_time`
-- FROM `ry_ics`.`biz_customer_tag`;

-- 商机主表尚未迁移前，商机标签关系只建结构不导入数据。
-- 商机模块完成后再执行：
-- INSERT IGNORE INTO `biz_business_opportunity_tag` (`opportunity_id`, `tag_id`, `create_time`)
-- SELECT `opportunity_id`, `tag_id`, `create_time`
-- FROM `ry_ics`.`biz_business_opportunity_tag`;

-- 菜单：招商管理 / 客户标签
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000001000000, 0, 'business', '招商管理', 'menu', '/business', 'iconfont iconicon_work', '', 20, 1, 0, 1, '招商入驻业务', 0),
  (1890000000001000100, 1890000000001000000, 'customer_tag', '客户标签', 'menu', '/business/tag', 'iconfont iconicon_label', 'views/business/tag', 1, 1, 0, 1, '客户标签管理', 0),
  (1890000000001000101, 1890000000001000100, 'customer_tag_add', '新增', 'add', '/business/tag/add', 'plus', '', 1, 2, 1, 1, NULL, 0),
  (1890000000001000102, 1890000000001000100, 'customer_tag_edit', '修改', 'edit', '/business/tag/edit', 'form', '', 2, 2, 2, 1, NULL, 0),
  (1890000000001000103, 1890000000001000100, 'customer_tag_delete', '删除', 'delete', '/api/blade-park/tag/remove', 'delete', '', 3, 2, 3, 1, NULL, 0),
  (1890000000001000104, 1890000000001000100, 'customer_tag_view', '查看', 'view', '/business/tag/view', 'file-text', '', 4, 2, 2, 1, NULL, 0),
  (1890000000001000105, 1890000000001000100, 'customer_tag_type_add', '新增类型', 'add', '/business/tag/type/add', 'plus', '', 5, 2, 1, 1, NULL, 0),
  (1890000000001000106, 1890000000001000100, 'customer_tag_type_edit', '修改类型', 'edit', '/business/tag/type/edit', 'form', '', 6, 2, 2, 1, NULL, 0),
  (1890000000001000107, 1890000000001000100, 'customer_tag_type_delete', '删除类型', 'delete', '/api/blade-park/tag-type/remove', 'delete', '', 7, 2, 3, 1, NULL, 0)
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

-- 默认给超级管理员角色绑定菜单，其他角色请在 BladeX 菜单授权界面按需分配。
INSERT IGNORE INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000002000001, 1123598816738675201, 1890000000001000000),
  (1890000000002000002, 1123598816738675201, 1890000000001000100),
  (1890000000002000003, 1123598816738675201, 1890000000001000101),
  (1890000000002000004, 1123598816738675201, 1890000000001000102),
  (1890000000002000005, 1123598816738675201, 1890000000001000103),
  (1890000000002000006, 1123598816738675201, 1890000000001000104),
  (1890000000002000007, 1123598816738675201, 1890000000001000105),
  (1890000000002000008, 1123598816738675201, 1890000000001000106),
  (1890000000002000009, 1123598816738675201, 1890000000001000107);
