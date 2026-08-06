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
 * 小程序园区管理员接口。
 *
 * @author Chill
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-miniapp/admin")
@Tag(name = "小程序园区管理", description = "通知、工单、概览和租客")
public class MiniAdminController {
	private final IMiniBusinessService businessService;

	@GetMapping("/notifications") @Operation(summary = "管理员通知")
	public R<List<Map<String, Object>>> notifications() { return R.data(businessService.notifications()); }
	@PostMapping("/notifications/{id}/read") @Operation(summary = "标记通知已读")
	public R<Void> read(@PathVariable Long id) { businessService.readNotification(id); return R.success("已读"); }
	@GetMapping("/work-orders") @Operation(summary = "管理员工单")
	public R<List<Map<String, Object>>> workOrders(@RequestParam(required = false) String type) { return R.data(businessService.adminWorkOrders(type)); }
	@GetMapping("/work-orders/{type}/{id}") @Operation(summary = "管理员工单详情")
	public R<Map<String, Object>> workOrder(@PathVariable String type, @PathVariable Long id) { return R.data(businessService.adminWorkOrder(type, id)); }
	@PostMapping("/work-orders/{type}/{id}/actions") @Operation(summary = "管理员处理工单")
	public R<Void> action(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody MiniBusinessDTO.AdminAction request) { businessService.adminAction(type, id, request); return R.success("处理成功"); }
	@GetMapping("/overview") @Operation(summary = "园区运营概览")
	public R<Map<String, Object>> overview() { return R.data(businessService.overview()); }
	@GetMapping("/tenants") @Operation(summary = "园区租客")
	public R<List<Map<String, Object>>> tenants(@RequestParam(required = false) String keyword) { return R.data(businessService.tenants(keyword)); }
	@GetMapping("/tenants/{id}") @Operation(summary = "租客详情")
	public R<Map<String, Object>> tenant(@PathVariable Long id) { return R.data(businessService.tenant(id)); }
}
