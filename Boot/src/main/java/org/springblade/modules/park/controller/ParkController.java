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
package org.springblade.modules.park.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.pojo.vo.ParkVO;
import org.springblade.modules.park.service.IParkService;
import org.springblade.modules.park.wrapper.ParkWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 园区 控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "park")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/park")
@Tag(name = "园区管理", description = "园区管理接口")
public class ParkController extends BladeController {

	private final IParkService parkService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入id")
	public R<ParkVO> detail(@RequestParam Long id) {
		Park detail = parkService.getById(id);
		return R.data(detail == null ? null : ParkWrapper.build().entityVO(detail));
	}

	/**
	 * 分页
	 */
	@GetMapping("/list")
	@Parameters({
		@Parameter(name = "name", description = "园区名称", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "code", description = "园区编码", in = ParameterIn.QUERY, schema = @Schema(type = "string")),
		@Parameter(name = "status", description = "状态", in = ParameterIn.QUERY, schema = @Schema(type = "string"))
	})
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入park")
	public R<IPage<ParkVO>> list(@Parameter(hidden = true) ParkVO park, Query query) {
		IPage<ParkVO> pages = parkService.selectParkPage(Condition.getPage(query), park);
		return R.data(pages);
	}

	/**
	 * 园区统计
	 */
	@GetMapping("/statistics")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "园区统计", description = "传入parkId")
	public R<Map<String, Object>> statistics(@RequestParam(required = false) Long parkId) {
		return R.data(parkService.selectParkStatistics(parkId));
	}

	/**
	 * 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入park")
	public R save(@Valid @RequestBody Park park) {
		return R.status(parkService.submit(park));
	}

	/**
	 * 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入park")
	public R update(@Valid @RequestBody Park park) {
		return R.status(parkService.submit(park));
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入park")
	public R submit(@Valid @RequestBody Park park) {
		return R.status(parkService.submit(park));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(parkService.removePark(ids));
	}

}
