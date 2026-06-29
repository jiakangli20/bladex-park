-- 企业服务-商户增值服务小程序预留接口菜单权限补丁
-- 说明：只补接口权限数据，不改业务表结构；服务处理 UI 与落库表后续再接入。

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000217, 1890000000007000210, 'merchant_service_miniapp_list', '小程序增值服务列表', 'list', '/api/blade-ics/merchant/miniapp/list', 'list', '', 7, 2, 0, 1, NULL, 0),
  (1890000000007000218, 1890000000007000210, 'merchant_service_miniapp_detail', '小程序增值服务详情', 'view', '/api/blade-ics/merchant/miniapp/detail', 'file-text', '', 8, 2, 0, 1, NULL, 0),
  (1890000000007000219, 1890000000007000210, 'merchant_service_miniapp_apply', '小程序申请增值服务', 'add', '/api/blade-ics/merchant/miniapp/apply', 'plus', '', 9, 2, 1, 1, NULL, 0),
  (1890000000007000221, 1890000000007000210, 'merchant_service_miniapp_follow', '小程序管理员跟进', 'edit', '/api/blade-ics/merchant/miniapp/admin/follow', 'form', '', 10, 2, 2, 1, NULL, 0),
  (1890000000007000222, 1890000000007000210, 'merchant_service_miniapp_deal', '小程序撮合成交', 'edit', '/api/blade-ics/merchant/miniapp/admin/deal', 'circle-check', '', 11, 2, 2, 1, NULL, 0)
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
  (1890000000008000113, 1123598816738675201, 1890000000007000217),
  (1890000000008000114, 1123598816738675201, 1890000000007000218),
  (1890000000008000115, 1123598816738675201, 1890000000007000219),
  (1890000000008000116, 1123598816738675201, 1890000000007000221),
  (1890000000008000117, 1123598816738675201, 1890000000007000222);
