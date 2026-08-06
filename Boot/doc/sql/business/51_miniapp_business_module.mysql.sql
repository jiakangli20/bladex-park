-- 园区微信小程序业务基础表（可重复执行）
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_mini_member` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `app_id` varchar(64) NOT NULL COMMENT '微信小程序AppID',
  `open_id` varchar(128) NOT NULL COMMENT '微信OpenID',
  `union_id` varchar(128) DEFAULT NULL COMMENT '微信UnionID',
  `user_id` bigint DEFAULT NULL COMMENT 'BladeX用户ID',
  `customer_id` bigint DEFAULT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `mobile` varchar(32) DEFAULT NULL COMMENT '绑定手机号',
  `role_code` varchar(64) NOT NULL COMMENT '小程序角色',
  `nickname` varchar(100) DEFAULT NULL COMMENT '显示名称',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_member_app_open` (`app_id`,`open_id`,`is_deleted`),
  KEY `idx_mini_member_user` (`tenant_id`,`user_id`),
  KEY `idx_mini_member_customer` (`tenant_id`,`park_id`,`customer_id`),
  KEY `idx_mini_member_mobile` (`tenant_id`,`mobile`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序成员绑定';

CREATE TABLE IF NOT EXISTS `biz_mini_invite` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `code_hash` varchar(128) NOT NULL COMMENT '邀请码哈希',
  `customer_id` bigint NOT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `role_code` varchar(64) NOT NULL COMMENT '邀请角色',
  `mobile` varchar(32) DEFAULT NULL COMMENT '限定手机号',
  `expire_time` datetime NOT NULL COMMENT '失效时间',
  `max_uses` int NOT NULL DEFAULT 1 COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_invite_hash` (`tenant_id`,`code_hash`,`is_deleted`),
  KEY `idx_mini_invite_customer` (`tenant_id`,`park_id`,`customer_id`),
  KEY `idx_mini_invite_expire` (`expire_time`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序企业邀请码';

CREATE TABLE IF NOT EXISTS `biz_mini_notification` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `recipient_user_id` bigint DEFAULT NULL COMMENT '接收用户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '企业客户ID',
  `notice_type` varchar(64) NOT NULL COMMENT '通知类型',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` varchar(1000) DEFAULT NULL COMMENT '内容',
  `target_type` varchar(64) NOT NULL COMMENT '目标业务类型',
  `target_id` varchar(64) NOT NULL COMMENT '目标业务ID',
  `read_status` int NOT NULL DEFAULT 0 COMMENT '已读状态',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_mini_notice_recipient` (`tenant_id`,`recipient_user_id`,`read_status`,`create_time`),
  KEY `idx_mini_notice_park` (`tenant_id`,`park_id`,`notice_type`,`create_time`),
  KEY `idx_mini_notice_target` (`target_type`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序站内通知';

CREATE TABLE IF NOT EXISTS `biz_house_appointment` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `appointment_no` varchar(40) NOT NULL COMMENT '预约编号',
  `member_id` bigint DEFAULT NULL COMMENT '小程序成员ID',
  `customer_id` bigint DEFAULT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `enterprise_name` varchar(200) NOT NULL COMMENT '企业名称',
  `contact_name` varchar(100) NOT NULL COMMENT '联系人',
  `contact_phone` varchar(32) NOT NULL COMMENT '联系电话',
  `preferred_time` datetime DEFAULT NULL COMMENT '意向时间',
  `demand_desc` varchar(1000) DEFAULT NULL COMMENT '需求说明',
  `appointment_status` varchar(32) NOT NULL DEFAULT 'PENDING' COMMENT '预约状态',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_house_appointment_no` (`appointment_no`),
  KEY `idx_house_appointment_customer` (`tenant_id`,`park_id`,`customer_id`,`create_time`),
  KEY `idx_house_appointment_room` (`park_id`,`room_id`,`appointment_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序看房预约';

CREATE TABLE IF NOT EXISTS `biz_park_activity` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `title` varchar(200) NOT NULL COMMENT '活动标题',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面地址',
  `summary` varchar(1000) DEFAULT NULL COMMENT '活动摘要',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `address` varchar(500) DEFAULT NULL COMMENT '活动地址',
  `price_text` varchar(100) DEFAULT NULL COMMENT '价格文案',
  `publish_status` int NOT NULL DEFAULT 0 COMMENT '发布状态',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_park_activity_publish` (`tenant_id`,`park_id`,`publish_status`,`sort_order`,`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='园区活动';

SET @has_source_room_id := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_business_opportunity' AND COLUMN_NAME = 'source_room_id'
);
SET @add_source_room_id_sql := IF(@has_source_room_id = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `source_room_id` bigint DEFAULT NULL COMMENT ''小程序来源房源ID'' AFTER `park_id`',
  'DO 1');
PREPARE add_source_room_id_stmt FROM @add_source_room_id_sql;
EXECUTE add_source_room_id_stmt;
DEALLOCATE PREPARE add_source_room_id_stmt;

SET @has_enterprise_scale := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_business_opportunity' AND COLUMN_NAME = 'enterprise_scale'
);
SET @add_enterprise_scale_sql := IF(@has_enterprise_scale = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `enterprise_scale` varchar(64) DEFAULT NULL COMMENT ''企业规模'' AFTER `industry_type`',
  'DO 1');
PREPARE add_enterprise_scale_stmt FROM @add_enterprise_scale_sql;
EXECUTE add_enterprise_scale_stmt;
DEALLOCATE PREPARE add_enterprise_scale_stmt;

SET @has_expected_entry_date := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'biz_business_opportunity' AND COLUMN_NAME = 'expected_entry_date'
);
SET @add_expected_entry_date_sql := IF(@has_expected_entry_date = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `expected_entry_date` date DEFAULT NULL COMMENT ''预计入驻日期'' AFTER `intent_area`',
  'DO 1');
PREPARE add_expected_entry_date_stmt FROM @add_expected_entry_date_sql;
EXECUTE add_expected_entry_date_stmt;
DEALLOCATE PREPARE add_expected_entry_date_stmt;

