-- 合同管理占位菜单
-- 导入目标库：bladex_boot
-- 说明：只新增/修正菜单入口，不修改业务表结构。

INSERT INTO `blade_menu`
(`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
VALUES
(2007600000000000004, 2007600000000000001, 'contract_expiring', '合同到期提醒', 'menu', '/contract/expiring', 'iconfont icon-time', 3, 1, 0, 1, 'views/contract/expiring', '合同到期提醒入口', 0),
(2007600000000000008, 2007600000000000001, 'contract_print_template', '合同打印模板', 'menu', '/contract/print-template', 'iconfont icon-printer', 4, 1, 0, 1, 'views/contract/print-template', '合同打印模板入口', 0),
(2007600000000000005, 2007600000000000001, 'contract_archive', '合同归档', 'menu', '/contract/archive', 'iconfont iconicon_doc', 5, 1, 0, 1, 'views/contract/archive', '合同档案聚合查看', 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
  `name` = VALUES(`name`),
  `alias` = VALUES(`alias`),
  `path` = VALUES(`path`),
  `source` = VALUES(`source`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `is_open` = VALUES(`is_open`),
  `component` = VALUES(`component`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT t.`id`, t.`menu_id`, t.`role_id`
FROM (
  SELECT 2007600000000100004 AS `id`, 2007600000000000004 AS `menu_id`, 1123598816738675201 AS `role_id`
  UNION ALL SELECT 2007600000000100008, 2007600000000000008, 1123598816738675201
  UNION ALL SELECT 2007600000000100005, 2007600000000000005, 1123598816738675201
) t
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` rm
  WHERE rm.`role_id` = t.`role_id`
    AND rm.`menu_id` = t.`menu_id`
);
