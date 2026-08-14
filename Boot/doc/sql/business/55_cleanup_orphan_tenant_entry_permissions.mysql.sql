-- 清理“入驻审核”合并至商机管理后遗留的孤儿按钮权限。
-- “发起审核”和“审批表”已迁移至商机管理，本脚本不处理这两个有效权限。
START TRANSACTION;

DELETE rm
FROM blade_role_menu rm
JOIN blade_menu menu ON menu.id = rm.menu_id
WHERE menu.code IN (
  'settlement_project_approval_delete',
  'settlement_project_approval_view',
  'settlement_project_approval_approve',
  'settlement_project_approval_reject',
  'settlement_project_approval_transfer',
  'settlement_project_approval_resubmit',
  'settlement_project_approval_archive'
);

UPDATE blade_menu
SET is_deleted = 1,
    remark = '旧入驻审核页面权限已停用，审批操作由协同办公流程控制'
WHERE code IN (
  'settlement_project_approval_delete',
  'settlement_project_approval_view',
  'settlement_project_approval_approve',
  'settlement_project_approval_reject',
  'settlement_project_approval_transfer',
  'settlement_project_approval_resubmit',
  'settlement_project_approval_archive'
);

COMMIT;

SELECT
  COUNT(*) AS active_orphan_permission_count,
  CASE WHEN COUNT(*) = 0 THEN 'PASS' ELSE 'FAIL' END AS check_status
FROM blade_menu child
LEFT JOIN blade_menu parent ON parent.id = child.parent_id
WHERE child.code IN (
  'settlement_project_approval_delete',
  'settlement_project_approval_view',
  'settlement_project_approval_approve',
  'settlement_project_approval_reject',
  'settlement_project_approval_transfer',
  'settlement_project_approval_resubmit',
  'settlement_project_approval_archive'
)
  AND child.is_deleted = 0
  AND (parent.id IS NULL OR parent.is_deleted = 1);

SELECT code, name, parent_id, is_deleted, remark
FROM blade_menu
WHERE code IN (
  'settlement_project_approval_add',
  'settlement_project_approval_form',
  'settlement_project_approval_delete',
  'settlement_project_approval_view',
  'settlement_project_approval_approve',
  'settlement_project_approval_reject',
  'settlement_project_approval_transfer',
  'settlement_project_approval_resubmit',
  'settlement_project_approval_archive'
)
ORDER BY code;
