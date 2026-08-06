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
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.modules.miniapp.pojo.vo.MiniLoginVO;
import org.springblade.modules.miniapp.service.IMiniAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序当前会话接口。
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-miniapp/me")
@Tag(name = "小程序会话", description = "当前成员与能力集")
public class MiniMeController extends BladeController {

	private final IMiniAuthService authService;

	@GetMapping("/session")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "获取当前会话")
	public R<MiniLoginVO> session() {
		return R.data(authService.session());
	}
}
