-- 企业服务-商户服务迁移脚本
-- 来源库：ry_ics
-- 目标库：bladex_boot
-- 说明：保留 RuoYi 业务表结构，并补齐企业服务下商户服务、政策服务、在园企业数据入口。

CREATE TABLE IF NOT EXISTS `biz_merchant` (
  `merchant_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `merchant_name` varchar(200) NOT NULL COMMENT '服务商名称',
  `business_type` varchar(100) DEFAULT NULL COMMENT '服务类型',
  `service_scope` varchar(500) DEFAULT NULL COMMENT '服务范围',
  `service_area` varchar(500) DEFAULT NULL COMMENT '服务区域',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系方式',
  `address` varchar(300) DEFAULT NULL COMMENT '地址',
  `qualification_files` text COMMENT '资质附件',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` char(1) NOT NULL DEFAULT '1' COMMENT '状态(0正常 1停用 2暂停)',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`merchant_id`),
  KEY `idx_merchant_park` (`park_id`),
  KEY `idx_merchant_status` (`status`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商户管理';

SET @has_qualification_files := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_merchant'
    AND COLUMN_NAME = 'qualification_files'
);
SET @add_qualification_files_sql := IF(
  @has_qualification_files = 0,
  'ALTER TABLE `biz_merchant` ADD COLUMN `qualification_files` text COMMENT ''资质附件'' AFTER `address`',
  'SELECT 1'
);
PREPARE add_qualification_files_stmt FROM @add_qualification_files_sql;
EXECUTE add_qualification_files_stmt;
DEALLOCATE PREPARE add_qualification_files_stmt;

INSERT INTO `biz_merchant` (
  `merchant_id`, `park_id`, `merchant_name`, `business_type`, `service_scope`, `service_area`,
  `contact_name`, `contact_phone`, `address`, `qualification_files`, `remark`, `status`, `del_flag`,
  `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT `merchant_id`, `park_id`, `merchant_name`, `business_type`, `service_scope`, `service_area`,
       `contact_name`, `contact_phone`, `address`, `qualification_files`, `remark`, `status`, `del_flag`,
       `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_merchant`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `merchant_name` = VALUES(`merchant_name`),
  `business_type` = VALUES(`business_type`),
  `service_scope` = VALUES(`service_scope`),
  `service_area` = VALUES(`service_area`),
  `contact_name` = VALUES(`contact_name`),
  `contact_phone` = VALUES(`contact_phone`),
  `address` = VALUES(`address`),
  `qualification_files` = VALUES(`qualification_files`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000000, 0, 'enterprise_service', '企业服务', 'menu', '/enterprise', 'iconfont iconicon_subordinate', '', 40, 1, 0, 1, '企业服务', 0),
  (1890000000007000200, 1890000000007000000, 'merchant_service', '商户管理', 'menu', '/enterprise/merchant-service', 'iconfont iconicon_work', 'views/enterprise/merchant-service', 2, 1, 0, 1, '商户管理', 0),
  (1890000000007000210, 1890000000007000200, 'merchant_service_merchant', '商户管理', 'menu', '/enterprise/merchant', 'iconfont iconicon_group', 'views/enterprise/merchant', 1, 1, 0, 1, '商户管理', 0),
  (1890000000007000211, 1890000000007000210, 'merchant_service_merchant_list', '商户列表', 'list', '/api/blade-ics/merchant/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000007000212, 1890000000007000210, 'merchant_service_merchant_add', '新增商户', 'add', '/api/blade-ics/merchant/save', 'plus', '', 2, 2, 1, 1, NULL, 0),
  (1890000000007000213, 1890000000007000210, 'merchant_service_merchant_edit', '修改商户', 'edit', '/api/blade-ics/merchant/update', 'form', '', 3, 2, 2, 1, NULL, 0),
  (1890000000007000214, 1890000000007000210, 'merchant_service_merchant_delete', '删除商户', 'delete', '/api/blade-ics/merchant/remove', 'delete', '', 4, 2, 3, 1, NULL, 0),
  (1890000000007000215, 1890000000007000210, 'merchant_service_merchant_view', '查看商户', 'view', '/api/blade-ics/merchant/detail', 'file-text', '', 5, 2, 2, 1, NULL, 0),
  (1890000000007000216, 1890000000007000210, 'merchant_service_merchant_status', '商户状态', 'status', '/api/blade-ics/merchant/changeStatus', 'circle-check', '', 6, 2, 2, 1, NULL, 0),
  (1890000000007000220, 1890000000007000200, 'merchant_service_ad', '广告管理', 'menu', '/enterprise/merchant-ad', 'iconfont iconicon_doc', 'views/enterprise/merchant-ad', 2, 1, 0, 1, '广告管理入口', 0),
  (1890000000007000230, 1890000000007000200, 'merchant_service_process', '服务处理', 'menu', '/enterprise/merchant-service-process', 'iconfont iconicon_task', 'views/enterprise/merchant-service-process', 3, 1, 0, 1, '服务处理入口', 0),
  (1890000000007000300, 1890000000007000000, 'enterprise_policy_service', '政策服务', 'menu', '/enterprise/policy-service', 'iconfont iconicon_doc', 'views/enterprise/policy-service', 3, 1, 0, 1, '政策服务入口', 0),
  (1890000000007000400, 1890000000007000000, 'enterprise_data', '在园企业数据', 'menu', '/enterprise/enterprise-data', 'iconfont iconicon_study', 'views/enterprise/enterprise-data', 4, 1, 0, 1, '在园企业数据入口', 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
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
  (1890000000008000101, 1123598816738675201, 1890000000007000200),
  (1890000000008000102, 1123598816738675201, 1890000000007000210),
  (1890000000008000103, 1123598816738675201, 1890000000007000211),
  (1890000000008000104, 1123598816738675201, 1890000000007000212),
  (1890000000008000105, 1123598816738675201, 1890000000007000213),
  (1890000000008000106, 1123598816738675201, 1890000000007000214),
  (1890000000008000107, 1123598816738675201, 1890000000007000215),
  (1890000000008000108, 1123598816738675201, 1890000000007000216),
  (1890000000008000109, 1123598816738675201, 1890000000007000220),
  (1890000000008000110, 1123598816738675201, 1890000000007000230),
  (1890000000008000111, 1123598816738675201, 1890000000007000300),
  (1890000000008000112, 1123598816738675201, 1890000000007000400);

-- 校验
SELECT COUNT(*) AS merchant_count FROM `biz_merchant` WHERE `del_flag` = '0';

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`
FROM `blade_menu`
WHERE `id` IN (
  1890000000007000000,
  1890000000007000200,
  1890000000007000210,
  1890000000007000220,
  1890000000007000230,
  1890000000007000300,
  1890000000007000400
)
ORDER BY `parent_id`, `sort`, `id`;
