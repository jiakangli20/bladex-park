-- 移除合同管理下的“费项配置”菜单与按钮权限
-- 导入目标库：bladex_boot

DELETE rm
FROM blade_role_menu rm
JOIN blade_menu m ON rm.menu_id = m.id
WHERE m.code IN (
  'contract_expense',
  'contract_expense_add',
  'contract_expense_edit',
  'contract_expense_delete',
  'contract_expense_view',
  'contract_expense_status'
);

DELETE FROM blade_menu
WHERE code IN (
  'contract_expense',
  'contract_expense_add',
  'contract_expense_edit',
  'contract_expense_delete',
  'contract_expense_view',
  'contract_expense_status'
);
