/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * Use of this software is governed by the Commercial License Agreement.
 */
package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;
import java.util.Date;

/**
 * 小程序成员绑定实体。
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_mini_member")
@Schema(description = "小程序成员绑定")
public class MiniMember extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	private String appId;
	private String openId;
	private String unionId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long userId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	private String mobile;
	private String roleCode;
	private String nickname;
	private Date lastLoginTime;
}
