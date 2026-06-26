-- 合同到期提醒规则
-- 导入目标库：bladex_boot

CREATE TABLE IF NOT EXISTS `biz_contract_expiry_rule` (
	`rule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
	`rule_name` varchar(100) NOT NULL COMMENT '规则名称',
	`building_ids` varchar(500) NOT NULL COMMENT '关联楼宇ID集合',
	`building_names` varchar(500) NULL DEFAULT NULL COMMENT '关联楼宇名称',
	`remind_days` int NOT NULL DEFAULT 30 COMMENT '提前提醒天数',
	`remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
	`update_by` varchar(64) NULL DEFAULT NULL COMMENT '更新人',
	`update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
	PRIMARY KEY (`rule_id`),
	KEY `idx_contract_expiry_rule_name` (`rule_name`, `del_flag`),
	KEY `idx_contract_expiry_rule_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同到期提醒规则表';

UPDATE `blade_menu`
SET `path` = '/contract/expiring',
    `component` = 'views/contract/expiring',
    `remark` = '合同到期提醒规则入口',
    `is_deleted` = 0
WHERE `code` = 'contract_expiring';
