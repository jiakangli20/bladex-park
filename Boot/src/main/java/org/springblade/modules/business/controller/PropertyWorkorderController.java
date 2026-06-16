/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.business.pojo.entity.WorkorderLog;
import org.springblade.modules.business.service.IPropertyWorkorderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 物业服务工单控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "property_service")
@RequestMapping("/blade-ics/property-workorder")
@io.swagger.v3.oas.annotations.tags.Tag(name = "物业服务工单", description = "物业服务工单接口")
public class PropertyWorkorderController extends BladeController {

	private final IPropertyWorkorderService propertyWorkorderService;

	@GetMapping("/detail")
	@PreAuth(menu = "property_workorder_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入orderId")
	public R<ServiceWorkorder> detail(@Parameter(description = "工单ID") @RequestParam Long orderId) {
		return R.data(propertyWorkorderService.selectWorkorderById(orderId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "property_workorder_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "物业服务工单分页")
	public R<IPage<ServiceWorkorder>> page(ServiceWorkorder workorder, Query query) {
		return R.data(propertyWorkorderService.selectWorkorderPage(Condition.getPage(query), workorder));
	}

	@GetMapping("/list")
	@PreAuth(menu = "property_workorder_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "物业服务工单列表")
	public R<List<ServiceWorkorder>> list(ServiceWorkorder workorder) {
		return R.data(propertyWorkorderService.selectWorkorderList(workorder));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "property_workorder_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "统计", description = "物业服务工单统计")
	public R<Map<String, Object>> statistics(ServiceWorkorder workorder) {
		return R.data(propertyWorkorderService.selectWorkorderStatistics(workorder));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "property_workorder_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入workorder")
	public R submit(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.submitWorkorder(workorder));
	}

	@PostMapping("/save")
	@PreAuth(menu = "property_workorder_add")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增", description = "传入workorder")
	public R save(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.insertWorkorder(workorder));
	}

	@PostMapping("/update")
	@PreAuth(menu = "property_workorder_edit")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "修改", description = "传入workorder")
	public R update(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.updateWorkorder(workorder));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "property_workorder_delete")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(propertyWorkorderService.deleteWorkorderByIds(ids));
	}

	@PostMapping("/assign")
	@PreAuth(menu = "property_workorder_assign")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "指派", description = "传入orderId与assignTo")
	public R assign(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.assignWorkorder(workorder));
	}

	@PostMapping("/finish")
	@PreAuth(menu = "property_workorder_finish")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "完成", description = "传入orderId与处置内容")
	public R finish(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.finishWorkorder(workorder));
	}

	@PostMapping("/close")
	@PreAuth(menu = "property_workorder_close")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "关闭", description = "传入orderId")
	public R close(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.closeWorkorder(workorder));
	}

	@PostMapping("/rate")
	@PreAuth(menu = "property_workorder_rate")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "评价", description = "传入orderId、rating与ratingContent")
	public R rate(@RequestBody ServiceWorkorder workorder) {
		return R.status(propertyWorkorderService.rateWorkorder(workorder));
	}

	@GetMapping("/log-list")
	@PreAuth(menu = "property_workorder_view")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "日志", description = "传入orderId")
	public R<List<WorkorderLog>> logList(@Parameter(description = "工单ID") @RequestParam Long orderId) {
		return R.data(propertyWorkorderService.selectLogByOrderId(orderId));
	}

}
