-- 吴中金控企业服务平台业务全量初始化脚本（已废弃，仅保留历史参考）
-- 说明：当前文件不完整，不再作为测试环境部署入口。
-- 后续部署请执行每次新增或修改的单个增量 SQL，具体命令记录在“模块迁移/部署/测试环境部署说明.md”。
-- 原适用场景：测试环境新库首次初始化。
-- 执行方式：请在项目根目录执行 mysql 导入命令，确保下方 SOURCE 路径可被 mysql 客户端找到。
-- 注意事项：
-- 1. 本脚本会串行执行协同办公、园区、入驻、合同、财务、企业服务等业务脚本。
-- 2. 仅建议新库首次执行一次；已有测试库请执行对应增量脚本，避免覆盖工作流配置或重复字段。
-- 3. Flowable 基础表不包含在此脚本中，请先执行 Boot/doc/sql/flowable/flowable.mysql.all.create.sql。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

SELECT 'workflow-plugin/bladex_workflow.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/workflow-plugin/bladex_workflow.mysql.sql

SELECT 'workflow-plugin/saber3_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/workflow-plugin/saber3_menu.mysql.sql

SELECT 'workflow-plugin/disable_legacy_flow_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/workflow-plugin/disable_legacy_flow_menu.mysql.sql

SELECT 'park/park.menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/park/park.menu.mysql.sql

SELECT 'park/building.menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/park/building.menu.mysql.sql

SELECT 'park/floor.menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/park/floor.menu.mysql.sql

SELECT 'park/rent-control.menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/park/rent-control.menu.mysql.sql

SELECT 'business/01_customer_tag_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/01_customer_tag_module.mysql.sql

SELECT 'business/02_settlement_opportunity_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/02_settlement_opportunity_module.mysql.sql

SELECT 'business/03_approval_base_tables.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/03_approval_base_tables.mysql.sql

SELECT 'business/04_property_service_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/04_property_service_module.mysql.sql

SELECT 'business/06_settlement_project_customer_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/06_settlement_project_customer_module.mysql.sql

SELECT 'business/07_approval_center_task_menus.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/07_approval_center_task_menus.mysql.sql

SELECT 'business/08_enterprise_merchant_service_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/08_enterprise_merchant_service_module.mysql.sql

SELECT 'business/09_home_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/09_home_menu.mysql.sql

SELECT 'business/10_tenant_entry_workflow.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/10_tenant_entry_workflow.mysql.sql

SELECT 'business/11_contract_workflow_record.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/11_contract_workflow_record.mysql.sql

SELECT 'business/12_finance_overdue_reminder_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/12_finance_overdue_reminder_menu.mysql.sql

SELECT 'business/13_contract_supplement_agreement.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/13_contract_supplement_agreement.mysql.sql

SELECT 'business/14_contract_expiring_rule.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/14_contract_expiring_rule.mysql.sql

SELECT 'business/18_property_service_miniapp_placeholder.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/18_property_service_miniapp_placeholder.mysql.sql

SELECT 'business/19_merchant_service_miniapp_placeholder.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/19_merchant_service_miniapp_placeholder.mysql.sql

SELECT 'business/20_merchant_ad_service_process.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/20_merchant_ad_service_process.mysql.sql

SELECT 'business/21_policy_service_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/21_policy_service_module.mysql.sql

SELECT 'business/22_migrate_home_to_desk.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/business/22_migrate_home_to_desk.mysql.sql

SELECT 'contract/bladex_contract_base_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/contract/bladex_contract_base_menu.mysql.sql

SELECT 'contract/bladex_contract_archive_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/contract/bladex_contract_archive_menu.mysql.sql

SELECT 'contract/bladex_contract_placeholder_menus.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/contract/bladex_contract_placeholder_menus.mysql.sql

SELECT 'contract/11_remove_contract_expense_menu.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/contract/11_remove_contract_expense_menu.mysql.sql

SELECT 'finance/01_payment_management_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/finance/01_payment_management_module.mysql.sql

SELECT 'finance/02_payment_notice_module.mysql.sql' AS executing_script;
SOURCE Boot/doc/sql/finance/02_payment_notice_module.mysql.sql

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'bladex_park_business.mysql.all.sql executed' AS result;
