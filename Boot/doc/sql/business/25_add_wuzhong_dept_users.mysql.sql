-- 新增吴中金融招商服务有限公司各部门测试用户
-- 执行库：wzjk / bladex_boot
-- 说明：为 6 个业务部门各新增 1 个用户，便于首页、权限、流程按部门/角色验收。
-- 初始密码：沿用系统参数 account.initPassword 的当前默认值加密结果；生产上线前必须改密。

SET NAMES utf8mb4;

SET @tenant_id := '000000';
SET @company_id := 2072600000000000001;
SET @role_manager := 1123598816738675204;
SET @post_staff := 1123598817738675208;
SET @password_hash := '10470c3b4b1fed12c3baac014be15fac67c6e815';
SET @admin_user := 1123598821738675201;

INSERT INTO `blade_user`
  (`id`, `tenant_id`, `code`, `user_type`, `account`, `password`, `name`, `real_name`, `avatar`, `email`, `phone`, `birthday`, `sex`, `role_id`, `dept_id`, `post_id`, `leader_id`, `is_leader`, `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`, `status`, `is_deleted`)
VALUES
  (2072600000000000101, @tenant_id, 'ZS', 1, 'zs_user', @password_hash, '招商部用户', '招商部用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000002', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0),
  (2072600000000000102, @tenant_id, 'FW', 1, 'fw_user', @password_hash, '服务部用户', '服务部用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000003', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0),
  (2072600000000000103, @tenant_id, 'ZHGL', 1, 'zhgl_user', @password_hash, '综合管理部用户', '综合管理部用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000004', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0),
  (2072600000000000104, @tenant_id, 'YYZX', 1, 'yyzx_user', @password_hash, '运营中心用户', '运营中心用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000005', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0),
  (2072600000000000105, @tenant_id, 'CW', 1, 'cw_user', @password_hash, '财务部用户', '财务部用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000006', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0),
  (2072600000000000106, @tenant_id, 'FK', 1, 'fk_user', @password_hash, '风控部用户', '风控部用户', NULL, NULL, NULL, NULL, 3, CAST(@role_manager AS CHAR), '2072600000000000007', CAST(@post_staff AS CHAR), NULL, 0, @admin_user, @company_id, NOW(), @admin_user, NOW(), 1, 0)
ON DUPLICATE KEY UPDATE
  `tenant_id` = VALUES(`tenant_id`),
  `code` = VALUES(`code`),
  `user_type` = VALUES(`user_type`),
  `account` = VALUES(`account`),
  `name` = VALUES(`name`),
  `real_name` = VALUES(`real_name`),
  `sex` = VALUES(`sex`),
  `role_id` = VALUES(`role_id`),
  `dept_id` = VALUES(`dept_id`),
  `post_id` = VALUES(`post_id`),
  `leader_id` = VALUES(`leader_id`),
  `is_leader` = VALUES(`is_leader`),
  `update_user` = VALUES(`update_user`),
  `update_time` = NOW(),
  `status` = VALUES(`status`),
  `is_deleted` = 0;

UPDATE `blade_user` u
JOIN (
  SELECT 'zs_user' AS account, 2072600000000000101 AS user_id, 'ZS' AS code, '招商部用户' AS user_name, '2072600000000000002' AS dept_id UNION ALL
  SELECT 'fw_user', 2072600000000000102, 'FW', '服务部用户', '2072600000000000003' UNION ALL
  SELECT 'zhgl_user', 2072600000000000103, 'ZHGL', '综合管理部用户', '2072600000000000004' UNION ALL
  SELECT 'yyzx_user', 2072600000000000104, 'YYZX', '运营中心用户', '2072600000000000005' UNION ALL
  SELECT 'cw_user', 2072600000000000105, 'CW', '财务部用户', '2072600000000000006' UNION ALL
  SELECT 'fk_user', 2072600000000000106, 'FK', '风控部用户', '2072600000000000007'
) seed ON seed.account = u.account
SET
  u.`id` = seed.user_id,
  u.`tenant_id` = @tenant_id,
  u.`code` = seed.code,
  u.`user_type` = 1,
  u.`name` = seed.user_name,
  u.`real_name` = seed.user_name,
  u.`sex` = 3,
  u.`role_id` = CAST(@role_manager AS CHAR),
  u.`dept_id` = seed.dept_id,
  u.`post_id` = CAST(@post_staff AS CHAR),
  u.`leader_id` = NULL,
  u.`is_leader` = 0,
  u.`update_user` = @admin_user,
  u.`update_time` = NOW(),
  u.`status` = 1,
  u.`is_deleted` = 0;

DELETE ud
FROM `blade_user_dept` ud
JOIN `blade_user` u ON u.id = ud.user_id
WHERE u.account IN ('zs_user', 'fw_user', 'zhgl_user', 'yyzx_user', 'cw_user', 'fk_user');

INSERT INTO `blade_user_dept`
  (`id`, `user_id`, `dept_id`, `status`, `is_deleted`)
VALUES
  (2072600000000000201, 2072600000000000101, 2072600000000000002, 1, 0),
  (2072600000000000202, 2072600000000000102, 2072600000000000003, 1, 0),
  (2072600000000000203, 2072600000000000103, 2072600000000000004, 1, 0),
  (2072600000000000204, 2072600000000000104, 2072600000000000005, 1, 0),
  (2072600000000000205, 2072600000000000105, 2072600000000000006, 1, 0),
  (2072600000000000206, 2072600000000000106, 2072600000000000007, 1, 0)
ON DUPLICATE KEY UPDATE
  `user_id` = VALUES(`user_id`),
  `dept_id` = VALUES(`dept_id`),
  `status` = VALUES(`status`),
  `is_deleted` = 0;

SELECT u.`account`, u.`real_name`, d.`dept_name`, r.`role_name`, p.`post_name`, u.`status`, u.`is_deleted`
FROM `blade_user` u
LEFT JOIN `blade_dept` d ON d.id = CAST(u.dept_id AS UNSIGNED)
LEFT JOIN `blade_role` r ON r.id = CAST(u.role_id AS UNSIGNED)
LEFT JOIN `blade_post` p ON p.id = CAST(u.post_id AS UNSIGNED)
WHERE u.`account` IN ('zs_user', 'fw_user', 'zhgl_user', 'yyzx_user', 'cw_user', 'fk_user')
ORDER BY d.`sort`, u.`id`;
