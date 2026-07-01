-- 财务管理一级菜单改名为“逾期管理”
-- 导入目标库：bladex_boot / wzjk
-- 说明：只修改一级菜单显示名称，不改路径、code、权限和子菜单。

UPDATE `blade_menu`
SET `name` = '逾期管理',
    `remark` = '逾期管理'
WHERE `code` = 'finance'
  AND `path` = '/finance'
  AND `parent_id` = 0;

SELECT `id`, `parent_id`, `code`, `name`, `path`, `remark`, `is_deleted`
FROM `blade_menu`
WHERE `code` = 'finance'
   OR `path` = '/finance';

