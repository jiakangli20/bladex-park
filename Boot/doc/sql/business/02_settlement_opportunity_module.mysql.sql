-- 入驻管理 - 商机管理模块迁移脚本（第一阶段）
-- 来源库：ry_ics
-- 目标库：bladex_boot
-- 说明：保留 RuoYi 商机业务表结构，不引入 BladeX 标准租户/审计字段。

CREATE TABLE IF NOT EXISTS `biz_business_opportunity` (
  `opportunity_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商机ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `opportunity_no` varchar(32) NOT NULL COMMENT '商机编号',
  `enterprise_name` varchar(200) NOT NULL COMMENT '企业名称',
  `credit_code` varchar(50) NOT NULL COMMENT '统一社会信用代码',
  `establish_date` date DEFAULT NULL COMMENT '成立日期',
  `registered_capital` decimal(18,2) DEFAULT NULL COMMENT '注册资本(万元)',
  `enterprise_type` varchar(100) DEFAULT NULL COMMENT '企业类型',
  `industry_type` varchar(100) DEFAULT NULL COMMENT '行业类型',
  `business_scope` text COMMENT '经营范围',
  `registered_address` varchar(500) DEFAULT NULL COMMENT '注册地址',
  `equity_structure` text COMMENT '股权结构',
  `organization_structure` text COMMENT '组织架构',
  `main_business` varchar(500) DEFAULT NULL COMMENT '主营业务',
  `last_year_revenue` decimal(18,2) DEFAULT NULL COMMENT '上年度营收规模',
  `major_clients` text COMMENT '主要合作客户',
  `major_illegal_flag` char(1) DEFAULT '0' COMMENT '有无重大违法违规记录(0否1是)',
  `dishonest_flag` char(1) DEFAULT '0' COMMENT '有无失信被执行记录(0否1是)',
  `industry_penalty_flag` char(1) DEFAULT '0' COMMENT '有无严重行业处罚记录(0否1是)',
  `carrier_types` varchar(200) DEFAULT NULL COMMENT '意向载体类型，逗号分隔',
  `intent_area` decimal(18,2) DEFAULT NULL COMMENT '意向租赁面积',
  `usage_purpose` varchar(500) DEFAULT NULL COMMENT '使用用途',
  `lease_term_years` decimal(8,2) DEFAULT NULL COMMENT '意向租赁期限(年)',
  `lease_term_label` varchar(50) DEFAULT NULL COMMENT '意向租赁期限分档',
  `decoration_requirement` text COMMENT '装修要求',
  `supporting_needs` text COMMENT '配套需求',
  `contact_name` varchar(100) NOT NULL COMMENT '负责人姓名',
  `contact_phone` varchar(50) NOT NULL COMMENT '联系电话',
  `contact_email` varchar(100) DEFAULT NULL COMMENT '电子邮箱',
  `contact_address` varchar(500) DEFAULT NULL COMMENT '联系地址',
  `id_card_files` text COMMENT '身份证附件',
  `contact_position` varchar(100) DEFAULT NULL COMMENT '职务',
  `channel` varchar(50) NOT NULL COMMENT '招商渠道',
  `third_party_channel_name` varchar(200) DEFAULT NULL COMMENT '第三方渠道名称',
  `follow_user` varchar(64) DEFAULT NULL COMMENT '跟进人',
  `follow_user_id` bigint DEFAULT NULL COMMENT '跟进人用户ID',
  `opportunity_status` varchar(32) DEFAULT 'DRAFT' COMMENT '商机状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `approval_project_id` bigint DEFAULT NULL COMMENT '关联项目审核ID',
  `submitted_audit_flag` char(1) DEFAULT '0' COMMENT '是否已提交审核(0否1是)',
  `last_follow_time` datetime DEFAULT NULL COMMENT '最后跟进时间',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志(0正常1删除)',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`opportunity_id`),
  UNIQUE KEY `uk_opportunity_no` (`opportunity_no`),
  KEY `idx_opportunity_customer` (`customer_id`),
  KEY `idx_opportunity_status` (`opportunity_status`),
  KEY `idx_opportunity_credit_code` (`credit_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商机主表';

CREATE TABLE IF NOT EXISTS `biz_business_opportunity_follow` (
  `follow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '跟进ID',
  `opportunity_id` bigint NOT NULL COMMENT '商机ID',
  `follow_time` datetime DEFAULT NULL COMMENT '跟进时间',
  `follow_user` varchar(64) DEFAULT NULL COMMENT '跟进人',
  `follow_user_id` bigint DEFAULT NULL COMMENT '跟进人用户ID',
  `opportunity_status` varchar(32) DEFAULT NULL COMMENT '跟进后商机状态',
  `follow_content` text COMMENT '跟进内容',
  `next_follow_time` datetime DEFAULT NULL COMMENT '下次跟进时间',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`follow_id`),
  KEY `idx_follow_opportunity` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商机跟进记录表';

CREATE TABLE IF NOT EXISTS `biz_business_opportunity_file` (
  `file_id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件ID',
  `opportunity_id` bigint NOT NULL COMMENT '商机ID',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名称',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址',
  `file_suffix` varchar(20) DEFAULT NULL COMMENT '文件后缀',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`file_id`),
  KEY `idx_file_opportunity` (`opportunity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商机附件表';

CREATE TABLE IF NOT EXISTS `biz_business_opportunity_tag` (
  `opportunity_id` bigint NOT NULL COMMENT '商机ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`opportunity_id`,`tag_id`),
  KEY `idx_opportunity_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商机客户标签关联表';

SET @has_opportunity_contact_address := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_business_opportunity'
    AND COLUMN_NAME = 'contact_address'
);
SET @add_opportunity_contact_address_sql := IF(
  @has_opportunity_contact_address = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `contact_address` varchar(500) DEFAULT NULL COMMENT ''联系地址'' AFTER `contact_email`',
  'SELECT 1'
);
PREPARE add_opportunity_contact_address_stmt FROM @add_opportunity_contact_address_sql;
EXECUTE add_opportunity_contact_address_stmt;
DEALLOCATE PREPARE add_opportunity_contact_address_stmt;

SET @has_opportunity_id_card_files := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_business_opportunity'
    AND COLUMN_NAME = 'id_card_files'
);
SET @add_opportunity_id_card_files_sql := IF(
  @has_opportunity_id_card_files = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `id_card_files` text COMMENT ''身份证附件'' AFTER `contact_address`',
  'SELECT 1'
);
PREPARE add_opportunity_id_card_files_stmt FROM @add_opportunity_id_card_files_sql;
EXECUTE add_opportunity_id_card_files_stmt;
DEALLOCATE PREPARE add_opportunity_id_card_files_stmt;

