-- 付款账单独立付款凭证字段
-- 说明：账单原附件与财务实际付款凭证分开保存，脚本可重复执行。

SET NAMES utf8mb4;

SET @payment_voucher_name_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract_payment'
     AND column_name = 'payment_voucher_name') = 0,
  'ALTER TABLE `biz_contract_payment` ADD COLUMN `payment_voucher_name` varchar(255) DEFAULT NULL COMMENT ''付款凭证名称'' AFTER `attachment_url`',
  'SELECT 1'
);
PREPARE payment_voucher_name_stmt FROM @payment_voucher_name_sql;
EXECUTE payment_voucher_name_stmt;
DEALLOCATE PREPARE payment_voucher_name_stmt;

SET @payment_voucher_url_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract_payment'
     AND column_name = 'payment_voucher_url') = 0,
  'ALTER TABLE `biz_contract_payment` ADD COLUMN `payment_voucher_url` varchar(500) DEFAULT NULL COMMENT ''付款凭证地址'' AFTER `payment_voucher_name`',
  'SELECT 1'
);
PREPARE payment_voucher_url_stmt FROM @payment_voucher_url_sql;
EXECUTE payment_voucher_url_stmt;
DEALLOCATE PREPARE payment_voucher_url_stmt;

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_contract_payment'
  AND column_name IN ('payment_voucher_name', 'payment_voucher_url')
ORDER BY ordinal_position;
