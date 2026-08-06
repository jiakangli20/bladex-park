/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 水电计费后台请求 DTO。
 *
 * @author Chill
 */
public interface UtilityBillingDTO {

	@Data
	class BillingRequest {
		@NotNull(message = "起始抄表记录不能为空")
		private Long startRecordId;

		@NotNull(message = "截止抄表记录不能为空")
		private Long endRecordId;

		@NotNull(message = "计费单价不能为空")
		@DecimalMin(value = "0.000001", message = "计费单价必须大于0")
		private BigDecimal unitPrice;

		@NotNull(message = "缴费截止日期不能为空")
		@JsonFormat(pattern = "yyyy-MM-dd")
		private Date payDeadline;

		private String remark;
	}

	@Data
	class SubmissionQuery {
		private Long parkId;
		private Long customerId;
		private Long paymentId;
		private String submitStatus;
	}

	@Data
	class AuditRequest {
		private String opinion;

		@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
		private Date payTime;
	}
}
