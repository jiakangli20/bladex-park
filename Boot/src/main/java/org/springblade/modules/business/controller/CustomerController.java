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
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.business.excel.CustomerExcel;
import org.springblade.modules.business.excel.CustomerImporter;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 客户管理控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "settlement_customer")
@RequestMapping("/blade-park/customer")
@io.swagger.v3.oas.annotations.tags.Tag(name = "客户管理", description = "客户管理接口")
public class CustomerController extends BladeController {

	private final ICustomerService customerService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入customerId")
	public R<Customer> detail(@Parameter(description = "客户ID") @RequestParam Long customerId) {
		return R.data(customerService.selectCustomerById(customerId));
	}

	@GetMapping("/get/{customerId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<Customer> get(@PathVariable Long customerId) {
		return R.data(customerService.selectCustomerById(customerId));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "客户分页")
	public R<IPage<Customer>> list(Customer customer, Query query) {
		return R.data(customerService.selectCustomerPage(Condition.getPage(query), customer));
	}

	@GetMapping("/page")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "分页", description = "客户分页")
	public R<IPage<Customer>> page(Customer customer, Query query) {
		return list(customer, query);
	}

	@GetMapping("/statistics")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "统计", description = "客户统计")
	public R<Kv> statistics(Customer customer) {
		return R.data(customerService.selectCustomerStatistics(customer));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增", description = "新增客户")
	public R<Customer> save(@RequestBody Customer customer) {
		return R.data(customerService.insertCustomer(customer));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "修改", description = "修改客户")
	public R update(@RequestBody Customer customer) {
		return R.status(customerService.updateCustomer(customer));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "新增或修改", description = "新增或修改客户")
	public R submit(@RequestBody Customer customer) {
		return R.status(customerService.submitCustomer(customer));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "删除", description = "逻辑删除客户")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(customerService.deleteCustomerByIds(ids));
	}

	@PostMapping("/changeStatus/{customerId}")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "状态变更", description = "启用、停用、归档客户")
	public R changeStatus(@PathVariable Long customerId, @RequestParam String status) {
		return R.status(customerService.changeStatus(customerId, status));
	}

	@PostMapping("/check/{customerId}")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "客户核验", description = "执行本地客户核验")
	public R<Customer> check(@PathVariable Long customerId) {
		return R.data(customerService.checkCustomer(customerId));
	}

	@GetMapping("/tag/list/{customerId}")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "客户标签", description = "查询客户标签")
	public R<List<Long>> tagList(@PathVariable Long customerId) {
		Customer customer = customerService.selectCustomerById(customerId);
		return R.data(customer == null ? List.of() : customer.getTagIds());
	}

	@PostMapping("/tag/set/{customerId}")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "设置标签", description = "设置客户标签")
	public R setTags(@PathVariable Long customerId, @RequestBody(required = false) List<Long> tagIds) {
		return R.status(customerService.setCustomerTags(customerId, tagIds));
	}

	@GetMapping("/export")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "导出", description = "导出客户数据")
	public void export(Customer customer, HttpServletResponse response) {
		List<CustomerExcel> list = customerService.exportCustomer(customer);
		ExcelUtil.export(response, "客户数据" + DateUtil.time(), "客户数据表", list, CustomerExcel.class);
	}

	@GetMapping("/export-template")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "导出模板", description = "导出客户导入模板")
	public void exportTemplate(HttpServletResponse response) {
		ExcelUtil.export(response, "客户数据模板", "客户数据表", List.of(), CustomerExcel.class);
	}

	@PostMapping("/import")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "导入", description = "导入客户数据")
	public R importCustomer(MultipartFile file) {
		CustomerImporter importer = new CustomerImporter(customerService);
		ExcelUtil.save(file, importer, CustomerExcel.class);
		return R.success("操作成功");
	}

}
