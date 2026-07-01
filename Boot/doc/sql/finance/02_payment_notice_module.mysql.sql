-- 逾期管理-收款通知模块
-- 执行库：bladex_boot
-- 说明：
-- 1. 收款通知复用 biz_contract_payment 账单数据，biz_payment_notice 只记录通知状态、文件地址和发送回执。
-- 2. 所有账单、逾期账单、收款通知为三个独立页面入口；缴费管理入口隐藏保留历史权限。
-- 3. 小程序发送接口当前为预留接口，后续接入真实小程序通道后更新发送实现。

CREATE TABLE IF NOT EXISTS `biz_payment_notice` (
	`notice_id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
	`payment_id` bigint NOT NULL COMMENT '账单ID',
	`notice_no` varchar(64) NOT NULL COMMENT '通知编号',
	`notice_type` varchar(64) NOT NULL DEFAULT 'payment-notice' COMMENT '通知类型',
	`sms_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '短信发送状态：pending/success/failed',
	`email_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '邮箱发送状态：pending/success/failed',
	`inbox_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '站内信发送状态：pending/success/failed',
	`send_count` int NOT NULL DEFAULT 0 COMMENT '发送次数',
	`last_send_time` datetime NULL DEFAULT NULL COMMENT '最后发送时间',
	`file_name` varchar(255) NULL DEFAULT NULL COMMENT '文件名称',
	`file_url` varchar(500) NULL DEFAULT NULL COMMENT '文件地址',
	`miniapp_status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '小程序发送状态：pending/success/failed',
	`miniapp_send_time` datetime NULL DEFAULT NULL COMMENT '小程序发送时间',
	`remark` varchar(500) NULL DEFAULT NULL COMMENT '备注',
	`del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
	`create_by` varchar(64) NULL DEFAULT NULL COMMENT '创建人',
	`create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
	`update_by` varchar(64) NULL DEFAULT NULL COMMENT '更新人',
	`update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
	PRIMARY KEY (`notice_id`),
	UNIQUE KEY `uk_payment_notice_payment` (`payment_id`, `del_flag`),
	KEY `idx_payment_notice_status` (`sms_status`, `email_status`, `inbox_status`),
	KEY `idx_payment_notice_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款通知记录表';

INSERT INTO `blade_menu` (`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `component`, `sort`, `category`, `action`, `is_open`, `remark`, `is_deleted`)
VALUES
  (1890000000006000201, 1890000000006000200, 'finance_bills_all_list', '账单列表', 'list', '/api/blade-ics/payment/page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000006000202, 1890000000006000200, 'finance_bills_all_view', '账单详情', 'view', '/api/blade-ics/payment/detail', 'file-text', '', 2, 2, 2, 1, NULL, 0),
  (1890000000006000203, 1890000000006000200, 'finance_bills_all_log', '合同日志', 'log', '/api/blade-ics/payment/log-list', 'list', '', 3, 2, 2, 1, NULL, 0),
  (1890000000006000401, 1890000000006000400, 'finance_payment_notice_list', '收款通知列表', 'list', '/api/blade-ics/payment/notice-page', 'list', '', 1, 2, 0, 1, NULL, 0),
  (1890000000006000402, 1890000000006000400, 'finance_payment_notice_resend', '重新发送', 'resend', '/api/blade-ics/payment/notice-resend', 'refresh', '', 2, 2, 2, 1, NULL, 0),
  (1890000000006000403, 1890000000006000400, 'finance_payment_notice_download', '下载', 'download', '/api/blade-ics/payment/notice-generate', 'download', '', 3, 2, 2, 1, NULL, 0),
  (1890000000006000404, 1890000000006000400, 'finance_payment_notice_miniapp', '小程序发送', 'miniapp', '/api/blade-ics/payment/notice-miniapp-send', 'promotion', '', 4, 2, 2, 1, NULL, 0)
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

UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` IN (
  'finance_payment',
  'finance_payment_list',
  'finance_payment_view',
  'finance_payment_confirm',
  'finance_payment_remind'
);

UPDATE `blade_menu`
SET `is_deleted` = 0,
    `remark` = '收款通知列表与发送状态'
WHERE `code` = 'finance_payment_notice';

UPDATE `blade_menu`
SET `sort` = 1,
    `is_deleted` = 0,
    `remark` = '所有账单入口，按收款/付款两个方向展示'
WHERE `code` = 'finance_bills_all';

UPDATE `blade_menu`
SET `sort` = 2,
    `is_deleted` = 0
WHERE `code` = 'finance_bills_overdue';

UPDATE `blade_menu`
SET `sort` = 3,
    `is_deleted` = 0
WHERE `code` = 'finance_payment_notice';

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
VALUES
  (1890000000006100201, 1123598816738675201, 1890000000006000201),
  (1890000000006100202, 1123598816738675201, 1890000000006000202),
  (1890000000006100203, 1123598816738675201, 1890000000006000203),
  (1890000000006100401, 1123598816738675201, 1890000000006000401),
  (1890000000006100402, 1123598816738675201, 1890000000006000402),
  (1890000000006100403, 1123598816738675201, 1890000000006000403),
  (1890000000006100404, 1123598816738675201, 1890000000006000404)
ON DUPLICATE KEY UPDATE
  `role_id` = VALUES(`role_id`),
  `menu_id` = VALUES(`menu_id`);

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.id, t.role_id, t.menu_id
FROM (
  SELECT 1890000000006110201 AS id, 1123598816738675203 AS role_id, 1890000000006000201 AS menu_id
  UNION ALL SELECT 1890000000006110202, 1123598816738675203, 1890000000006000202
  UNION ALL SELECT 1890000000006110203, 1123598816738675203, 1890000000006000203
  UNION ALL SELECT 1890000000006110204, 1123598816738675204, 1890000000006000201
  UNION ALL SELECT 1890000000006110205, 1123598816738675204, 1890000000006000202
  UNION ALL SELECT 1890000000006110206, 1123598816738675204, 1890000000006000203
  UNION ALL SELECT 1890000000006110207, 1123598816738675205, 1890000000006000201
  UNION ALL SELECT 1890000000006110208, 1123598816738675205, 1890000000006000202
  UNION ALL SELECT 1890000000006110209, 1123598816738675205, 1890000000006000203
  UNION ALL SELECT 1890000000006110401, 1123598816738675203, 1890000000006000401
  UNION ALL SELECT 1890000000006110402, 1123598816738675203, 1890000000006000402
  UNION ALL SELECT 1890000000006110403, 1123598816738675203, 1890000000006000403
  UNION ALL SELECT 1890000000006110404, 1123598816738675203, 1890000000006000404
  UNION ALL SELECT 1890000000006110405, 1123598816738675204, 1890000000006000401
  UNION ALL SELECT 1890000000006110406, 1123598816738675204, 1890000000006000402
  UNION ALL SELECT 1890000000006110407, 1123598816738675204, 1890000000006000403
  UNION ALL SELECT 1890000000006110408, 1123598816738675204, 1890000000006000404
  UNION ALL SELECT 1890000000006110409, 1123598816738675205, 1890000000006000401
  UNION ALL SELECT 1890000000006110410, 1123598816738675205, 1890000000006000402
  UNION ALL SELECT 1890000000006110411, 1123598816738675205, 1890000000006000403
  UNION ALL SELECT 1890000000006110412, 1123598816738675205, 1890000000006000404
) t
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` rm
  WHERE rm.role_id = t.role_id
    AND rm.menu_id = t.menu_id
)
AND NOT EXISTS (
  SELECT 1
  FROM `blade_role_menu` rm_id
  WHERE rm_id.id = t.id
);

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `category`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN (
  'finance_payment',
  'finance_payment_list',
  'finance_payment_view',
  'finance_payment_confirm',
  'finance_payment_remind',
  'finance_bills_all',
  'finance_bills_all_list',
  'finance_bills_all_view',
  'finance_bills_all_log',
  'finance_bills_overdue',
  'finance_payment_notice',
  'finance_payment_notice_list',
  'finance_payment_notice_resend',
  'finance_payment_notice_download',
  'finance_payment_notice_miniapp'
)
ORDER BY `parent_id`, `sort`, `id`;
