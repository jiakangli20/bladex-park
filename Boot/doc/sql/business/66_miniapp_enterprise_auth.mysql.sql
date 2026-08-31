-- 小程序游客认证、企业/园区成员关系与员工加入申请
-- 可重复执行，目标库：bladex_boot / wzjk
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_enterprise_subject` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000',
  `enterprise_name` varchar(200) NOT NULL,
  `enterprise_name_norm` varchar(200) NOT NULL,
  `credit_code` varchar(32) DEFAULT NULL,
  `enterprise_type` varchar(32) DEFAULT NULL,
  `legal_representative` varchar(100) DEFAULT NULL,
  `registered_capital` decimal(12,2) DEFAULT NULL,
  `contact_name` varchar(64) NOT NULL,
  `contact_phone` varchar(32) NOT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `customer_id` bigint DEFAULT NULL,
  `owner_user_id` bigint DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `create_user` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_subject_name` (`tenant_id`,`enterprise_name_norm`,`is_deleted`),
  KEY `idx_enterprise_subject_customer` (`tenant_id`,`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业主体';

CREATE TABLE IF NOT EXISTS `biz_mini_enterprise_certification` (
  `id` bigint NOT NULL,
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000',
  `applicant_user_id` bigint NOT NULL,
  `enterprise_subject_id` bigint DEFAULT NULL,
  `application_type` varchar(32) NOT NULL DEFAULT 'CERTIFICATION',
  `subject_type` varchar(32) NOT NULL DEFAULT 'ENTERPRISE',
  `enterprise_name` varchar(200) NOT NULL,
  `credit_code` varchar(32) DEFAULT NULL,
  `legal_representative` varchar(100) DEFAULT NULL,
  `registered_capital` decimal(12,2) DEFAULT NULL,
  `contact_name` varchar(64) NOT NULL,
  `contact_phone` varchar(32) NOT NULL,
  `contact_email` varchar(128) DEFAULT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `review_user_id` bigint DEFAULT NULL, `review_time` datetime DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_cert_status` (`tenant_id`,`status`,`application_type`),
  KEY `idx_cert_applicant` (`tenant_id`,`applicant_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序企业认证及新增园区申请';

-- 兼容已执行过旧版本脚本的数据库。
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_enterprise_subject' AND column_name='legal_representative');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_enterprise_subject ADD COLUMN legal_representative varchar(100) NULL COMMENT ''法定代表人'' AFTER enterprise_type', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_enterprise_subject' AND column_name='registered_capital');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_enterprise_subject ADD COLUMN registered_capital decimal(12,2) NULL COMMENT ''注册资本（万）'' AFTER legal_representative', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_certification' AND column_name='subject_type');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_enterprise_certification ADD COLUMN subject_type varchar(32) NOT NULL DEFAULT ''ENTERPRISE'' COMMENT ''认证主体类型'' AFTER application_type', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_certification' AND column_name='legal_representative');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_enterprise_certification ADD COLUMN legal_representative varchar(100) NULL COMMENT ''法定代表人'' AFTER credit_code', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_certification' AND column_name='registered_capital');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_enterprise_certification ADD COLUMN registered_capital decimal(12,2) NULL COMMENT ''注册资本（万）'' AFTER legal_representative', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS `biz_mini_enterprise_certification_park` (
  `id` bigint NOT NULL, `certification_id` bigint NOT NULL, `tenant_id` varchar(12) NOT NULL DEFAULT '000000',
  `park_id` bigint NOT NULL, `status` varchar(32) NOT NULL DEFAULT 'PENDING',
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_cert_park` (`certification_id`,`park_id`), KEY `idx_cert_park_status` (`park_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='认证申请园区';

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_certification_park' AND column_name='is_deleted');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_enterprise_certification_park ADD COLUMN is_deleted int NOT NULL DEFAULT 0 COMMENT ''删除标记'' AFTER status', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS `biz_mini_enterprise_invite` (
  `id` bigint NOT NULL, `tenant_id` varchar(12) NOT NULL DEFAULT '000000', `enterprise_subject_id` bigint NOT NULL,
  `customer_id` bigint DEFAULT NULL, `park_id` bigint NOT NULL, `invite_code` varchar(32) DEFAULT NULL, `code_hash` varchar(128) NOT NULL,
  `expire_time` datetime NOT NULL, `max_uses` int NOT NULL DEFAULT 1, `used_count` int NOT NULL DEFAULT 0,
  `status` varchar(16) NOT NULL DEFAULT 'ACTIVE', `create_user` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL, `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), UNIQUE KEY `uk_invite_subject_park` (`tenant_id`,`enterprise_subject_id`,`park_id`,`is_deleted`),
  KEY `idx_invite_hash` (`code_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业园区邀请码';

SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_invite' AND column_name='invite_code');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_enterprise_invite ADD COLUMN invite_code varchar(32) NULL COMMENT ''邀请码明文（安全审查留存）'' AFTER park_id', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

CREATE TABLE IF NOT EXISTS `biz_mini_enterprise_join_application` (
  `id` bigint NOT NULL, `tenant_id` varchar(12) NOT NULL DEFAULT '000000', `applicant_user_id` bigint NOT NULL,
  `enterprise_subject_id` bigint NOT NULL, `customer_id` bigint DEFAULT NULL, `park_id` bigint NOT NULL, `invite_id` bigint NOT NULL,
  `name` varchar(64) NOT NULL, `mobile` varchar(32) NOT NULL, `email` varchar(128) NOT NULL,
  `id_type` varchar(32) NOT NULL, `id_no` varchar(64) NOT NULL, `birth_date` date NOT NULL, `gender` varchar(16) NOT NULL,
  `status` varchar(32) NOT NULL DEFAULT 'PENDING', `review_user_id` bigint DEFAULT NULL, `review_time` datetime DEFAULT NULL,
  `review_remark` varchar(500) DEFAULT NULL, `create_time` datetime DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`), KEY `idx_join_status` (`tenant_id`,`applicant_user_id`,`enterprise_subject_id`,`park_id`,`status`),
  KEY `idx_join_owner` (`tenant_id`,`enterprise_subject_id`,`park_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工加入企业申请';

-- 待审核唯一性由事务内业务校验保证；历史驳回/通过记录必须允许保留多条。
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_join_application' AND index_name='uk_join_pending');
SET @sql := IF(@idx_exists > 0, 'ALTER TABLE biz_mini_enterprise_join_application DROP INDEX uk_join_pending', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_mini_enterprise_join_application' AND index_name='idx_join_status');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE biz_mini_enterprise_join_application ADD KEY idx_join_status (tenant_id,applicant_user_id,enterprise_subject_id,park_id,status)', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 多企业多园区关系：移除旧的单成员唯一约束，建立有效关系维度约束。
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_mini_customer_member' AND index_name='uk_mini_customer_member_active');
SET @sql := IF(@idx_exists > 0, 'ALTER TABLE biz_mini_customer_member DROP INDEX uk_mini_customer_member_active', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col_exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_mini_customer_member' AND column_name='enterprise_subject_id');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE biz_mini_customer_member ADD COLUMN enterprise_subject_id bigint NULL COMMENT ''企业主体ID'' AFTER customer_id', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
-- 旧库可能仍保留 mini_customer_member 默认值；新关系统一使用 OWNER/MEMBER。
ALTER TABLE `biz_mini_customer_member`
  MODIFY COLUMN `role_code` varchar(64) NOT NULL DEFAULT 'MEMBER' COMMENT '企业成员角色：OWNER/MEMBER';
SET @uk_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_mini_customer_member' AND index_name='uk_mini_customer_member_scope');
SET @sql := IF(@uk_exists = 0, 'ALTER TABLE biz_mini_customer_member ADD UNIQUE KEY uk_mini_customer_member_scope (member_id,enterprise_subject_id,park_id,is_deleted)', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @idx_exists := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='biz_mini_customer_member' AND index_name='idx_mini_customer_member_subject');
SET @sql := IF(@idx_exists = 0, 'ALTER TABLE biz_mini_customer_member ADD KEY idx_mini_customer_member_subject (tenant_id,enterprise_subject_id,park_id,status)', 'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 企业管理员和企业员工必须同时归属“园区企业”主部门及用户部门关系。
SET @park_enterprise_dept_id := (
  SELECT `id` FROM `blade_dept`
  WHERE `tenant_id` = '000000' AND `dept_name` = '园区企业' AND `status` = 1 AND `is_deleted` = 0
  ORDER BY `id` LIMIT 1
);

UPDATE `blade_user` u
JOIN `biz_mini_customer_member` r
  ON r.`user_id` = u.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET u.`dept_id` = CAST(@park_enterprise_dept_id AS CHAR), u.`update_time` = NOW()
WHERE @park_enterprise_dept_id IS NOT NULL AND u.`is_deleted` = 0;

UPDATE `blade_user_dept` ud
JOIN `biz_mini_customer_member` r
  ON r.`user_id` = ud.`user_id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET ud.`is_deleted` = 1
WHERE @park_enterprise_dept_id IS NOT NULL AND ud.`is_deleted` = 0 AND ud.`dept_id` <> @park_enterprise_dept_id;

UPDATE `blade_user_dept` ud
JOIN `biz_mini_customer_member` r
  ON r.`user_id` = ud.`user_id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET ud.`status` = 1, ud.`is_deleted` = 0
WHERE @park_enterprise_dept_id IS NOT NULL AND ud.`dept_id` = @park_enterprise_dept_id;

INSERT INTO `blade_user_dept` (`id`, `user_id`, `dept_id`, `status`, `is_deleted`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('mini-park-enterprise-dept:', r.`user_id`)), 1, 15), 16, 10) AS UNSIGNED),
       r.`user_id`, @park_enterprise_dept_id, 1, 0
FROM `biz_mini_customer_member` r
JOIN `blade_user` u ON u.`id` = r.`user_id` AND u.`is_deleted` = 0
WHERE @park_enterprise_dept_id IS NOT NULL AND r.`status` = 1 AND r.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_user_dept` ud
    WHERE ud.`user_id` = r.`user_id` AND ud.`dept_id` = @park_enterprise_dept_id AND ud.`is_deleted` = 0
  )
GROUP BY r.`user_id`;

INSERT INTO `blade_menu` (`id`,`parent_id`,`code`,`name`,`alias`,`path`,`source`,`component`,`sort`,`category`,`action`,`is_open`,`remark`,`is_deleted`)
VALUES (2095000000007000300,1890000000007000000,'enterprise_auth','企业认证审核','menu','/enterprise/enterprise-auth','iconfont iconicon_audit','views/enterprise/enterprise-auth',30,1,0,1,'企业认证及新增园区申请审核',0)
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`),`path`=VALUES(`path`),`component`=VALUES(`component`),`is_deleted`=0;
INSERT IGNORE INTO `blade_role_menu` (`id`,`role_id`,`menu_id`) SELECT 2095000000008000300,id,2095000000007000300 FROM blade_role WHERE role_alias IN ('administrator','admin') AND is_deleted=0;

-- 顶部“企业服务”会按 blade_top_menu_setting 二次过滤左侧菜单，必须同步建立映射。
INSERT INTO `blade_top_menu_setting` (`id`,`top_menu_id`,`menu_id`)
SELECT 2095000000008000400,t.id,2095000000007000300
FROM `blade_top_menu` t
WHERE t.code='service' AND t.is_deleted=0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_top_menu_setting` s
    WHERE s.top_menu_id=t.id AND s.menu_id=2095000000007000300
  )
  AND NOT EXISTS (
    SELECT 1 FROM `blade_top_menu_setting` s WHERE s.id=2095000000008000400
  );
