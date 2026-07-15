-- 所有账单：手工创建收款/付款账单字段
-- 执行库：bladex_boot / wzjk

DROP PROCEDURE IF EXISTS `add_payment_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `add_payment_column_if_missing`(
	IN column_name_value varchar(64),
	IN column_definition_value text
)
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM information_schema.columns
		WHERE table_schema = DATABASE()
		  AND table_name = 'biz_contract_payment'
		  AND column_name = column_name_value
	) THEN
		SET @payment_column_sql = CONCAT(
			'ALTER TABLE `biz_contract_payment` ADD COLUMN `',
			column_name_value,
			'` ',
			column_definition_value
		);
		PREPARE payment_column_statement FROM @payment_column_sql;
		EXECUTE payment_column_statement;
		DEALLOCATE PREPARE payment_column_statement;
	END IF;
END$$
DELIMITER ;

CALL `add_payment_column_if_missing`('direction', 'varchar(20) NOT NULL DEFAULT ''receivable'' COMMENT ''账单方向：receivable收款/payable付款'' AFTER `contract_id`');
CALL `add_payment_column_if_missing`('tax_rate', 'decimal(6,2) NOT NULL DEFAULT 0.00 COMMENT ''税率（%）'' AFTER `amount_due`');
CALL `add_payment_column_if_missing`('special_bill_type', 'varchar(30) NOT NULL DEFAULT ''regular'' COMMENT ''特殊账单类型'' AFTER `tax_rate`');
CALL `add_payment_column_if_missing`('late_fee_start_days', 'int NULL DEFAULT 0 COMMENT ''滞纳金起算天数'' AFTER `special_bill_type`');
CALL `add_payment_column_if_missing`('late_fee_ratio', 'decimal(10,4) NULL COMMENT ''滞纳金比例（%/天）'' AFTER `late_fee_start_days`');
CALL `add_payment_column_if_missing`('late_fee_cap', 'decimal(12,2) NULL COMMENT ''滞纳金上限（%）'' AFTER `late_fee_ratio`');
CALL `add_payment_column_if_missing`('company_name', 'varchar(100) NULL COMMENT ''所属公司'' AFTER `late_fee_cap`');
CALL `add_payment_column_if_missing`('attachment_name', 'varchar(255) NULL COMMENT ''附件名称'' AFTER `remark`');
CALL `add_payment_column_if_missing`('attachment_url', 'varchar(500) NULL COMMENT ''附件地址'' AFTER `attachment_name`');
CALL `add_payment_column_if_missing`('selected_room_ids', 'varchar(500) NULL COMMENT ''账单选择房源ID集合'' AFTER `attachment_url`');
CALL `add_payment_column_if_missing`('selected_room_name', 'varchar(500) NULL COMMENT ''账单选择房源名称'' AFTER `selected_room_ids`');
CALL `add_payment_column_if_missing`('selected_building_name', 'varchar(500) NULL COMMENT ''账单选择楼宇名称'' AFTER `selected_room_name`');

DROP PROCEDURE IF EXISTS `add_payment_column_if_missing`;

UPDATE `biz_contract_payment`
SET `direction` = CASE
	WHEN `fee_type` = 'deposit_refund' THEN 'payable'
	ELSE 'receivable'
END
WHERE `direction` IS NULL
   OR `direction` = ''
   OR (`fee_type` = 'deposit_refund' AND `direction` != 'payable');

SET @payment_direction_index_sql = IF(
	EXISTS (
		SELECT 1
		FROM information_schema.statistics
		WHERE table_schema = DATABASE()
		  AND table_name = 'biz_contract_payment'
		  AND index_name = 'idx_payment_direction'
	),
	'SELECT 1',
	'CREATE INDEX `idx_payment_direction` ON `biz_contract_payment` (`direction`)'
);
PREPARE payment_direction_index_statement FROM @payment_direction_index_sql;
EXECUTE payment_direction_index_statement;
DEALLOCATE PREPARE payment_direction_index_statement;

SELECT `column_name`, `column_type`, `is_nullable`, `column_default`
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_contract_payment'
  AND column_name IN (
	'direction',
	'tax_rate',
	'special_bill_type',
	'late_fee_start_days',
	'late_fee_ratio',
	'late_fee_cap',
	'company_name',
	'attachment_name',
	'attachment_url',
	'selected_room_ids',
	'selected_room_name',
	'selected_building_name'
  )
ORDER BY ordinal_position;

SELECT `direction`, COUNT(*) AS `bill_count`
FROM `biz_contract_payment`
GROUP BY `direction`
ORDER BY `direction`;
