-- 通知公告增加小程序首页展示标识。
ALTER TABLE `blade_notice`
  ADD COLUMN `home_flag` tinyint NOT NULL DEFAULT 0 COMMENT '小程序首页展示：1是，0否' AFTER `content`;

-- 仅保留一条默认首页公告，后续由后台“首页展示”开关维护。
UPDATE `blade_notice` n
JOIN (
  SELECT `id`
  FROM `blade_notice`
  WHERE `tenant_id` = '000000'
    AND `status` = 1
    AND `is_deleted` = 0
  ORDER BY `release_time` DESC, `create_time` DESC
  LIMIT 1
) selected ON selected.id = n.id
SET n.`home_flag` = 1;
