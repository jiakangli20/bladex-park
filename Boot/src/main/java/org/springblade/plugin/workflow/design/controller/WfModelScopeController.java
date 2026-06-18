package org.springblade.plugin.workflow.design.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;

import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.design.vo.WfModelScopeVO;
import org.springframework.web.bind.annotation.*;
import org.springblade.plugin.workflow.design.entity.WfModelScope;
import org.springblade.plugin.workflow.design.service.IWfModelScopeService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程模型权限 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/model/scope")
@PreAuth(menu = "wf_design_model")
@Tag(name = "流程模型权限", description = "流程模型权限接口")
public class WfModelScopeController extends BladeController {

	private final IWfModelScopeService wfModelScopeService;

	@GetMapping("list")
	@Operation(summary = "列表")
	@Parameters({
		@Parameter(name = "modelId", description = "模型id"),
	})
	public R list(@Parameter(hidden = true) WfModelScope modelScope) {
		if (ObjectUtil.isAnyEmpty(modelScope.getModelId())) {
			return R.fail("参数错误");
		}
		return R.data(wfModelScopeService.list(new LambdaQueryWrapper<WfModelScope>().eq(WfModelScope::getModelId, modelScope.getModelId())));
	}


	@PostMapping("submit")
	@Operation(summary = "保存")
	@Parameters({
		@Parameter(name = "modelId", description = "模型id"),
	})
	public R submit(@RequestBody WfModelScopeVO wfModelScope) {
		wfModelScopeService.submit(wfModelScope);
		return R.success("操作成功");
	}
}
