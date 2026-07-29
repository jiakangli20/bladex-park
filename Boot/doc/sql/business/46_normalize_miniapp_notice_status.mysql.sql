-- 当前未接入真实微信小程序发送通道，清理历史代码误写的成功状态
UPDATE `biz_payment_notice`
SET `miniapp_status` = 'reserved',
    `miniapp_send_time` = NULL,
    `update_by` = 'system-p1-fix',
    `update_time` = NOW()
WHERE `del_flag` = '0'
  AND `miniapp_status` = 'success';

SELECT COUNT(*) AS invalid_miniapp_success_count
FROM `biz_payment_notice`
WHERE `del_flag` = '0'
  AND `miniapp_status` = 'success';
