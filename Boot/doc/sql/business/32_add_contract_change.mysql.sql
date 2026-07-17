-- 合同变更登记与归档记录
-- 导入目标库：bladex_boot / wzjk

CREATE TABLE IF NOT EXISTS `biz_contract_change` (
	`change_id` bigint NOT NULL AUTO_INCREMENT COMMENT '变更ID',
	`change_no` varchar(64) NOT NULL COMMENT '变更单号',
	`contract_id` bigint NOT NULL COMMENT '合同ID',
	`contract_no` varchar(64) NOT NULL COMMENT '合同编号',
	`contract_name` varchar(200) NULL DEFAULT NULL COMMENT '合同名称',
	`customer_name` varchar(200) NULL DEFAULT NULL COMMENT '客户名称',
	`change_type` varchar(32) NOT NULL COMMENT '变更类型',
	`old_rent_price` decimal(12,2) NULL DEFAULT NULL COMMENT '原租金单价（元/平方米/月）',
	`new_rent_price` decimal(12,2) NULL DEFAULT NULL COMMENT '新租金单价（元/平方米/月）',
	`old_monthly_rent` decimal(12,2) NULL DEFAULT NULL COMMENT '原月租金（元）',
	`new_monthly_rent` decimal(12,2) NULL DEFAULT NULL COMMENT '新月租金（元）',
	`old_end_date` date NULL DEFAULT NULL COMMENT '原合同结束日期',
	`new_end_date` date NULL DEFAULT NULL COMMENT '新合同结束日期',
	`reason` varchar(1000) NOT NULL COMMENT '变更原因',
	`approval_status` varchar(32) NOT NULL DEFAULT 'completed' COMMENT '生效状态：completed已生效',
	`approval_opinion` varchar(500) NULL DEFAULT NULL COMMENT '生效说明',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常 1删除',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NOT NULL COMMENT '创建时间',
	`update_by` varchar(64) NULL DEFAULT NULL COMMENT '更新人',
	`update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
	PRIMARY KEY (`change_id`),
	UNIQUE KEY `uk_contract_change_no` (`change_no`),
	KEY `idx_contract_change_contract` (`contract_id`, `del_flag`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同变更登记表';

SELECT COUNT(*) AS contract_change_count
FROM `biz_contract_change`;
