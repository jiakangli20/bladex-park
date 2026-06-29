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
import org.springblade.modules.business.pojo.entity.PolicyService;
import org.springblade.modules.business.service.IPolicyServiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 政策服务发布控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "enterprise_policy_service")
@RequestMapping("/blade-ics/policy-service")
@io.swagger.v3.oas.annotations.tags.Tag(name = "政策服务", description = "政策服务发布接口")
public class PolicyServiceController extends BladeController {

	private final IPolicyServiceService policyServiceService;

	@GetMapping("/detail")
	@PreAuth(menu = "enterprise_policy_service_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入policyId")
	public R<PolicyService> detail(@Parameter(description = "政策ID") @RequestParam Long policyId) {
		return R.data(policyServiceService.selectPolicyById(policyId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "enterprise_policy_service_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "政策服务分页")
	public R<IPage<PolicyService>> page(PolicyService policy, Query query) {
		return R.data(policyServiceService.selectPolicyPage(Condition.getPage(query), policy));
	}

	@GetMapping("/list")
	@PreAuth(menu = "enterprise_policy_service_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "政策服务列表")
	public R<List<PolicyService>> list(PolicyService policy) {
		return R.data(policyServiceService.selectPolicyList(policy));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "enterprise_policy_service_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "统计", description = "政策服务统计")
	public R<Kv> statistics(PolicyService policy) {
		return R.data(policyServiceService.selectPolicyStatistics(policy));
	}

	@PostMapping("/save")
	@PreAuth(menu = "enterprise_policy_service_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增", description = "新增政策服务")
	public R save(@Valid @RequestBody PolicyService policy) {
		return R.status(policyServiceService.insertPolicy(policy));
	}

	@PostMapping("/update")
	@PreAuth(menu = "enterprise_policy_service_edit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改", description = "修改政策服务")
	public R update(@Valid @RequestBody PolicyService policy) {
		return R.status(policyServiceService.updatePolicy(policy));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "enterprise_policy_service_add")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "新增或修改", description = "新增或修改政策服务")
	public R submit(@Valid @RequestBody PolicyService policy) {
		return R.status(policyServiceService.submitPolicy(policy));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "enterprise_policy_service_delete")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "逻辑删除政策服务")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(policyServiceService.deletePolicyByIds(ids));
	}

	@PostMapping("/changeOnline/{policyId}")
	@PreAuth(menu = "enterprise_policy_service_online")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "上架变更", description = "政策服务上架或下架")
	public R changeOnline(@PathVariable Long policyId, @RequestParam String onlineFlag) {
		return R.status(policyServiceService.changeOnlineFlag(policyId, onlineFlag));
	}

	@PostMapping("/copy/{policyId}")
	@PreAuth(menu = "enterprise_policy_service_add")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "复制", description = "复制政策服务")
	public R copy(@PathVariable Long policyId) {
		return R.status(policyServiceService.copyPolicy(policyId));
	}

	@GetMapping("/submit-record")
	@PreAuth(menu = "enterprise_policy_service_view")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "提交记录", description = "传入policyId")
	public R<Kv> submitRecord(@Parameter(description = "政策ID") @RequestParam Long policyId) {
		PolicyService policy = policyServiceService.selectPolicyById(policyId);
		if (policy == null) {
			return R.fail("政策服务不存在");
		}
		return R.data(Kv.create()
			.set("policyId", policy.getPolicyId())
			.set("serviceTitle", policy.getServiceTitle())
			.set("serviceStatus", policy.getServiceStatus())
			.set("onlineFlag", policy.getOnlineFlag())
			.set("createBy", policy.getCreateBy())
			.set("createTime", policy.getCreateTime())
			.set("updateBy", policy.getUpdateBy())
			.set("updateTime", policy.getUpdateTime()));
	}

	@GetMapping("/miniapp/list")
	@PreAuth(menu = "enterprise_policy_service_miniapp_list")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "小程序政策服务列表", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<List<PolicyService>> miniAppList(PolicyService policy) {
		policy.setServiceStatus("0");
		policy.setOnlineFlag("0");
		return R.data(policyServiceService.selectPolicyList(policy));
	}

	@GetMapping("/miniapp/detail")
	@PreAuth(menu = "enterprise_policy_service_miniapp_detail")
	@ApiOperationSupport(order = 21)
	@Operation(summary = "小程序政策服务详情", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<PolicyService> miniAppDetail(@Parameter(description = "政策ID") @RequestParam Long policyId) {
		return R.data(policyServiceService.selectMiniAppPolicyById(policyId));
	}

}
