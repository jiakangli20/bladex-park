-- 审批中心 - 待办任务 / 已办任务 / 抄送我的二级菜单

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000005000000, 0, 'approval_center', '审批中心', 'menu', '/approval', 'iconfont icon-tianshenpi', '', 40, 1, 0, 1, '审批中心', 0),
  (1890000000005000100, 1890000000005000000, 'approval_my_flow', '我的审批', 'menu', '/approval/my-flow', 'iconfont iconicon_compile', 'views/approval/my-flow', 1, 1, 0, 1, '我的审批', 0),
  (1890000000005000300, 1890000000005000000, 'approval_todo_task', '待办任务', 'menu', '/approval/todo', 'iconfont iconicon_task', 'views/approval/todo', 2, 1, 0, 1, '待办任务', 0),
  (1890000000005000301, 1890000000005000300, 'approval_todo_task_view', '查看', 'view', '/approval/todo/view', 'file-text', '', 1, 2, 2, 1, NULL, 0),
  (1890000000005000302, 1890000000005000300, 'approval_todo_task_form', '审批表', 'form', '/approval/todo/form', 'file', '', 2, 2, 2, 1, NULL, 0),
  (1890000000005000303, 1890000000005000300, 'approval_todo_task_approve', '通过', 'approve', '/api/blade-approval/project/approve', 'check', '', 3, 2, 2, 1, NULL, 0),
  (1890000000005000304, 1890000000005000300, 'approval_todo_task_reject', '驳回', 'reject', '/api/blade-approval/project/reject', 'close', '', 4, 2, 2, 1, NULL, 0),
  (1890000000005000305, 1890000000005000300, 'approval_todo_task_transfer', '转审', 'transfer', '/api/blade-approval/project/transfer', 'share', '', 5, 2, 2, 1, NULL, 0),
  (1890000000005000306, 1890000000005000300, 'approval_todo_task_resubmit', '重新提交', 'resubmit', '/api/blade-approval/project/resubmit', 'refresh', '', 6, 2, 2, 1, NULL, 0),
  (1890000000005000307, 1890000000005000300, 'approval_todo_task_archive', '归档', 'archive', '/api/blade-approval/project/archive', 'folder', '', 7, 2, 2, 1, NULL, 0),
  (1890000000005000400, 1890000000005000000, 'approval_done_task', '已办任务', 'menu', '/approval/done', 'iconfont iconicon_select', 'views/approval/done', 3, 1, 0, 1, '已办任务', 0),
  (1890000000005000401, 1890000000005000400, 'approval_done_task_view', '查看', 'view', '/approval/done/view', 'file-text', '', 1, 2, 2, 1, NULL, 0),
  (1890000000005000402, 1890000000005000400, 'approval_done_task_form', '审批表', 'form', '/approval/done/form', 'file', '', 2, 2, 2, 1, NULL, 0),
  (1890000000005000403, 1890000000005000400, 'approval_done_task_archive', '归档', 'archive', '/api/blade-approval/project/archive', 'folder', '', 3, 2, 2, 1, NULL, 0),
  (1890000000005000500, 1890000000005000000, 'approval_cc_task', '抄送我的', 'menu', '/approval/cc', 'iconfont iconicon_copy', 'views/approval/cc', 4, 1, 0, 1, '抄送我的', 0),
  (1890000000005000501, 1890000000005000500, 'approval_cc_task_view', '查看', 'view', '/approval/cc/view', 'file-text', '', 1, 2, 2, 1, NULL, 0),
  (1890000000005000502, 1890000000005000500, 'approval_cc_task_form', '审批表', 'form', '/approval/cc/form', 'file', '', 2, 2, 2, 1, NULL, 0),
  (1890000000005000200, 1890000000005000000, 'approval_flow_config', '流程配置', 'menu', '/approval/flow-config', 'iconfont icon-guize', 'views/approval/flow-config', 5, 1, 0, 1, '审批流程配置', 0)
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
  SELECT 1890000000005300001 AS `id`, 1123598816738675201 AS `role_id`, 1890000000005000000 AS `menu_id`
  UNION ALL SELECT 1890000000005300002, 1123598816738675201, 1890000000005000100
  UNION ALL SELECT 1890000000005300003, 1123598816738675201, 1890000000005000300
  UNION ALL SELECT 1890000000005300004, 1123598816738675201, 1890000000005000301
  UNION ALL SELECT 1890000000005300005, 1123598816738675201, 1890000000005000302
  UNION ALL SELECT 1890000000005300006, 1123598816738675201, 1890000000005000303
  UNION ALL SELECT 1890000000005300007, 1123598816738675201, 1890000000005000304
  UNION ALL SELECT 1890000000005300008, 1123598816738675201, 1890000000005000305
  UNION ALL SELECT 1890000000005300009, 1123598816738675201, 1890000000005000306
  UNION ALL SELECT 1890000000005300010, 1123598816738675201, 1890000000005000307
  UNION ALL SELECT 1890000000005300011, 1123598816738675201, 1890000000005000400
  UNION ALL SELECT 1890000000005300012, 1123598816738675201, 1890000000005000401
  UNION ALL SELECT 1890000000005300013, 1123598816738675201, 1890000000005000402
  UNION ALL SELECT 1890000000005300014, 1123598816738675201, 1890000000005000403
  UNION ALL SELECT 1890000000005300015, 1123598816738675201, 1890000000005000500
  UNION ALL SELECT 1890000000005300016, 1123598816738675201, 1890000000005000501
  UNION ALL SELECT 1890000000005300017, 1123598816738675201, 1890000000005000502
  UNION ALL SELECT 1890000000005300018, 1123598816738675201, 1890000000005000200
) t
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` rm
  WHERE rm.`role_id` = t.`role_id`
    AND rm.`menu_id` = t.`menu_id`
);
