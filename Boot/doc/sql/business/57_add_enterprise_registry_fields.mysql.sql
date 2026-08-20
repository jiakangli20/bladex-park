-- 客户与商机统一工商接口字段。
ALTER TABLE `biz_customer`
  ADD COLUMN `legal_representative` varchar(100) DEFAULT NULL COMMENT '法定代表人' AFTER `credit_code`,
  ADD COLUMN `enterprise_phone` varchar(100) DEFAULT NULL COMMENT '企业联系电话' AFTER `legal_representative`,
  ADD COLUMN `business_term` varchar(200) DEFAULT NULL COMMENT '营业期限' AFTER `enterprise_type`,
  ADD COLUMN `operating_status` varchar(50) DEFAULT NULL COMMENT '经营状态' AFTER `business_term`;

ALTER TABLE `biz_business_opportunity`
  ADD COLUMN `legal_representative` varchar(100) DEFAULT NULL COMMENT '法定代表人' AFTER `credit_code`,
  ADD COLUMN `enterprise_phone` varchar(100) DEFAULT NULL COMMENT '企业联系电话' AFTER `legal_representative`,
  ADD COLUMN `business_term` varchar(200) DEFAULT NULL COMMENT '营业期限' AFTER `enterprise_type`,
  ADD COLUMN `operating_status` varchar(50) DEFAULT NULL COMMENT '经营状态' AFTER `business_term`;
