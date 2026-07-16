-- 优化楼层列表和租控看板的房源筛选索引，可重复执行
SET @idx_room_park_exists = (
	SELECT COUNT(1)
	FROM information_schema.statistics
	WHERE table_schema = DATABASE()
	  AND table_name = 'ics_room'
	  AND index_name = 'idx_room_park'
);
SET @idx_room_park_sql = IF(
	@idx_room_park_exists = 0,
	'ALTER TABLE `ics_room` ADD INDEX `idx_room_park` (`park_id`)',
	'SELECT ''idx_room_park already exists'''
);
PREPARE idx_room_park_stmt FROM @idx_room_park_sql;
EXECUTE idx_room_park_stmt;
DEALLOCATE PREPARE idx_room_park_stmt;

SET @idx_room_building_floor_exists = (
	SELECT COUNT(1)
	FROM information_schema.statistics
	WHERE table_schema = DATABASE()
	  AND table_name = 'ics_room'
	  AND index_name = 'idx_room_building_floor'
);
SET @idx_room_building_floor_sql = IF(
	@idx_room_building_floor_exists = 0,
	'ALTER TABLE `ics_room` ADD INDEX `idx_room_building_floor` (`building_id`, `floor`)',
	'SELECT ''idx_room_building_floor already exists'''
);
PREPARE idx_room_building_floor_stmt FROM @idx_room_building_floor_sql;
EXECUTE idx_room_building_floor_stmt;
DEALLOCATE PREPARE idx_room_building_floor_stmt;
