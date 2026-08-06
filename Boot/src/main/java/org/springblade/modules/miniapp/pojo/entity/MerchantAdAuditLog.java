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
 * 广告审核与上下架操作日志。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_merchant_ad_audit_log")
@Schema(description = "广告审核与上下架操作日志")
public class MerchantAdAuditLog extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long adId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	private String actionType;
	private String beforeAuditStatus;
	private String afterAuditStatus;
	private String beforeOnlineStatus;
	private String afterOnlineStatus;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long operatorUserId;
	private String operatorName;
	private String opinion;
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date operateTime;
}
