/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;

import java.util.Date;

/**
 * 用户邮箱配置视图.
 */
@Data
@Schema(description = "用户邮箱配置视图")
public class UserMailAccountVO {

	private String emailAddress;

	private Boolean authCodeConfigured;

	private Boolean enabled;

	private String lastTestStatus;

	private String lastTestMessage;

	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date lastTestTime;

}
