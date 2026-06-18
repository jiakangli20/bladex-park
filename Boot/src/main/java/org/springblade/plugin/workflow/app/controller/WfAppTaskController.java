package org.springblade.plugin.workflow.app.controller;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.app.entity.WfAppProcess;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.design.entity.WfButton;
import org.springblade.plugin.workflow.design.service.IWfButtonService;
import org.springblade.plugin.workflow.design.service.IWfFormService;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.process.service.IWfProcessService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/blade-workflow/app/task")
@AllArgsConstructor
@PreAuth(menu = "wf_process")
@Tag(name = "workflow - app - 流程任务")
public class WfAppTaskController {

	private final IWfFormService formService;
	private final IWfButtonService buttonService;
	private final IWfProcessService processService;

	@GetMapping("getFormByProcessDefId")
	@Operation(summary = "获取流程发起表单")
	@Parameters({
		@Parameter(name = "processDefId", description = "流程定义id", required = true),
	})
	public R getFormByProcessId(String processDefId) {
		if (ObjectUtil.isAnyEmpty(processDefId)) {
			return R.fail("参数错误");
		}
		return R.data(formService.getFormByProcessDefId(processDefId));
	}

	@PostMapping("startProcess")
	@Operation(summary = "发起流程")
	@Parameters({
		@Parameter(name = "processId", description = "流程定义id", required = true),
	})
	public R startProcess(@Parameter(hidden = true) @RequestBody JSONObject body) {
		String processDefId = body.getString("processId");
		body.remove("processId");
		try {
			return R.data(processService.startProcessInstanceById(processDefId, body));
		} catch (Exception e) {
			return R.fail(e.getLocalizedMessage());
		}
	}

	@GetMapping("detail")
	@Operation(summary = "任务详情")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "processInsId", description = "流程实例id", required = true),
	})
	public R detail(String taskId, String processInsId) throws ExecutionException, InterruptedException {
		if (ObjectUtil.isAnyEmpty(taskId, processInsId)) {
			return R.fail("参数错误");
		}
		Future<WfProcess> processFuture = processService.detail(taskId, WfTaskUtil.getTaskUser(), WfTaskUtil.getCandidateGroup());
		Future<Map<String, Object>> formFuture = formService.getFormByTaskId(taskId);
		Future<List<WfProcess>> flowFuture = processService.historyFlowList(processInsId, null, null, WfTaskUtil.getTokenUser());
		Future<List<WfButton>> buttonFuture = buttonService.getButtonByTaskId(taskId);

		Map<String, Object> result = new HashMap<>();
		result.put("process", processFuture.get());
		result.put("form", formFuture.get());
		result.put("flow", flowFuture.get());
		result.put("button", buttonFuture.get());
		return R.data(result);
	}

	@PostMapping("completeTask")
	@Operation(summary = "完成任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "processInstanceId", description = "流程实例id", required = true),
		@Parameter(name = "pass", description = "同意/驳回", required = true),
		@Parameter(name = "comment", description = "评论"),
		@Parameter(name = "copyUser", description = "抄送人"),
		@Parameter(name = "assignee", description = "指定审批人"),
		@Parameter(name = "variables", description = "表单参数"),
	})
	public R completeTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId(), process.getProcessInstanceId())) {
			return R.fail("参数错误");
		}
		return (R) processService.completeTask(process);
	}

	@PostMapping("transferTask")
	@Operation(summary = "转办任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "assignee", description = "接受人", required = true),
	})
	public R transferTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId(), process.getAssignee())) {
			return R.fail("参数错误");
		}
		return (R) processService.transferTask(process);
	}

	@PostMapping("delegateTask")
	@Operation(summary = "代理任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "assignee", description = "接受人", required = true),
	})
	public R delegateTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId(), process.getAssignee())) {
			return R.fail("参数错误");
		}
		return (R) processService.delegateTask(process);
	}

	@PostMapping("claimTask")
	@Operation(summary = "签收任务（签收成功后回到待办任务中）")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
	})
	public R claimTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId())) {
			return R.fail("参数错误");
		}
		return (R) processService.claimTask(process.getTaskId());
	}

	@GetMapping("getBackNodes")
	@Operation(summary = "获取可退回节点")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
	})
	public R getBackNodes(@Parameter(hidden = true) WfAppProcess process) {
		return R.data(processService.getBackNodes(process));
	}

	@PostMapping("rollbackTask")
	@Operation(summary = "退回到指定节点")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "nodeId", description = "节点id", required = true),
		@Parameter(name = "comment", description = "评论", required = true),
	})
	public R rollbackTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId(), process.getNodeId(), process.getComment())) {
			return R.fail("参数错误");
		}
		return (R) processService.rollbackTask(process);
	}

	@PostMapping("terminateProcess")
	@Operation(summary = "终止流程")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
	})
	public R terminateProcess(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId())) {
			return R.fail("参数错误");
		}
		return (R) processService.terminateProcess(process);
	}

	@PostMapping("addMultiInstance")
	@Operation(summary = "加签")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "assignee", description = "加签人员", required = true),
	})
	public R addMultiInstance(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId(), process.getAssignee())) {
			return R.fail("参数错误");
		}
		return (R) processService.addMultiInstance(process);
	}

	@PostMapping("withdrawTask")
	@Operation(summary = "撤销/撤回")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "withdrawType", description = "类型", required = true),
	})
	public R withdrawTask(@Parameter(hidden = true) @RequestBody WfAppProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId())) {
			return R.fail("参数错误");
		}
		return (R) processService.withdrawTask(process);
	}

}
