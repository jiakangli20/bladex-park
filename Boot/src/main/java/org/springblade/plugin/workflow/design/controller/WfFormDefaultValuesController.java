package org.springblade.plugin.workflow.design.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.springblade.plugin.workflow.design.entity.WfFormDefaultValues;
import org.springblade.plugin.workflow.design.vo.WfFormDefaultValuesVO;
import org.springblade.plugin.workflow.design.wrapper.WfFormDefaultValuesWrapper;
import org.springblade.plugin.workflow.design.service.IWfFormDefaultValuesService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程表单字段默认值 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/form/default-values")
@PreAuth(menu = "wf_form_default_values")
@Tag(name = "流程表单字段默认值", description = "流程表单字段默认值接口")
public class WfFormDefaultValuesController extends BladeController {

	private final IWfFormDefaultValuesService wfFormDefaultValuesService;

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfFormDefaultValues")
	public R<IPage<WfFormDefaultValuesVO>> list(WfFormDefaultValues wfFormDefaultValues, Query query) {
		IPage<WfFormDefaultValues> pages = wfFormDefaultValuesService.page(Condition.getPage(query), Condition.getQueryWrapper(wfFormDefaultValues));
		return R.data(WfFormDefaultValuesWrapper.build().pageVO(pages));
	}

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfFormDefaultValues")
	public R<WfFormDefaultValuesVO> detail(WfFormDefaultValues wfFormDefaultValues) {
		WfFormDefaultValues detail = wfFormDefaultValuesService.getOne(Condition.getQueryWrapper(wfFormDefaultValues));
		return R.data(WfFormDefaultValuesWrapper.build().entityVO(detail));
	}

	@GetMapping("/listType")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "字段类型")
	public R listType() {
		QueryWrapper<WfFormDefaultValues> wrapper = new QueryWrapper<>();
		wrapper.select("field_type fieldType").groupBy("field_type");
		return R.data(wfFormDefaultValuesService.list(wrapper));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfFormDefaultValues")
	public R submit(@RequestBody WfFormDefaultValues wfFormDefaultValues) {
		return R.status(wfFormDefaultValuesService.saveOrUpdate(wfFormDefaultValues));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfFormDefaultValuesService.deleteLogic(Func.toLongList(ids)));
	}


}
