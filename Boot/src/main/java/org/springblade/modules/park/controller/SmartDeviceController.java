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
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.park.constant.ParkConstant;
import org.springblade.modules.park.pojo.entity.SmartDevice;
import org.springblade.modules.park.service.ISmartDeviceService;
import org.springframework.web.bind.annotation.*;

/**
 * 智能设备台账控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "rent_control")
@RequestMapping(ParkConstant.APPLICATION_PARK_NAME + "/smart-device")
@Tag(name = "智能设备台账", description = "智能设备台账接口")
public class SmartDeviceController extends BladeController {

	private final ISmartDeviceService smartDeviceService;

	@GetMapping("/detail")
	@PreAuth(menu = "rent_control_device_list")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入deviceId")
	public R<SmartDevice> detail(@RequestParam Long deviceId) {
		return R.data(smartDeviceService.selectDeviceById(deviceId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "rent_control_device_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入设备筛选条件")
	public R<IPage<SmartDevice>> page(SmartDevice device, Query query) {
		return R.data(smartDeviceService.selectDevicePage(Condition.getPage(query), device));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "rent_control_device_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "统计", description = "传入设备筛选条件")
	public R<Kv> statistics(SmartDevice device) {
		return R.data(smartDeviceService.selectDeviceStatistics(device));
	}

	@PostMapping("/submit")
	@PreAuth("(#device.deviceId == null && hasMenu('rent_control_device_add')) || " +
		"(#device.deviceId != null && hasMenu('rent_control_device_edit'))")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增或修改", description = "传入设备台账")
	public R submit(@Valid @RequestBody SmartDevice device) {
		return R.status(smartDeviceService.submitDevice(device));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "rent_control_device_delete")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(smartDeviceService.removeDevice(ids));
	}

}
