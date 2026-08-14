/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 收款通知汇总.
 *
 * @author BladeX
 */
@Data
@Schema(description = "收款通知汇总")
public class PaymentNoticeSummaryVO {

	@Schema(description = "账单总数")
	private Long totalCount = 0L;

	@Schema(description = "已生成")
	private Long generatedCount = 0L;

	@Schema(description = "短信发送成功")
	private Long smsSuccessCount = 0L;

	@Schema(description = "邮箱发送成功")
	private Long emailSuccessCount = 0L;

	@Schema(description = "站内信发送成功")
	private Long inboxSuccessCount = 0L;

	@Schema(description = "小程序发送成功")
	private Long miniappSuccessCount = 0L;

	@Schema(description = "累计发送次数")
	private Long sendCount = 0L;

	@Schema(description = "达到5个工作日的账单数")
	private Long fiveBusinessDayCount = 0L;

	@Schema(description = "已催款账单数")
	private Long remindedBillCount = 0L;

	@Schema(description = "累计催款次数")
	private Long reminderCount = 0L;

	@Schema(description = "达到20个工作日的账单数")
	private Long twentyBusinessDayCount = 0L;

	@Schema(description = "律师函审批通过数")
	private Long approvedLegalCount = 0L;

}
