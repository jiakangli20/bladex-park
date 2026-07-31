/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.BackgroundInvestigation;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 入驻管理背景调查控制器。
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "settlement_background_investigation")
@RequestMapping("/blade-park/background-investigation")
@io.swagger.v3.oas.annotations.tags.Tag(name = "背景调查", description = "企业背景调查及人工核验留痕接口")
public class BackgroundInvestigationController extends BladeController {

	private final IBusinessOpportunityService businessOpportunityService;

	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "待调查企业分页", description = "按商机展示入驻前背景调查企业")
	public R<IPage<BusinessOpportunity>> page(BusinessOpportunity opportunity, Query query) {
		return R.data(businessOpportunityService.selectBackgroundInvestigationPage(Condition.getPage(query), opportunity));
	}

	@GetMapping("/detail")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "调查详情", description = "查询指定商机的人工核验历史")
	public R<Map<String, Object>> detail(@RequestParam Long opportunityId) {
		return R.data(businessOpportunityService.queryBackgroundInvestigation(opportunityId));
	}

	@PostMapping("/save")
	@PreAuth(menu = "settlement_background_investigation_save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "保存人工核验", description = "追加保存人工核验结果并保留历史记录")
	public R<Map<String, Object>> save(@RequestBody BackgroundInvestigation investigation) {
		return R.data(businessOpportunityService.saveBackgroundInvestigation(investigation));
	}

}
