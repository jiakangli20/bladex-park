-- 商机补充入驻审核回填字段
-- 说明：为商机保存意向楼层、免租期和租赁单价，脚本可重复执行。

SET NAMES utf8mb4;

SET @lease_floor_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_business_opportunity'
     AND column_name = 'lease_floor') = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `lease_floor` varchar(100) DEFAULT NULL COMMENT ''意向租赁楼层'' AFTER `intent_area`',
  'SELECT 1'
);
PREPARE lease_floor_stmt FROM @lease_floor_sql;
EXECUTE lease_floor_stmt;
DEALLOCATE PREPARE lease_floor_stmt;

SET @rent_free_period_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_business_opportunity'
     AND column_name = 'rent_free_period') = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `rent_free_period` varchar(100) DEFAULT NULL COMMENT ''免租期'' AFTER `lease_floor`',
  'SELECT 1'
);
PREPARE rent_free_period_stmt FROM @rent_free_period_sql;
EXECUTE rent_free_period_stmt;
DEALLOCATE PREPARE rent_free_period_stmt;

SET @unit_price_sql = IF(
  (SELECT COUNT(*)
   FROM information_schema.columns
   WHERE table_schema = DATABASE()
     AND table_name = 'biz_business_opportunity'
     AND column_name = 'unit_price') = 0,
  'ALTER TABLE `biz_business_opportunity` ADD COLUMN `unit_price` decimal(18,2) DEFAULT NULL COMMENT ''租赁单价（元）'' AFTER `rent_free_period`',
  'SELECT 1'
);
PREPARE unit_price_stmt FROM @unit_price_sql;
EXECUTE unit_price_stmt;
DEALLOCATE PREPARE unit_price_stmt;

SELECT column_name, column_type, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'biz_business_opportunity'
  AND column_name IN ('lease_floor', 'rent_free_period', 'unit_price')
ORDER BY ordinal_position;
