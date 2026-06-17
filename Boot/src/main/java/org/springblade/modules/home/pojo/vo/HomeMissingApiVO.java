/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页待补接口说明.
 *
 * @author BladeX
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "首页待补接口说明")
public class HomeMissingApiVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "模块")
	private String module;

	@Schema(description = "接口或数据源")
	private String api;

	@Schema(description = "说明")
	private String reason;

	@Schema(description = "建议阶段")
	private String phase;

}
