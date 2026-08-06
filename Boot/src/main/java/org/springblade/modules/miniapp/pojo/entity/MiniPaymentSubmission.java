/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * Use of this software is governed by the Commercial License Agreement.
 */
package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 小程序线下付款凭证提交。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_mini_payment_submission")
@Schema(description = "小程序线下付款凭证提交")
public class MiniPaymentSubmission extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long paymentId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long memberId;
	private BigDecimal submitAmount;
	private String voucherName;
	private String voucherUrl;
	private String submitStatus;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long auditUserId;
	private String auditUserName;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date auditTime;
	private String auditOpinion;

	@TableField(exist = false)
	private String customerName;
	@TableField(exist = false)
	private String feeName;
	@TableField(exist = false)
	private BigDecimal amountDue;
	@TableField(exist = false)
	private BigDecimal amountPaid;
	@TableField(exist = false)
	private String payStatus;
	@TableField(exist = false)
	private String roomName;
}
