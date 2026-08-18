ALTER TABLE `biz_contract`
  ADD COLUMN `rent_free_period` varchar(100) DEFAULT NULL COMMENT '免租期' AFTER `property_fee`,
  ADD COLUMN `annual_rent_2026` decimal(18,2) DEFAULT NULL COMMENT '2026年租金（合同年度费用）' AFTER `rent_free_period`,
  ADD COLUMN `annual_property_fee_2026` decimal(18,2) DEFAULT NULL COMMENT '2026年物业费（合同年度费用）' AFTER `annual_rent_2026`;
