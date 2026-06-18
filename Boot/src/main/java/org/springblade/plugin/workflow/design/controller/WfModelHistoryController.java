package org.springblade.plugin.workflow.design.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.plugin.workflow.design.entity.WfModelHistory;
import org.springblade.plugin.workflow.design.service.IWfModelHistoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/model/history")
@PreAuth(menu = "wf_design_model")
@Tag(name = "流程模型 - 历史")
public class WfModelHistoryController {

	private final IWfModelHistoryService wfModelHistoryService;

	@GetMapping("list")
	@Operation(summary = "分页")
	public R list(WfModelHistory modelHistory, Query query) {
		IPage<WfModelHistory> pages = wfModelHistoryService.page(Condition.getPage(query), Condition.getQueryWrapper(modelHistory).orderByDesc("version"));
		return R.data(pages);
	}

	@PostMapping("remove")
	@Operation(summary = "删除")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfModelHistoryService.removeByIds(Func.toStrList(ids)));
	}

	@PostMapping("setMainVersion")
	@Operation(summary = "设置主版本")
	@Parameters({
		@Parameter(name = "id", description = "历史模型id"),
	})
	public R setMainVersion(@RequestBody WfModelHistory modelHistory) {
		return (R) wfModelHistoryService.setMainVersion(modelHistory);
	}

}
