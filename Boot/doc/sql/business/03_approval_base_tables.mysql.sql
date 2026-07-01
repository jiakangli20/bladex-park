-- 审批基础表迁移脚本（商机/合同依赖）
-- 来源库：ry_ics
-- 目标库：bladex_boot
-- 说明：仅迁移审批基础业务表与数据，不迁移 RuoYi 菜单与审批页面。

CREATE TABLE IF NOT EXISTS `biz_approval_flow` (
  `flow_id` bigint NOT NULL AUTO_INCREMENT COMMENT '流程ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `flow_name` varchar(100) NOT NULL COMMENT '流程名称',
  `business_type` varchar(50) DEFAULT 'tenant_entry' COMMENT '业务类型',
  `flow_version` int DEFAULT '1' COMMENT '版本号',
  `node_config` text COMMENT '节点配置(JSON或逗号分隔)',
  `status` char(1) DEFAULT '1' COMMENT '状态（1启用 0停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批流程配置表';

CREATE TABLE IF NOT EXISTS `biz_approval_node` (
  `node_id` bigint NOT NULL AUTO_INCREMENT COMMENT '节点ID',
  `flow_id` bigint NOT NULL COMMENT '流程模板ID',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称',
  `node_order` int NOT NULL DEFAULT '1' COMMENT '节点顺序',
  `node_type` varchar(20) DEFAULT 'approve' COMMENT '节点类型(submit/approve/cc)',
  `approver_login` varchar(200) DEFAULT NULL COMMENT '审批人loginName，多个逗号分隔',
  `approver_name` varchar(200) DEFAULT NULL COMMENT '审批人显示名',
  `complete_condition` varchar(20) DEFAULT 'all' COMMENT '完成条件(all/any)',
  `time_limit` int DEFAULT NULL COMMENT '节点时限(小时)',
  `cc_users` varchar(500) DEFAULT NULL COMMENT '抄送人loginName，多个逗号分隔',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  PRIMARY KEY (`node_id`),
  KEY `idx_flow_id` (`flow_id`),
  KEY `idx_park_flow` (`park_id`,`flow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批节点结构化配置表';

CREATE TABLE IF NOT EXISTS `biz_approval_project` (
  `project_id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `flow_id` bigint DEFAULT NULL COMMENT '流程ID',
  `business_id` bigint DEFAULT NULL COMMENT '业务ID',
  `business_type` varchar(50) DEFAULT 'tenant_entry' COMMENT '业务类型',
  `project_name` varchar(200) DEFAULT NULL COMMENT '项目名称',
  `enterprise_name` varchar(200) DEFAULT NULL COMMENT '企业名称',
  `credit_code` varchar(50) DEFAULT NULL COMMENT '统一社会信用代码',
  `applicant` varchar(100) DEFAULT NULL COMMENT '发起人',
  `applicant_dept` varchar(100) DEFAULT NULL COMMENT '发起部门',
  `applicant_time` datetime DEFAULT NULL COMMENT '发起时间',
  `shareholder_info` varchar(1000) DEFAULT NULL COMMENT '股东信息',
  `business_scope` varchar(2000) DEFAULT NULL COMMENT '经营范围',
  `responsible_person` varchar(100) DEFAULT NULL COMMENT '负责人',
  `contact_phone` varchar(100) DEFAULT NULL COMMENT '联系方式',
  `lease_floor` varchar(100) DEFAULT NULL COMMENT '租赁楼层',
  `lease_area` decimal(12,2) DEFAULT NULL COMMENT '租赁面积',
  `rent_price` decimal(12,2) DEFAULT NULL COMMENT '单价',
  `deposit` decimal(12,2) DEFAULT NULL COMMENT '保证金',
  `rent_free_period` varchar(100) DEFAULT NULL COMMENT '免租期',
  `lease_start_date` date DEFAULT NULL COMMENT '租赁开始日期',
  `lease_end_date` date DEFAULT NULL COMMENT '租赁结束日期',
  `current_node` varchar(100) DEFAULT NULL COMMENT '当前节点编码',
  `current_node_name` varchar(100) DEFAULT NULL COMMENT '当前节点名称',
  `process_status` char(1) DEFAULT '0' COMMENT '流程状态（0草稿 1审批中 2已通过 3已驳回 4已撤回 5已归档）',
  `approval_result` varchar(50) DEFAULT NULL COMMENT '审批结果',
  `summary` varchar(1000) DEFAULT NULL COMMENT '审批摘要',
  `approval_matter` varchar(2000) DEFAULT NULL COMMENT '审批事项',
  `approval_form_json` longtext COMMENT '审批表JSON',
  `approval_form_url` varchar(500) DEFAULT NULL COMMENT '审批表地址',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`project_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_process_status` (`process_status`),
  KEY `idx_flow_id` (`flow_id`),
  KEY `idx_business_type_id` (`business_type`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批项目表';

CREATE TABLE IF NOT EXISTS `biz_approval_material` (
  `material_id` bigint NOT NULL AUTO_INCREMENT COMMENT '资料ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `material_type` varchar(50) DEFAULT NULL COMMENT '资料类型',
  `file_name` varchar(200) DEFAULT NULL COMMENT '文件名',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址',
  `file_suffix` varchar(20) DEFAULT NULL COMMENT '文件后缀',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`material_id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批资料表';

CREATE TABLE IF NOT EXISTS `biz_approval_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `project_id` bigint NOT NULL COMMENT '项目ID',
  `flow_id` bigint DEFAULT NULL COMMENT '流程ID',
  `node_name` varchar(100) DEFAULT NULL COMMENT '节点名称',
  `node_type` varchar(50) DEFAULT NULL COMMENT '节点类型',
  `action_type` varchar(50) DEFAULT NULL COMMENT '动作类型',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人',
  `opinion` varchar(1000) DEFAULT NULL COMMENT '审批意见',
  `result_status` varchar(50) DEFAULT NULL COMMENT '结果状态',
  `transfer_to` varchar(100) DEFAULT NULL COMMENT '转审人',
  `cc_users` varchar(500) DEFAULT NULL COMMENT '抄送人',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件地址',
  `operate_time` datetime DEFAULT NULL COMMENT '操作时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审批操作日志表';

INSERT INTO `biz_approval_flow` (`flow_id`, `park_id`, `flow_name`, `business_type`, `flow_version`, `node_config`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT `flow_id`, `park_id`, `flow_name`, `business_type`, `flow_version`, `node_config`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_approval_flow`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `flow_name` = VALUES(`flow_name`),
  `business_type` = VALUES(`business_type`),
  `flow_version` = VALUES(`flow_version`),
  `node_config` = VALUES(`node_config`),
  `status` = VALUES(`status`),
  `remark` = VALUES(`remark`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_approval_node` (`node_id`, `flow_id`, `node_name`, `node_order`, `node_type`, `approver_login`, `approver_name`, `complete_condition`, `time_limit`, `cc_users`, `park_id`)
SELECT `node_id`, `flow_id`, `node_name`, `node_order`, `node_type`, `approver_login`, `approver_name`, `complete_condition`, `time_limit`, `cc_users`, `park_id`
FROM `ry_ics`.`biz_approval_node`
ON DUPLICATE KEY UPDATE
  `flow_id` = VALUES(`flow_id`),
  `node_name` = VALUES(`node_name`),
  `node_order` = VALUES(`node_order`),
  `node_type` = VALUES(`node_type`),
  `approver_login` = VALUES(`approver_login`),
  `approver_name` = VALUES(`approver_name`),
  `complete_condition` = VALUES(`complete_condition`),
  `time_limit` = VALUES(`time_limit`),
  `cc_users` = VALUES(`cc_users`),
  `park_id` = VALUES(`park_id`);

INSERT INTO `biz_approval_project` (
  `project_id`, `park_id`, `customer_id`, `flow_id`, `business_id`, `business_type`, `project_name`, `enterprise_name`, `credit_code`,
  `applicant`, `applicant_dept`, `applicant_time`, `shareholder_info`, `business_scope`, `responsible_person`, `contact_phone`,
  `lease_floor`, `lease_area`, `rent_price`, `deposit`, `rent_free_period`, `lease_start_date`, `lease_end_date`, `current_node`,
  `current_node_name`, `process_status`, `approval_result`, `summary`, `approval_matter`, `approval_form_json`, `approval_form_url`,
  `archive_time`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
)
SELECT
  `project_id`, `park_id`, `customer_id`, `flow_id`, `business_id`, `business_type`, `project_name`, `enterprise_name`, `credit_code`,
  `applicant`, `applicant_dept`, `applicant_time`, `shareholder_info`, `business_scope`, `responsible_person`, `contact_phone`,
  `lease_floor`, `lease_area`, `rent_price`, `deposit`, `rent_free_period`, `lease_start_date`, `lease_end_date`, `current_node`,
  `current_node_name`, `process_status`, `approval_result`, `summary`, `approval_matter`, `approval_form_json`, `approval_form_url`,
  `archive_time`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`
FROM `ry_ics`.`biz_approval_project`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `customer_id` = VALUES(`customer_id`),
  `flow_id` = VALUES(`flow_id`),
  `business_id` = VALUES(`business_id`),
  `business_type` = VALUES(`business_type`),
  `project_name` = VALUES(`project_name`),
  `enterprise_name` = VALUES(`enterprise_name`),
  `credit_code` = VALUES(`credit_code`),
  `applicant` = VALUES(`applicant`),
  `applicant_dept` = VALUES(`applicant_dept`),
  `applicant_time` = VALUES(`applicant_time`),
  `shareholder_info` = VALUES(`shareholder_info`),
  `business_scope` = VALUES(`business_scope`),
  `responsible_person` = VALUES(`responsible_person`),
  `contact_phone` = VALUES(`contact_phone`),
  `lease_floor` = VALUES(`lease_floor`),
  `lease_area` = VALUES(`lease_area`),
  `rent_price` = VALUES(`rent_price`),
  `deposit` = VALUES(`deposit`),
  `rent_free_period` = VALUES(`rent_free_period`),
  `lease_start_date` = VALUES(`lease_start_date`),
  `lease_end_date` = VALUES(`lease_end_date`),
  `current_node` = VALUES(`current_node`),
  `current_node_name` = VALUES(`current_node_name`),
  `process_status` = VALUES(`process_status`),
  `approval_result` = VALUES(`approval_result`),
  `summary` = VALUES(`summary`),
  `approval_matter` = VALUES(`approval_matter`),
  `approval_form_json` = VALUES(`approval_form_json`),
  `approval_form_url` = VALUES(`approval_form_url`),
  `archive_time` = VALUES(`archive_time`),
  `del_flag` = VALUES(`del_flag`),
  `update_by` = VALUES(`update_by`),
  `update_time` = VALUES(`update_time`);

INSERT INTO `biz_approval_material` (`material_id`, `park_id`, `project_id`, `material_type`, `file_name`, `file_url`, `file_suffix`, `file_size`, `sort_order`, `remark`, `create_by`, `create_time`)
SELECT `material_id`, `park_id`, `project_id`, `material_type`, `file_name`, `file_url`, `file_suffix`, `file_size`, `sort_order`, `remark`, `create_by`, `create_time`
FROM `ry_ics`.`biz_approval_material`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `project_id` = VALUES(`project_id`),
  `material_type` = VALUES(`material_type`),
  `file_name` = VALUES(`file_name`),
  `file_url` = VALUES(`file_url`),
  `file_suffix` = VALUES(`file_suffix`),
  `file_size` = VALUES(`file_size`),
  `sort_order` = VALUES(`sort_order`),
  `remark` = VALUES(`remark`);

INSERT INTO `biz_approval_log` (`log_id`, `park_id`, `project_id`, `flow_id`, `node_name`, `node_type`, `action_type`, `operator_name`, `opinion`, `result_status`, `transfer_to`, `cc_users`, `attachment_url`, `operate_time`, `create_by`, `create_time`)
SELECT `log_id`, `park_id`, `project_id`, `flow_id`, `node_name`, `node_type`, `action_type`, `operator_name`, `opinion`, `result_status`, `transfer_to`, `cc_users`, `attachment_url`, `operate_time`, `create_by`, `create_time`
FROM `ry_ics`.`biz_approval_log`
ON DUPLICATE KEY UPDATE
  `park_id` = VALUES(`park_id`),
  `project_id` = VALUES(`project_id`),
  `flow_id` = VALUES(`flow_id`),
  `node_name` = VALUES(`node_name`),
  `node_type` = VALUES(`node_type`),
  `action_type` = VALUES(`action_type`),
  `operator_name` = VALUES(`operator_name`),
  `opinion` = VALUES(`opinion`),
  `result_status` = VALUES(`result_status`),
  `transfer_to` = VALUES(`transfer_to`),
  `cc_users` = VALUES(`cc_users`),
  `attachment_url` = VALUES(`attachment_url`),
  `operate_time` = VALUES(`operate_time`);

-- 业务一级菜单：逾期管理。
-- 审批中心已弃用，流程配置 / 待办 / 已办 / 抄送统一走协同办公 workflow 原生插件。
INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000006000000, 0, 'finance', '逾期管理', 'menu', '/finance', 'iconfont icon-shujukanban', '', 45, 1, 0, 1, '逾期管理', 0)
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
  (1890000000006100001, 1123598816738675201, 1890000000006000000);

UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` = 'approval_center'
   OR LEFT(`code`, 9) = 'approval_';

-- 审批模板清单（先整理为数据，再落到 biz_approval_flow / biz_approval_node / biz_approval_project / biz_approval_log）
-- 1. 入驻审批 tenant_entry：商机提交入驻审核；建议节点：经办人提交 -> 部门审批 -> 分管领导审批；关联 biz_business_opportunity.approval_project_id。
-- 2. 合同续签 contract_renewal：合同变更/续签审批；建议节点：经办人提交 -> 财务/运营审核 -> 分管领导审批；关联合同审批字段。
-- 3. 退租审批 termination：退租申请审批；建议节点：经办人提交 -> 物业核验 -> 财务结算 -> 分管领导审批；关联退租业务字段。
-- 4. 通用项目 project_common：临时项目审批兜底模板；建议节点：经办人提交 -> 部门审批；用于未拆业务模块的审批数据承接。
