-- 房源工作台：水电抄表记录与绑定车辆
-- 说明：新增两张独立业务表，脚本可重复执行。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_room_utility_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `device_id` bigint NOT NULL COMMENT '水表/电表设备ID',
  `record_type` varchar(20) NOT NULL COMMENT '记录类型：water水/electric电',
  `reading_time` datetime NOT NULL COMMENT '抄表时间',
  `previous_reading` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '上次读数',
  `current_reading` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '本次读数',
  `usage_amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '本期用量',
  `amount` decimal(18,2) NOT NULL DEFAULT 0 COMMENT '本期金额',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '抄表人',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常、1删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_utility_record_room_time` (`room_id`, `reading_time`),
  KEY `idx_utility_record_device_time` (`device_id`, `reading_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房源水电抄表记录';

CREATE TABLE IF NOT EXISTS `biz_room_vehicle` (
  `vehicle_id` bigint NOT NULL AUTO_INCREMENT COMMENT '车辆绑定ID',
  `room_id` bigint NOT NULL COMMENT '房源ID',
  `plate_no` varchar(32) NOT NULL COMMENT '车牌号',
  `vehicle_type` varchar(20) NOT NULL DEFAULT 'car' COMMENT '车辆类型',
  `owner_name` varchar(100) DEFAULT NULL COMMENT '车主姓名',
  `owner_phone` varchar(50) DEFAULT NULL COMMENT '联系电话',
  `valid_start` date DEFAULT NULL COMMENT '有效期开始',
  `valid_end` date DEFAULT NULL COMMENT '有效期结束',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态：active有效/disabled停用',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志：0正常、1删除',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`vehicle_id`),
  KEY `idx_room_vehicle_room_status` (`room_id`, `status`),
  KEY `idx_room_vehicle_plate` (`plate_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='房源绑定车辆';

SELECT COUNT(*) AS `room_utility_record_table_count`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'biz_room_utility_record';

SELECT COUNT(*) AS `room_vehicle_table_count`
FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'biz_room_vehicle';
