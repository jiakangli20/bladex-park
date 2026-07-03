-- 逾期管理 - 逾期提醒菜单

SET NAMES utf8mb4;

INSERT INTO `blade_menu`
(`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
SELECT 1890000000006000500,
       1890000000006000000,
       'finance_overdue_reminder',
       '逾期提醒',
       'menu',
       '/finance/overdue-reminder',
       'iconfont iconicon_doc',
       5,
       1,
       0,
       1,
       'views/finance/overdue-reminder',
       '财务逾期提醒与逾期审批闭环入口',
       0
WHERE NOT EXISTS (
	SELECT 1 FROM `blade_menu` WHERE `id` = 1890000000006000500
);

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.id, t.role_id, 1890000000006000500
FROM (
	SELECT 1890000000006500501 AS id, 1123598816738675201 AS role_id
	UNION ALL SELECT 1890000000006500502, 1123598816738675203
	UNION ALL SELECT 1890000000006500503, 1123598816738675204
	UNION ALL SELECT 1890000000006500504, 1123598816738675205
) t
WHERE NOT EXISTS (
	SELECT 1
	FROM `blade_role_menu` rm
	WHERE rm.role_id = t.role_id
	  AND rm.menu_id = 1890000000006000500
)
AND NOT EXISTS (
	SELECT 1
	FROM `blade_role_menu` rm_id
	WHERE rm_id.id = t.id
);

INSERT INTO `blade_menu`
(`id`, `parent_id`, `code`, `name`, `alias`, `path`, `source`, `sort`, `category`, `action`, `is_open`, `component`, `remark`, `is_deleted`)
SELECT 1890000000006000501,
       1890000000006000500,
       'finance_overdue_reminder_log',
       '联动日志',
       'log',
       '/api/blade-ics/payment/overdue-log-list',
       'list',
       1,
       2,
       2,
       1,
       '',
       '查看逾期处置合同联动日志',
       0
WHERE NOT EXISTS (
	SELECT 1 FROM `blade_menu` WHERE `id` = 1890000000006000501
);

INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT t.id, t.role_id, 1890000000006000501
FROM (
	SELECT 1890000000006500511 AS id, 1123598816738675201 AS role_id
	UNION ALL SELECT 1890000000006500512, 1123598816738675203
	UNION ALL SELECT 1890000000006500513, 1123598816738675204
	UNION ALL SELECT 1890000000006500514, 1123598816738675205
) t
WHERE NOT EXISTS (
	SELECT 1
	FROM `blade_role_menu` rm
	WHERE rm.role_id = t.role_id
	  AND rm.menu_id = 1890000000006000501
)
AND NOT EXISTS (
	SELECT 1
	FROM `blade_role_menu` rm_id
	WHERE rm_id.id = t.id
);
