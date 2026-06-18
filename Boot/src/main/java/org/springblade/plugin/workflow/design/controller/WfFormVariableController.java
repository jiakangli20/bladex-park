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
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import org.springblade.plugin.workflow.design.vo.WfFormVariableVO;
import org.springblade.plugin.workflow.design.wrapper.WfFormVariableWrapper;
import org.springblade.plugin.workflow.design.service.IWfFormVariableService;
import org.springblade.core.boot.ctrl.BladeController;
import java.util.Map;

/**
 * 流程表单 - 历史变量 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/form/variable")
@PreAuth(menu = "wf_process")
@Tag(name = "流程表单 - 历史变量", description = "流程表单 - 历史变量接口")
public class WfFormVariableController extends BladeController {

	private final IWfFormVariableService wfFormVariableService;

	/**
	 * 流程表单 - 历史变量 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfFormVariable")
	public R<WfFormVariableVO> detail(WfFormVariable wfFormVariable) {
		WfFormVariable detail = wfFormVariableService.getOne(Condition.getQueryWrapper(wfFormVariable));
		return R.data(WfFormVariableWrapper.build().entityVO(detail));
	}

	/**
	 * 流程表单 - 历史变量 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfFormVariable")
	public R<IPage<WfFormVariableVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> wfFormVariable, Query query) {
		IPage<WfFormVariable> pages = wfFormVariableService.page(Condition.getPage(query), Condition.getQueryWrapper(wfFormVariable, WfFormVariable.class));
		return R.data(WfFormVariableWrapper.build().pageVO(pages));
	}

	/**
	 * 流程表单 - 历史变量 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入wfFormVariable")
	public R<IPage<WfFormVariableVO>> page(WfFormVariableVO wfFormVariable, Query query) {
		IPage<WfFormVariableVO> pages = wfFormVariableService.selectWfFormVariablePage(Condition.getPage(query), wfFormVariable);
		return R.data(pages);
	}

	/**
	 * 流程表单 - 历史变量 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfFormVariable")
	public R save(@RequestBody WfFormVariable wfFormVariable) {
		return R.status(wfFormVariableService.save(wfFormVariable));
	}

	/**
	 * 流程表单 - 历史变量 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfFormVariable")
	public R update(@RequestBody WfFormVariable wfFormVariable) {
		return R.status(wfFormVariableService.updateById(wfFormVariable));
	}

	/**
	 * 流程表单 - 历史变量 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfFormVariable")
	public R submit(@RequestBody WfFormVariable wfFormVariable) {
		return R.status(wfFormVariableService.saveOrUpdate(wfFormVariable));
	}

	/**
	 * 流程表单 - 历史变量 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfFormVariableService.deleteLogic(Func.toLongList(ids)));
	}
}
