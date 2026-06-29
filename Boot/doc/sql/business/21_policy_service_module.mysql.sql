-- 企业服务-政策服务发布
-- 说明：补齐政策服务页面所需业务表、后台接口权限与小程序接口权限；可重复执行。

CREATE TABLE IF NOT EXISTS `biz_policy_service` (
  `policy_id` bigint NOT NULL AUTO_INCREMENT COMMENT '政策ID',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `service_title` varchar(200) NOT NULL COMMENT '服务标题',
  `project_scope` varchar(100) NOT NULL COMMENT '项目范围/投放人群',
  `service_status` char(1) NOT NULL DEFAULT '0' COMMENT '服务状态(0已发布 1草稿 2已下线)',
  `view_count` bigint NOT NULL DEFAULT 0 COMMENT '浏览总数',
  `permanent_flag` char(1) NOT NULL DEFAULT '0' COMMENT '是否永久有效(0永久有效 1指定时间)',
  `valid_time` datetime DEFAULT NULL COMMENT '有效期',
  `online_flag` char(1) NOT NULL DEFAULT '0' COMMENT '上架状态(0上架 1下架)',
  `cover_url` varchar(500) DEFAULT NULL COMMENT '封面图',
  `content` text COMMENT '政策内容',
  `attachment_files` text COMMENT '附件',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`policy_id`),
  KEY `idx_policy_service_park` (`park_id`),
  KEY `idx_policy_service_status` (`service_status`, `online_flag`, `del_flag`),
  KEY `idx_policy_service_scope` (`project_scope`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='政策服务发布';

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000007000300, 1890000000007000000, 'enterprise_policy_service', '政策服务', 'menu', '/enterprise/policy-service', 'iconfont iconicon_doc', 'views/enterprise/policy-service', 3, 1, 0, 1, '政策服务入口', 0),
  (1890000000007000311, 1890000000007000300, 'enterprise_policy_service_list', '政策服务列表', 'list', '/api/blade-ics/policy-service/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000007000312, 1890000000007000300, 'enterprise_policy_service_add', '新增政策服务', 'add', '/api/blade-ics/policy-service/save', 'plus', '', 2, 2, 1, 1, NULL, 0),
  (1890000000007000313, 1890000000007000300, 'enterprise_policy_service_edit', '修改政策服务', 'edit', '/api/blade-ics/policy-service/update', 'form', '', 3, 2, 2, 1, NULL, 0),
  (1890000000007000314, 1890000000007000300, 'enterprise_policy_service_delete', '删除政策服务', 'delete', '/api/blade-ics/policy-service/remove', 'delete', '', 4, 2, 3, 1, NULL, 0),
  (1890000000007000315, 1890000000007000300, 'enterprise_policy_service_view', '查看政策服务', 'view', '/api/blade-ics/policy-service/detail', 'file-text', '', 5, 2, 2, 1, NULL, 0),
  (1890000000007000316, 1890000000007000300, 'enterprise_policy_service_online', '政策上架', 'status', '/api/blade-ics/policy-service/changeOnline', 'circle-check', '', 6, 2, 2, 1, NULL, 0),
  (1890000000007000317, 1890000000007000300, 'enterprise_policy_service_record', '提交记录', 'view', '/api/blade-ics/policy-service/submit-record', 'file-text', '', 7, 2, 0, 1, NULL, 0),
  (1890000000007000318, 1890000000007000300, 'enterprise_policy_service_miniapp_list', '小程序政策列表', 'list', '/api/blade-ics/policy-service/miniapp/list', 'list', '', 8, 2, 0, 1, NULL, 0),
  (1890000000007000319, 1890000000007000300, 'enterprise_policy_service_miniapp_detail', '小程序政策详情', 'view', '/api/blade-ics/policy-service/miniapp/detail', 'file-text', '', 9, 2, 0, 1, NULL, 0)
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `code` = VALUES(`code`),
  `name` = VALUES(`name`),
  `alias` = VALUES(`alias`),
  `path` = VALUES(`path`),
  `source` = VALUES(`source`),
  `component` = VALUES(`component`),
  `sort` = VALUES(`sort`),
  `category` = VALUES(`category`),
  `action` = VALUES(`action`),
  `is_open` = VALUES(`is_open`),
  `remark` = VALUES(`remark`),
  `is_deleted` = 0;

INSERT IGNORE INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000008000301, 1123598816738675201, 1890000000007000311),
  (1890000000008000302, 1123598816738675201, 1890000000007000312),
  (1890000000008000303, 1123598816738675201, 1890000000007000313),
  (1890000000008000304, 1123598816738675201, 1890000000007000314),
  (1890000000008000305, 1123598816738675201, 1890000000007000315),
  (1890000000008000306, 1123598816738675201, 1890000000007000316),
  (1890000000008000307, 1123598816738675201, 1890000000007000317),
  (1890000000008000308, 1123598816738675201, 1890000000007000318),
  (1890000000008000309, 1123598816738675201, 1890000000007000319);

SELECT COUNT(*) AS policy_service_table_ready FROM `biz_policy_service`;
