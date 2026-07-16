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

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.service.IRentControlService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 租控管理控制器
 *
 * @author Chill
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "rent_control")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/rent-control")
@Tag(name = "租赁管理", description = "租赁管理接口")
public class RentControlController extends BladeController {

	private final IRentControlService rentControlService;

	/**
	 * 看板
	 */
	@GetMapping("/board")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "租控看板", description = "传入筛选条件")
	public R<Map<String, Object>> board(Long parkId, Long buildingId, Integer floorNo, String keyword, String searchType, String status, String orientation, Boolean includeTree) {
		return R.data(rentControlService.getBoard(parkId, buildingId, floorNo, keyword, searchType, status, orientation, !Boolean.FALSE.equals(includeTree)));
	}

	/**
	 * 工单占位列表
	 */
	@GetMapping("/workorders")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "工单列表", description = "第一阶段返回占位数据")
	public R<Map<String, Object>> workorders() {
		return R.data(rentControlService.workorders());
	}

	/**
	 * 上报工单占位
	 */
	@PostMapping("/workorders/report")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "上报工单", description = "第一阶段返回占位提示")
	public R<Map<String, Object>> reportWorkorder() {
		return R.data(rentControlService.reportWorkorder());
	}

}
