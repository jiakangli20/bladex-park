-- 入驻管理 - 新增背景调查独立菜单
-- 说明：页面复用客户列表与按企业名称查询背景调查接口，可重复执行。

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000003000400, 1890000000003000000, 'settlement_background_investigation', '背景调查', 'menu', '/settlement/background-investigation', 'iconfont iconicon_safety', 'views/settlement/background-investigation', 4, 1, 0, 1, '企业背景调查', 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
  `name` = VALUES(`name`),
  `alias` = VALUES(`alias`),
  `path` = VALUES(`path`),
  `source` = VALUES(`source`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `is_open` = VALUES(`is_open`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

UPDATE `blade_menu`
SET `sort` = 5
WHERE `id` = 1890000000001000100
  AND `parent_id` = 1890000000003000000;

-- 同步加入已配置“入驻管理”的顶部菜单，否则从顶部切换后左侧不会显示本菜单。
INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 1890000000004400001, settlement_top.`top_menu_id`, 1890000000003000400
FROM (
  SELECT DISTINCT `top_menu_id`
  FROM `blade_top_menu_setting`
  WHERE `menu_id` = 1890000000003000000
  ORDER BY `top_menu_id`
  LIMIT 1
) settlement_top
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_top_menu_setting` current_setting
  WHERE current_setting.`top_menu_id` = settlement_top.`top_menu_id`
    AND current_setting.`menu_id` = 1890000000003000400
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_top_menu_setting` current_id
    WHERE current_id.`id` = 1890000000004400001
  );

-- 权限范围与客户管理一致：超级管理员、人事、经理、老板。
INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.`id`, t.`role_id`, t.`menu_id`
FROM (
  SELECT 1890000000004300030 AS `id`, 1123598816738675201 AS `role_id`, 1890000000003000400 AS `menu_id`
  UNION ALL SELECT 1890000000004300031, 1123598816738675203, 1890000000003000400
  UNION ALL SELECT 1890000000004300032, 1123598816738675204, 1890000000003000400
  UNION ALL SELECT 1890000000004300033, 1123598816738675205, 1890000000003000400
) t
INNER JOIN `blade_role_menu` customer_permission
  ON customer_permission.`role_id` = t.`role_id`
 AND customer_permission.`menu_id` = 1890000000003000300
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` current_permission
  WHERE current_permission.`role_id` = t.`role_id`
    AND current_permission.`menu_id` = t.`menu_id`
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_role_menu` current_id
    WHERE current_id.`id` = t.`id`
  );

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` = 'settlement_background_investigation';

SELECT COUNT(*) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000003000400;

SELECT COUNT(*) AS `top_menu_setting_count`
FROM `blade_top_menu_setting`
WHERE `menu_id` = 1890000000003000400;
