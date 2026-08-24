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
import java.util.Date;

/**
 * 园区活动实体。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_park_activity")
@Schema(description = "园区活动")
public class ParkActivity extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long memberId;

	private String title;
	private String coverUrl;
	private String summary;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date endTime;

	private String address;
	private String priceText;
	private String contactName;
	private String contactPhone;
	private String auditStatus;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long auditUserId;
	private String auditUserName;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date auditTime;
	private String auditOpinion;
	private Integer publishStatus;
	private Integer sortOrder;

	@TableField(exist = false)
	private String customerName;
}
