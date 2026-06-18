package org.springblade.plugin.workflow.design.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.design.entity.WfProcessDef;
import org.springblade.plugin.workflow.design.service.IWfDesignService;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/deployment")
@PreAuth(menu = "wf_design_deployment")
@Tag(name = "流程部署")
public class WfDeploymentController {

	private final IWfDesignService flowDesignService;

	@GetMapping("list")
	@Operation(summary = "分页")
	public R list(WfProcessDef wfProcessDef, Query query) {
		return R.data(flowDesignService.deploymentPage(wfProcessDef, query));
	}

	@PostMapping("remove")
	@Operation(summary = "删除")
	public R remove(@RequestBody String body) {
		String ids = parseDeploymentIds(body);
		if (ids == null || ids.isBlank()) {
			return R.fail("参数错误");
		}
		for (String id : ids.split(",")) {
			String deploymentId = id.trim();
			if (!deploymentId.isEmpty()) {
				flowDesignService.deleteDeployment(deploymentId);
			}
		}
		return R.success("操作成功");
	}

	@PostMapping("changeStatus")
	@Operation(summary = "改变部署流程状态")
	public R changeStatus(@RequestBody String body) {
		JSONObject data = JSON.parseObject(body);

		String id = data.getString("id");
		String status = data.getString("status");

		flowDesignService.changeDeploymentStatus(id, status);

		return R.success("操作成功");
	}

	@PostMapping("changeCategory")
	@Operation(summary = "改变部署流程分类")
	public R changeCategory(@RequestBody String body) {
		JSONObject data = JSON.parseObject(body);

		String id = data.getString("deploymentId");
		String category = data.getString("category");

		flowDesignService.changeDeploymentCategory(id, category);

		return R.success("操作成功");
	}

	private String parseDeploymentIds(String body) {
		if (body == null || body.isBlank()) {
			return null;
		}
		try {
			Object object = JSON.parse(body);
			if (object instanceof JSONObject data) {
				String ids = data.getString("deploymentId");
				if (ids == null || ids.isBlank()) {
					ids = data.getString("deploymentIds");
				}
				if (ids == null || ids.isBlank()) {
					ids = data.getString("ids");
				}
				return ids;
			}
			if (object instanceof String ids) {
				return ids;
			}
		} catch (Exception ignored) {
			return body;
		}
		return body;
	}

}
