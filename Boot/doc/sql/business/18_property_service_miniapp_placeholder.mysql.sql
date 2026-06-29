-- 企业服务-物业服务小程序预留接口菜单权限补丁
-- 说明：只补接口权限数据，不改业务表结构；小程序真实鉴权接入后可继续沿用这些接口路径。

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000115, 1890000000007000150, 'property_service_miniapp_list', '小程序服务列表', 'list', '/api/blade-ics/property-service/miniapp/list', 'list', '', 10, 2, 0, 1, NULL, 0),
  (1890000000007000116, 1890000000007000150, 'property_service_miniapp_detail', '小程序服务详情', 'view', '/api/blade-ics/property-service/miniapp/detail', 'file-text', '', 11, 2, 0, 1, NULL, 0),
  (1890000000007000117, 1890000000007000160, 'property_workorder_miniapp_apply', '小程序申请工单', 'add', '/api/blade-ics/property-workorder/miniapp/apply', 'plus', '', 10, 2, 1, 1, NULL, 0),
  (1890000000007000118, 1890000000007000160, 'property_workorder_miniapp_my_page', '小程序我的工单', 'list', '/api/blade-ics/property-workorder/miniapp/my-page', 'list', '', 11, 2, 0, 1, NULL, 0),
  (1890000000007000119, 1890000000007000160, 'property_workorder_miniapp_admin_page', '小程序管理员工单', 'list', '/api/blade-ics/property-workorder/miniapp/admin/page', 'list', '', 12, 2, 0, 1, NULL, 0),
  (1890000000007000120, 1890000000007000160, 'property_workorder_miniapp_admin_handle', '小程序处理工单', 'edit', '/api/blade-ics/property-workorder/miniapp/admin/handle', 'form', '', 13, 2, 2, 1, NULL, 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
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

INSERT IGNORE INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000008000019, 1123598816738675201, 1890000000007000115),
  (1890000000008000020, 1123598816738675201, 1890000000007000116),
  (1890000000008000021, 1123598816738675201, 1890000000007000117),
  (1890000000008000022, 1123598816738675201, 1890000000007000118),
  (1890000000008000023, 1123598816738675201, 1890000000007000119),
  (1890000000008000024, 1123598816738675201, 1890000000007000120);
