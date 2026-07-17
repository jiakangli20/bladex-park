-- 逾期管理 - 我的逾期通知
-- 说明：为拥有逾期管理一级菜单的角色增加个人通知入口，可重复执行。

SET NAMES utf8mb4;

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000006000600, 1890000000006000000, 'finance_overdue_notice', '我的逾期通知', 'menu', '/finance/overdue-notice', 'iconfont iconicon_sms', 'views/finance/overdue-notice', 6, 1, 0, 1, '当前账号收到的首次逾期通知', 0)
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

-- 普通部门账号也可能被选为接收人，权限范围与逾期管理一级菜单保持一致。
INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.`id`, t.`role_id`, 1890000000006000600
FROM (
  SELECT 1890000000006600601 AS `id`, 1123598816738675201 AS `role_id`
  UNION ALL SELECT 1890000000006600602, 1123598816738675202
  UNION ALL SELECT 1890000000006600603, 1123598816738675203
  UNION ALL SELECT 1890000000006600604, 1123598816738675204
  UNION ALL SELECT 1890000000006600605, 1123598816738675205
) t
INNER JOIN `blade_role_menu` finance_permission
  ON finance_permission.`role_id` = t.`role_id`
 AND finance_permission.`menu_id` = 1890000000006000000
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` current_permission
  WHERE current_permission.`role_id` = t.`role_id`
    AND current_permission.`menu_id` = 1890000000006000600
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_role_menu` current_id
    WHERE current_id.`id` = t.`id`
  );

-- 同步加入已配置“逾期管理”的顶部菜单。
INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 1890000000006600699, finance_top.`top_menu_id`, 1890000000006000600
FROM (
  SELECT DISTINCT `top_menu_id`
  FROM `blade_top_menu_setting`
  WHERE `menu_id` = 1890000000006000000
  ORDER BY `top_menu_id`
  LIMIT 1
) finance_top
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_top_menu_setting` current_setting
  WHERE current_setting.`top_menu_id` = finance_top.`top_menu_id`
    AND current_setting.`menu_id` = 1890000000006000600
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_top_menu_setting` current_id
    WHERE current_id.`id` = 1890000000006600699
  );

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` = 'finance_overdue_notice';

SELECT COUNT(*) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000006000600;

SELECT COUNT(*) AS `top_menu_setting_count`
FROM `blade_top_menu_setting`
WHERE `menu_id` = 1890000000006000600;
