/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页待办提醒条目.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首页待办提醒条目")
public class HomeTodoItemVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "标题")
	private String title;

	@Schema(description = "描述")
	private String desc;

	@Schema(description = "跳转地址")
	private String path;

	@Schema(description = "图标")
	private String icon;

	@Schema(description = "色调")
	private String tone;

}
