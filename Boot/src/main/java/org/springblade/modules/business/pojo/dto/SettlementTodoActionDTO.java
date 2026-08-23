/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 招商待办处理参数。
 */
@Data
public class SettlementTodoActionDTO {
	@NotBlank(message = "处理动作不能为空")
	private String action;
	private String content;
	private String reason;
}
