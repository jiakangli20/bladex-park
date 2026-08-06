-- 小程序水电缴纳与广告推送业务（可重复执行）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_utility_bill_detail` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `payment_id` bigint NOT NULL COMMENT '合同账单ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `customer_id` bigint NOT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `room_id` bigint NOT NULL COMMENT '房间ID',
  `device_id` bigint NOT NULL COMMENT '表计设备ID',
  `record_type` varchar(20) NOT NULL COMMENT '类型：water/electric',
  `start_record_id` bigint NOT NULL COMMENT '起始抄表记录ID',
  `end_record_id` bigint NOT NULL COMMENT '截止抄表记录ID',
  `previous_reading` decimal(18,4) NOT NULL COMMENT '起始读数',
  `current_reading` decimal(18,4) NOT NULL COMMENT '截止读数',
  `usage_amount` decimal(18,4) NOT NULL COMMENT '实际用量',
  `unit_price` decimal(18,6) NOT NULL COMMENT '计费单价',
  `amount` decimal(18,2) NOT NULL COMMENT '应缴金额',
  `period_start` date NOT NULL COMMENT '账期开始',
  `period_end` date NOT NULL COMMENT '账期结束',
  `pay_deadline` date NOT NULL COMMENT '缴费截止日期',
  `publish_status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '发布状态：DRAFT/PUBLISHED',
  `published_by` bigint DEFAULT NULL COMMENT '发布人',
  `published_time` datetime DEFAULT NULL COMMENT '发布时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_utility_bill_payment` (`payment_id`,`is_deleted`),
  UNIQUE KEY `uk_utility_bill_record_range` (`room_id`,`device_id`,`start_record_id`,`end_record_id`,`is_deleted`),
  KEY `idx_utility_bill_customer` (`tenant_id`,`park_id`,`customer_id`,`publish_status`,`period_end`),
  KEY `idx_utility_bill_contract` (`contract_id`,`record_type`,`period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='水电账单计费明细';

CREATE TABLE IF NOT EXISTS `biz_mini_payment_submission` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `payment_id` bigint NOT NULL COMMENT '合同账单ID',
  `customer_id` bigint NOT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `member_id` bigint NOT NULL COMMENT '提交成员ID',
  `submit_amount` decimal(18,2) NOT NULL COMMENT '申报付款金额',
  `voucher_name` varchar(255) NOT NULL COMMENT '凭证名称',
  `voucher_url` varchar(500) NOT NULL COMMENT '凭证地址',
  `submit_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/CONFIRMED/REJECTED',
  `audit_user_id` bigint DEFAULT NULL COMMENT '审核用户ID',
  `audit_user_name` varchar(100) DEFAULT NULL COMMENT '审核人',
  `audit_time` datetime DEFAULT NULL COMMENT '审核时间',
  `audit_opinion` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_mini_payment_bill` (`tenant_id`,`payment_id`,`create_time`),
  KEY `idx_mini_payment_customer` (`tenant_id`,`park_id`,`customer_id`,`submit_status`,`create_time`),
  KEY `idx_mini_payment_audit` (`submit_status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序线下付款凭证提交';

CREATE TABLE IF NOT EXISTS `biz_merchant_ad_audit_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `ad_id` bigint NOT NULL COMMENT '广告ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '企业客户ID',
  `action_type` varchar(32) NOT NULL COMMENT '动作类型',
  `before_audit_status` varchar(20) DEFAULT NULL COMMENT '操作前审核状态',
  `after_audit_status` varchar(20) DEFAULT NULL COMMENT '操作后审核状态',
  `before_online_status` char(1) DEFAULT NULL COMMENT '操作前上下架状态',
  `after_online_status` char(1) DEFAULT NULL COMMENT '操作后上下架状态',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人',
  `opinion` varchar(500) DEFAULT NULL COMMENT '操作意见',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_ad_audit_log_ad` (`tenant_id`,`ad_id`,`operate_time`),
  KEY `idx_ad_audit_log_customer` (`tenant_id`,`park_id`,`customer_id`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='广告审核与上下架日志';

DROP PROCEDURE IF EXISTS `add_miniapp_ad_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_miniapp_ad_column_if_missing`(
  IN p_column_name varchar(64),
  IN p_definition text
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'biz_merchant_ad'
      AND COLUMN_NAME = p_column_name
  ) THEN
    SET @column_sql = CONCAT('ALTER TABLE `biz_merchant_ad` ADD COLUMN `', p_column_name, '` ', p_definition);
    PREPARE column_stmt FROM @column_sql;
    EXECUTE column_stmt;
    DEALLOCATE PREPARE column_stmt;
  END IF;
END$$
DELIMITER ;

CALL `add_miniapp_ad_column_if_missing`('customer_id', 'bigint DEFAULT NULL COMMENT ''企业客户ID'' AFTER `park_id`');
CALL `add_miniapp_ad_column_if_missing`('member_id', 'bigint DEFAULT NULL COMMENT ''小程序提交成员ID'' AFTER `customer_id`');
CALL `add_miniapp_ad_column_if_missing`('audit_status', 'varchar(20) NOT NULL DEFAULT ''APPROVED'' COMMENT ''审核状态：DRAFT/PENDING/APPROVED/REJECTED'' AFTER `end_time`');
CALL `add_miniapp_ad_column_if_missing`('audit_user_id', 'bigint DEFAULT NULL COMMENT ''审核用户ID'' AFTER `audit_status`');
CALL `add_miniapp_ad_column_if_missing`('audit_user_name', 'varchar(100) DEFAULT NULL COMMENT ''审核人'' AFTER `audit_user_id`');
CALL `add_miniapp_ad_column_if_missing`('audit_time', 'datetime DEFAULT NULL COMMENT ''审核时间'' AFTER `audit_user_name`');
CALL `add_miniapp_ad_column_if_missing`('audit_opinion', 'varchar(500) DEFAULT NULL COMMENT ''审核意见'' AFTER `audit_time`');
DROP PROCEDURE IF EXISTS `add_miniapp_ad_column_if_missing`;

SET @has_ad_audit_index := (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_merchant_ad' AND INDEX_NAME = 'idx_merchant_ad_audit'
);
SET @add_ad_audit_index_sql := IF(@has_ad_audit_index = 0,
  'ALTER TABLE `biz_merchant_ad` ADD INDEX `idx_merchant_ad_audit` (`park_id`,`customer_id`,`audit_status`,`status`,`start_time`,`end_time`)',
  'DO 1');
PREPARE add_ad_audit_index_stmt FROM @add_ad_audit_index_sql;
EXECUTE add_ad_audit_index_stmt;
DEALLOCATE PREPARE add_ad_audit_index_stmt;

-- 存量后台广告保持原有可用状态，视为已通过审核。
UPDATE `biz_merchant_ad`
SET `audit_status` = 'APPROVED'
WHERE `audit_status` IS NULL OR `audit_status` = '';
