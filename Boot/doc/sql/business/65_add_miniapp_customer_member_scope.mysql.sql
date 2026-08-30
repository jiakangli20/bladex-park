-- 小程序游客身份与企业成员关系（可重复执行）。
SET NAMES utf8mb4;

SET @guest_dept_seed_id := 2093000000000000101;
SET @guest_role_seed_id := 2093000000000000102;
SET @miniapp_post_seed_id := 2093000000000000103;

INSERT INTO `blade_dept`
  (`id`, `tenant_id`, `parent_id`, `ancestors`, `dept_category`, `dept_name`, `full_name`, `sort`, `remark`, `status`, `is_deleted`)
SELECT
  @guest_dept_seed_id, '000000', 0, '0', 1, '游客', '游客', 90,
  '已登录但尚未加入企业的小程序账号', 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_dept`
  WHERE `tenant_id` = '000000' AND `dept_name` = '游客' AND `is_deleted` = 0
);

SET @guest_dept_id := (
  SELECT `id` FROM `blade_dept`
  WHERE `tenant_id` = '000000' AND `dept_name` = '游客' AND `is_deleted` = 0
  ORDER BY `id` LIMIT 1
);

UPDATE `blade_dept`
SET `parent_id` = 0, `ancestors` = '0', `dept_category` = 1,
    `full_name` = '游客', `remark` = '已登录但尚未加入企业的小程序账号',
    `status` = 1, `is_deleted` = 0
WHERE `id` = @guest_dept_id;

INSERT INTO `blade_role`
  (`id`, `tenant_id`, `parent_id`, `role_name`, `sort`, `role_alias`, `status`, `is_deleted`)
SELECT
  @guest_role_seed_id, '000000', 0, '小程序游客', 90, 'mini_guest', 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_role`
  WHERE `tenant_id` = '000000' AND `role_alias` = 'mini_guest' AND `is_deleted` = 0
);

SET @guest_role_id := (
  SELECT `id` FROM `blade_role`
  WHERE `tenant_id` = '000000' AND `role_alias` = 'mini_guest' AND `is_deleted` = 0
  ORDER BY `id` LIMIT 1
);

UPDATE `blade_role`
SET `role_name` = '小程序游客', `parent_id` = 0, `sort` = 90,
    `status` = 1, `is_deleted` = 0
WHERE `id` = @guest_role_id;

-- 游客角色必须保持零菜单权限，不能用于登录 Web 后台。
DELETE FROM `blade_role_menu` WHERE `role_id` = @guest_role_id;

INSERT INTO `blade_post`
  (`id`, `tenant_id`, `category`, `post_code`, `post_name`, `sort`, `remark`, `create_time`, `status`, `is_deleted`)
SELECT
  @miniapp_post_seed_id, '000000', 1, 'miniapp_user', '小程序用户', 90,
  '仅用于用户管理归类，不授予后台岗位权限', NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_post`
  WHERE `tenant_id` = '000000' AND `post_code` = 'miniapp_user' AND `is_deleted` = 0
);

SET @miniapp_post_id := (
  SELECT `id` FROM `blade_post`
  WHERE `tenant_id` = '000000' AND `post_code` = 'miniapp_user' AND `is_deleted` = 0
  ORDER BY `id` LIMIT 1
);

UPDATE `blade_post`
SET `post_name` = '小程序用户', `category` = 1, `sort` = 90,
    `remark` = '仅用于用户管理归类，不授予后台岗位权限',
    `status` = 1, `is_deleted` = 0
WHERE `id` = @miniapp_post_id;

CREATE TABLE IF NOT EXISTS `biz_mini_customer_member` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `member_id` bigint NOT NULL COMMENT '小程序成员ID',
  `user_id` bigint NOT NULL COMMENT 'BladeX用户ID',
  `customer_id` bigint NOT NULL COMMENT '企业客户ID',
  `park_id` bigint NOT NULL COMMENT '企业所属园区ID',
  `role_code` varchar(64) NOT NULL DEFAULT 'mini_customer_member' COMMENT '小程序企业角色',
  `mobile` varchar(32) DEFAULT NULL COMMENT '加入时手机号快照',
  `join_source` varchar(32) NOT NULL DEFAULT 'ADMIN' COMMENT '加入来源',
  `certification_id` bigint DEFAULT NULL COMMENT '企业认证申请ID',
  `invite_id` bigint DEFAULT NULL COMMENT '企业邀请码ID',
  `join_time` datetime NOT NULL COMMENT '加入时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `create_dept` bigint DEFAULT NULL COMMENT '创建部门',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `status` int NOT NULL DEFAULT 1 COMMENT '状态',
  `is_deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mini_customer_member_active` (`member_id`, `is_deleted`),
  KEY `idx_mini_customer_member_customer` (`tenant_id`, `customer_id`, `status`),
  KEY `idx_mini_customer_member_user` (`tenant_id`, `user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='小程序企业成员关系';

