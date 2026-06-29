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
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrder;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrderLog;
import org.springblade.modules.business.service.IMerchantServiceOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商户增值服务处理单控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "merchant_service_process")
@RequestMapping("/blade-ics/merchant-service-order")
@io.swagger.v3.oas.annotations.tags.Tag(name = "商户服务处理", description = "商户增值服务处理接口")
public class MerchantServiceOrderController extends BladeController {

	private final IMerchantServiceOrderService merchantServiceOrderService;

	@GetMapping("/detail")
	@PreAuth(menu = "merchant_service_process_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入orderId")
	public R<MerchantServiceOrder> detail(@Parameter(description = "服务单ID") @RequestParam Long orderId) {
		return R.data(merchantServiceOrderService.selectOrderById(orderId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "merchant_service_process_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "服务处理分页")
	public R<IPage<MerchantServiceOrder>> page(MerchantServiceOrder order, Query query) {
		return R.data(merchantServiceOrderService.selectOrderPage(Condition.getPage(query), order));
	}

	@GetMapping("/list")
	@PreAuth(menu = "merchant_service_process_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "服务处理列表")
	public R<List<MerchantServiceOrder>> list(MerchantServiceOrder order) {
		return R.data(merchantServiceOrderService.selectOrderList(order));
	}

	@GetMapping("/statistics")
	@PreAuth(menu = "merchant_service_process_list")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "统计", description = "服务处理统计")
	public R<Kv> statistics(MerchantServiceOrder order) {
		return R.data(merchantServiceOrderService.selectOrderStatistics(order));
	}

	@PostMapping("/save")
	@PreAuth(menu = "merchant_service_process_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增", description = "新增服务申请")
	public R save(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.insertOrder(order));
	}

	@PostMapping("/update")
	@PreAuth(menu = "merchant_service_process_edit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改", description = "修改服务申请")
	public R update(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.updateOrder(order));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "merchant_service_process_add")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "新增或修改", description = "新增或修改服务申请")
	public R submit(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.submitOrder(order));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "merchant_service_process_delete")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "逻辑删除服务单")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(merchantServiceOrderService.deleteOrderByIds(ids));
	}

	@PostMapping("/assign")
	@PreAuth(menu = "merchant_service_process_assign")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "指派", description = "指派专人处理")
	public R assign(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.assignOrder(order));
	}

	@PostMapping("/follow")
	@PreAuth(menu = "merchant_service_process_follow")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "跟进", description = "更新处理进展")
	public R follow(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.followOrder(order));
	}

	@PostMapping("/deal")
	@PreAuth(menu = "merchant_service_process_deal")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "成交", description = "服务撮合成交")
	public R deal(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.dealOrder(order));
	}

	@PostMapping("/close")
	@PreAuth(menu = "merchant_service_process_close")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "关闭", description = "关闭服务单")
	public R close(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.closeOrder(order));
	}

	@GetMapping("/log-list")
	@PreAuth(menu = "merchant_service_process_view")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "日志", description = "传入orderId")
	public R<List<MerchantServiceOrderLog>> logList(@Parameter(description = "服务单ID") @RequestParam Long orderId) {
		return R.data(merchantServiceOrderService.selectLogByOrderId(orderId));
	}

	@PostMapping("/miniapp/apply")
	@PreAuth(menu = "merchant_service_process_miniapp_apply")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "小程序申请增值服务接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R miniAppApply(@RequestBody MerchantServiceOrder order) {
		order.setOrderStatus("0");
		return R.status(merchantServiceOrderService.insertOrder(order));
	}

	@GetMapping("/miniapp/my-page")
	@PreAuth(menu = "merchant_service_process_miniapp_my")
	@ApiOperationSupport(order = 21)
	@Operation(summary = "小程序我的服务申请接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<IPage<MerchantServiceOrder>> miniAppMyPage(MerchantServiceOrder order, Query query) {
		return R.data(merchantServiceOrderService.selectOrderPage(Condition.getPage(query), order));
	}

	@GetMapping("/miniapp/detail")
	@PreAuth(menu = "merchant_service_process_view")
	@ApiOperationSupport(order = 22)
	@Operation(summary = "小程序服务申请详情接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<MerchantServiceOrder> miniAppDetail(@Parameter(description = "服务单ID") @RequestParam Long orderId) {
		return R.data(merchantServiceOrderService.selectOrderById(orderId));
	}

	@GetMapping("/miniapp/log-list")
	@PreAuth(menu = "merchant_service_process_view")
	@ApiOperationSupport(order = 23)
	@Operation(summary = "小程序服务处理进展接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R<List<MerchantServiceOrderLog>> miniAppLogList(@Parameter(description = "服务单ID") @RequestParam Long orderId) {
		return R.data(merchantServiceOrderService.selectLogByOrderId(orderId));
	}

	@PostMapping("/miniapp/admin/follow")
	@PreAuth(menu = "merchant_service_process_miniapp_follow")
	@ApiOperationSupport(order = 24)
	@Operation(summary = "小程序管理员跟进服务接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R miniAppAdminFollow(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.followOrder(order));
	}

	@PostMapping("/miniapp/admin/deal")
	@PreAuth(menu = "merchant_service_process_miniapp_deal")
	@ApiOperationSupport(order = 25)
	@Operation(summary = "小程序管理员撮合成交接口", description = "待小程序鉴权接入后开放，当前仅后台联调使用")
	public R miniAppAdminDeal(@RequestBody MerchantServiceOrder order) {
		return R.status(merchantServiceOrderService.dealOrder(order));
	}

}
