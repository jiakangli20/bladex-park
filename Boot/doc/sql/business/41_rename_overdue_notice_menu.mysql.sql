-- 逾期管理 - “我的逾期通知”改名为“逾期通知”
-- 说明：只调整菜单展示名称，可重复执行。

SET NAMES utf8mb4;

UPDATE `blade_menu`
SET `name` = '逾期通知'
WHERE `code` = 'finance_overdue_notice'
  AND `is_deleted` = 0;

SELECT `id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` = 'finance_overdue_notice';
