/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收款通知占位信息.
 *
 * @author BladeX
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收款通知占位信息")
public class PaymentNoticePlaceholderVO {

	@Schema(description = "标题")
	private String title;

	@Schema(description = "当前阶段说明")
	private String message;

	@Schema(description = "后续扩展建议")
	private String nextStep;

}
