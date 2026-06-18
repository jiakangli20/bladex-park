package org.springblade.plugin.workflow.ops.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.ops.model.WfOps;
import org.springblade.plugin.workflow.ops.service.IWfOpsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blade-workflow/ops")
@AllArgsConstructor
@PreAuth(menu = "wf_ops")
public class WfOpsController {

	private final IWfOpsService wfOpsService;

	@GetMapping("list")
	@Operation(summary = "全部待办任务")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "processDefinitionKey", description = "流程标识"),
		@Parameter(name = "serialNumber", description = "流水号"),
		@Parameter(name = "category", description = "分类"),
		@Parameter(name = "taskName", description = "当前节点"),
		@Parameter(name = "assignee", description = "审核人"),
		@Parameter(name = "applyUserName", description = "申请人"),
		@Parameter(name = "isSuspended", description = "是否挂起"),
		@Parameter(name = "date", description = "时间范围，逗号分隔"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R list(@Parameter(hidden = true) WfOps ops, Query query) {
		return R.data(wfOpsService.list(ops, query));
	}

	@GetMapping("processList")
	@Operation(summary = "全部流程列表")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "processDefinitionKey", description = "流程标识"),
		@Parameter(name = "serialNumber", description = "流水号"),
		@Parameter(name = "category", description = "分类"),
		@Parameter(name = "applyUserName", description = "申请人"),
		@Parameter(name = "date", description = "时间范围，逗号分隔"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R processList(@Parameter(hidden = true) WfOps ops, Query query) {
		return R.data(wfOpsService.processList(ops, query));
	}

	@GetMapping("doneList")
	@Operation(summary = "办结流程")
	@Parameters({
		@Parameter(name = "processDefinitionName", description = "流程名称"),
		@Parameter(name = "processDefinitionKey", description = "流程标识"),
		@Parameter(name = "serialNumber", description = "流水号"),
		@Parameter(name = "category", description = "分类"),
		@Parameter(name = "applyUserName", description = "申请人"),
		@Parameter(name = "date", description = "时间范围，逗号分隔"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R doneList(@Parameter(hidden = true) WfOps ops, Query query) {
		ops.setStatus(WfProcessConstant.STATUS_FINISH);
		return R.data(wfOpsService.processList(ops, query));
	}

	@PostMapping("completeTask")
	@Operation(summary = "完成任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "isPass", description = "审核是否通过", required = true),
	})
	public R completeTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.isPass())) {
			return R.fail("参数错误");
		}
		wfOpsService.completeTask(ops);
		return R.success("操作成功");
	}

	@PostMapping("changeTaskAssignee")
	@Operation(summary = "变更审核人")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "assignee", description = "审核人", required = true),
	})
	public R changeTaskAssignee(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getAssignee())) {
			return R.fail("参数错误");
		}
		wfOpsService.changeTaskAssignee(ops);
		return R.success("变更成功");
	}

	@PostMapping("changeTaskStatus")
	@Operation(summary = "变更任务状态")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "isSuspended", description = "是否挂起", required = true),
	})
	public R changeTaskStatus(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getIsSuspended())) {
			return R.fail("参数错误");
		}
		wfOpsService.changeTaskStatus(ops);
		return R.success("变更成功");
	}

	@PostMapping("transferTask")
	@Operation(summary = "转办任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "assignee", description = "审核人", required = true),
	})
	public R transferTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getAssignee())) {
			return R.fail("参数错误");
		}
		wfOpsService.transferTask(ops);
		return R.success("转办成功");
	}

	@PostMapping("delegateTask")
	@Operation(summary = "委托任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "assignee", description = "审核人", required = true),
	})
	public R delegateTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getAssignee())) {
			return R.fail("参数错误");
		}
		wfOpsService.delegateTask(ops);
		return R.success("委托成功");
	}

	@PostMapping("copyTask")
	@Operation(summary = "抄送任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
		@Parameter(name = "assignee", description = "审核人", required = true),
	})
	public R copyTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getAssignee())) {
			return R.fail("参数错误");
		}
		wfOpsService.copyTask(ops);
		return R.success("抄送成功");
	}

	@PostMapping("urgeTask")
	@Operation(summary = "催办任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
	})
	public R urgeTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId())) {
			return R.fail("参数错误");
		}
		wfOpsService.urgeTask(ops);
		return R.success("催办成功");
	}

	@PostMapping("terminateProcess")
	@Operation(summary = "终止流程")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id，多条逗号分割", required = true),
	})
	public R terminateProcess(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId())) {
			return R.fail("参数错误");
		}
		wfOpsService.terminateProcess(ops);
		return R.success("变更成功");
	}

	@GetMapping("processNodes")
	@Operation(summary = "流程节点")
	@Parameters({
		@Parameter(name = "processInstanceId", description = "流程", required = true),
		@Parameter(name = "taskId", description = "任务id"),
	})
	public R processNodes(@Parameter(hidden = true) WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getProcessInstanceId())) {
			return R.fail("参数错误");
		}
		return R.data(wfOpsService.processNodes(ops));
	}

	@PostMapping("rollbackTask")
	@Operation(summary = "指定回退")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "nodeId", description = "节点id", required = true),
	})
	public R rollbackTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getNodeId())) {
			return R.fail("参数错误");
		}
		wfOpsService.rollbackTask(ops);
		return R.success("操作成功");
	}

	@PostMapping("dispatchTask")
	@Operation(summary = "调度任务")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "nodeId", description = "节点id", required = true),
	})
	public R dispatchTask(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getNodeId())) {
			return R.fail("参数错误");
		}
		wfOpsService.dispatchTask(ops);
		return R.success("操作成功");
	}

	@PostMapping("addMultiInstance")
	@Operation(summary = "加签")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "assignee", description = "加签人员", required = true),
	})
	public R addMultiInstance(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId(), ops.getAssignee())) {
			return R.fail("参数错误");
		}
		wfOpsService.addMultiInstance(ops);
		return R.success("操作成功");
	}

	@PostMapping("deleteMultiInstance")
	@Operation(summary = "减签")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
	})
	public R deleteMultiInstance(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getTaskId())) {
			return R.fail("参数错误");
		}
		wfOpsService.deleteMultiInstance(ops);
		return R.success("操作成功");
	}

	@PostMapping("deleteProcess")
	@Operation(summary = "删除流程")
	@Parameters({
		@Parameter(name = "processInstanceId", description = "流程实例id", required = true),
	})
	public R deleteProcess(@Parameter(hidden = true) @RequestBody WfOps ops) {
		if (ObjectUtil.isAnyEmpty(ops.getProcessInstanceId())) {
			return R.fail("参数错误");
		}
		wfOpsService.deleteProcessInstance(ops);
		return R.success("操作成功");
	}

}
