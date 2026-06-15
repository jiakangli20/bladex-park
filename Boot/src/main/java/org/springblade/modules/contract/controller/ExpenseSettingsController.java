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
package org.springblade.modules.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.pojo.entity.ExpenseSettings;
import org.springblade.modules.contract.service.IExpenseSettingsService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 费项配置控制器
 *
 * @author Chill
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "contract_expense")
@RequestMapping("/blade-contract/expense")
@Tag(name = "费项配置", description = "费项配置接口")
public class ExpenseSettingsController extends BladeController {

	private final IExpenseSettingsService expenseSettingsService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入expenseSettings")
	public R<ExpenseSettings> detail(ExpenseSettings expenseSettings) {
		return R.data(expenseSettingsService.getOne(Condition.getQueryWrapper(expenseSettings)));
	}

	/**
	 * 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入expenseSettings")
	public R<IPage<ExpenseSettings>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> expenseSettings, Query query) {
		IPage<ExpenseSettings> pages = expenseSettingsService.page(Condition.getPage(query), Condition.getQueryWrapper(expenseSettings, ExpenseSettings.class));
		return R.data(pages);
	}

	/**
	 * 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入expenseSettings")
	public R submit(@RequestBody ExpenseSettings expenseSettings) {
		return R.status(expenseSettingsService.submit(expenseSettings));
	}

	/**
	 * 启用或停用
	 */
	@PostMapping("/status")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "启用或停用", description = "传入expenseSettings")
	public R status(@RequestBody ExpenseSettings expenseSettings) {
		ExpenseSettings update = new ExpenseSettings();
		update.setId(expenseSettings.getId());
		update.setIsEnabled(expenseSettings.getIsEnabled());
		update.setUpdateBy(AuthUtil.getUserName());
		update.setUpdateTime(DateUtil.now());
		return R.status(expenseSettingsService.updateById(update));
	}

	/**
	 * 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(expenseSettingsService.removeByIds(Func.toLongList(ids)));
	}

}
