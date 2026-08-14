-- 保留“入驻管理”一级菜单，仅删除“入驻审核”二级菜单并将入口合并至商机管理。
-- 历史流程路由由前端动态注册，不依赖旧菜单记录。
START TRANSACTION;

UPDATE blade_menu
SET remark = '商机跟进、企业详情和入驻审核统一入口'
WHERE code = 'business_opportunity';

-- 已有商机管理权限的角色删除重复旧父菜单授权。
DELETE old_rm
FROM blade_role_menu old_rm
JOIN blade_menu old_parent ON old_parent.code = 'settlement_project_approval'
JOIN blade_menu new_parent ON new_parent.code = 'business_opportunity'
JOIN blade_role_menu new_rm
  ON new_rm.role_id = old_rm.role_id
 AND new_rm.menu_id = new_parent.id
WHERE old_rm.menu_id = old_parent.id;

-- 仅将尚无商机管理权限的原入驻审核角色迁移到商机管理。
UPDATE blade_role_menu rm
JOIN blade_menu old_parent ON old_parent.code = 'settlement_project_approval'
JOIN blade_menu new_parent ON new_parent.code = 'business_opportunity'
SET rm.menu_id = new_parent.id
WHERE rm.menu_id = old_parent.id;

-- 旧入驻审核按钮权限继续挂到商机菜单，便于原角色在新详情弹窗内使用审批能力。
UPDATE blade_menu child
JOIN blade_menu old_parent ON old_parent.code = 'settlement_project_approval'
JOIN blade_menu new_parent ON new_parent.code = 'business_opportunity'
SET child.parent_id = new_parent.id
WHERE child.parent_id = old_parent.id
  AND child.code IN ('settlement_project_approval_add', 'settlement_project_approval_form');

UPDATE blade_menu
SET is_deleted = 1,
    remark = '已合并至商机管理，保留历史路由兼容'
WHERE code = 'settlement_project_approval';

DELETE setting
FROM blade_top_menu_setting setting
JOIN blade_menu old_parent ON old_parent.id = setting.menu_id
WHERE old_parent.code = 'settlement_project_approval';

SELECT code, name, parent_id, path, component, is_deleted, remark
FROM blade_menu
WHERE code IN (
  'business_opportunity',
  'settlement_project_approval',
  'settlement_project_approval_add',
  'settlement_project_approval_form'
)
ORDER BY category, sort, id;

COMMIT;
