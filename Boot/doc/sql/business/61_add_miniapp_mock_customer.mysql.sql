-- 本地小程序联调企业客户（可重复执行）。
-- 默认绑定龙西产业园的苏州交通银行，仅用于 mock 登录，生产环境禁止启用 mock。

INSERT INTO `blade_user` (
  `id`, `tenant_id`, `user_type`, `account`, `password`, `name`, `real_name`, `phone`,
  `role_id`, `dept_id`, `post_id`, `create_user`, `create_dept`, `create_time`,
  `update_user`, `update_time`, `status`, `is_deleted`
)
SELECT
  2091500000000000101, '000000', 3, 'mini_mock_customer', SHA1(UUID()),
  '小程序企业测试用户', '企业测试用户', '13862061912',
  '1123598816738675202', '1123598813738675201', '1123598817738675201',
  1123598821738675201, 1123598813738675201, NOW(),
  1123598821738675201, NOW(), 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_user` WHERE `id` = 2091500000000000101 OR `account` = 'mini_mock_customer'
);

UPDATE `blade_user`
SET `user_type` = 3, `role_id` = '1123598816738675202',
    `dept_id` = '1123598813738675201', `post_id` = '1123598817738675201',
    `status` = 1, `is_deleted` = 0,
    `update_user` = 1123598821738675201, `update_time` = NOW()
WHERE `id` = 2091500000000000101 AND `account` = 'mini_mock_customer';

INSERT INTO `blade_user_other` (`id`, `user_id`, `user_ext`, `status`, `is_deleted`)
SELECT 2091500000000000102, 2091500000000000101, 'miniapp', 1, 0
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_user_other` WHERE `user_id` = 2091500000000000101 AND `is_deleted` = 0
);

INSERT INTO `biz_mini_member` (
  `id`, `tenant_id`, `app_id`, `open_id`, `user_id`, `customer_id`, `park_id`, `mobile`,
  `role_code`, `nickname`, `last_login_time`, `create_user`, `create_dept`, `create_time`,
  `update_user`, `update_time`, `status`, `is_deleted`
)
SELECT
  2091500000000000103, '000000', 'wx3feaa2327be80026',
  'mock-fixed-user-2091500000000000101', 2091500000000000101,
  14, 5, '13862061912', 'mini_customer_admin', '企业测试用户', NOW(),
  1123598821738675201, 1123598813738675201, NOW(),
  1123598821738675201, NOW(), 1, 0
WHERE EXISTS (
  SELECT 1 FROM `biz_customer`
  WHERE `customer_id` = 14 AND `park_id` = 5 AND `status` = '0' AND `del_flag` = '0'
)
AND NOT EXISTS (
  SELECT 1 FROM `biz_mini_member`
  WHERE (`id` = 2091500000000000103 OR `user_id` = 2091500000000000101) AND `is_deleted` = 0
);

SELECT u.`id`, u.`account`, m.`role_code`, m.`customer_id`, m.`park_id`, c.`enterprise_name`
FROM `blade_user` u
JOIN `biz_mini_member` m ON m.`user_id` = u.`id` AND m.`is_deleted` = 0
LEFT JOIN `biz_customer` c ON c.`customer_id` = m.`customer_id`
WHERE u.`id` = 2091500000000000101;
