/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.home.constant.HomeConstant;
import org.springblade.modules.home.pojo.vo.HomeMissingApiVO;
import org.springblade.modules.home.pojo.vo.HomeWorkbenchVO;
import org.springblade.modules.home.service.IHomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页工作台控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "desk")
@RequestMapping(HomeConstant.APPLICATION_HOME_NAME + "/home")
@Tag(name = "首页工作台", description = "首页聚合接口")
public class HomeController extends BladeController {

	private final IHomeService homeService;

	/**
	 * 首页工作台聚合数据.
	 */
	@GetMapping("/workbench")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "首页工作台聚合数据")
	public R<HomeWorkbenchVO> workbench() {
		return R.data(homeService.workbench());
	}

	/**
	 * 待补接口清单.
	 */
	@GetMapping("/missing-apis")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "首页待补接口清单")
	public R<List<HomeMissingApiVO>> missingApis() {
		return R.data(homeService.missingApis());
	}

}
