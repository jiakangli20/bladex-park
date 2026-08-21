-- 区分内部逾期处置提醒与外部客户通知。
-- 可重复执行。

SET NAMES utf8mb4;

UPDATE `blade_menu`
SET `name` = '我的消息',
    `remark` = '当前账号收到的内部逾期处置提醒，PC与小程序共用消息记录'
WHERE `code` = 'finance_overdue_notice'
  AND `is_deleted` = 0;

UPDATE `blade_menu`
SET `remark` = '面向租户和客户发送收款、逾期和催款外部通知'
WHERE `code` = 'finance_payment_notice'
  AND `is_deleted` = 0;

UPDATE `biz_overdue_internal_notice`
SET `notice_title` = '逾期处置提醒'
WHERE `del_flag` = '0'
  AND `notice_title` IN ('合同账单催缴通知', '首次逾期通知');

SELECT `id`, `code`, `name`, `path`, `component`, `remark`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('finance_payment_notice', 'finance_overdue_reminder', 'finance_overdue_notice')
ORDER BY `id`;
