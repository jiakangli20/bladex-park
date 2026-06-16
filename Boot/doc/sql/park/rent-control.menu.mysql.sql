-- 园区管理 / 租赁管理菜单与按钮权限
-- 执行库：bladex_boot
-- 说明：仅新增租赁管理菜单与超级管理员授权，不改动 ics_park / ics_building / ics_floor / ics_room 表结构。

DELETE FROM `blade_role_menu`
WHERE `menu_id` IN (
  1963598815738675501,
  1963598815738675502,
  1963598815738675503,
  1963598815738675504,
  1963598815738675505,
  1963598815738675506,
  1963598815738675507,
  1963598815738675508,
  1963598815738675509,
  1963598815738675510
);

DELETE FROM `blade_menu`
WHERE `id` IN (
  1963598815738675501,
  1963598815738675502,
  1963598815738675503,
  1963598815738675504,
  1963598815738675505,
  1963598815738675506,
  1963598815738675507,
  1963598815738675508,
  1963598815738675509,
  1963598815738675510
);

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
VALUES
  (1963598815738675501, 1963598815738675401, 'rent_control', '租赁管理', 'menu', '/ics/rent-control', 'iconfont icon-caidanguanli', 4, 1, 0, 1, '', NULL, 0),
  (1963598815738675502, 1963598815738675501, 'rent_control_list', '看板查询', 'list', '/api/blade-ics/rent-control/board', 'search', 1, 2, 1, 1, '', NULL, 0),
  (1963598815738675503, 1963598815738675501, 'rent_control_detail', '房源详情', 'detail', '/api/blade-ics/room/detail', 'file-text', 2, 2, 2, 1, '', NULL, 0),
  (1963598815738675504, 1963598815738675501, 'rent_control_room_add', '新增房间', 'add', '/api/blade-ics/room/submit', 'plus', 3, 2, 1, 1, '', NULL, 0),
  (1963598815738675505, 1963598815738675501, 'rent_control_room_edit', '编辑房间', 'edit', '/api/blade-ics/room/submit', 'form', 4, 2, 2, 1, '', NULL, 0),
  (1963598815738675506, 1963598815738675501, 'rent_control_room_delete', '删除房间', 'delete', '/api/blade-ics/room/remove', 'delete', 5, 2, 3, 1, '', NULL, 0),
  (1963598815738675507, 1963598815738675501, 'rent_control_room_status', '状态流转', 'status', '/api/blade-ics/room/change-status', 'retweet', 6, 2, 2, 1, '', NULL, 0),
  (1963598815738675508, 1963598815738675501, 'rent_control_room_sync', '同步小程序', 'sync', '/api/blade-ics/room/sync-mini', 'cloud-upload', 7, 2, 2, 1, '', NULL, 0),
  (1963598815738675509, 1963598815738675501, 'rent_control_workorder_list', '工单刷新', 'workorder-list', '/api/blade-ics/rent-control/workorders', 'reload', 8, 2, 1, 1, '', NULL, 0),
  (1963598815738675510, 1963598815738675501, 'rent_control_workorder_report', '上报工单', 'workorder-report', '/api/blade-ics/rent-control/workorders/report', 'plus', 9, 2, 1, 1, '', NULL, 0);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
VALUES
  (2066361740728168601, 1963598815738675501, 1123598816738675201),
  (2066361740728168602, 1963598815738675502, 1123598816738675201),
  (2066361740728168603, 1963598815738675503, 1123598816738675201),
  (2066361740728168604, 1963598815738675504, 1123598816738675201),
  (2066361740728168605, 1963598815738675505, 1123598816738675201),
  (2066361740728168606, 1963598815738675506, 1123598816738675201),
  (2066361740728168607, 1963598815738675507, 1123598816738675201),
  (2066361740728168608, 1963598815738675508, 1123598816738675201),
  (2066361740728168609, 1963598815738675509, 1123598816738675201),
  (2066361740728168610, 1963598815738675510, 1123598816738675201);
