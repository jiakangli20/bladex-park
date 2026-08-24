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
import jakarta.annotation.security.PermitAll;
import lombok.AllArgsConstructor;
import org.springblade.core.tool.api.R;
import org.springblade.modules.miniapp.service.IMiniBusinessService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 小程序公共接口。
 *
 * @author Chill
 */
@PermitAll
@RestController
@AllArgsConstructor
@RequestMapping("/blade-miniapp/public")
@Tag(name = "小程序公共内容", description = "游客可访问的园区、房源和服务")
public class MiniPublicController {
	private final IMiniBusinessService businessService;

	@GetMapping("/home") @Operation(summary = "首页")
	public R<Map<String, Object>> home() { return R.data(businessService.home()); }
	@GetMapping("/notices") @Operation(summary = "公开公告")
	public R<List<Map<String, Object>>> notices() { return R.data(businessService.publicNotices()); }
	@GetMapping("/notices/{id}") @Operation(summary = "公开公告详情")
	public R<Map<String, Object>> notice(@PathVariable Long id) { return R.data(businessService.publicNotice(id)); }
	@GetMapping("/policies") @Operation(summary = "公开政策服务")
	public R<List<Map<String, Object>>> policies() { return R.data(businessService.publicPolicies()); }
	@GetMapping("/policies/{id}") @Operation(summary = "公开政策服务详情")
	public R<Map<String, Object>> policy(@PathVariable Long id) { return R.data(businessService.publicPolicy(id)); }
	@GetMapping("/ads") @Operation(summary = "公开广告")
	public R<List<Map<String, Object>>> ads() { return R.data(businessService.publicAds()); }
	@GetMapping("/ads/{id}") @Operation(summary = "公开广告详情")
	public R<Map<String, Object>> ad(@PathVariable Long id) { return R.data(businessService.publicAd(id)); }
	@GetMapping("/activities") @Operation(summary = "公开园区活动")
	public R<List<Map<String, Object>>> activities() { return R.data(businessService.publicActivities()); }
	@GetMapping("/activities/{id}") @Operation(summary = "公开园区活动详情")
	public R<Map<String, Object>> activity(@PathVariable Long id) { return R.data(businessService.publicActivity(id)); }
	@GetMapping("/houses") @Operation(summary = "公开房源")
	public R<List<Map<String, Object>>> houses(@RequestParam(required = false) String keyword) { return R.data(businessService.houses(keyword)); }
	@GetMapping("/houses/{id}") @Operation(summary = "公开房源详情")
	public R<Map<String, Object>> house(@PathVariable Long id) { return R.data(businessService.house(id)); }
	@GetMapping("/property-services") @Operation(summary = "物业服务")
	public R<List<Map<String, Object>>> propertyServices() { return R.data(businessService.propertyServices()); }
	@GetMapping("/value-services") @Operation(summary = "增值服务")
	public R<List<Map<String, Object>>> valueServices(@RequestParam(required = false) String keyword) { return R.data(businessService.valueServices(keyword)); }
	@GetMapping("/value-services/{id}") @Operation(summary = "增值服务详情")
	public R<Map<String, Object>> valueService(@PathVariable Long id) { return R.data(businessService.valueService(id)); }
}
