SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 协同办公工作流插件作为唯一工作流入口。
-- 逻辑下线原生 BladeX「流程管理」、顶层「我的事务」与二开「审批中心」菜单。
-- 不删除流程/审批历史数据，不关闭 /blade-flow/** 与 /blade-approval/** 兼容接口。
UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE (
    `code` REGEXP '^(flow|work)(_|$)'
    OR `code` = 'approval_center'
    OR LEFT(`code`, 9) = 'approval_'
  )
  AND `is_deleted` = 0;

-- 如需恢复原生工作流菜单，执行下面 SQL：
-- UPDATE `blade_menu`
-- SET `is_deleted` = 0
-- WHERE `code` REGEXP '^(flow|work)(_|$)';
--
-- 如需恢复旧审批中心菜单，执行下面 SQL：
-- UPDATE `blade_menu`
-- SET `is_deleted` = 0
-- WHERE `code` = 'approval_center'
--    OR LEFT(`code`, 9) = 'approval_';

SET FOREIGN_KEY_CHECKS = 1;
