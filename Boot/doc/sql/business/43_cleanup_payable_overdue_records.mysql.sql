-- 逾期管理 - 清理误进入催缴链路的付款账单记录
-- 导入目标库：bladex_boot / wzjk
-- 规则：payable 为园区付款，不允许进入逾期催缴或内部逾期通知。

START TRANSACTION;

UPDATE `biz_overdue_reminder_record` r
INNER JOIN `biz_contract_payment` p ON p.`payment_id` = r.`payment_id`
SET r.`del_flag` = '1'
WHERE r.`del_flag` = '0'
  AND p.`direction` = 'payable';

UPDATE `biz_overdue_internal_notice` n
INNER JOIN `biz_contract_payment` p ON p.`payment_id` = n.`payment_id`
SET n.`del_flag` = '1'
WHERE n.`del_flag` = '0'
  AND p.`direction` = 'payable';

UPDATE `biz_contract_payment`
SET `remind_status` = '0',
	`remind_time` = NULL
WHERE `direction` = 'payable'
  AND (`remind_status` = '1' OR `remind_time` IS NOT NULL);

COMMIT;

SELECT COUNT(*) AS payable_reminder_record_count
FROM `biz_overdue_reminder_record` r
INNER JOIN `biz_contract_payment` p ON p.`payment_id` = r.`payment_id`
WHERE r.`del_flag` = '0'
  AND p.`direction` = 'payable';

SELECT COUNT(*) AS payable_internal_notice_count
FROM `biz_overdue_internal_notice` n
INNER JOIN `biz_contract_payment` p ON p.`payment_id` = n.`payment_id`
WHERE n.`del_flag` = '0'
  AND p.`direction` = 'payable';

SELECT COUNT(*) AS payable_reminded_payment_count
FROM `biz_contract_payment`
WHERE `direction` = 'payable'
  AND (`remind_status` = '1' OR `remind_time` IS NOT NULL);
