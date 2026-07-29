-- 企业背景调查人工核验记录：采用追加记录方式保留每次修改留痕
CREATE TABLE IF NOT EXISTS `biz_background_investigation` (
  `investigation_id` bigint NOT NULL AUTO_INCREMENT COMMENT '调查记录ID',
  `opportunity_id` bigint DEFAULT NULL COMMENT '商机ID',
  `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
  `enterprise_name` varchar(200) NOT NULL COMMENT '企业名称',
  `verify_status` varchar(20) NOT NULL DEFAULT '1' COMMENT '核验状态',
  `risk_level` varchar(20) NOT NULL DEFAULT '0' COMMENT '风险等级',
  `legal_risk_flag` varchar(10) NOT NULL DEFAULT '0' COMMENT '法律风险标识',
  `executive_risk_flag` varchar(10) NOT NULL DEFAULT '0' COMMENT '高管风险标识',
  `shareholder_risk_flag` varchar(10) NOT NULL DEFAULT '0' COMMENT '股东风险标识',
  `risk_summary` varchar(1000) DEFAULT NULL COMMENT '风险摘要',
  `source_remark` varchar(500) DEFAULT NULL COMMENT '核验来源说明',
  `risk_detail_json` longtext COMMENT '人工录入风险明细JSON',
  `external_status` varchar(20) NOT NULL DEFAULT 'reserved' COMMENT '第三方接口状态',
  `create_by` varchar(64) DEFAULT NULL COMMENT '核验人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '核验时间',
  PRIMARY KEY (`investigation_id`),
  KEY `idx_background_enterprise_time` (`enterprise_name`,`create_time`),
  KEY `idx_background_opportunity` (`opportunity_id`),
  KEY `idx_background_customer` (`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业背景调查人工核验记录';
