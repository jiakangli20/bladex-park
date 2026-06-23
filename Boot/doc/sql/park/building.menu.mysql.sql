-- 园区管理 / 建筑管理菜单与按钮权限
-- 执行库：bladex_boot
-- 说明：仅新增建筑管理菜单与超级管理员授权，不改动 ics_park / ics_building / ics_floor / ics_room 表结构。

DELETE FROM `blade_role_menu`
WHERE `menu_id` IN (
  1963598815738675601,
  1963598815738675602,
  1963598815738675603,
  1963598815738675604,
  1963598815738675605,
  1963598815738675606,
  1963598815738675607,
  1963598815738675608
);

DELETE FROM `blade_menu`
WHERE `id` IN (
  1963598815738675601,
  1963598815738675602,
  1963598815738675603,
  1963598815738675604,
  1963598815738675605,
  1963598815738675606,
  1963598815738675607,
  1963598815738675608
);

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
VALUES
  (1963598815738675601, 1963598815738675401, 'building', '建筑管理', 'menu', '/park/building', 'iconfont icon-caidanguanli', 3, 1, 0, 1, '', NULL, 0),
  (1963598815738675602, 1963598815738675601, 'building_view', '查看', 'view', '/api/blade-park/building/detail', 'file-text', 1, 2, 2, 1, '', NULL, 0),
  (1963598815738675603, 1963598815738675601, 'building_add', '新增', 'add', '/api/blade-park/building/submit', 'plus', 2, 2, 1, 1, '', NULL, 0),
  (1963598815738675604, 1963598815738675601, 'building_edit', '修改', 'edit', '/api/blade-park/building/submit', 'form', 3, 2, 2, 1, '', NULL, 0),
  (1963598815738675605, 1963598815738675601, 'building_delete', '删除', 'delete', '/api/blade-park/building/remove', 'delete', 4, 2, 3, 1, '', NULL, 0),
  (1963598815738675606, 1963598815738675601, 'building_import', '导入', 'import', '/api/blade-park/building/import', 'upload', 5, 2, 1, 1, '', NULL, 0),
  (1963598815738675607, 1963598815738675601, 'building_export', '导出', 'export', '/api/blade-park/building/export', 'download', 6, 2, 1, 1, '', NULL, 0),
  (1963598815738675608, 1963598815738675601, 'building_template', '模板下载', 'template', '/api/blade-park/building/export-template', 'download', 7, 2, 1, 1, '', NULL, 0);

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
VALUES
  (2066361740728168701, 1963598815738675601, 1123598816738675201),
  (2066361740728168702, 1963598815738675602, 1123598816738675201),
  (2066361740728168703, 1963598815738675603, 1123598816738675201),
  (2066361740728168704, 1963598815738675604, 1123598816738675201),
  (2066361740728168705, 1963598815738675605, 1123598816738675201),
  (2066361740728168706, 1963598815738675606, 1123598816738675201),
  (2066361740728168707, 1963598815738675607, 1123598816738675201),
  (2066361740728168708, 1963598815738675608, 1123598816738675201);
