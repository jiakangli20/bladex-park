/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.service.IEnterpriseDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 在园企业数据看板控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "enterprise_data")
@RequestMapping("/blade-ics/enterprise-data")
@io.swagger.v3.oas.annotations.tags.Tag(name = "在园企业数据", description = "在园企业数据看板接口")
public class EnterpriseDataController extends BladeController {

	private final IEnterpriseDataService enterpriseDataService;

	@GetMapping("/overview")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "在园企业数据总览", description = "传入parkId")
	public R<Kv> overview(@RequestParam(required = false) Long parkId) {
		return R.data(enterpriseDataService.overview(parkId));
	}

}
