-- AI 企业综合信息报告：HTML 成品按租户和创建用户隔离并持久化。
CREATE TABLE IF NOT EXISTS `biz_ai_enterprise_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '创建用户ID',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `enterprise_name` varchar(200) NOT NULL COMMENT '企业名称快照',
  `title` varchar(255) NOT NULL COMMENT '报告标题',
  `request_content` varchar(500) DEFAULT NULL COMMENT '用户分析要求',
  `company_overview` text COMMENT 'AI企业概述',
  `risk_analysis` text COMMENT 'AI风险分析',
  `html_content` longtext NOT NULL COMMENT '完整HTML报告',
  `status` varchar(20) NOT NULL DEFAULT 'completed' COMMENT '生成状态',
  `generated_time` datetime NOT NULL COMMENT '生成时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_report_user_time` (`tenant_id`, `user_id`, `generated_time`),
  KEY `idx_ai_report_customer` (`tenant_id`, `customer_id`, `generated_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI企业综合信息报告';

SET @report_id_column_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_ai_message' AND COLUMN_NAME = 'report_id'
);
SET @report_id_column_sql := IF(
  @report_id_column_exists = 0,
  'ALTER TABLE `biz_ai_message` ADD COLUMN `report_id` bigint DEFAULT NULL COMMENT ''关联企业报告ID'' AFTER `in_scope`',
  'SELECT 1'
);
PREPARE report_id_column_stmt FROM @report_id_column_sql;
EXECUTE report_id_column_stmt;
DEALLOCATE PREPARE report_id_column_stmt;
