/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户邮箱配置请求.
 */
@Data
@Schema(description = "用户邮箱配置请求")
public class UserMailAccountDTO {

	@Schema(description = "163邮箱地址")
	private String emailAddress;

	@Schema(description = "163 SMTP授权码，留空表示不修改")
	private String authCode;

	@Schema(description = "是否启用")
	private Boolean enabled;

}
