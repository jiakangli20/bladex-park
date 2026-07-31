-- 房源工作台：水电记录与绑定车辆按钮权限
-- 说明：为既有租控管理授权角色补充查询、新增、删除权限；脚本可重复执行。

SET NAMES utf8mb4;

INSERT INTO `blade_menu`
  (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1963598815738675525, 1963598815738675501, 'rent_control_utility_list', '水电记录查询', 'list', '/api/blade-park/room-extension/utility/page', 'search', '', 19, 2, 1, 1, '房源水电记录查询权限', 0),
  (1963598815738675526, 1963598815738675501, 'rent_control_utility_add', '新增水电记录', 'add', '/api/blade-park/room-extension/utility/submit', 'plus', '', 20, 2, 1, 1, '新增房源水电抄表记录', 0),
  (1963598815738675527, 1963598815738675501, 'rent_control_utility_delete', '删除水电记录', 'delete', '/api/blade-park/room-extension/utility/remove', 'delete', '', 21, 2, 3, 1, '删除房源水电抄表记录', 0),
  (1963598815738675528, 1963598815738675501, 'rent_control_vehicle_list', '绑定车辆查询', 'list', '/api/blade-park/room-extension/vehicle/page', 'search', '', 22, 2, 1, 1, '房源绑定车辆查询权限', 0),
  (1963598815738675529, 1963598815738675501, 'rent_control_vehicle_add', '新增绑定车辆', 'add', '/api/blade-park/room-extension/vehicle/submit', 'plus', '', 23, 2, 1, 1, '新增房源绑定车辆', 0),
  (1963598815738675530, 1963598815738675501, 'rent_control_vehicle_delete', '删除绑定车辆', 'delete', '/api/blade-park/room-extension/vehicle/remove', 'delete', '', 24, 2, 3, 1, '删除房源绑定车辆', 0)
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
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT grants.`id`, grants.`role_id`, grants.`menu_id`
FROM (
  SELECT 2072000000000000037 AS `id`, 1123598816738675201 AS `role_id`, 1963598815738675525 AS `menu_id`
  UNION ALL SELECT 2072000000000000038, 1123598816738675201, 1963598815738675526
  UNION ALL SELECT 2072000000000000039, 1123598816738675201, 1963598815738675527
  UNION ALL SELECT 2072000000000000040, 1123598816738675201, 1963598815738675528
  UNION ALL SELECT 2072000000000000041, 1123598816738675201, 1963598815738675529
  UNION ALL SELECT 2072000000000000042, 1123598816738675201, 1963598815738675530
  UNION ALL SELECT 2072000000000000043, 1123598816738675203, 1963598815738675525
  UNION ALL SELECT 2072000000000000044, 1123598816738675203, 1963598815738675526
  UNION ALL SELECT 2072000000000000045, 1123598816738675203, 1963598815738675527
  UNION ALL SELECT 2072000000000000046, 1123598816738675203, 1963598815738675528
  UNION ALL SELECT 2072000000000000047, 1123598816738675203, 1963598815738675529
  UNION ALL SELECT 2072000000000000048, 1123598816738675203, 1963598815738675530
  UNION ALL SELECT 2072000000000000049, 1123598816738675204, 1963598815738675525
  UNION ALL SELECT 2072000000000000050, 1123598816738675204, 1963598815738675526
  UNION ALL SELECT 2072000000000000051, 1123598816738675204, 1963598815738675527
  UNION ALL SELECT 2072000000000000052, 1123598816738675204, 1963598815738675528
  UNION ALL SELECT 2072000000000000053, 1123598816738675204, 1963598815738675529
  UNION ALL SELECT 2072000000000000054, 1123598816738675204, 1963598815738675530
  UNION ALL SELECT 2072000000000000055, 1123598816738675205, 1963598815738675525
  UNION ALL SELECT 2072000000000000056, 1123598816738675205, 1963598815738675526
  UNION ALL SELECT 2072000000000000057, 1123598816738675205, 1963598815738675527
  UNION ALL SELECT 2072000000000000058, 1123598816738675205, 1963598815738675528
  UNION ALL SELECT 2072000000000000059, 1123598816738675205, 1963598815738675529
  UNION ALL SELECT 2072000000000000060, 1123598816738675205, 1963598815738675530
) grants
INNER JOIN `blade_role_menu` parent_grant
  ON parent_grant.`role_id` = grants.`role_id`
 AND parent_grant.`menu_id` = 1963598815738675501
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` existing_grant
  WHERE existing_grant.`role_id` = grants.`role_id`
    AND existing_grant.`menu_id` = grants.`menu_id`
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_role_menu` existing_id
    WHERE existing_id.`id` = grants.`id`
  );

SELECT `parent_id`, `code`, `name`, `path`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` LIKE 'rent_control_utility_%'
   OR `code` LIKE 'rent_control_vehicle_%'
ORDER BY `sort`;

SELECT `menu_id`, COUNT(DISTINCT `role_id`) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` BETWEEN 1963598815738675525 AND 1963598815738675530
GROUP BY `menu_id`
ORDER BY `menu_id`;
