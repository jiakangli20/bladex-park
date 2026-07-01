-- 移除合同管理下的“合同创建模板”二级菜单
-- 导入目标库：bladex_boot / wzjk
-- 说明：只移除菜单入口和角色授权，不删除前端页面，合同管理内的新建/续租跳转仍可继续使用。

DELETE rm
FROM blade_role_menu rm
JOIN blade_menu m ON rm.menu_id = m.id
WHERE m.code = 'contract_create_template'
   OR m.path = '/contract/create-template';

DELETE FROM blade_menu
WHERE code = 'contract_create_template'
   OR path = '/contract/create-template';

