/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.modules.miniapp.pojo.entity.UtilityBillDetail;

/**
 * 水电计费预览。
 *
 * @author Chill
 */
@Data
@Schema(description = "水电计费预览")
public class UtilityBillingPreviewVO {

	@Schema(description = "是否可以生成账单")
	private boolean valid;

	@Schema(description = "异常或提示信息")
	private String message;

	@Schema(description = "计费快照")
	private UtilityBillDetail detail;
}
