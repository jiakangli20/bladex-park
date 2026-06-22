-- 入驻管理 - 项目审核 / 客户管理菜单闭环脚本
-- 业务顺序：商机管理 -> 项目审核 -> 客户管理 -> 客户标签

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000003000000, 0, 'settlement', '入驻管理', 'menu', '/settlement', 'iconfont iconicon_compile', '', 30, 1, 0, 1, '入驻管理', 0),
  (1890000000003000100, 1890000000003000000, 'business_opportunity', '商机管理', 'menu', '/settlement/opportunity', 'iconfont iconicon_doc', 'views/settlement/opportunity', 1, 1, 0, 1, '商机管理', 0),
  (1890000000003000200, 1890000000003000000, 'settlement_project_approval', '项目审核', 'menu', '/settlement/project-approval', 'iconfont iconicon_compile', 'views/settlement/project-approval', 2, 1, 0, 1, '入驻项目审核', 0),
  (1890000000003000201, 1890000000003000200, 'settlement_project_approval_view', '查看', 'view', '/settlement/project-approval/view', 'file-text', '', 3, 2, 2, 1, NULL, 0),
  (1890000000003000202, 1890000000003000200, 'settlement_project_approval_form', '审批表', 'form', '/settlement/project-approval/form', 'file', '', 4, 2, 2, 1, NULL, 0),
  (1890000000003000203, 1890000000003000200, 'settlement_project_approval_approve', '通过', 'approve', '/api/blade-approval/project/approve', 'check', '', 5, 2, 2, 1, NULL, 0),
  (1890000000003000204, 1890000000003000200, 'settlement_project_approval_reject', '驳回', 'reject', '/api/blade-approval/project/reject', 'close', '', 6, 2, 2, 1, NULL, 0),
  (1890000000003000205, 1890000000003000200, 'settlement_project_approval_transfer', '转审', 'transfer', '/api/blade-approval/project/transfer', 'share', '', 7, 2, 2, 1, NULL, 0),
  (1890000000003000206, 1890000000003000200, 'settlement_project_approval_resubmit', '重新提交', 'resubmit', '/api/blade-approval/project/resubmit', 'refresh', '', 8, 2, 2, 1, NULL, 0),
  (1890000000003000207, 1890000000003000200, 'settlement_project_approval_archive', '归档', 'archive', '/api/blade-approval/project/archive', 'folder', '', 9, 2, 2, 1, NULL, 0),
  (1890000000003000208, 1890000000003000200, 'settlement_project_approval_add', '发起审核', 'add', '/api/blade-approval/project/save', 'plus', '', 1, 2, 1, 1, NULL, 0),
  (1890000000003000209, 1890000000003000200, 'settlement_project_approval_delete', '删除', 'delete', '/api/blade-approval/project/remove', 'delete', '', 2, 2, 3, 1, NULL, 0),
  (1890000000003000300, 1890000000003000000, 'settlement_customer', '客户管理', 'menu', '/settlement/customer', 'iconfont iconicon_group', 'views/settlement/customer', 3, 1, 0, 1, '入驻客户管理', 0),
  (1890000000003000301, 1890000000003000300, 'settlement_customer_add', '新增', 'add', '/settlement/customer/add', 'plus', '', 1, 2, 1, 1, NULL, 0),
  (1890000000003000302, 1890000000003000300, 'settlement_customer_edit', '修改', 'edit', '/settlement/customer/edit', 'form', '', 2, 2, 2, 1, NULL, 0),
  (1890000000003000303, 1890000000003000300, 'settlement_customer_delete', '删除', 'delete', '/api/blade-park/customer/remove', 'delete', '', 3, 2, 3, 1, NULL, 0),
  (1890000000003000304, 1890000000003000300, 'settlement_customer_view', '查看', 'view', '/settlement/customer/view', 'file-text', '', 4, 2, 2, 1, NULL, 0),
  (1890000000003000305, 1890000000003000300, 'settlement_customer_status', '状态变更', 'status', '/api/blade-park/customer/changeStatus', 'switch', '', 5, 2, 2, 1, NULL, 0),
  (1890000000003000306, 1890000000003000300, 'settlement_customer_tag', '设置标签', 'tag', '/api/blade-park/customer/tag/set', 'collection-tag', '', 6, 2, 2, 1, NULL, 0),
  (1890000000003000307, 1890000000003000300, 'settlement_customer_check', '客户核验', 'check', '/api/blade-park/customer/check', 'check', '', 7, 2, 2, 1, NULL, 0),
  (1890000000003000308, 1890000000003000300, 'settlement_customer_import', '导入', 'import', '/api/blade-park/customer/import', 'upload', '', 8, 2, 1, 1, NULL, 0),
  (1890000000003000309, 1890000000003000300, 'settlement_customer_export', '导出', 'export', '/api/blade-park/customer/export', 'download', '', 9, 2, 2, 1, NULL, 0),
  (1890000000001000100, 1890000000003000000, 'customer_tag', '客户标签', 'menu', '/settlement/tag', 'iconfont iconicon_label', 'views/settlement/tag', 4, 1, 0, 1, '客户标签管理', 0)
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

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.`id`, t.`role_id`, t.`menu_id`
FROM (
  SELECT 1890000000004300001 AS `id`, 1123598816738675201 AS `role_id`, 1890000000003000000 AS `menu_id`
  UNION ALL SELECT 1890000000004300002, 1123598816738675201, 1890000000003000100
  UNION ALL SELECT 1890000000004300003, 1123598816738675201, 1890000000003000200
  UNION ALL SELECT 1890000000004300004, 1123598816738675201, 1890000000003000201
  UNION ALL SELECT 1890000000004300005, 1123598816738675201, 1890000000003000202
  UNION ALL SELECT 1890000000004300006, 1123598816738675201, 1890000000003000203
  UNION ALL SELECT 1890000000004300007, 1123598816738675201, 1890000000003000204
  UNION ALL SELECT 1890000000004300008, 1123598816738675201, 1890000000003000205
  UNION ALL SELECT 1890000000004300009, 1123598816738675201, 1890000000003000206
  UNION ALL SELECT 1890000000004300010, 1123598816738675201, 1890000000003000207
  UNION ALL SELECT 1890000000004300021, 1123598816738675201, 1890000000003000208
  UNION ALL SELECT 1890000000004300022, 1123598816738675201, 1890000000003000209
  UNION ALL SELECT 1890000000004300011, 1123598816738675201, 1890000000003000300
  UNION ALL SELECT 1890000000004300012, 1123598816738675201, 1890000000003000301
  UNION ALL SELECT 1890000000004300013, 1123598816738675201, 1890000000003000302
  UNION ALL SELECT 1890000000004300014, 1123598816738675201, 1890000000003000303
  UNION ALL SELECT 1890000000004300015, 1123598816738675201, 1890000000003000304
  UNION ALL SELECT 1890000000004300016, 1123598816738675201, 1890000000003000305
  UNION ALL SELECT 1890000000004300017, 1123598816738675201, 1890000000003000306
  UNION ALL SELECT 1890000000004300018, 1123598816738675201, 1890000000003000307
  UNION ALL SELECT 1890000000004300019, 1123598816738675201, 1890000000003000308
  UNION ALL SELECT 1890000000004300020, 1123598816738675201, 1890000000003000309
  UNION ALL SELECT 1890000000004300023, 1123598816738675201, 1890000000001000100
) t
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` rm
  WHERE rm.`role_id` = t.`role_id`
    AND rm.`menu_id` = t.`menu_id`
)
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_role_menu` rm_id
    WHERE rm_id.`id` = t.`id`
  );
