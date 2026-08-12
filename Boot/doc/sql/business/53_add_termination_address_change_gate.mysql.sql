-- 退租押金付款申请增加客户注册地址变更前置条件
-- 说明：按合同保存注册地址变更确认快照，脚本可重复执行。

SET NAMES utf8mb4;

SET @address_change_status_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract'
     AND column_name = 'address_change_status') = 0,
  'ALTER TABLE `biz_contract` ADD COLUMN `address_change_status` char(1) NOT NULL DEFAULT ''0'' COMMENT ''退租注册地址变更状态：0未确认，1已确认'' AFTER `contract_file_url`',
  'SELECT 1'
);
PREPARE address_change_status_stmt FROM @address_change_status_sql;
EXECUTE address_change_status_stmt;
DEALLOCATE PREPARE address_change_status_stmt;

SET @address_change_address_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract'
     AND column_name = 'address_change_address') = 0,
  'ALTER TABLE `biz_contract` ADD COLUMN `address_change_address` varchar(500) DEFAULT NULL COMMENT ''退租后注册地址'' AFTER `address_change_status`',
  'SELECT 1'
);
PREPARE address_change_address_stmt FROM @address_change_address_sql;
EXECUTE address_change_address_stmt;
DEALLOCATE PREPARE address_change_address_stmt;

SET @address_change_time_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract'
     AND column_name = 'address_change_time') = 0,
  'ALTER TABLE `biz_contract` ADD COLUMN `address_change_time` datetime DEFAULT NULL COMMENT ''退租注册地址变更确认时间'' AFTER `address_change_address`',
  'SELECT 1'
);
PREPARE address_change_time_stmt FROM @address_change_time_sql;
EXECUTE address_change_time_stmt;
DEALLOCATE PREPARE address_change_time_stmt;

SET @address_change_by_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_contract'
     AND column_name = 'address_change_by') = 0,
  'ALTER TABLE `biz_contract` ADD COLUMN `address_change_by` varchar(64) DEFAULT NULL COMMENT ''退租注册地址变更确认人'' AFTER `address_change_time`',
  'SELECT 1'
);
PREPARE address_change_by_stmt FROM @address_change_by_sql;
EXECUTE address_change_by_stmt;
DEALLOCATE PREPARE address_change_by_stmt;

SELECT column_name, column_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_contract'
  AND column_name IN (
    'address_change_status',
    'address_change_address',
    'address_change_time',
    'address_change_by'
  )
ORDER BY ordinal_position;
