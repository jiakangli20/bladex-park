-- 新增吴中金融招商服务有限公司组织机构
-- 执行库：wzjk / bladex_boot
-- 说明：在机构管理中新增公司及 6 个下属部门；脚本可重复执行，重复执行会恢复并更新同名机构。

SET NAMES utf8mb4;

SET @company_id := 2072600000000000001;
SET @tenant_id := '000000';

INSERT INTO `blade_dept`
  (`id`, `tenant_id`, `parent_id`, `ancestors`, `leader_id`, `dept_category`, `dept_name`, `full_name`, `sort`, `remark`, `status`, `is_deleted`)
VALUES
  (@company_id, @tenant_id, 0, '0', NULL, 1, '苏州市吴中金融招商服务有限公司', '苏州市吴中金融招商服务有限公司', 2, '园区业务组织机构', 1, 0)
ON DUPLICATE KEY UPDATE
  `tenant_id` = VALUES(`tenant_id`),
  `parent_id` = VALUES(`parent_id`),
  `ancestors` = VALUES(`ancestors`),
  `dept_category` = VALUES(`dept_category`),
  `dept_name` = VALUES(`dept_name`),
  `full_name` = VALUES(`full_name`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`),
  `is_deleted` = 0;

UPDATE `blade_dept`
SET
  `tenant_id` = @tenant_id,
  `parent_id` = 0,
  `ancestors` = '0',
  `dept_category` = 1,
  `dept_name` = '苏州市吴中金融招商服务有限公司',
  `full_name` = '苏州市吴中金融招商服务有限公司',
  `sort` = 2,
  `remark` = '园区业务组织机构',
  `status` = 1,
  `is_deleted` = 0
WHERE `dept_name` = '苏州市吴中金融招商服务有限公司';

INSERT INTO `blade_dept`
  (`id`, `tenant_id`, `parent_id`, `ancestors`, `leader_id`, `dept_category`, `dept_name`, `full_name`, `sort`, `remark`, `status`, `is_deleted`)
VALUES
  (2072600000000000002, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '招商部', '招商部', 1, '苏州市吴中金融招商服务有限公司下属部门', 1, 0),
  (2072600000000000003, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '服务部', '服务部', 2, '苏州市吴中金融招商服务有限公司下属部门', 1, 0),
  (2072600000000000004, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '综合管理部', '综合管理部', 3, '苏州市吴中金融招商服务有限公司下属部门', 1, 0),
  (2072600000000000005, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '运营中心', '运营中心', 4, '苏州市吴中金融招商服务有限公司下属部门', 1, 0),
  (2072600000000000006, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '财务部', '财务部', 5, '苏州市吴中金融招商服务有限公司下属部门', 1, 0),
  (2072600000000000007, @tenant_id, @company_id, CONCAT('0,', @company_id), NULL, 1, '风控部', '风控部', 6, '苏州市吴中金融招商服务有限公司下属部门', 1, 0)
ON DUPLICATE KEY UPDATE
  `tenant_id` = VALUES(`tenant_id`),
  `parent_id` = VALUES(`parent_id`),
  `ancestors` = VALUES(`ancestors`),
  `dept_category` = VALUES(`dept_category`),
  `dept_name` = VALUES(`dept_name`),
  `full_name` = VALUES(`full_name`),
  `sort` = VALUES(`sort`),
  `remark` = VALUES(`remark`),
  `status` = VALUES(`status`),
  `is_deleted` = 0;

UPDATE `blade_dept` d
JOIN (
  SELECT '招商部' AS dept_name, 1 AS sort_no UNION ALL
  SELECT '服务部', 2 UNION ALL
  SELECT '综合管理部', 3 UNION ALL
  SELECT '运营中心', 4 UNION ALL
  SELECT '财务部', 5 UNION ALL
  SELECT '风控部', 6
) seed ON seed.dept_name = d.dept_name
SET
  d.`tenant_id` = @tenant_id,
  d.`parent_id` = @company_id,
  d.`ancestors` = CONCAT('0,', @company_id),
  d.`dept_category` = 1,
  d.`full_name` = seed.dept_name,
  d.`sort` = seed.sort_no,
  d.`remark` = '苏州市吴中金融招商服务有限公司下属部门',
  d.`status` = 1,
  d.`is_deleted` = 0
WHERE d.`parent_id` = @company_id
   OR d.`dept_name` IN ('招商部', '服务部', '综合管理部', '运营中心', '财务部', '风控部');

SELECT `id`, `parent_id`, `ancestors`, `dept_name`, `full_name`, `sort`, `status`, `is_deleted`
FROM `blade_dept`
WHERE `id` = @company_id
   OR `parent_id` = @company_id
ORDER BY `parent_id`, `sort`, `id`;
