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
import org.springblade.modules.business.pojo.entity.MerchantAd;
import org.springblade.modules.business.service.IMerchantAdService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户小程序广告控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "merchant_service_ad")
@RequestMapping("/blade-ics/merchant-ad")
@io.swagger.v3.oas.annotations.tags.Tag(name = "商户广告管理", description = "商户小程序广告接口")
public class MerchantAdController extends BladeController {

	private final IMerchantAdService merchantAdService;

	@GetMapping("/detail")
	@PreAuth(menu = "merchant_service_ad_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入adId")
	public R<MerchantAd> detail(@Parameter(description = "广告ID") @RequestParam Long adId) {
		return R.data(merchantAdService.selectAdById(adId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "merchant_service_ad_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "广告分页")
	public R<IPage<MerchantAd>> page(MerchantAd ad, Query query) {
		return R.data(merchantAdService.selectAdPage(Condition.getPage(query), ad));
	}

	@GetMapping("/list")
	@PreAuth(menu = "merchant_service_ad_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "广告列表")
	public R<List<MerchantAd>> list(MerchantAd ad) {
		return R.data(merchantAdService.selectAdList(ad));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "merchant_service_ad_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "统计", description = "广告统计")
	public R<Kv> statistics(MerchantAd ad) {
		return R.data(merchantAdService.selectAdStatistics(ad));
	}

	@PostMapping("/save")
	@PreAuth(menu = "merchant_service_ad_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增", description = "新增广告")
	public R save(@Valid @RequestBody MerchantAd ad) {
		return R.status(merchantAdService.insertAd(ad));
	}

	@PostMapping("/update")
	@PreAuth(menu = "merchant_service_ad_edit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改", description = "修改广告")
	public R update(@Valid @RequestBody MerchantAd ad) {
		return R.status(merchantAdService.updateAd(ad));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "merchant_service_ad_add")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "新增或修改", description = "新增或修改广告")
	public R submit(@Valid @RequestBody MerchantAd ad) {
		return R.status(merchantAdService.submitAd(ad));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "merchant_service_ad_delete")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "逻辑删除广告")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(merchantAdService.deleteAdByIds(ids));
	}

	@PostMapping("/changeStatus/{adId}")
	@PreAuth(menu = "merchant_service_ad_status")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "状态变更", description = "广告上架或下架")
	public R changeStatus(@PathVariable Long adId, @RequestParam String status) {
		return R.status(merchantAdService.changeStatus(adId, status));
	}

	@GetMapping("/miniapp/list")
	@PreAuth(menu = "merchant_service_ad_miniapp_list")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "小程序广告列表接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<List<MerchantAd>> miniAppList(MerchantAd ad) {
		ad.setStatus("0");
		return R.data(merchantAdService.selectAdList(ad));
	}

	@GetMapping("/miniapp/detail")
	@PreAuth(menu = "merchant_service_ad_miniapp_detail")
	@ApiOperationSupport(order = 21)
	@Operation(summary = "小程序广告详情接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<MerchantAd> miniAppDetail(@Parameter(description = "广告ID") @RequestParam Long adId) {
		return R.data(merchantAdService.selectAdById(adId));
	}

}
