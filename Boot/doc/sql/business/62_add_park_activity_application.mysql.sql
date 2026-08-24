-- 企业活动申请、审核和发布管理。执行一次。
SET NAMES utf8mb4;

ALTER TABLE `biz_park_activity`
  ADD COLUMN `customer_id` bigint DEFAULT NULL COMMENT '申请企业客户ID' AFTER `park_id`,
  ADD COLUMN `member_id` bigint DEFAULT NULL COMMENT '小程序申请成员ID' AFTER `customer_id`,
  ADD COLUMN `contact_name` varchar(30) DEFAULT NULL COMMENT '联系人' AFTER `price_text`,
  ADD COLUMN `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话' AFTER `contact_name`,
  ADD COLUMN `audit_status` varchar(20) NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态' AFTER `contact_phone`,
  ADD COLUMN `audit_user_id` bigint DEFAULT NULL COMMENT '审核用户ID' AFTER `audit_status`,
  ADD COLUMN `audit_user_name` varchar(100) DEFAULT NULL COMMENT '审核人' AFTER `audit_user_id`,
  ADD COLUMN `audit_time` datetime DEFAULT NULL COMMENT '审核时间' AFTER `audit_user_name`,
  ADD COLUMN `audit_opinion` varchar(500) DEFAULT NULL COMMENT '审核意见' AFTER `audit_time`,
  ADD KEY `idx_park_activity_customer` (`tenant_id`,`park_id`,`customer_id`,`create_time`),
  ADD KEY `idx_park_activity_audit` (`tenant_id`,`park_id`,`audit_status`,`create_time`);

UPDATE `biz_park_activity`
SET `audit_status` = 'APPROVED'
WHERE `audit_status` IS NULL OR `audit_status` = '';

CREATE TABLE IF NOT EXISTS `biz_park_activity_audit_log` (
  `id` bigint NOT NULL COMMENT '主键',
  `tenant_id` varchar(12) NOT NULL DEFAULT '000000' COMMENT '租户ID',
  `activity_id` bigint NOT NULL COMMENT '活动ID',
  `park_id` bigint NOT NULL COMMENT '园区ID',
  `customer_id` bigint DEFAULT NULL COMMENT '申请企业ID',
  `action_type` varchar(32) NOT NULL COMMENT '操作类型',
  `before_audit_status` varchar(20) DEFAULT NULL,
  `after_audit_status` varchar(20) DEFAULT NULL,
  `before_publish_status` int DEFAULT NULL,
  `after_publish_status` int DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `operator_name` varchar(100) DEFAULT NULL,
  `opinion` varchar(500) DEFAULT NULL,
  `operate_time` datetime NOT NULL,
  `create_user` bigint DEFAULT NULL, `create_dept` bigint DEFAULT NULL, `create_time` datetime DEFAULT NULL,
  `update_user` bigint DEFAULT NULL, `update_time` datetime DEFAULT NULL,
  `status` int NOT NULL DEFAULT 1, `is_deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_activity_log_activity` (`tenant_id`,`activity_id`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='园区活动审核发布日志';

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (2091700000000000100, 1890000000007000000, 'park_activity', '园区活动', 'menu', '/enterprise/park-activity', 'iconfont iconicon_task', 'views/enterprise/park-activity', 7, 1, 0, 1, '园区活动申请审核与发布', 0),
  (2091700000000000101, 2091700000000000100, 'park_activity_view', '查看活动', 'view', '/api/blade-ics/park-activity/page', 'file-text', '', 1, 2, 0, 1, NULL, 0),
  (2091700000000000102, 2091700000000000100, 'park_activity_edit', '新增修改', 'edit', '/api/blade-ics/park-activity/submit', 'form', '', 2, 2, 2, 1, NULL, 0),
  (2091700000000000103, 2091700000000000100, 'park_activity_audit', '审核活动', 'audit', '/api/blade-ics/park-activity/audit', 'finished', '', 3, 2, 2, 1, NULL, 0),
  (2091700000000000104, 2091700000000000100, 'park_activity_publish', '发布活动', 'publish', '/api/blade-ics/park-activity/publish', 'circle-check', '', 4, 2, 2, 1, NULL, 0),
  (2091700000000000105, 2091700000000000100, 'park_activity_delete', '删除活动', 'delete', '/api/blade-ics/park-activity/remove', 'delete', '', 5, 2, 3, 1, NULL, 0)
ON DUPLICATE KEY UPDATE `parent_id`=VALUES(`parent_id`), `name`=VALUES(`name`), `path`=VALUES(`path`), `component`=VALUES(`component`), `is_deleted`=0;

INSERT INTO `blade_role_menu` (`id`, `menu_id`, `role_id`)
SELECT 2091710000000000000 + ROW_NUMBER() OVER (ORDER BY p.role_id, m.id), m.id, p.role_id
FROM `blade_role_menu` p
JOIN `blade_menu` m ON m.id BETWEEN 2091700000000000100 AND 2091700000000000105
WHERE p.menu_id = 1890000000007000000
  AND NOT EXISTS (SELECT 1 FROM `blade_role_menu` x WHERE x.role_id=p.role_id AND x.menu_id=m.id);

INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 2091720000000000001, t.id, 2091700000000000100
FROM `blade_top_menu` t
WHERE t.code='service' AND t.is_deleted=0
  AND NOT EXISTS (SELECT 1 FROM `blade_top_menu_setting` x WHERE x.top_menu_id=t.id AND x.menu_id=2091700000000000100)
LIMIT 1;