-- 历史 customer_id 仅用于一次性回填，后续身份只认本关系表。
INSERT IGNORE INTO `biz_mini_customer_member` (
  `id`, `tenant_id`, `member_id`, `user_id`, `customer_id`, `park_id`, `role_code`,
  `mobile`, `join_source`, `join_time`, `create_user`, `create_dept`, `create_time`,
  `update_user`, `update_time`, `status`, `is_deleted`
)
SELECT
  m.`id`, m.`tenant_id`, m.`id`, m.`user_id`, m.`customer_id`, m.`park_id`,
  'mini_customer_member', m.`mobile`, 'ADMIN', COALESCE(m.`create_time`, NOW()),
  m.`create_user`, m.`create_dept`, COALESCE(m.`create_time`, NOW()),
  m.`update_user`, NOW(), 1, 0
FROM `biz_mini_member` m
JOIN `biz_customer` c
  ON c.`customer_id` = m.`customer_id` AND c.`status` = '0' AND c.`del_flag` = '0'
WHERE m.`customer_id` IS NOT NULL AND m.`user_id` IS NOT NULL AND m.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `biz_mini_customer_member` existing
    WHERE existing.`member_id` = m.`id`
  );

-- 历史企业管理员在小程序中按普通企业用户处理。
UPDATE `biz_mini_member` m
JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET m.`customer_id` = r.`customer_id`, m.`park_id` = r.`park_id`,
    m.`role_code` = 'mini_customer_member', m.`update_time` = NOW()
WHERE m.`is_deleted` = 0;

-- 没有企业关系的外部账号统一归入游客。
UPDATE `blade_user` u
JOIN `biz_mini_member` m ON m.`user_id` = u.`id` AND m.`is_deleted` = 0
LEFT JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET u.`role_id` = CAST(@guest_role_id AS CHAR),
    u.`dept_id` = CAST(@guest_dept_id AS CHAR),
    u.`post_id` = CAST(@miniapp_post_id AS CHAR),
    u.`update_time` = NOW()
WHERE r.`id` IS NULL AND u.`user_type` = 3 AND u.`is_deleted` = 0;

UPDATE `biz_mini_member` m
LEFT JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET m.`customer_id` = NULL, m.`role_code` = 'mini_user', m.`update_time` = NOW()
WHERE r.`id` IS NULL AND m.`is_deleted` = 0;

UPDATE `blade_user_dept` ud
JOIN `blade_user` u ON u.`id` = ud.`user_id`
JOIN `biz_mini_member` m ON m.`user_id` = u.`id` AND m.`is_deleted` = 0
LEFT JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET ud.`is_deleted` = 1
WHERE r.`id` IS NULL AND u.`user_type` = 3 AND ud.`dept_id` <> @guest_dept_id;

UPDATE `blade_user_dept` ud
JOIN `blade_user` u ON u.`id` = ud.`user_id`
JOIN `biz_mini_member` m ON m.`user_id` = u.`id` AND m.`is_deleted` = 0
LEFT JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
SET ud.`status` = 1, ud.`is_deleted` = 0
WHERE r.`id` IS NULL AND u.`user_type` = 3 AND ud.`dept_id` = @guest_dept_id;

INSERT INTO `blade_user_dept` (`id`, `user_id`, `dept_id`, `status`, `is_deleted`)
SELECT CAST(CONV(SUBSTRING(MD5(CONCAT('mini-guest-dept:', u.`id`)), 1, 15), 16, 10) AS UNSIGNED),
       u.`id`, @guest_dept_id, 1, 0
FROM `blade_user` u
JOIN `biz_mini_member` m ON m.`user_id` = u.`id` AND m.`is_deleted` = 0
LEFT JOIN `biz_mini_customer_member` r
  ON r.`member_id` = m.`id` AND r.`status` = 1 AND r.`is_deleted` = 0
WHERE r.`id` IS NULL AND u.`user_type` = 3 AND u.`is_deleted` = 0
  AND NOT EXISTS (
    SELECT 1 FROM `blade_user_dept` ud
    WHERE ud.`user_id` = u.`id` AND ud.`dept_id` = @guest_dept_id AND ud.`is_deleted` = 0
  );
