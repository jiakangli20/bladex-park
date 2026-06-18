package org.springblade.plugin.workflow.design.controller;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.design.entity.WfFormHistory;
import org.springblade.plugin.workflow.design.vo.WfFormHistoryVO;
import org.springblade.plugin.workflow.design.wrapper.WfFormHistoryWrapper;
import org.springblade.plugin.workflow.design.service.IWfFormHistoryService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程表单 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/form/history")
@PreAuth(menu = "wf_design_form")
@Tag(name = "流程表单 - 历史")
public class WfFormHistoryController extends BladeController {

	private final IWfFormHistoryService wfFormHistoryService;

	/**
	 * 分页 流程表单
	 */
	@GetMapping("/list")
	@Operation(summary = "分页", description = "传入wfFormHistory")
	public R<IPage<WfFormHistoryVO>> list(WfFormHistory wfFormHistory, Query query) {
		IPage<WfFormHistory> pages = wfFormHistoryService.page(Condition.getPage(query), Condition.getQueryWrapper(wfFormHistory).orderByDesc("version"));
		return R.data(WfFormHistoryWrapper.build().pageVO(pages));
	}

	/**
	 * 删除 流程表单
	 */
	@PostMapping("/remove")
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfFormHistoryService.deleteLogic(Func.toLongList(ids)));
	}

	@PostMapping("/setMainVersion")
	@Operation(summary = "设为主版本")
	@Parameters({
		@Parameter(name = "id", description = "历史表单id"),
	})
	public R setMainVersion(@Parameter(hidden = true) @RequestBody WfFormHistory formHistory) {
		return (R) wfFormHistoryService.setMainVersion(formHistory);
	}

}
