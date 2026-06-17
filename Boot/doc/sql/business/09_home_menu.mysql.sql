-- 首页一级菜单
-- 执行库：bladex_boot
-- 说明：第一阶段仅新增首页菜单和超级管理员角色授权，不接入首页业务数据。

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000009000000, 0, 'home', '首页', 'menu', '/home', 'iconfont icon-shouye', 'views/home', 1, 1, 0, 1, '园区运营首页', 0)
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

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT 1890000000009100001, 1123598816738675201, 1890000000009000000
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu`
  WHERE `role_id` = 1123598816738675201
    AND `menu_id` = 1890000000009000000
);

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `id` = 1890000000009000000;