SET @has_opportunity_follow_user_id := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_business_opportunity'
    AND COLUMN_NAME = 'follow_user_id'
);
SET @add_opportunity_follow_user_id_sql := IF(
  @has_opportunity_follow_user_id = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `follow_user_id` bigint DEFAULT NULL COMMENT ''跟进人用户ID'' AFTER `follow_user`',
  'SELECT 1'
);
PREPARE add_opportunity_follow_user_id_stmt FROM @add_opportunity_follow_user_id_sql;
EXECUTE add_opportunity_follow_user_id_stmt;
DEALLOCATE PREPARE add_opportunity_follow_user_id_stmt;

SET @has_opportunity_follow_record_user_id := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'biz_business_opportunity_follow'
    AND COLUMN_NAME = 'follow_user_id'
);
SET @add_opportunity_follow_record_user_id_sql := IF(
  @has_opportunity_follow_record_user_id = 0,
  'ALTER TABLE `biz_business_opportunity_follow` ADD COLUMN `follow_user_id` bigint DEFAULT NULL COMMENT ''跟进人用户ID'' AFTER `follow_user`',
  'SELECT 1'
);
PREPARE add_opportunity_follow_record_user_id_stmt FROM @add_opportunity_follow_record_user_id_sql;
EXECUTE add_opportunity_follow_record_user_id_stmt;
DEALLOCATE PREPARE add_opportunity_follow_record_user_id_stmt;

CREATE TABLE IF NOT EXISTS `biz_industry_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `industry_keyword` varchar(100) NOT NULL COMMENT '行业关键词',
  `access_type` char(1) NOT NULL DEFAULT '2' COMMENT '准入类型（2限制 3禁入）',
  `reason` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `status` char(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0正常 1删除）',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`rule_id`),
  UNIQUE KEY `uk_industry_rule_keyword` (`industry_keyword`),
  KEY `idx_industry_rule_access_type` (`access_type`),
  KEY `idx_industry_rule_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行业准入规则表';

