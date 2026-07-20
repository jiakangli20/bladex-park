-- 清理本地/测试库验收造数与乱码数据
-- 范围：ACCEPT-* 合同及其账单、通知、日志、业务流程记录和已结束 Flowable 历史实例；BladeX 测试公告。
-- 保护：如果命中的验收流程仍有运行时实例，脚本会中止，不删除业务数据。

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS `cleanup_contract_ids`;
CREATE TEMPORARY TABLE `cleanup_contract_ids` (
  `id` bigint NOT NULL PRIMARY KEY
) ENGINE=Memory;

INSERT IGNORE INTO `cleanup_contract_ids` (`id`)
SELECT `contract_id`
FROM `biz_contract`
WHERE `contract_no` LIKE 'ACCEPT-%';

DROP TEMPORARY TABLE IF EXISTS `cleanup_payment_ids`;
CREATE TEMPORARY TABLE `cleanup_payment_ids` (
  `id` bigint NOT NULL PRIMARY KEY
) ENGINE=Memory;

INSERT IGNORE INTO `cleanup_payment_ids` (`id`)
SELECT p.`payment_id`
FROM `biz_contract_payment` p
INNER JOIN `cleanup_contract_ids` c ON c.`id` = p.`contract_id`;

DROP TEMPORARY TABLE IF EXISTS `cleanup_process_ids`;
CREATE TEMPORARY TABLE `cleanup_process_ids` (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL PRIMARY KEY
) ENGINE=Memory;

INSERT IGNORE INTO `cleanup_process_ids` (`id`)
SELECT DISTINCT w.`process_ins_id`
FROM `biz_contract_workflow_record` w
INNER JOIN `cleanup_contract_ids` c ON c.`id` = w.`contract_id`
WHERE w.`process_ins_id` IS NOT NULL
  AND w.`process_ins_id` != '';

