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
 * 水电账单计费明细。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_utility_bill_detail")
@Schema(description = "水电账单计费明细")
public class UtilityBillDetail extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long paymentId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long contractId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long roomId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;
	private String recordType;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long startRecordId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long endRecordId;
	private BigDecimal previousReading;
	private BigDecimal currentReading;
	private BigDecimal usageAmount;
	private BigDecimal unitPrice;
	private BigDecimal amount;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodStart;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodEnd;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date payDeadline;
	private String publishStatus;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long publishedBy;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date publishedTime;

	@TableField(exist = false)
	private String contractNo;
	@TableField(exist = false)
	private String customerName;
	@TableField(exist = false)
	private String roomName;
	@TableField(exist = false)
	private String deviceName;
}
