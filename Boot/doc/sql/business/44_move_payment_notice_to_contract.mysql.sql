-- 将“收款通知”从逾期管理迁移到合同管理。
-- 说明：页面与按钮权限继续复用原菜单，仅调整父菜单、路由、排序和顶部菜单归属，可重复执行。

SET NAMES utf8mb4;

SET @contract_menu_id := (
  SELECT `id`
  FROM `blade_menu`
  WHERE `code` = 'contract'
    AND `is_deleted` = 0
  LIMIT 1
);

SET @contract_top_menu_id := (
  SELECT MIN(setting.`top_menu_id`)
  FROM `blade_top_menu_setting` setting
  WHERE setting.`menu_id` = @contract_menu_id
);

UPDATE `blade_menu`
SET `parent_id` = @contract_menu_id,
    `name` = '收款通知',
    `path` = '/contract/payment-notice',
    `component` = 'views/contract/payment-notice',
    `sort` = 7,
    `remark` = '合同收款账单开票申请与通知发送',
    `is_deleted` = 0
WHERE `code` = 'finance_payment_notice';

-- 固定合同管理左侧菜单顺序。
UPDATE `blade_menu`
SET `sort` = CASE `code`
  WHEN 'contract_contract' THEN 1
  WHEN 'contract_expiry_notice' THEN 2
  WHEN 'contract_expiring' THEN 3
  WHEN 'contract_archive' THEN 4
  WHEN 'contract_termination' THEN 5
  WHEN 'contract_print_template' THEN 6
  WHEN 'finance_payment_notice' THEN 7
  ELSE `sort`
END
WHERE `parent_id` = @contract_menu_id
  AND `code` IN (
    'contract_contract',
    'contract_expiry_notice',
    'contract_expiring',
    'contract_archive',
    'contract_termination',
    'contract_print_template',
    'finance_payment_notice'
  );

-- 删除原逾期管理顶部菜单映射，再加入合同管理顶部菜单。
DELETE FROM `blade_top_menu_setting`
WHERE `menu_id` = 1890000000006000400
  AND @contract_top_menu_id IS NOT NULL
  AND `top_menu_id` != @contract_top_menu_id;

INSERT INTO `blade_top_menu_setting` (`id`, `top_menu_id`, `menu_id`)
SELECT 1890000000006600440, @contract_top_menu_id, 1890000000006000400
WHERE @contract_top_menu_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_top_menu_setting`
    WHERE `top_menu_id` = @contract_top_menu_id
      AND `menu_id` = 1890000000006000400
  )
  AND NOT EXISTS (
    SELECT 1
    FROM `blade_top_menu_setting`
    WHERE `id` = 1890000000006600440
  );

SELECT `id`, `parent_id`, `code`, `name`, `path`, `component`, `sort`, `is_deleted`
FROM `blade_menu`
WHERE `code` = 'finance_payment_notice';

SELECT COUNT(*) AS `button_count`
FROM `blade_menu`
WHERE `parent_id` = 1890000000006000400
  AND `category` = 2
  AND `is_deleted` = 0;

SELECT COUNT(DISTINCT `role_id`) AS `authorized_role_count`
FROM `blade_role_menu`
WHERE `menu_id` = 1890000000006000400;

SELECT `top_menu_id`, `menu_id`
FROM `blade_top_menu_setting`
WHERE `menu_id` = 1890000000006000400;
