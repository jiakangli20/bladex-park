-- 合同归档菜单与按钮权限
-- 导入目标库：bladex_boot
-- 说明：本脚本只新增 BladeX 菜单和按钮，不修改业务表结构。

INSERT IGNORE INTO `blade_menu`
(`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
VALUES
(2007600000000000005, 2007600000000000001, 'contract_archive', '合同归档', 'menu', '/contract/archive', 'iconfont iconicon_doc', 4, 1, 0, 1, '', '合同档案聚合查看', 0),
(2007600000000000030, 2007600000000000005, 'contract_archive_list', '列表', 'list', '/api/blade-contract/archive/page', 'list', 1, 2, 0, 1, '', NULL, 0),
(2007600000000000031, 2007600000000000005, 'contract_archive_detail', '查看档案', 'detail', '/api/blade-contract/archive/detail', 'file-text', 2, 2, 2, 1, '', NULL, 0),
(2007600000000000032, 2007600000000000005, 'contract_archive_export_approval', '导出审批表', 'export-approval', '/api/blade-contract/archive/export-approval', 'download', 3, 2, 2, 1, '', NULL, 0),
(2007600000000000033, 2007600000000000005, 'contract_archive_print', '打印预览', 'print', '/api/blade-contract/archive/print', 'printer', 4, 2, 2, 1, '', NULL, 0);

INSERT IGNORE INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2007600000000100000 + menu_offset.offset_value, menu_offset.menu_id, 1123598816738675201
FROM (
  SELECT 5 AS offset_value, 2007600000000000005 AS menu_id UNION ALL
  SELECT 30, 2007600000000000030 UNION ALL
  SELECT 31, 2007600000000000031 UNION ALL
  SELECT 32, 2007600000000000032 UNION ALL
  SELECT 33, 2007600000000000033
) menu_offset;
