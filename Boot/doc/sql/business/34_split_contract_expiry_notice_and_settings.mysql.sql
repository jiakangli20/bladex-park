-- 合同管理 - 拆分合同到期提醒与到期设置
-- 说明：原 contract_expiring 保留为规则设置，新增规则驱动的合同到期提醒页面，可重复执行。

SET NAMES utf8mb4;

UPDATE `blade_menu`
SET `name` = '合同到期设置',
    `path` = '/contract/expiring',
    `source` = 'iconfont iconicon_setting',
    `component` = 'views/contract/expiring',
    `sort` = 3,
    `remark` = '合同到期提醒规则设置',
    `is_deleted` = 0
WHERE `code` = 'contract_expiring';

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (2007600000000000010, 2007600000000000001, 'contract_expiry_notice', '合同到期提醒', 'menu', '/contract/expiry-notice', 'iconfont icon-time', 'views/contract/expiry-notice', 2, 1, 0, 1, '按到期设置规则展示即将到期合同', 0)
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

-- 合同管理左侧菜单固定业务顺序。
UPDATE `blade_menu`
SET `sort` = CASE `code`
  WHEN 'contract_contract' THEN 1
  WHEN 'contract_expiry_notice' THEN 2
  WHEN 'contract_expiring' THEN 3
  WHEN 'contract_archive' THEN 4
  WHEN 'contract_termination' THEN 5
  WHEN 'contract_print_template' THEN 6
  ELSE `sort`
END
WHERE `parent_id` = 2007600000000000001
  AND `code` IN (
    'contract_contract',
    'contract_expiry_notice',
    'contract_expiring',
    'contract_archive',
    'contract_termination',
    'contract_print_template'
  );

-- 权限范围沿用原到期设置菜单。
INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.`id`, t.`role_id`, 2007600000000000010
FROM (
  SELECT 2007600000000200011 AS `id`, 1123598816738675201 AS `role_id`
  UNION ALL SELECT 2007600000000200012, 1123598816738675203
  UNION ALL SELECT 2007600000000200013, 1123598816738675204
  UNION ALL SELECT 2007600000000200014, 1123598816738675205
) t
INNER JOIN `blade_role_menu` setting_permission
  ON setting_permission.`role_id` = t.`role_id`
 AND setting_permission.`menu_id` = 2007600000000000004
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` current_permission
  WHERE current_permission.`role_id` = t.`role_id`
    AND current_permission.`menu_id` = 2007600000000000010
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_role_menu` current_id
    WHERE current_id.`id` = t.`id`
  );

-- 同步加入已配置“合同管理”的顶部菜单。
INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 2007600000000300010, contract_top.`top_menu_id`, 2007600000000000010
FROM (
  SELECT DISTINCT `top_menu_id`
  FROM `blade_top_menu_setting`
  WHERE `menu_id` = 2007600000000000001
  ORDER BY `top_menu_id`
  LIMIT 1
) contract_top
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_top_menu_setting` current_setting
  WHERE current_setting.`top_menu_id` = contract_top.`top_menu_id`
    AND current_setting.`menu_id` = 2007600000000000010
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_top_menu_setting` current_id
    WHERE current_id.`id` = 2007600000000300010
  );

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('contract_expiring', 'contract_expiry_notice')
ORDER BY `sort`;

SELECT COUNT(*) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` = 2007600000000000010;

SELECT COUNT(*) AS `top_menu_setting_count`
FROM `blade_top_menu_setting`
WHERE `menu_id` = 2007600000000000010;
