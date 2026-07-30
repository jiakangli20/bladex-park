-- 收付款账单逐笔确认记录
-- 依赖：47_add_payment_voucher_fields.mysql.sql
-- 说明：每次财务确认收付款独立保存金额、时间、凭证和操作人，账单主表仅保存累计金额与最新凭证。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `biz_contract_payment_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `payment_id` bigint NOT NULL COMMENT '账单ID',
  `contract_id` bigint NOT NULL COMMENT '合同ID',
  `payment_amount` decimal(18,2) NOT NULL COMMENT '本次收付款金额',
  `cumulative_amount` decimal(18,2) NOT NULL COMMENT '确认后累计收付款金额',
  `payment_time` datetime NOT NULL COMMENT '收付款时间',
  `voucher_name` varchar(255) DEFAULT NULL COMMENT '收付款凭证名称',
  `voucher_url` varchar(500) DEFAULT NULL COMMENT '收付款凭证地址',
  `remark` varchar(500) DEFAULT NULL COMMENT '收付款备注',
  `operator_user_id` bigint DEFAULT NULL COMMENT '操作人用户ID',
  `operator_account` varchar(64) DEFAULT NULL COMMENT '操作人账号',
  `operator_name` varchar(64) DEFAULT NULL COMMENT '操作人姓名',
  `park_id` bigint DEFAULT NULL COMMENT '园区ID',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志',
  `create_by` varchar(64) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_payment_record_payment` (`payment_id`, `del_flag`, `payment_time`),
  KEY `idx_payment_record_contract` (`contract_id`, `del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款账单逐笔确认记录';

-- 对上线前已确认的收付款账单建立一条历史快照；重复执行不会重复写入。
INSERT INTO `biz_contract_payment_record` (
  `payment_id`, `contract_id`, `payment_amount`, `cumulative_amount`, `payment_time`,
  `voucher_name`, `voucher_url`, `remark`, `operator_account`, `operator_name`,
  `park_id`, `del_flag`, `create_by`, `create_time`
)
SELECT
  p.`payment_id`, p.`contract_id`, p.`amount_paid`, p.`amount_paid`,
  COALESCE(p.`pay_time`, p.`update_time`, p.`create_time`, NOW()),
  p.`payment_voucher_name`, p.`payment_voucher_url`,
  CASE WHEN p.`direction` = 'payable' THEN '历史付款数据迁移' ELSE '历史收款数据迁移' END,
  COALESCE(p.`update_by`, p.`create_by`), COALESCE(p.`update_by`, p.`create_by`),
  p.`park_id`, '0', COALESCE(p.`update_by`, p.`create_by`, 'system'),
  COALESCE(p.`update_time`, p.`pay_time`, p.`create_time`, NOW())
FROM `biz_contract_payment` p
WHERE p.`direction` IN ('receivable', 'payable')
  AND COALESCE(p.`amount_paid`, 0) > 0
  AND NOT EXISTS (
    SELECT 1
    FROM `biz_contract_payment_record` r
    WHERE r.`payment_id` = p.`payment_id`
      AND r.`del_flag` = '0'
  );

-- 合同管理旧确认接口已下线，付款只能从所有账单财务入口确认。
UPDATE `blade_menu`
SET `is_deleted` = 1
WHERE `code` = 'contract_payment_confirm';

-- 将原财务确认按钮迁移到“所有账单”，接口继续使用独立按钮权限控制。
UPDATE `blade_menu` confirm_menu
JOIN `blade_menu` bills_menu ON bills_menu.`code` = 'finance_bills_all'
SET confirm_menu.`parent_id` = bills_menu.`id`,
    confirm_menu.`name` = '确认收付款',
    confirm_menu.`path` = '/api/blade-ics/payment/confirm',
    confirm_menu.`sort` = 4,
    confirm_menu.`is_deleted` = 0
WHERE confirm_menu.`code` = 'finance_payment_confirm';

-- 仅继承已有“所有账单”角色授权，避免合同管理角色绕过财务账单权限。
INSERT INTO `blade_role_menu` (`id`, `role_id`, `menu_id`)
SELECT UUID_SHORT(), bills_role.`role_id`, confirm_menu.`id`
FROM `blade_role_menu` bills_role
JOIN `blade_menu` bills_menu
  ON bills_menu.`id` = bills_role.`menu_id`
 AND bills_menu.`code` = 'finance_bills_all'
JOIN `blade_menu` confirm_menu
  ON confirm_menu.`code` = 'finance_payment_confirm'
LEFT JOIN `blade_role_menu` existing_role
  ON existing_role.`role_id` = bills_role.`role_id`
 AND existing_role.`menu_id` = confirm_menu.`id`
WHERE existing_role.`id` IS NULL;

SELECT COUNT(*) AS `payment_record_count`
FROM `biz_contract_payment_record`
WHERE `del_flag` = '0';

SELECT `id`, `code`, `is_deleted`
FROM `blade_menu`
WHERE `code` IN ('contract_payment_confirm', 'finance_payment_confirm')
ORDER BY `code`;

SELECT COUNT(*) AS `finance_payment_confirm_role_count`
FROM `blade_role_menu` role_menu
JOIN `blade_menu` menu ON menu.`id` = role_menu.`menu_id`
WHERE menu.`code` = 'finance_payment_confirm';
