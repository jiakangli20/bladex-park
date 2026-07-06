-- 补齐账号锁定时长系统参数
-- 执行库：wzjk / bladex_boot
-- 说明：认证失败计数依赖 account.lockDuration；老库缺失时会导致登录失败处理出现系统错误。

SET NAMES utf8mb4;

INSERT INTO `blade_param`
  (`id`, `param_name`, `param_key`, `param_value`, `remark`, `create_user`, `create_dept`, `create_time`, `update_user`, `update_time`, `status`, `is_deleted`)
SELECT
  2074009000000000001,
  '账号锁定时长',
  'account.lockDuration',
  '30',
  '连续登录失败后的系统自动锁定时长，单位分钟',
  1123598821738675201,
  1123598813738675201,
  NOW(),
  1123598821738675201,
  NOW(),
  1,
  0
WHERE NOT EXISTS (
  SELECT 1
  FROM `blade_param`
  WHERE `param_key` = 'account.lockDuration'
    AND `is_deleted` = 0
);

UPDATE `blade_param`
SET
  `param_name` = '账号锁定时长',
  `param_value` = '30',
  `remark` = '连续登录失败后的系统自动锁定时长，单位分钟',
  `update_user` = 1123598821738675201,
  `update_time` = NOW(),
  `status` = 1,
  `is_deleted` = 0
WHERE `param_key` = 'account.lockDuration';

SELECT `param_key`, `param_value`, `status`, `is_deleted`
FROM `blade_param`
WHERE `param_key` IN ('account.failCount', 'account.lockDuration')
ORDER BY `param_key`;
