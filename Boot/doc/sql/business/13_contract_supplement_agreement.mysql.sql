-- 合同补充协议归档表与旧合同变更审批菜单清理
-- 导入目标库：bladex_boot
-- 说明：合同变更不再走单独二级菜单审批，线下办理后在合同归档中上传补充协议。

CREATE TABLE IF NOT EXISTS `biz_contract_supplement_agreement` (
	`agreement_id` bigint NOT NULL AUTO_INCREMENT COMMENT '补充协议ID',
	`contract_id` bigint NOT NULL COMMENT '合同ID',
	`agreement_name` varchar(100) NOT NULL COMMENT '协议名称',
	`change_item` varchar(300) NULL DEFAULT NULL COMMENT '变更事项',
	`file_name` varchar(255) NULL DEFAULT NULL COMMENT '文件名称',
	`file_url` varchar(500) NOT NULL COMMENT '文件地址',
	`file_type` varchar(32) NULL DEFAULT NULL COMMENT '文件类型',
	`remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
	`update_by` varchar(64) NULL DEFAULT NULL COMMENT '更新人',
	`update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
	PRIMARY KEY (`agreement_id`),
	KEY `idx_contract_supplement_contract` (`contract_id`, `del_flag`),
	KEY `idx_contract_supplement_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同补充协议归档表';

UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` = 'contract_change_approval'
   OR `path` = '/contract/change-approval';
