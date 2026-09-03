-- AI 房源问答：会话和消息。数据通过 tenant_id + user_id 双重隔离。
-- 可重复执行；DeepSeek 密钥请配置在服务环境变量 DEEPSEEK_API_KEY 中。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_ai_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `tenant_id` varchar(12) NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `domain` varchar(32) NOT NULL DEFAULT 'property' COMMENT '问答领域：property/customer等',
  `title` varchar(100) NOT NULL COMMENT '会话标题',
  `last_message_time` datetime NOT NULL COMMENT '最近消息时间',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_conversation_user_time` (`tenant_id`, `user_id`, `last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI问答会话';

CREATE TABLE IF NOT EXISTS `biz_ai_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `conversation_id` bigint NOT NULL COMMENT '会话ID',
  `tenant_id` varchar(12) NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(16) NOT NULL COMMENT '消息角色：user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `domain` varchar(32) NOT NULL DEFAULT 'property' COMMENT '问答领域',
  `in_scope` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否在当前领域范围内',
  `report_id` bigint DEFAULT NULL COMMENT '关联企业报告ID',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_ai_message_conversation` (`conversation_id`, `tenant_id`, `user_id`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI问答消息';
