
package org.springblade.plugin.workflow.design.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;

import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.design.entity.WfCondition;
import org.springblade.plugin.workflow.design.vo.WfConditionVO;
import org.springblade.plugin.workflow.design.wrapper.WfConditionWrapper;
import org.springblade.plugin.workflow.design.service.IWfConditionService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程表达式 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/condition")
@PreAuth(menu = "wf_condition")
@Tag(name = "流程表达式", description = "流程表达式接口")
public class WfConditionController extends BladeController {

	private final IWfConditionService wfConditionService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfCondition")
	public R<WfConditionVO> detail(WfCondition wfCondition) {
		WfCondition detail = wfConditionService.getOne(Condition.getQueryWrapper(wfCondition));
		return R.data(WfConditionWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 流程表达式
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfCondition")
	public R<IPage<WfConditionVO>> list(WfCondition wfCondition, Query query) {
		IPage<WfCondition> pages = wfConditionService.page(Condition.getPage(query), Condition.getQueryWrapper(wfCondition));
		return R.data(WfConditionWrapper.build().pageVO(pages));
	}


	/**
	 * 自定义分页 流程表达式
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入wfCondition")
	public R<IPage<WfConditionVO>> page(WfConditionVO wfCondition, Query query) {
		IPage<WfConditionVO> pages = wfConditionService.selectWfConditionPage(Condition.getPage(query), wfCondition);
		return R.data(pages);
	}

	/**
	 * 新增 流程表达式
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfCondition")
	public R save(@RequestBody WfCondition wfCondition) {
		return R.status(wfConditionService.save(wfCondition));
	}

	/**
	 * 修改 流程表达式
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfCondition")
	public R update(@RequestBody WfCondition wfCondition) {
		return R.status(wfConditionService.updateById(wfCondition));
	}

	/**
	 * 新增或修改 流程表达式
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfCondition")
	public R submit(@RequestBody WfCondition wfCondition) {
		boolean b;
		try {
			b = wfConditionService.saveOrUpdate(wfCondition);
		} catch (Exception e) {
			return R.fail(String.format("相同类型的表达式：%s 已存在", wfCondition.getExpression()));
		}
		return R.status(b);
	}


	/**
	 * 删除 流程表达式
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfConditionService.deleteLogic(Func.toLongList(ids)));
	}


}
