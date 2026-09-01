-- 恢复通知公告删除权限。历史数据中权限菜单被误标记为已删除，导致前端不显示删除按钮。
UPDATE `blade_menu`
SET `is_deleted` = 0
WHERE `code` = 'notice_delete'
  AND `parent_id` = (SELECT `id` FROM (SELECT `id` FROM `blade_menu` WHERE `code` = 'notice' LIMIT 1) parent_menu);
