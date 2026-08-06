/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.miniapp.pojo.dto.UtilityBillingDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniPaymentSubmission;
import org.springblade.modules.miniapp.pojo.entity.UtilityBillDetail;
import org.springblade.modules.miniapp.pojo.vo.UtilityBillingPreviewVO;
import org.springblade.modules.miniapp.service.IUtilityBillingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 园区后台水电计费与付款凭证审核控制器。
 *
 * @author Chill
 */
@NonDS
@RestController
@RequiredArgsConstructor
@RequestMapping("/blade-ics/utility-billing")
@Tag(name = "水电账单管理", description = "抄表计费、账单发布和小程序付款凭证审核")
public class UtilityBillingController extends BladeController {

	private final IUtilityBillingService utilityBillingService;

	@PostMapping("/preview")
	@PreAuth(menu = "rent_control_utility_add")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "预览水电计费")
	public R<UtilityBillingPreviewVO> preview(@Valid @RequestBody UtilityBillingDTO.BillingRequest request) {
		return R.data(utilityBillingService.preview(request));
	}

	@PostMapping("/generate")
	@PreAuth(menu = "rent_control_utility_add")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "生成水电账单")
	public R<UtilityBillDetail> generate(@Valid @RequestBody UtilityBillingDTO.BillingRequest request) {
		return R.data(utilityBillingService.generate(request));
	}

	@PostMapping("/{id}/publish")
	@PreAuth(menu = "rent_control_utility_add")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "发布水电账单")
	public R publish(@PathVariable Long id) {
		return R.status(utilityBillingService.publish(id));
	}

	@GetMapping("/payment-submissions/page")
	@PreAuth(menu = "finance_payment_confirm")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "小程序付款凭证分页")
	public R<IPage<MiniPaymentSubmission>> paymentSubmissions(UtilityBillingDTO.SubmissionQuery request, Query query) {
		return R.data(utilityBillingService.paymentSubmissionPage(Condition.getPage(query), request));
	}

	@PostMapping("/payment-submissions/{id}/confirm")
	@PreAuth(menu = "finance_payment_confirm")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "确认付款凭证到账")
	public R confirm(@PathVariable Long id, @RequestBody(required = false) UtilityBillingDTO.AuditRequest request) {
		return R.status(utilityBillingService.confirmSubmission(id, request));
	}

	@PostMapping("/payment-submissions/{id}/reject")
	@PreAuth(menu = "finance_payment_confirm")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "驳回付款凭证")
	public R reject(@PathVariable Long id, @RequestBody UtilityBillingDTO.AuditRequest request) {
		return R.status(utilityBillingService.rejectSubmission(id, request));
	}
}
