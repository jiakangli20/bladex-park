/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.pojo.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 小程序登录及会话视图。
 *
 * @author Chill
 */
@Data
public class MiniLoginVO {
	private Boolean needBind = false;
	private String bindTicket;
	private String accessToken;
	private String refreshToken;
	private Integer expiresIn;
	private String tenantId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	private List<String> roleCodes = Collections.emptyList();
	private List<String> capabilities = Collections.emptyList();
	private MiniProfileVO profile;
}
