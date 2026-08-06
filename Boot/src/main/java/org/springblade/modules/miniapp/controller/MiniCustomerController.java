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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.miniapp.pojo.dto.MiniBusinessDTO;
import org.springblade.modules.miniapp.service.IMiniBusinessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 小程序企业客户接口。
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-miniapp/customer")
@Tag(name = "小程序企业客户", description = "企业资料、合同账单和业务申请")
public class MiniCustomerController {
	private final IMiniBusinessService businessService;

	@PostMapping("/appointments") @Operation(summary = "提交看房预约")
	public R<Map<String, Object>> createAppointment(@RequestHeader("X-Request-Id") String requestId, @Valid @RequestBody MiniBusinessDTO.Appointment request) { return R.data(businessService.createAppointment(requestId, request)); }
	@GetMapping("/appointments") @Operation(summary = "我的看房预约")
	public R<List<Map<String, Object>>> appointments() { return R.data(businessService.appointments()); }
	@PostMapping("/appointments/{id}/cancel") @Operation(summary = "取消看房预约")
	public R<Void> cancelAppointment(@PathVariable Long id, @RequestParam(required = false) String reason) { businessService.cancelAppointment(id, reason); return R.success("取消成功"); }
	@PostMapping("/settlement-intentions") @Operation(summary = "提交入驻意向")
	public R<Map<String, Object>> settlement(@RequestHeader("X-Request-Id") String requestId, @Valid @RequestBody MiniBusinessDTO.Settlement request) { return R.data(businessService.createSettlement(requestId, request)); }
	@PostMapping("/property-work-orders") @Operation(summary = "提交物业申请")
	public R<Map<String, Object>> propertyOrder(@RequestHeader("X-Request-Id") String requestId, @Valid @RequestBody MiniBusinessDTO.PropertyOrder request) { return R.data(businessService.createPropertyOrder(requestId, request)); }
	@PostMapping("/value-service-orders") @Operation(summary = "提交增值服务申请")
	public R<Map<String, Object>> valueOrder(@RequestHeader("X-Request-Id") String requestId, @Valid @RequestBody MiniBusinessDTO.ValueOrder request) { return R.data(businessService.createValueOrder(requestId, request)); }

	@GetMapping("/company") @Operation(summary = "企业资料")
	public R<Map<String, Object>> company() { return R.data(businessService.company()); }
	@PostMapping("/company") @Operation(summary = "保存企业资料")
	public R<Void> saveCompany(@Valid @RequestBody MiniBusinessDTO.Company request) { businessService.saveCompany(request); return R.success("保存成功"); }
	@GetMapping("/contracts") @Operation(summary = "合同列表")
	public R<List<Map<String, Object>>> contracts() { return R.data(businessService.contracts()); }
	@GetMapping("/contracts/{id}") @Operation(summary = "合同详情")
	public R<Map<String, Object>> contract(@PathVariable Long id) { return R.data(businessService.contract(id)); }
	@GetMapping("/bills") @Operation(summary = "账单列表")
	public R<List<Map<String, Object>>> bills() { return R.data(businessService.bills()); }
	@GetMapping("/bills/{id}") @Operation(summary = "账单详情")
	public R<Map<String, Object>> bill(@PathVariable Long id) { return R.data(businessService.bill(id)); }
	@GetMapping("/utility-bills") @Operation(summary = "已发布水电账单")
	public R<List<Map<String, Object>>> utilityBills() { return R.data(businessService.utilityBills()); }
	@GetMapping("/utility-bills/{id}") @Operation(summary = "水电账单详情")
	public R<Map<String, Object>> utilityBill(@PathVariable Long id) { return R.data(businessService.utilityBill(id)); }
	@GetMapping("/utility-bills/{id}/submissions") @Operation(summary = "水电付款凭证记录")
	public R<List<Map<String, Object>>> utilitySubmissions(@PathVariable Long id) { return R.data(businessService.utilityBillSubmissions(id)); }
	@PostMapping("/utility-bills/{id}/submissions") @Operation(summary = "提交水电付款凭证")
	public R<Map<String, Object>> submitUtilityPayment(@RequestHeader("X-Request-Id") String requestId,
		@PathVariable Long id, @Valid @RequestBody MiniBusinessDTO.PaymentSubmission request) {
		return R.data(businessService.submitUtilityPayment(requestId, id, request));
	}

	@GetMapping("/ads") @Operation(summary = "本企业广告")
	public R<List<Map<String, Object>>> ads() { return R.data(businessService.customerAds()); }
	@GetMapping("/ads/{id}") @Operation(summary = "企业广告详情")
	public R<Map<String, Object>> ad(@PathVariable Long id) { return R.data(businessService.customerAd(id)); }
	@PostMapping("/ads") @Operation(summary = "新建企业广告草稿")
	public R<Map<String, Object>> createAd(@RequestHeader("X-Request-Id") String requestId,
		@Valid @RequestBody MiniBusinessDTO.CustomerAd request) {
		return R.data(businessService.createCustomerAd(requestId, request));
	}
	@PostMapping("/ads/{id}") @Operation(summary = "修改企业广告草稿")
	public R<Map<String, Object>> updateAd(@PathVariable Long id, @Valid @RequestBody MiniBusinessDTO.CustomerAd request) {
		return R.data(businessService.updateCustomerAd(id, request));
	}
	@PostMapping("/ads/{id}/submit") @Operation(summary = "提交企业广告审核")
	public R<Void> submitAd(@RequestHeader("X-Request-Id") String requestId, @PathVariable Long id) {
		businessService.submitCustomerAd(requestId, id); return R.success("提交成功");
	}
	@PostMapping("/ads/{id}/withdraw") @Operation(summary = "撤回企业广告审核")
	public R<Void> withdrawAd(@PathVariable Long id) {
		businessService.withdrawCustomerAd(id); return R.success("撤回成功");
	}

	@GetMapping("/work-orders") @Operation(summary = "统一工单列表")
	public R<List<Map<String, Object>>> workOrders() { return R.data(businessService.customerWorkOrders()); }
	@GetMapping("/work-orders/{type}/{id}") @Operation(summary = "工单详情")
	public R<Map<String, Object>> workOrder(@PathVariable String type, @PathVariable Long id) { return R.data(businessService.customerWorkOrder(type, id)); }
	@PostMapping("/work-orders/{type}/{id}/actions") @Operation(summary = "取消、确认或评价工单")
	public R<Void> workOrderAction(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody MiniBusinessDTO.CustomerAction request) { businessService.customerAction(type, id, request); return R.success("操作成功"); }

	@GetMapping("/members") @Operation(summary = "企业成员")
	public R<List<Map<String, Object>>> members() { return R.data(businessService.members()); }
	@PostMapping("/members/{id}/disable") @Operation(summary = "停用企业成员")
	public R<Void> disableMember(@PathVariable Long id) { businessService.disableMember(id); return R.success("停用成功"); }
	@GetMapping("/invites") @Operation(summary = "企业邀请码")
	public R<List<Map<String, Object>>> invites() { return R.data(businessService.invites()); }
	@PostMapping("/invites") @Operation(summary = "生成企业邀请码")
	public R<Map<String, Object>> createInvite(@Valid @RequestBody MiniBusinessDTO.Invite request) { return R.data(businessService.createInvite(request)); }
}
