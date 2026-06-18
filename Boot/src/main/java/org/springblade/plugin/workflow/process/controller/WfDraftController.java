package org.springblade.plugin.workflow.process.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;

import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.process.entity.WfDraft;
import org.springframework.web.bind.annotation.*;
import org.springblade.plugin.workflow.process.service.IWfDraftService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程草稿箱 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/process/draft")
@PreAuth(menu = "wf_process_draft")
@Tag(name = "流程草稿箱", description = "流程草稿箱接口")
public class WfDraftController extends BladeController {

	private final IWfDraftService wfDraftService;

	@GetMapping("detail")
	@Operation(summary = "详情")
	@Parameters({
		@Parameter(name = "processDefId", description = "流程定义id"),
		@Parameter(name = "platform", description = "平台 pc/app"),
	})
	public R<WfDraft> detail(WfDraft wfDraft) {
		wfDraft.setUserId(Long.valueOf(WfTaskUtil.getTaskUser()));
		return R.data(wfDraftService.getOne(new QueryWrapper<>(wfDraft)));
	}

	@PostMapping("submit")
	@Operation(summary = "保存")
	@Parameters({
		@Parameter(name = "processDefId", description = "流程定义id"),
		@Parameter(name = "platform", description = "平台 pc/app"),
	})
	public R<String> submit(@RequestBody WfDraft wfDraft) {
		wfDraftService.submit(wfDraft);
		return R.success("保存成功");
	}
}
