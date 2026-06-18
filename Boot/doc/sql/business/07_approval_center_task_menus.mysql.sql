SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 审批中心已弃用。
-- 流程配置 / 新建流程 / 待办 / 已办 / 抄送 / 统计统一走协同办公 workflow 原生插件。
-- 本脚本仅逻辑下线旧审批中心菜单，不删除 biz_approval_* 历史数据，不关闭 /blade-approval/** 兼容接口。
UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` = 'approval_center'
   OR LEFT(`code`, 9) = 'approval_';

SET FOREIGN_KEY_CHECKS = 1;
