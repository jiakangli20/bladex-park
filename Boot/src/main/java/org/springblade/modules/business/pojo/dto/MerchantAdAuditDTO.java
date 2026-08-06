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
package org.springblade.modules.business.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 广告审核请求。
 *
 * @author Chill
 */
@Data
public class MerchantAdAuditDTO {

	@NotBlank(message = "审核结果不能为空")
	@Pattern(regexp = "APPROVED|REJECTED", message = "审核结果不正确")
	private String auditStatus;

	private String opinion;
}
