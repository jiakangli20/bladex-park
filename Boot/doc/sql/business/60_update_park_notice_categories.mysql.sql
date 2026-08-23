-- 将 BladeX 通用通知类型调整为园区运营公告类型（可重复执行）。
SET NAMES utf8mb4;

UPDATE `blade_dict`
SET `dict_value` = '园区公告', `sort` = 1, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '1';

UPDATE `blade_dict`
SET `dict_value` = '政策通知', `sort` = 2, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '2';

UPDATE `blade_dict`
SET `dict_value` = '活动通知', `sort` = 3, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '3';

UPDATE `blade_dict`
SET `dict_value` = '服务通知', `sort` = 4, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '4';

UPDATE `blade_dict`
SET `dict_value` = '停水停电通知', `sort` = 5, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '5';

UPDATE `blade_dict`
SET `dict_value` = '安全提醒', `sort` = 6, `remark` = '园区运营公告发布类型', `status` = 1, `is_deleted` = 0
WHERE `parent_id` = 1123598814738675204 AND `code` = 'notice' AND `dict_key` = '6';

UPDATE `blade_dict`
SET `dict_value` = '园区公告类型', `remark` = '园区运营公告发布类型'
WHERE `id` = 1123598814738675204 AND `code` = 'notice' AND `parent_id` = 0;