INSERT INTO `biz_business_opportunity` (
  `opportunity_id`, `park_id`, `customer_id`, `opportunity_no`, `enterprise_name`, `credit_code`, `establish_date`, `registered_capital`,
  `enterprise_type`, `industry_type`, `business_scope`, `registered_address`, `equity_structure`, `organization_structure`,
  `main_business`, `last_year_revenue`, `major_clients`, `major_illegal_flag`, `dishonest_flag`, `industry_penalty_flag`,
  `carrier_types`, `intent_area`, `usage_purpose`, `lease_term_years`, `lease_term_label`, `decoration_requirement`,
  `supporting_needs`, `contact_name`, `contact_phone`, `contact_email`, `contact_address`, `id_card_files`, `contact_position`, `channel`,
  `third_party_channel_name`, `follow_user`, `follow_user_id`, `opportunity_status`, `remark`, `approval_project_id`, `submitted_audit_flag`,
  `last_follow_time`, `next_follow_time`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  `opportunity_id`, `park_id`, `customer_id`, `opportunity_no`, `enterprise_name`, `credit_code`, `establish_date`, `registered_capital`,
  `enterprise_type`, `industry_type`, `business_scope`, `registered_address`, `equity_structure`, `organization_structure`,
  `main_business`, `last_year_revenue`, `major_clients`, `major_illegal_flag`, `dishonest_flag`, `industry_penalty_flag`,
  `carrier_types`, `intent_area`, `usage_purpose`, `lease_term_years`, `lease_term_label`, `decoration_requirement`,
  `supporting_needs`, `contact_name`, `contact_phone`, `contact_email`, null, null, `contact_position`, `channel`,
  `third_party_channel_name`, `follow_user`, null,
  case when `opportunity_status` in ('DRAFT', 'AUDIT') or `opportunity_status` is null or `opportunity_status` = '' then 'INITIAL' else `opportunity_status` end,
  `remark`, `approval_project_id`, `submitted_audit_flag`, `last_follow_time`, `next_follow_time`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_business_opportunity`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `customer_id` = VALUES(`customer_id`),
  `enterprise_name` = VALUES(`enterprise_name`),
  `credit_code` = VALUES(`credit_code`),
  `contact_email` = VALUES(`contact_email`),
  `contact_address` = VALUES(`contact_address`),
  `id_card_files` = VALUES(`id_card_files`),
  `follow_user` = VALUES(`follow_user`),
  `follow_user_id` = VALUES(`follow_user_id`),
  `opportunity_status` = VALUES(`opportunity_status`),
  `submitted_audit_flag` = VALUES(`submitted_audit_flag`),
  `last_follow_time` = VALUES(`last_follow_time`),
  `next_follow_time` = VALUES(`next_follow_time`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_business_opportunity_follow` (
  `follow_id`, `opportunity_id`, `follow_time`, `follow_user`, `follow_user_id`, `opportunity_status`, `follow_content`, `next_follow_time`, `create_by`, `create_time`
)
SELECT `follow_id`, `opportunity_id`, `follow_time`, `follow_user`, null,
       case when `opportunity_status` in ('DRAFT', 'AUDIT') or `opportunity_status` is null or `opportunity_status` = '' then 'INITIAL' else `opportunity_status` end,
       `follow_content`, `next_follow_time`, `create_by`, `create_time`
FROM `ry_ics`.`biz_business_opportunity_follow`
ON DUPLICATE KEY UPDATE
  `follow_user` = VALUES(`follow_user`),
  `follow_user_id` = VALUES(`follow_user_id`),
  `opportunity_status` = VALUES(`opportunity_status`),
  `follow_content` = VALUES(`follow_content`),
  `next_follow_time` = VALUES(`next_follow_time`);

INSERT INTO `biz_business_opportunity_file` (
  `file_id`, `opportunity_id`, `file_name`, `file_url`, `file_suffix`, `file_size`, `create_by`, `create_time`
)
SELECT `file_id`, `opportunity_id`, `file_name`, `file_url`, `file_suffix`, `file_size`, `create_by`, `create_time`
FROM `ry_ics`.`biz_business_opportunity_file`
ON DUPLICATE KEY UPDATE
  `file_name` = VALUES(`file_name`),
  `file_url` = VALUES(`file_url`),
  `file_suffix` = VALUES(`file_suffix`),
  `file_size` = VALUES(`file_size`);

INSERT IGNORE INTO `biz_business_opportunity_tag` (`opportunity_id`, `tag_id`, `create_time`)
SELECT ot.`opportunity_id`, ot.`tag_id`, ot.`create_time`
FROM `ry_ics`.`biz_business_opportunity_tag` ot
INNER JOIN `bladex_boot`.`biz_business_opportunity` o ON o.`opportunity_id` = ot.`opportunity_id`
INNER JOIN `bladex_boot`.`biz_tag` t ON t.`tag_id` = ot.`tag_id`;

-- 客户模块第一阶段只保留已迁入客户关联，缺失客户不迁入本模块，置空避免悬空关系。
UPDATE `biz_business_opportunity` o
LEFT JOIN `biz_customer` c ON c.`customer_id` = o.`customer_id`
SET o.`customer_id` = NULL
WHERE o.`customer_id` IS NOT NULL
  AND c.`customer_id` IS NULL;

-- 已关联客户的商机标签同步为客户标签，避免客户管理/客户标签页显示未设置。
INSERT IGNORE INTO `biz_customer_tag` (`customer_id`, `tag_id`, `create_time`)
SELECT DISTINCT o.`customer_id`, ot.`tag_id`, COALESCE(ot.`create_time`, now())
FROM `biz_business_opportunity` o
INNER JOIN `biz_business_opportunity_tag` ot ON ot.`opportunity_id` = o.`opportunity_id`
INNER JOIN `biz_customer` c ON c.`customer_id` = o.`customer_id`
INNER JOIN `biz_tag` t ON t.`tag_id` = ot.`tag_id`
WHERE o.`customer_id` IS NOT NULL;

-- 菜单：入驻管理 / 商机管理
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000003000000, 0, 'settlement', '入驻管理', 'menu', '/settlement', 'iconfont iconicon_compile', '', 30, 1, 0, 1, '入驻管理', 0),
  (1890000000003000100, 1890000000003000000, 'business_opportunity', '商机管理', 'menu', '/settlement/opportunity', 'iconfont iconicon_doc', 'views/settlement/opportunity', 1, 1, 0, 1, '商机管理', 0),
  (1890000000003000101, 1890000000003000100, 'business_opportunity_add', '新增', 'add', '/settlement/opportunity/add', 'plus', '', 1, 2, 1, 1, NULL, 0),
  (1890000000003000102, 1890000000003000100, 'business_opportunity_edit', '修改', 'edit', '/settlement/opportunity/edit', 'form', '', 2, 2, 2, 1, NULL, 0),
  (1890000000003000103, 1890000000003000100, 'business_opportunity_delete', '删除', 'delete', '/api/blade-park/opportunity/remove', 'delete', '', 3, 2, 3, 1, NULL, 0),
  (1890000000003000104, 1890000000003000100, 'business_opportunity_view', '查看', 'view', '/settlement/opportunity/view', 'file-text', '', 4, 2, 2, 1, NULL, 0),
  (1890000000003000105, 1890000000003000100, 'business_opportunity_follow', '跟进', 'follow', '/settlement/opportunity/follow', 'message', '', 5, 2, 2, 1, NULL, 0),
  (1890000000003000106, 1890000000003000100, 'business_opportunity_audit', '提交审核', 'audit', '/settlement/opportunity/audit', 'upload', '', 6, 2, 2, 1, NULL, 0),
  (1890000000003000107, 1890000000003000100, 'business_opportunity_file_upload', '附件上传', 'upload', '/settlement/opportunity/file/upload', 'upload', '', 7, 2, 1, 1, NULL, 0),
  (1890000000003000108, 1890000000003000100, 'business_opportunity_industry_rule', '行业规则', 'setting', '/settlement/opportunity/industry-rule', 'setting', '', 8, 2, 2, 1, NULL, 0)
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
  (1890000000004000001, 1123598816738675201, 1890000000003000000),
  (1890000000004000002, 1123598816738675201, 1890000000003000100),
  (1890000000004000003, 1123598816738675201, 1890000000003000101),
  (1890000000004000004, 1123598816738675201, 1890000000003000102),
  (1890000000004000005, 1123598816738675201, 1890000000003000103),
  (1890000000004000006, 1123598816738675201, 1890000000003000104),
  (1890000000004000007, 1123598816738675201, 1890000000003000105),
  (1890000000004000008, 1123598816738675201, 1890000000003000106),
  (1890000000004000009, 1123598816738675201, 1890000000003000107),
  (1890000000004000010, 1123598816738675201, 1890000000003000108);
