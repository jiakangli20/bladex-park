package org.springblade.plugin.workflow.app.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.app.entity.WfAppProcess;
import org.springblade.plugin.workflow.app.service.IWfAppProcessService;
import org.springblade.plugin.workflow.app.vo.WfAppProcessVo;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.process.entity.WfCopy;
import org.springblade.plugin.workflow.process.service.IWfCopyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blade-workflow/app/process")
@AllArgsConstructor
@PreAuth(menu = "wf_process")
@Tag(name = "workflow - app - 流程")
public class WfAppProcessController {

	private final IWfCopyService copyService;
	private final IWfAppProcessService processService;

	@GetMapping("list")
	@Operation(summary = "可发起流程列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
	})
	public R<List<WfAppProcessVo>> list(@Parameter(hidden = true) WfAppProcess wfAppProcess) {
		return R.data(processService.list(wfAppProcess));
	}

	@GetMapping("todoList")
	@Operation(summary = "待办列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfAppProcess>> todoList(@Parameter(hidden = true) WfAppProcess wfAppProcess, Query query) {
		wfAppProcess.setStatus(WfProcessConstant.STATUS_TODO);
		return R.data(processService.taskList(wfAppProcess, query));
	}

	@GetMapping("doneList")
	@Operation(summary = "办结列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfAppProcess>> doneList(@Parameter(hidden = true) WfAppProcess wfAppProcess, Query query) {
		wfAppProcess.setStatus(WfProcessConstant.STATUS_DONE);
		return R.data(processService.processList(wfAppProcess, query));
	}

	@GetMapping("myDoneList")
	@Operation(summary = "我的已办列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfAppProcess>> myDoneList(@Parameter(hidden = true) WfAppProcess wfAppProcess, Query query) {
		wfAppProcess.setStatus(WfProcessConstant.STATUS_DONE);
		return R.data(processService.taskList(wfAppProcess, query));
	}

	@GetMapping("sendList")
	@Operation(summary = "我的请求")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfAppProcess>> sendList(@Parameter(hidden = true) WfAppProcess wfAppProcess, Query query) {
		wfAppProcess.setStatus(WfProcessConstant.STATUS_SEND);
		return R.data(processService.processList(wfAppProcess, query));
	}

	@GetMapping("claimList")
	@Operation(summary = "待签列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfAppProcess>> claimList(@Parameter(hidden = true) WfAppProcess wfAppProcess, Query query) {
		wfAppProcess.setStatus(WfProcessConstant.STATUS_CLAIM);
		return R.data(processService.taskList(wfAppProcess, query));
	}

	@GetMapping("copyList")
	@Operation(summary = "抄送列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R page(@Parameter(hidden = true) @RequestParam Map<String, Object> copy, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		Object processDefinitionName = copy.get("processDefinitionName");
		if (processDefinitionName != null) {
			copy.remove("processDefinitionName");
			copy.put("title", processDefinitionName);
		}
		return R.data(copyService.page(Condition.getPage(query), Condition.getQueryWrapper(copy, WfCopy.class).eq("user_id", Long.valueOf(WfTaskUtil.getTaskUser())).orderByDesc("id")));
	}

}
