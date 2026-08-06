/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.modules.miniapp.pojo.dto.MiniBindDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniRefreshDTO;
import org.springblade.modules.miniapp.pojo.dto.MiniWechatLoginDTO;
import org.springblade.modules.miniapp.pojo.vo.MiniLoginVO;
import org.springblade.modules.miniapp.service.IMiniAuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序认证接口。
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-miniapp/auth")
@Tag(name = "小程序认证", description = "园区小程序微信登录与绑定")
public class MiniAuthController extends BladeController {

	private final IMiniAuthService authService;

	@PermitAll
	@PostMapping("/wechat-login")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "微信登录")
	public R<MiniLoginVO> wechatLogin(@Valid @RequestBody MiniWechatLoginDTO request) {
		return R.data(authService.wechatLogin(request));
	}

	@PermitAll
	@PostMapping("/bind")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "绑定手机号或企业邀请码")
	public R<MiniLoginVO> bind(@Valid @RequestBody MiniBindDTO request) {
		return R.data(authService.bind(request));
	}

	@PermitAll
	@PostMapping("/refresh")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "刷新访问令牌")
	public R<MiniLoginVO> refresh(@Valid @RequestBody MiniRefreshDTO request) {
		return R.data(authService.refresh(request));
	}

	@PostMapping("/logout")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "退出登录")
	public R<Void> logout() {
		authService.logout();
		return R.success("退出成功");
	}
}
