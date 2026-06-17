-- 财务管理-缴费管理迁移脚本
-- 执行库：bladex_boot
-- 说明：
-- 1. 缴费管理为合同账单主流程，复用 biz_contract_payment / biz_contract_log。
-- 2. 所有账单、逾期账单为同一账单页的真实入口，逾期采用 pay_deadline < CURDATE() 且未缴清的动态口径。
-- 3. 收款通知当前阶段仅保留入口，暂不开发通知单主流程。

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000006000000, 0, 'finance', '财务管理', 'menu', '/finance', 'iconfont icon-shujukanban', '', 45, 1, 0, 1, '财务管理', 0),
  (1890000000006000100, 1890000000006000000, 'finance_payment', '缴费管理', 'menu', '/finance/payment', 'iconfont iconicon_savememo', 'views/finance/payment', 1, 1, 0, 1, '财务缴费管理主流程', 0),
  (1890000000006000101, 1890000000006000100, 'finance_payment_list', '账单列表', 'list', '/api/blade-ics/payment/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000006000102, 1890000000006000100, 'finance_payment_view', '账单详情', 'view', '/api/blade-ics/payment/detail', 'file-text', '', 2, 2, 2, 1, NULL, 0),
  (1890000000006000103, 1890000000006000100, 'finance_payment_confirm', '确认缴费', 'confirm', '/api/blade-ics/payment/confirm', 'circle-check', '', 3, 2, 2, 1, NULL, 0),
  (1890000000006000104, 1890000000006000100, 'finance_payment_remind', '催缴', 'remind', '/api/blade-ics/payment/remind', 'bell', '', 4, 2, 2, 1, NULL, 0),
  (1890000000006000200, 1890000000006000000, 'finance_bills_all', '所有账单', 'menu', '/finance/bills-all', 'iconfont iconicon_doc', 'views/finance/bills-all', 2, 1, 0, 1, '所有账单入口，复用缴费管理账单页', 0),
  (1890000000006000300, 1890000000006000000, 'finance_bills_overdue', '逾期账单', 'menu', '/finance/bills-overdue', 'iconfont iconicon_clock', 'views/finance/bills-overdue', 3, 1, 0, 1, '逾期账单入口，动态逾期口径', 0),
  (1890000000006000400, 1890000000006000000, 'finance_payment_notice', '收款通知', 'menu', '/finance/payment-notice', 'iconfont iconicon_sms', 'views/finance/payment-notice', 4, 1, 0, 1, '当前阶段仅保留入口，暂不开发通知单主流程', 0)
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
SELECT 1890000000006100001, 1123598816738675201, 1890000000006000000
WHERE NOT EXISTS (
  SELECT 1 FROM `blade_role_menu`
  WHERE `role_id` = 1123598816738675201 AND `menu_id` = 1890000000006000000
);

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000006100101, 1123598816738675201, 1890000000006000100),
  (1890000000006100102, 1123598816738675201, 1890000000006000101),
  (1890000000006100103, 1123598816738675201, 1890000000006000102),
  (1890000000006100104, 1123598816738675201, 1890000000006000103),
  (1890000000006100105, 1123598816738675201, 1890000000006000104),
  (1890000000006100106, 1123598816738675201, 1890000000006000200),
  (1890000000006100107, 1123598816738675201, 1890000000006000300),
  (1890000000006100108, 1123598816738675201, 1890000000006000400)
ON DUPLICATE KEY UPDATE
  `role_id` = VALUES(`role_id`),
  `menu_id` = VALUES(`menu_id`);

-- 首页租金收缴入口应跳转到 BladeX 财务缴费管理
UPDATE `blade_menu`
SET `path` = '/finance/payment',
    `component` = 'views/finance/payment',
    `is_deleted` = 0
WHERE `code` IN ('finance_payment');

-- 校验
SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `category`, `is_deleted`
FROM `blade_menu`
WHERE `id` IN (
  1890000000006000000,
  1890000000006000100,
  1890000000006000101,
  1890000000006000102,
  1890000000006000103,
  1890000000006000104,
  1890000000006000200,
  1890000000006000300,
  1890000000006000400
)
ORDER BY `parent_id`, `sort`, `id`;

SELECT COUNT(*) AS payment_total FROM `biz_contract_payment`;
SELECT COUNT(*) AS dynamic_overdue_total
FROM `biz_contract_payment`
WHERE `pay_status` <> '1'
  AND `pay_deadline` < CURDATE();
