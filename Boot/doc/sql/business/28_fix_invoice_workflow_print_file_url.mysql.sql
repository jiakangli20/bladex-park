-- 修复历史开票流程记录打印地址误指向付款通知单的问题
-- 幂等条件：仅处理开票流程/开票模板/开票表单，但 print_file_url 不是开票申请单地址的记录

UPDATE biz_contract_workflow_record
SET print_file_url = CONCAT('/blade-contract/print/invoice-apply/', payment_id),
    update_time = NOW()
WHERE COALESCE(del_flag, '0') = '0'
  AND payment_id IS NOT NULL
  AND (
    process_def_key = 'invoice'
    OR template_key = 'invoice-apply'
    OR form_key = 'invoice'
  )
  AND (
    print_file_url IS NULL
    OR print_file_url NOT LIKE '%/invoice-apply/%'
  );

-- 验证：结果应为 0
SELECT COUNT(*) AS invoice_workflow_wrong_print_url
FROM biz_contract_workflow_record
WHERE COALESCE(del_flag, '0') = '0'
  AND (
    process_def_key = 'invoice'
    OR template_key = 'invoice-apply'
    OR form_key = 'invoice'
  )
  AND (
    print_file_url IS NULL
    OR print_file_url NOT LIKE '%/invoice-apply/%'
  );
