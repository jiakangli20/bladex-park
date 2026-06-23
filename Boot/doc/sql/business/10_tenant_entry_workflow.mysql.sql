-- tenant_entry 入驻审批 Flowable 闭环字段

ALTER TABLE `biz_business_opportunity`
	ADD COLUMN `tenant_entry_process_ins_id` varchar(64) NULL DEFAULT NULL COMMENT '入驻流程实例ID' AFTER `submitted_audit_flag`,
	ADD COLUMN `tenant_entry_status` varchar(32) NULL DEFAULT NULL COMMENT '入驻流程状态' AFTER `tenant_entry_process_ins_id`,
	ADD COLUMN `tenant_entry_current_node` varchar(100) NULL DEFAULT NULL COMMENT '入驻流程当前节点' AFTER `tenant_entry_status`,
	ADD COLUMN `tenant_entry_approval_pdf_url` varchar(500) NULL DEFAULT NULL COMMENT '入驻审批表文件地址' AFTER `tenant_entry_current_node`,
	ADD COLUMN `tenant_entry_approval_time` datetime NULL DEFAULT NULL COMMENT '入驻审批通过时间' AFTER `tenant_entry_approval_pdf_url`;

ALTER TABLE `biz_customer`
	ADD COLUMN `tenant_entry_process_ins_id` varchar(64) NULL DEFAULT NULL COMMENT '入驻流程实例ID' AFTER `settlement_status`,
	ADD COLUMN `tenant_entry_status` varchar(32) NULL DEFAULT NULL COMMENT '入驻流程状态' AFTER `tenant_entry_process_ins_id`,
	ADD COLUMN `tenant_entry_current_node` varchar(100) NULL DEFAULT NULL COMMENT '入驻流程当前节点' AFTER `tenant_entry_status`,
	ADD COLUMN `tenant_entry_approval_pdf_url` varchar(500) NULL DEFAULT NULL COMMENT '入驻审批表文件地址' AFTER `tenant_entry_current_node`,
	ADD COLUMN `tenant_entry_approval_time` datetime NULL DEFAULT NULL COMMENT '入驻审批通过时间' AFTER `tenant_entry_approval_pdf_url`;

CREATE INDEX `idx_biz_opportunity_tenant_entry_process`
	ON `biz_business_opportunity` (`tenant_entry_process_ins_id`);

CREATE INDEX `idx_biz_customer_tenant_entry_process`
	ON `biz_customer` (`tenant_entry_process_ins_id`);
