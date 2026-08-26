CREATE TABLE IF NOT EXISTS `blade_user_park` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL COMMENT '租户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `is_default` tinyint NOT NULL DEFAULT 0 COMMENT '是否默认园区',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user_park` (`tenant_id`, `user_id`, `park_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_park_id` (`park_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户园区授权关联表';
