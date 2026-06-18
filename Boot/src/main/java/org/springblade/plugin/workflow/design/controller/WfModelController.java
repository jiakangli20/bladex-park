package org.springblade.plugin.workflow.design.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.flowable.bpmn.exceptions.XMLException;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.design.entity.WfModel;
import org.springblade.plugin.workflow.design.service.IWfModelService;
import org.springblade.plugin.workflow.design.vo.WfModelVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/model")
@PreAuth(menu = "wf_design_model")
@Tag(name = "流程模型")
public class WfModelController {

	private final IWfModelService modelService;

	@GetMapping("detail")
	@Operation(summary = "详情")
	public R detail(WfModel model) {
		if (!WfTaskUtil.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) {
			model.setTenantId(WfTaskUtil.getTenantId());
		}
		return R.data(modelService.detail(model));
	}

	@GetMapping("list")
	@Operation(summary = "分页")
	public R list(@Parameter(hidden = true) @RequestParam Map<String, Object> model, Query query) {
		Object category = model.get("categoryId_equal");
		if (category != null) {
			model.put("categoryId_equal", Long.valueOf(category.toString()));
		}
		if (!WfTaskUtil.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) {
			model.put("tenantId_equal", WfTaskUtil.getTenantId());
		}
		QueryWrapper<WfModel> wrapper = Condition.getQueryWrapper(model, WfModel.class);
		wrapper.select("id,model_key modelKey,name,description,version,category_id categoryId,icon")
			.orderByDesc("last_updated");
		IPage<WfModel> pages = modelService.page(Condition.getPage(query), wrapper);
		return R.data(pages);
	}

	@PostMapping("submit")
	@Operation(summary = "新增或修改")
	public R submit(@RequestBody WfModelVO model) {
		if (StringUtil.isBlank(model.getId())) {
			try {
				return R.data(modelService.saveModel(model));
			} catch (XMLException xmlException) {
				return R.fail("请查看文档配置xss拦截");
			} catch (DuplicateKeyException duplicateKeyException) {
				return R.fail(String.format("当前流程key：%s 已存在", model.getModelKey()));
			} catch (Exception e) {
				return R.fail("未知错误：" + e.getLocalizedMessage());
			}
		} else {
			try {
				return R.data(modelService.updateModel(model));
			} catch (XMLException xmlException) {
				return R.fail("请查看文档配置xss拦截");
			} catch (Exception e) {
				return R.fail("未知错误：" + e.getLocalizedMessage());
			}
		}
	}

	@PostMapping("remove")
	@Operation(summary = "删除")
	public R remove(@RequestBody WfModelVO wfModel) {
		modelService.remove(wfModel);
		return R.success("操作成功");
	}

	@PostMapping("deploy")
	@Operation(summary = "部署")
	public R deploy(@RequestBody WfModelVO modelVo) {
		WfModel model = modelService.getById(modelVo.getId());
		if (model == null) {
			return R.fail("查询不到此模型");
		}

		return R.data(modelService.deploy(modelVo));
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
		if (ObjectUtil.isAnyEmpty(ids, categoryId)) {
			return R.fail("参数错误");
		}
		modelService.changeCategory(ids, categoryId);
		return R.success("修改成功");
	}

	@PostMapping("changeIcon")
	@Operation(summary = "修改图标")
	@Parameters({
		@Parameter(name = "id", description = "id", required = true),
		@Parameter(name = "icon", description = "图标", required = true),
	})
	public R changeIcon(@Parameter(hidden = true) @RequestBody JSONObject body) {
		String id = body.getString("id");
		String icon = body.getString("icon");
		if (ObjectUtil.isAnyEmpty(id, icon)) {
			return R.fail("参数错误");
		}
		modelService.changeIcon(id, icon);
		return R.success("修改成功");
	}

}
