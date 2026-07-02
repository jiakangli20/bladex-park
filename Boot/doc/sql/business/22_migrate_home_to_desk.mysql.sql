-- 首页迁移至 BladeX 原工作台菜单
-- 执行库：wzjk
-- 说明：将原工作台一级菜单改为“首页”，由 /desk 承载工作台首页页面，并停用早期单独新增的 /home 菜单。

SET NAMES utf8mb4;

UPDATE `blade_role_menu` old_rm
LEFT JOIN `blade_role_menu` desk_rm
  ON desk_rm.`role_id` = old_rm.`role_id`
  AND desk_rm.`menu_id` = 1123598815738675201
SET old_rm.`menu_id` = 1123598815738675201
WHERE old_rm.`menu_id` = 1890000000009000000
  AND desk_rm.`id` IS NULL;

DELETE rm
FROM `blade_role_menu` rm
WHERE rm.`menu_id` = 1890000000009000000;

UPDATE `blade_menu`
SET
  `name` = '首页',
  `alias` = 'menu',
  `path` = '/desk',
  `source` = 'iconfont icon-shouye',
  `component` = 'views/desk',
  `sort` = 1,
  `category` = 1,
  `action` = 0,
  `is_open` = 1,
  `remark` = '园区运营首页',
  `is_deleted` = 0
WHERE `id` = 1123598815738675201
   OR `code` = 'desk';

UPDATE `blade_menu`
SET
  `is_deleted` = 1,
  `remark` = '已迁移至工作台 /desk'
WHERE `id` = 1890000000009000000
   OR `code` = 'home'
   OR `path` = '/home';

UPDATE `blade_top_menu`
SET
  `name` = '首页',
  `path` = '/desk',
  `source` = 'iconfont icon-shouye',
  `is_main` = 1,
  `status` = 1,
  `is_deleted` = 0
WHERE `code` = 'desk'
   OR `path` = '/desk'
   OR `name` = '工作台';

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('desk', 'home')
   OR `path` IN ('/desk', '/home')
ORDER BY `sort`, `id`;
