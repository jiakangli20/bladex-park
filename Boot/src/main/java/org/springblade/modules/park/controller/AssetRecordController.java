/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.pojo.entity.AssetRecord;
import org.springblade.modules.park.service.IAssetRecordService;
import org.springframework.web.bind.annotation.*;

/**
 * 资产登记台账控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "rent_control")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/asset-record")
@Tag(name = "资产登记台账", description = "资产登记台账接口")
public class AssetRecordController extends BladeController {

	private final IAssetRecordService assetRecordService;

	@GetMapping("/detail")
	@PreAuth(menu = "rent_control_asset_list")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入assetId")
	public R<AssetRecord> detail(@RequestParam Long assetId) {
		return R.data(assetRecordService.selectAssetById(assetId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "rent_control_asset_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入资产筛选条件")
	public R<IPage<AssetRecord>> page(AssetRecord asset, Query query) {
		return R.data(assetRecordService.selectAssetPage(Condition.getPage(query), asset));
	}

	@PostMapping("/submit")
	@PreAuth("(#asset.assetId == null && hasMenu('rent_control_asset_add')) || " +
		"(#asset.assetId != null && hasMenu('rent_control_asset_edit'))")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入资产登记")
	public R submit(@Valid @RequestBody AssetRecord asset) {
		return R.status(assetRecordService.submitAsset(asset));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "rent_control_asset_delete")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(assetRecordService.removeAsset(ids));
	}

}
