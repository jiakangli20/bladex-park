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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
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

	@GetMapping("/room-contracts")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "房源关联合同", description = "按房源ID查询单房源及多房源合同")
	public R<IPage<Contract>> roomContracts(@RequestParam Long roomId, Query query) {
		return R.data(rentControlService.roomContracts(Condition.getPage(query), roomId));
	}

	@GetMapping("/room-payments")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "房源关联账单", description = "按房源ID查询合同及手工账单")
	public R<IPage<ContractPayment>> roomPayments(@RequestParam Long roomId, Query query) {
		return R.data(rentControlService.roomPayments(Condition.getPage(query), roomId));
	}

	/**
	 * 工单列表
	 */
	@GetMapping("/workorders")
	@PreAuth(menu = "rent_control_workorder_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "工单列表", description = "复用企业服务物业工单数据")
	public R<Map<String, Object>> workorders() {
		return R.data(rentControlService.workorders());
	}

	/**
	 * 上报工单
	 */
	@PostMapping("/workorders/report")
	@PreAuth(menu = "rent_control_workorder_report")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "上报工单", description = "写入企业服务物业工单")
	public R<Map<String, Object>> reportWorkorder(@RequestBody ServiceWorkorder workorder) {
		return R.data(rentControlService.reportWorkorder(workorder));
	}

}
