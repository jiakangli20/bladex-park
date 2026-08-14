-- 通知管理三类状态隔离
-- 说明：同一账单允许分别保存收款、催款和逾期通知的发送状态。
-- 可重复执行。

SET NAMES utf8mb4;

UPDATE `biz_payment_notice`
SET `notice_type` = 'payment-notice'
WHERE `notice_type` IS NULL
   OR `notice_type` = ''
   OR `notice_type` = 'invoice-apply';

SET @drop_old_index = (
	SELECT IF(
		COUNT(*) > 0,
		'ALTER TABLE `biz_payment_notice` DROP INDEX `uk_payment_notice_payment`',
		'SELECT 1'
	)
	FROM `information_schema`.`statistics`
	WHERE `table_schema` = DATABASE()
	  AND `table_name` = 'biz_payment_notice'
	  AND `index_name` = 'uk_payment_notice_payment'
);
PREPARE stmt FROM @drop_old_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_type_index = (
	SELECT IF(
		COUNT(*) = 0,
		'ALTER TABLE `biz_payment_notice` ADD UNIQUE KEY `uk_payment_notice_type` (`payment_id`, `notice_type`, `del_flag`)',
		'SELECT 1'
	)
	FROM `information_schema`.`statistics`
	WHERE `table_schema` = DATABASE()
	  AND `table_name` = 'biz_payment_notice'
	  AND `index_name` = 'uk_payment_notice_type'
);
PREPARE stmt FROM @add_type_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT `index_name`, GROUP_CONCAT(`column_name` ORDER BY `seq_in_index`) AS `columns`
FROM `information_schema`.`statistics`
WHERE `table_schema` = DATABASE()
  AND `table_name` = 'biz_payment_notice'
  AND `non_unique` = 0
GROUP BY `index_name`;
