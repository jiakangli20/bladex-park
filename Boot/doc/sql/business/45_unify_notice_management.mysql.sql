-- 通知管理与逾期处理入口统一
-- 说明：通知管理统一承载收款、催款、逾期三类通知；律师函审批和发送归入逾期处理。
-- 可重复执行。

SET NAMES utf8mb4;

UPDATE `blade_menu`
SET `name` = '通知管理',
    `remark` = '统一管理收款、催款和逾期三类通知',
    `is_deleted` = 0
WHERE `code` = 'finance_payment_notice';

UPDATE `blade_menu`
SET `name` = '逾期处理',
    `remark` = '逾期账单、催款和律师函审批处置闭环',
    `is_deleted` = 0
WHERE `code` = 'finance_overdue_reminder';

UPDATE `blade_menu`
SET `is_deleted` = 1,
    `remark` = '已并入逾期处理，保留旧权限兼容'
WHERE `code` = 'finance_bills_overdue';

UPDATE `blade_menu`
SET `name` = '我的逾期消息',
    `remark` = '当前账号收到的内部逾期通知和催款记录'
WHERE `code` = 'finance_overdue_notice';

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('finance_payment_notice', 'finance_overdue_reminder', 'finance_bills_overdue', 'finance_overdue_notice')
ORDER BY `id`;
