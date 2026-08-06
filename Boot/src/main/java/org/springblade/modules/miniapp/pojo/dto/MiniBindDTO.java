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
package org.springblade.modules.miniapp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信身份绑定请求。
 *
 * @author Chill
 */
@Data
public class MiniBindDTO {
	@NotBlank(message = "绑定票据不能为空")
	private String bindTicket;
	@NotBlank(message = "微信手机号凭证不能为空")
	private String phoneCode;
	private String inviteCode;
	private String nickname;
}
