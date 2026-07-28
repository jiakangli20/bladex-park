-- 逾期管理 - 新增可重复追溯的催缴记录
-- 导入目标库：bladex_boot / wzjk

CREATE TABLE IF NOT EXISTS `biz_overdue_reminder_record` (
	`record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '催缴记录ID',
	`payment_id` bigint NOT NULL COMMENT '账单ID',
	`contract_id` bigint NOT NULL COMMENT '合同ID',
	`operator_user_id` bigint NOT NULL COMMENT '操作用户ID',
	`operator_account` varchar(64) NULL DEFAULT NULL COMMENT '操作账号',
	`operator_name` varchar(100) NULL DEFAULT NULL COMMENT '操作人姓名',
	`source` varchar(32) NOT NULL DEFAULT 'bill_management' COMMENT '催缴入口',
	`remind_time` datetime NOT NULL COMMENT '催缴时间',
	`park_id` bigint NULL DEFAULT NULL COMMENT '园区ID',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NOT NULL COMMENT '创建时间',
	PRIMARY KEY (`record_id`),
	KEY `idx_overdue_reminder_operator_time` (`operator_user_id`, `remind_time`, `del_flag`),
	KEY `idx_overdue_reminder_payment` (`payment_id`, `del_flag`),
	KEY `idx_overdue_reminder_contract` (`contract_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逾期催缴操作记录表';

SELECT COUNT(*) AS overdue_reminder_record_count
FROM `biz_overdue_reminder_record`;