SET @dirty_running_process_count = (
  SELECT COUNT(*)
  FROM `ACT_RU_EXECUTION` e
  INNER JOIN `cleanup_process_ids` p
    ON CONVERT(e.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`
);

DROP PROCEDURE IF EXISTS `assert_acceptance_processes_finished`;
DELIMITER $$
CREATE PROCEDURE `assert_acceptance_processes_finished`()
BEGIN
  IF @dirty_running_process_count > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'ACCEPT 验收数据仍有关联运行中流程，请先在 Flowable 中终止后再清理';
  END IF;
END$$
DELIMITER ;
CALL `assert_acceptance_processes_finished`();
DROP PROCEDURE `assert_acceptance_processes_finished`;

DROP TEMPORARY TABLE IF EXISTS `cleanup_bytearray_ids`;
CREATE TEMPORARY TABLE `cleanup_bytearray_ids` (
  `id` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL PRIMARY KEY
) ENGINE=Memory;

INSERT IGNORE INTO `cleanup_bytearray_ids` (`id`)
SELECT DISTINCT v.`BYTEARRAY_ID_`
FROM `ACT_HI_VARINST` v
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(v.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`
WHERE v.`BYTEARRAY_ID_` IS NOT NULL;

INSERT IGNORE INTO `cleanup_bytearray_ids` (`id`)
SELECT DISTINCT d.`BYTEARRAY_ID_`
FROM `ACT_HI_DETAIL` d
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(d.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`
WHERE d.`BYTEARRAY_ID_` IS NOT NULL;

START TRANSACTION;

-- BladeX Workflow 扩展历史数据。
DELETE c
FROM `blade_wf_copy` c
INNER JOIN `cleanup_process_ids` p ON c.`process_id` = p.`id`;

DELETE v
FROM `blade_wf_form_variable` v
INNER JOIN `cleanup_process_ids` p ON v.`process_ins_id` = p.`id`;

DELETE l
FROM `blade_wf_process_leave` l
INNER JOIN `cleanup_process_ids` p ON l.`process_ins_id` = p.`id`;

DELETE l
FROM `blade_process_leave` l
INNER JOIN `cleanup_process_ids` p ON l.`process_instance_id` = p.`id`;

-- Flowable 已结束实例的历史与关联内容。
DELETE e
FROM `ACT_EVT_LOG` e
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(e.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE f
FROM `ACT_FO_FORM_INSTANCE` f
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(f.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE c
FROM `ACT_CO_CONTENT_ITEM` c
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(c.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE e
FROM `ACT_HI_ENTITYLINK` e
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(e.`SCOPE_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE e
FROM `ACT_HI_ENTITYLINK` e
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(e.`ROOT_SCOPE_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE e
FROM `ACT_HI_ENTITYLINK` e
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(e.`REF_SCOPE_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE a
FROM `ACT_HI_ATTACHMENT` a
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(a.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE c
FROM `ACT_HI_COMMENT` c
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(c.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE d
FROM `ACT_HI_DETAIL` d
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(d.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE i
FROM `ACT_HI_IDENTITYLINK` i
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(i.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE l
FROM `ACT_HI_TSK_LOG` l
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(l.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE a
FROM `ACT_HI_ACTINST` a
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(a.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE t
FROM `ACT_HI_TASKINST` t
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(t.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE v
FROM `ACT_HI_VARINST` v
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(v.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE h
FROM `ACT_HI_PROCINST` h
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(h.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;

DELETE b
FROM `ACT_GE_BYTEARRAY` b
INNER JOIN `cleanup_bytearray_ids` d ON d.`id` = b.`ID_`
WHERE NOT EXISTS (SELECT 1 FROM `ACT_RU_VARIABLE` rv WHERE rv.`BYTEARRAY_ID_` = b.`ID_`)
  AND NOT EXISTS (SELECT 1 FROM `ACT_HI_VARINST` hv WHERE hv.`BYTEARRAY_ID_` = b.`ID_`)
  AND NOT EXISTS (SELECT 1 FROM `ACT_HI_DETAIL` hd WHERE hd.`BYTEARRAY_ID_` = b.`ID_`);

-- 合同业务链路数据。
DELETE n
FROM `biz_payment_notice` n
INNER JOIN `cleanup_payment_ids` p ON p.`id` = n.`payment_id`;

DELETE n
FROM `biz_overdue_internal_notice` n
LEFT JOIN `cleanup_contract_ids` c ON c.`id` = n.`contract_id`
LEFT JOIN `cleanup_payment_ids` p ON p.`id` = n.`payment_id`
WHERE c.`id` IS NOT NULL OR p.`id` IS NOT NULL;

DELETE a
FROM `biz_contract_supplement_agreement` a
INNER JOIN `cleanup_contract_ids` c ON c.`id` = a.`contract_id`;

DELETE c
FROM `biz_contract_change` c
INNER JOIN `cleanup_contract_ids` ids ON ids.`id` = c.`contract_id`;

DELETE l
FROM `biz_contract_log` l
INNER JOIN `cleanup_contract_ids` c ON c.`id` = l.`contract_id`;

DELETE w
FROM `biz_contract_workflow_record` w
INNER JOIN `cleanup_contract_ids` c ON c.`id` = w.`contract_id`;

DELETE p
FROM `biz_contract_payment` p
INNER JOIN `cleanup_contract_ids` c ON c.`id` = p.`contract_id`;

DELETE c
FROM `biz_contract` c
INNER JOIN `cleanup_contract_ids` ids ON ids.`id` = c.`contract_id`;

-- BladeX 默认演示公告不是本项目业务数据。
DELETE FROM `blade_notice`
WHERE `title` LIKE '测试公告%';

-- 历史商机用 -1 表示“无审批项目”，改为真正空值。
UPDATE `biz_business_opportunity`
SET `approval_project_id` = NULL,
    `update_by` = 'data-clean',
    `update_time` = NOW()
WHERE `approval_project_id` IS NOT NULL
  AND `approval_project_id` <= 0;

COMMIT;

SELECT COUNT(*) AS `acceptance_contract_count`
FROM `biz_contract`
WHERE `contract_no` LIKE 'ACCEPT-%';

SELECT COUNT(*) AS `acceptance_payment_count`
FROM `biz_contract_payment` p
LEFT JOIN `biz_contract` c ON c.`contract_id` = p.`contract_id`
WHERE c.`contract_id` IS NULL;

SELECT COUNT(*) AS `question_mark_contract_name_count`
FROM `biz_contract`
WHERE `customer_name` REGEXP '[?][?]+';

SELECT COUNT(*) AS `invalid_opportunity_project_count`
FROM `biz_business_opportunity`
WHERE `approval_project_id` IS NOT NULL
  AND `approval_project_id` <= 0;

SELECT COUNT(*) AS `test_notice_count`
FROM `blade_notice`
WHERE `title` LIKE '测试公告%';

SELECT COUNT(*) AS `remaining_dirty_process_count`
FROM `ACT_HI_PROCINST` h
INNER JOIN `cleanup_process_ids` p
  ON CONVERT(h.`PROC_INST_ID_` USING utf8mb4) COLLATE utf8mb4_general_ci = p.`id`;
