/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 财务缴费汇总.
 *
 * @author BladeX
 */
@Data
@Schema(description = "财务缴费汇总")
public class PaymentSummaryVO {

	@Schema(description = "账单总数")
	private Long totalCount = 0L;

	@Schema(description = "未缴数量")
	private Long unpaidCount = 0L;

	@Schema(description = "已缴数量")
	private Long paidCount = 0L;

	@Schema(description = "逾期数量")
	private Long overdueCount = 0L;

	@Schema(description = "部分缴纳数量")
	private Long partialCount = 0L;

	@Schema(description = "已催缴数量")
	private Long remindedCount = 0L;

	@Schema(description = "应收金额")
	private BigDecimal amountDue = BigDecimal.ZERO;

	@Schema(description = "实收金额")
	private BigDecimal amountPaid = BigDecimal.ZERO;

	@Schema(description = "待收金额")
	private BigDecimal amountPending = BigDecimal.ZERO;

}
