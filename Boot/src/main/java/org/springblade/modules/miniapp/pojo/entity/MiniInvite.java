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
 * 小程序企业邀请码实体。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_mini_invite")
@Schema(description = "小程序企业邀请码")
public class MiniInvite extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	private String codeHash;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	private String roleCode;
	private String mobile;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date expireTime;

	private Integer maxUses;
	private Integer usedCount;
}
