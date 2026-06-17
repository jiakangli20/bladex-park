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
import org.springblade.modules.business.pojo.entity.Merchant;
import org.springblade.modules.business.service.IMerchantService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户服务控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "merchant_service_merchant")
@RequestMapping("/blade-ics/merchant")
@io.swagger.v3.oas.annotations.tags.Tag(name = "商户服务", description = "商户服务接口")
public class MerchantController extends BladeController {

	private final IMerchantService merchantService;

	@GetMapping("/detail")
	@PreAuth(menu = "merchant_service_merchant_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入merchantId")
	public R<Merchant> detail(@Parameter(description = "商户ID") @RequestParam Long merchantId) {
		return R.data(merchantService.selectMerchantById(merchantId));
	}

	@GetMapping("/get/{merchantId}")
	@PreAuth(menu = "merchant_service_merchant_view")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<Merchant> get(@PathVariable Long merchantId) {
		return R.data(merchantService.selectMerchantById(merchantId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "merchant_service_merchant_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "商户分页")
	public R<IPage<Merchant>> page(Merchant merchant, Query query) {
		return R.data(merchantService.selectMerchantPage(Condition.getPage(query), merchant));
	}

	@GetMapping("/list")
	@PreAuth(menu = "merchant_service_merchant_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "列表", description = "商户列表")
	public R<List<Merchant>> list(Merchant merchant) {
		return R.data(merchantService.selectMerchantList(merchant));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "merchant_service_merchant_list")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "统计", description = "商户统计")
	public R<Kv> statistics(Merchant merchant) {
		return R.data(merchantService.selectMerchantStatistics(merchant));
	}

	@PostMapping("/save")
	@PreAuth(menu = "merchant_service_merchant_add")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增", description = "新增商户")
	public R save(@Valid @RequestBody Merchant merchant) {
		return R.status(merchantService.insertMerchant(merchant));
	}

	@PostMapping("/update")
	@PreAuth(menu = "merchant_service_merchant_edit")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "修改", description = "修改商户")
	public R update(@Valid @RequestBody Merchant merchant) {
		return R.status(merchantService.updateMerchant(merchant));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "merchant_service_merchant_add")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "新增或修改", description = "新增或修改商户")
	public R submit(@Valid @RequestBody Merchant merchant) {
		return R.status(merchantService.submitMerchant(merchant));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "merchant_service_merchant_delete")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "删除", description = "逻辑删除商户")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(merchantService.deleteMerchantByIds(ids));
	}

	@PostMapping("/changeStatus/{merchantId}")
	@PreAuth(menu = "merchant_service_merchant_status")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "状态变更", description = "启用、停用、暂停商户")
	public R changeStatus(@PathVariable Long merchantId, @RequestParam String status) {
		return R.status(merchantService.changeStatus(merchantId, status));
	}

}
