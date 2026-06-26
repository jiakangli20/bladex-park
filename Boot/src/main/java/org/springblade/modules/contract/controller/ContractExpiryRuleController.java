/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.modules.contract.pojo.entity.ContractExpiryRule;
import org.springblade.modules.contract.service.IContractExpiryRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同到期提醒规则控制器
 *
 * @author Chill
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "contract_expiring")
@RequestMapping("/blade-contract/expiring-rule")
@Tag(name = "合同到期提醒规则", description = "合同到期提醒规则接口")
public class ContractExpiryRuleController extends BladeController {

	private final IContractExpiryRuleService contractExpiryRuleService;

	/**
	 * 分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入rule")
	public R<IPage<ContractExpiryRule>> page(ContractExpiryRule rule, Query query) {
		return R.data(contractExpiryRuleService.selectRulePage(Condition.getPage(query), rule));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "新增或修改", description = "传入rule")
	public R<Boolean> submit(@RequestBody ContractExpiryRule rule) {
		return R.status(contractExpiryRuleService.submitRule(rule));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "删除", description = "传入ids")
	public R<Boolean> remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(contractExpiryRuleService.removeRules(ids));
	}

}
