-- 2.3 数据校验脏数据清理
-- 适用条件：
-- 1. 业务表 park_id 仍保存历史园区编码，无法直接关联 ics_park.id；
-- 2. 手填企业合同流程记录中 customer_id 写入 -1 等无效客户；
-- 3. 运行中的入驻流程 current_node 误显示为“流程结束”；
-- 4. blade_role_menu 仍保留已删除菜单的授权记录。

START TRANSACTION;

-- 历史园区编码回写为当前园区主键，保留原有 ics_park.park_id 映射语义。
UPDATE biz_customer c
JOIN ics_park p ON p.park_id = c.park_id
LEFT JOIN ics_park active_park ON active_park.id = c.park_id
SET c.park_id = p.id,
    c.update_by = 'data-clean',
    c.update_time = NOW()
WHERE COALESCE(c.del_flag, '0') = '0'
  AND c.park_id IS NOT NULL
  AND active_park.id IS NULL;

UPDATE biz_business_opportunity o
JOIN ics_park p ON p.park_id = o.park_id
LEFT JOIN ics_park active_park ON active_park.id = o.park_id
SET o.park_id = p.id,
    o.update_by = 'data-clean',
    o.update_time = NOW()
WHERE COALESCE(o.del_flag, '0') = '0'
  AND o.park_id IS NOT NULL
  AND active_park.id IS NULL;

-- 合同流程记录的客户以合同主档为准；手填企业无客户主档时置空，不保留 -1 伪主键。
UPDATE biz_contract_workflow_record w
LEFT JOIN biz_customer current_customer
       ON current_customer.customer_id = w.customer_id
      AND COALESCE(current_customer.del_flag, '0') = '0'
LEFT JOIN biz_contract c
       ON c.contract_id = w.contract_id
      AND COALESCE(c.del_flag, '0') = '0'
LEFT JOIN biz_customer contract_customer
       ON contract_customer.customer_id = c.customer_id
      AND COALESCE(contract_customer.del_flag, '0') = '0'
SET w.customer_id = contract_customer.customer_id,
    w.update_by = 'data-clean',
    w.update_time = NOW()
WHERE COALESCE(w.del_flag, '0') = '0'
  AND w.customer_id IS NOT NULL
  AND current_customer.customer_id IS NULL;

-- 运行中流程不能展示“流程结束”，按当前 Flowable 待办节点 / 办理人回填。
UPDATE biz_business_opportunity o
JOIN ACT_RU_TASK t ON t.PROC_INST_ID_ = o.tenant_entry_process_ins_id
LEFT JOIN blade_user u ON CAST(u.id AS CHAR) = t.ASSIGNEE_
SET o.tenant_entry_current_node = COALESCE(NULLIF(t.NAME_, ''), u.real_name, t.TASK_DEF_KEY_, '审批中'),
    o.update_by = 'data-clean',
    o.update_time = NOW()
WHERE COALESCE(o.del_flag, '0') = '0'
  AND o.tenant_entry_status = 'running'
  AND o.tenant_entry_current_node = '流程结束';

UPDATE biz_customer c
JOIN ACT_RU_TASK t ON t.PROC_INST_ID_ = c.tenant_entry_process_ins_id
LEFT JOIN blade_user u ON CAST(u.id AS CHAR) = t.ASSIGNEE_
SET c.tenant_entry_current_node = COALESCE(NULLIF(t.NAME_, ''), u.real_name, t.TASK_DEF_KEY_, '审批中'),
    c.update_by = 'data-clean',
    c.update_time = NOW()
WHERE COALESCE(c.del_flag, '0') = '0'
  AND c.tenant_entry_status = 'running'
  AND c.tenant_entry_current_node = '流程结束';

-- 角色菜单授权不应继续挂在已删除菜单上。
DELETE rm
FROM blade_role_menu rm
JOIN blade_menu m ON m.id = rm.menu_id
WHERE m.is_deleted <> 0;

COMMIT;

-- 验证：
-- SELECT COUNT(*) AS customer_missing_park
-- FROM biz_customer c LEFT JOIN ics_park p ON p.id = c.park_id
-- WHERE COALESCE(c.del_flag,'0')='0' AND c.park_id IS NOT NULL AND p.id IS NULL;
-- SELECT COUNT(*) AS opportunity_missing_park
-- FROM biz_business_opportunity o LEFT JOIN ics_park p ON p.id = o.park_id
-- WHERE COALESCE(o.del_flag,'0')='0' AND o.park_id IS NOT NULL AND p.id IS NULL;
-- SELECT COUNT(*) AS workflow_customer_missing_customer
-- FROM biz_contract_workflow_record w LEFT JOIN biz_customer c ON c.customer_id = w.customer_id AND COALESCE(c.del_flag,'0')='0'
-- WHERE COALESCE(w.del_flag,'0')='0' AND w.customer_id IS NOT NULL AND c.customer_id IS NULL;
-- SELECT COUNT(*) AS running_entry_end_node
-- FROM biz_business_opportunity
-- WHERE COALESCE(del_flag,'0')='0' AND tenant_entry_status='running' AND tenant_entry_current_node='流程结束';
-- SELECT COUNT(*) AS role_menu_deleted_menu
-- FROM blade_role_menu rm JOIN blade_menu m ON m.id = rm.menu_id
-- WHERE m.is_deleted <> 0;
