/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * Use of this software is governed by the Commercial License Agreement.
 */
package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
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
import java.util.Date;

/**
 * 小程序看房预约实体。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_house_appointment")
@Schema(description = "小程序看房预约")
public class HouseAppointment extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	private String appointmentNo;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long memberId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long roomId;

	private String enterpriseName;
	private String contactName;
	private String contactPhone;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date preferredTime;

	private String demandDesc;
	private String appointmentStatus;
	private String cancelReason;
}
