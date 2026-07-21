-- 楼宇档案增加实景图片
-- 说明：保存 OSS 图片地址，多个地址以英文逗号分隔。脚本可重复执行。

SET NAMES utf8mb4;

SET @building_scene_images_exists = (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'ics_building'
    AND column_name = 'scene_images'
);
SET @building_scene_images_sql = IF(
  @building_scene_images_exists = 0,
  'ALTER TABLE `ics_building` ADD COLUMN `scene_images` text DEFAULT NULL COMMENT ''楼宇实景图片，多个地址以英文逗号分隔'' AFTER `standard_floor_height`',
  'SELECT ''ics_building.scene_images already exists'' AS message'
);
PREPARE building_scene_images_stmt FROM @building_scene_images_sql;
EXECUTE building_scene_images_stmt;
DEALLOCATE PREPARE building_scene_images_stmt;

SELECT COUNT(*) AS `building_scene_images_column_count`
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'ics_building'
  AND column_name = 'scene_images';
