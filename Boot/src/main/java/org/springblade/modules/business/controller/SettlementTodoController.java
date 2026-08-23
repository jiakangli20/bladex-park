/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.dto.SettlementTodoActionDTO;
import org.springblade.modules.business.pojo.entity.SettlementTodo;
import org.springblade.modules.business.service.ISettlementTodoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 招商待办控制器。
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "settlement_todo")
@RequestMapping("/blade-park/settlement-todo")
@Tag(name = "招商待办", description = "小程序入驻意向处理")
public class SettlementTodoController extends BladeController {

	private final ISettlementTodoService settlementTodoService;

	@GetMapping("/list")
	@Operation(summary = "招商待办分页")
	public R<IPage<SettlementTodo>> list(SettlementTodo settlementTodo, Query query) {
		return R.data(settlementTodoService.selectPage(Condition.getPage(query), settlementTodo));
	}

	@GetMapping("/statistics")
	@Operation(summary = "招商待办统计")
	public R<Map<String, Object>> statistics(SettlementTodo settlementTodo) {
		return R.data(settlementTodoService.statistics(settlementTodo));
	}

	@GetMapping("/detail")
	@Operation(summary = "招商待办详情")
	public R<SettlementTodo> detail(@RequestParam Long id) {
		return R.data(settlementTodoService.requireScoped(id, null, null));
	}

	@PostMapping("/{id}/actions")
	@PreAuth(menu = "settlement_todo_process")
	@Operation(summary = "处理招商待办")
	public R<Void> action(@PathVariable Long id, @Valid @RequestBody SettlementTodoActionDTO action) {
		settlementTodoService.process(id, action);
		return R.success("处理成功");
	}
}
