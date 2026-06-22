/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.IndustryRule;
import org.springblade.modules.business.service.IIndustryRuleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行业准入规则控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "business_opportunity")
@RequestMapping("/blade-park/industry-rule")
@io.swagger.v3.oas.annotations.tags.Tag(name = "行业准入规则", description = "行业准入规则接口")
public class IndustryRuleController extends BladeController {

	private final IIndustryRuleService industryRuleService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入ruleId")
	public R<IndustryRule> detail(@Parameter(description = "规则ID") @RequestParam Long ruleId) {
		return R.data(industryRuleService.selectIndustryRuleById(ruleId));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "列表", description = "行业准入规则列表")
	public R<List<IndustryRule>> list(IndustryRule rule) {
		return R.data(industryRuleService.selectIndustryRuleList(rule));
	}

	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "行业准入规则分页")
	public R<IPage<IndustryRule>> page(IndustryRule rule, Query query) {
		return R.data(industryRuleService.selectIndustryRulePage(Condition.getPage(query), rule));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "新增行业准入规则")
	public R save(@Valid @RequestBody IndustryRule rule) {
		return R.status(industryRuleService.insertIndustryRule(rule));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "修改行业准入规则")
	public R update(@Valid @RequestBody IndustryRule rule) {
		return R.status(industryRuleService.updateIndustryRule(rule));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "新增或修改行业准入规则")
	public R submit(@Valid @RequestBody IndustryRule rule) {
		return R.status(industryRuleService.submitIndustryRule(rule));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "逻辑删除行业准入规则")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(industryRuleService.deleteIndustryRuleByIds(ids));
	}

	@GetMapping("/check")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "行业检测", description = "按行业关键词检测准入结果")
	public R<Kv> check(@RequestParam(value = "industryKeyword", required = false) String industryKeyword) {
		return R.data(industryRuleService.checkIndustry(industryKeyword));
	}

}
