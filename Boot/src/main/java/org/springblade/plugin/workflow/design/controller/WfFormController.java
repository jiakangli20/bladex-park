package org.springblade.plugin.workflow.design.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.ObjectUtil;
import org.springblade.plugin.workflow.design.entity.WfForm;
import org.springblade.plugin.workflow.design.service.IWfFormDefaultValuesService;
import org.springblade.plugin.workflow.design.service.IWfFormService;
import org.springblade.plugin.workflow.design.vo.WfFormVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/form")
@PreAuth(menu = "wf_design_form")
@Tag(name = "流程表单")
public class WfFormController {

	private final IWfFormService formService;
	private final IWfFormDefaultValuesService formDefaultValuesService;

	@GetMapping("detail")
	@Operation(summary = "详情")
	@Parameters({
		@Parameter(name = "id", description = "id", required = true),
	})
	public R detail(@Parameter(hidden = true) WfForm form) {
		return R.data(formService.getOne(Condition.getQueryWrapper(form)));
	}

	@GetMapping("list")
	@Operation(summary = "分页")
	@Parameters({
		@Parameter(name = "key", description = "表单key"),
		@Parameter(name = "name", description = "表单名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R list(@Parameter(hidden = true) @RequestParam Map<String, Object> form, Query query) {
		Object categoryIdEqual = form.get("categoryId_equal");
		if (categoryIdEqual != null) {
			form.put("categoryId_equal", NumberUtils.toLong(categoryIdEqual.toString()));
		}
		return R.data(formService.page(Condition.getPage(query), Condition.getQueryWrapper(form, WfForm.class).orderByDesc("update_time")));
	}

	@PostMapping("submit")
	@Operation(summary = "新增或修改")
	@Parameters({
		@Parameter(name = "id", description = "id，有值=修改，无值=新增"),
		@Parameter(name = "key", description = "表单key", required = true),
		@Parameter(name = "name", description = "表单名称", required = true),
		@Parameter(name = "content", description = "表单JSON", required = true),
		@Parameter(name = "newVersion", description = "是否新版本"),
	})
	public R submit(@Parameter(hidden = true) @RequestBody WfFormVO form) {
		if (ObjectUtil.isEmpty(form.getId())) {
			return (R) formService.create(form);
		} else {
			return R.data(formService.edit(form));
		}
	}

	@PostMapping("remove")
	@Operation(summary = "删除")
	public R remove(@Parameter(hidden = true) @RequestBody WfFormVO form) {
		formService.remove(form);
		return R.success("操作成功");
	}

	@GetMapping("listType")
	@Operation(summary = "字段类型默认值列表")
	public R listType() {
		return R.data(formDefaultValuesService.listType());
	}

	@PostMapping("changeCategory")
	@Operation(summary = "修改分类")
	@Parameters({
		@Parameter(name = "ids", description = "ids", required = true),
		@Parameter(name = "category", description = "分类id", required = true),
	})
	public R changeCategory(@Parameter(hidden = true) @RequestBody JSONObject body) {
		String ids = body.getString("ids");
		Long categoryId = body.getLong("category");
		if (org.springblade.plugin.workflow.core.utils.ObjectUtil.isAnyEmpty(ids, categoryId)) {
			return R.fail("参数错误");
		}
		formService.changeCategory(ids, categoryId);
		return R.success("修改成功");
	}

}
