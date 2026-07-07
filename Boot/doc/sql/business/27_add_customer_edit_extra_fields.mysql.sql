-- 客户编辑抽屉补充字段：租客类型、审批/账单/签署联系人、身份证正反面
ALTER TABLE `biz_customer`
	ADD COLUMN `tenant_type` varchar(20) NULL DEFAULT 'personal' COMMENT '租客类型：personal个人/company企业' AFTER `industry`,
	ADD COLUMN `approval_contact_name` varchar(100) NULL COMMENT '审批联系人' AFTER `contact_name`,
	ADD COLUMN `bill_contact_name` varchar(100) NULL COMMENT '账单联系人' AFTER `approval_contact_name`,
	ADD COLUMN `contract_signer` varchar(100) NULL COMMENT '合同签署人' AFTER `bill_contact_name`,
	ADD COLUMN `identity_front_url` varchar(500) NULL COMMENT '身份证人像面' AFTER `third_party_channel_name`,
	ADD COLUMN `identity_back_url` varchar(500) NULL COMMENT '身份证国徽面' AFTER `identity_front_url`;
