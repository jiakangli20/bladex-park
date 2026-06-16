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
import org.springblade.modules.business.pojo.entity.PropertyService;
import org.springblade.modules.business.service.IPropertyServiceService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物业服务项控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "property_service")
@RequestMapping("/blade-ics/property-service")
@io.swagger.v3.oas.annotations.tags.Tag(name = "物业服务项", description = "物业服务项接口")
public class PropertyServiceController extends BladeController {

	private final IPropertyServiceService propertyServiceService;

	@GetMapping("/detail")
	@PreAuth(menu = "property_service_view")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入serviceId")
	public R<PropertyService> detail(@Parameter(description = "服务ID") @RequestParam Long serviceId) {
		return R.data(propertyServiceService.selectPropertyServiceById(serviceId));
	}

	@GetMapping("/page")
	@PreAuth(menu = "property_service_list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "物业服务项分页")
	public R<IPage<PropertyService>> page(PropertyService service, Query query) {
		return R.data(propertyServiceService.selectPropertyServicePage(Condition.getPage(query), service));
	}

	@GetMapping("/list")
	@PreAuth(menu = "property_service_list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "列表", description = "物业服务项列表")
	public R<List<PropertyService>> list(PropertyService service) {
		return R.data(propertyServiceService.selectPropertyServiceList(service));
	}

	@PostMapping("/submit")
	@PreAuth(menu = "property_service_add")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增或修改", description = "传入propertyService")
	public R submit(@Valid @RequestBody PropertyService service) {
		return R.status(propertyServiceService.submitPropertyService(service));
	}

	@PostMapping("/save")
	@PreAuth(menu = "property_service_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增", description = "传入propertyService")
	public R save(@Valid @RequestBody PropertyService service) {
		return R.status(propertyServiceService.insertPropertyService(service));
	}

	@PostMapping("/update")
	@PreAuth(menu = "property_service_edit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改", description = "传入propertyService")
	public R update(@Valid @RequestBody PropertyService service) {
		return R.status(propertyServiceService.updatePropertyService(service));
	}

	@PostMapping("/remove")
	@PreAuth(menu = "property_service_delete")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(propertyServiceService.deletePropertyServiceByIds(ids));
	}

}
