-- 首次逾期多角色内部通知数据表
-- 导入目标库：bladex_boot / wzjk

CREATE TABLE IF NOT EXISTS `biz_overdue_internal_notice` (
	`notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
	`payment_id` bigint NOT NULL COMMENT '账单ID',
	`contract_id` bigint NOT NULL COMMENT '合同ID',
	`recipient_user_id` bigint NOT NULL COMMENT '接收用户ID',
	`recipient_account` varchar(64) NULL DEFAULT NULL COMMENT '接收账号',
	`recipient_name` varchar(100) NULL DEFAULT NULL COMMENT '接收人姓名',
	`recipient_roles` varchar(255) NOT NULL COMMENT '本次通知职责，多个顿号分隔',
	`notice_title` varchar(200) NOT NULL COMMENT '通知标题',
	`notice_content` varchar(1000) NOT NULL COMMENT '通知内容',
	`read_status` char(1) NOT NULL DEFAULT '0' COMMENT '已读状态：0未读 1已读',
	`read_time` datetime NULL DEFAULT NULL COMMENT '已读时间',
	`park_id` bigint NULL DEFAULT NULL COMMENT '园区ID',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NOT NULL COMMENT '创建时间',
	PRIMARY KEY (`notice_id`),
	UNIQUE KEY `uk_overdue_notice_payment_user` (`payment_id`, `recipient_user_id`),
	KEY `idx_overdue_notice_user_read` (`recipient_user_id`, `read_status`, `del_flag`),
	KEY `idx_overdue_notice_contract` (`contract_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单首次逾期内部通知表';

SELECT COUNT(*) AS overdue_internal_notice_count
FROM `biz_overdue_internal_notice`;
