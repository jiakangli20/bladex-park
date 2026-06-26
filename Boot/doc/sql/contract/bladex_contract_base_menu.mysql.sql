-- 合同管理第一阶段菜单与按钮权限
-- 导入目标库：bladex_boot
-- 说明：本脚本只新增 BladeX 菜单和按钮，不修改业务表结构。

INSERT IGNORE INTO `blade_menu`
(`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
VALUES
(2007600000000000001, 0, 'contract', '合同管理', 'menu', '/contract', 'iconfont iconicon_doc', 20, 1, 0, 1, '', 'RuoYi 合同管理迁移第一阶段', 0),
(2007600000000000003, 2007600000000000001, 'contract_contract', '合同管理', 'menu', '/contract/contract', 'iconfont iconicon_doc', 1, 1, 0, 1, '', '合同主档', 0),
(2007600000000000004, 2007600000000000001, 'contract_expiring', '合同到期提醒', 'menu', '/contract/expiring', 'iconfont icon-time', 2, 1, 0, 1, 'views/contract/expiring', '合同到期提醒规则入口', 0),

(2007600000000000020, 2007600000000000003, 'contract_contract_add', '新增', 'add', '/contract/contract/add', 'plus', 1, 2, 1, 1, '', NULL, 0),
(2007600000000000021, 2007600000000000003, 'contract_contract_edit', '修改', 'edit', '/contract/contract/edit', 'form', 2, 2, 2, 1, '', NULL, 0),
(2007600000000000022, 2007600000000000003, 'contract_contract_delete', '删除', 'delete', '/api/blade-contract/contract/remove', 'delete', 3, 2, 3, 1, '', NULL, 0),
(2007600000000000023, 2007600000000000003, 'contract_contract_view', '查看', 'view', '/contract/contract/view', 'file-text', 4, 2, 2, 1, '', NULL, 0),
(2007600000000000024, 2007600000000000003, 'contract_contract_renew', '续签', 'renew', '/api/blade-contract/contract/renew', 'retweet', 5, 2, 2, 1, '', NULL, 0),
(2007600000000000025, 2007600000000000003, 'contract_contract_terminate', '终止', 'terminate', '/api/blade-contract/contract/terminate', 'close', 6, 2, 2, 1, '', NULL, 0),
(2007600000000000026, 2007600000000000003, 'contract_payment_confirm', '确认缴费', 'confirm', '/api/blade-contract/contract/payment/confirm', 'check', 7, 2, 2, 1, '', NULL, 0),
(2007600000000000027, 2007600000000000003, 'contract_payment_remind', '催缴', 'remind', '/api/blade-contract/contract/payment/remind', 'bell', 8, 2, 2, 1, '', NULL, 0);

INSERT IGNORE INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2007600000000100000 + menu_offset.offset_value, menu_offset.menu_id, 1123598816738675201
FROM (
  SELECT 1 AS offset_value, 2007600000000000001 AS menu_id UNION ALL
  SELECT 3, 2007600000000000003 UNION ALL
  SELECT 4, 2007600000000000004 UNION ALL
  SELECT 20, 2007600000000000020 UNION ALL
  SELECT 21, 2007600000000000021 UNION ALL
  SELECT 22, 2007600000000000022 UNION ALL
  SELECT 23, 2007600000000000023 UNION ALL
  SELECT 24, 2007600000000000024 UNION ALL
  SELECT 25, 2007600000000000025 UNION ALL
  SELECT 26, 2007600000000000026 UNION ALL
  SELECT 27, 2007600000000000027
) menu_offset;
