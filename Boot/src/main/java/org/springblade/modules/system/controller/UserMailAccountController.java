/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.pojo.dto.UserMailAccountDTO;
import org.springblade.modules.system.pojo.vo.UserMailAccountVO;
import org.springblade.modules.system.service.IUserMailAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前用户邮箱配置.
 */
@NonDS
@RestController
@RequiredArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/user/mail-account")
@Tag(name = "用户邮箱配置", description = "当前用户QQ邮箱绑定")
public class UserMailAccountController {

	private final IUserMailAccountService userMailAccountService;

	@GetMapping
	@Operation(summary = "查询当前用户邮箱配置")
	public R<UserMailAccountVO> detail() {
		return R.data(userMailAccountService.getCurrent());
	}

	@PostMapping
	@Operation(summary = "保存当前用户邮箱配置")
	public R<UserMailAccountVO> save(@RequestBody UserMailAccountDTO request) {
		return R.data(userMailAccountService.saveCurrent(request));
	}

	@PostMapping("/test")
	@Operation(summary = "测试当前用户邮箱连接")
	public R<UserMailAccountVO> test(@RequestBody UserMailAccountDTO request) {
		return R.data(userMailAccountService.testCurrent(request));
	}

}
