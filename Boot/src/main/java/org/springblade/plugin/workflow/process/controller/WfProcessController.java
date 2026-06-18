package org.springblade.plugin.workflow.process.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.design.entity.WfButton;
import org.springblade.plugin.workflow.design.entity.WfProcessDef;
import org.springblade.plugin.workflow.design.service.IWfButtonService;
import org.springblade.plugin.workflow.design.service.IWfDesignService;
import org.springblade.plugin.workflow.design.service.IWfFormService;
import org.springblade.plugin.workflow.process.entity.WfCopy;
import org.springblade.plugin.workflow.process.model.WfNode;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.process.service.IWfCopyService;
import org.springblade.plugin.workflow.process.service.IWfProcessService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@RestController
@RequestMapping("/blade-workflow/process")
@AllArgsConstructor
@PreAuth(menu = "wf_process")
public class WfProcessController {

	private final IWfDesignService designService;
	private final IWfProcessService processService;
	private final IWfFormService formService;
	private final IWfButtonService buttonService;
	private final IWfCopyService copyService;

	private final HistoryService historyService;

	@GetMapping("processList")
	@Operation(summary = "可发起流程列表")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcessDef>> processList(@Parameter(hidden = true) WfProcessDef processDef, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		processDef.setStatus(1);
		processDef.setScope(true);
		return R.data(designService.deploymentPage(processDef, query));
	}

	@GetMapping("getFormByProcessId")
	@Operation(summary = "获取流程表单")
	@Parameters({
		@Parameter(name = "processId", description = "流程定义id", required = true),
	})
	public R<Map<String, Object>> getFormByProcessId(String processId) {
		if (ObjectUtil.isAnyEmpty(processId)) {
			return R.fail("参数错误");
		}
		return R.data(formService.getFormByProcessDefId(processId));
	}

	@GetMapping("getFormByProcessDefKey")
	@Operation(summary = "获取流程表单")
	@Parameters({
		@Parameter(name = "processDefKey", description = "流程定义key", required = true),
	})
	public R<Map<String, Object>> getFormByProcessDefKey(String processDefKey) {
		if (ObjectUtil.isAnyEmpty(processDefKey)) {
			return R.fail("参数错误");
		}
		return R.data(formService.getFormByProcessDefKey(processDefKey));
	}

	@PostMapping("startProcess")
	@Operation(summary = "发起流程")
	@Parameters({
		@Parameter(name = "processId", description = "流程定义id", required = true),
	})
	public R<String> startProcess(@Parameter(hidden = true) @RequestBody JSONObject body) {
		String processDefId = body.getString("processId");
		body.remove("processId");
		try {
			return R.data(processService.startProcessInstanceById(processDefId, body));
		} catch (Exception e) {
			return R.fail(e.getLocalizedMessage());
		}
	}

	@PostMapping("startProcessByKey")
	@Operation(summary = "发起流程 - 根据key")
	@Parameters({
		@Parameter(name = "processDefKey", description = "流程定义key", required = true),
	})
	public R<String> startProcessByKey(@Parameter(hidden = true) @RequestBody JSONObject body) {
		String processDefKey = body.getString("processDefKey");
		body.remove("processDefKey");
		String businessKey = body.getString("businessKey");
		body.remove("businessKey");
		try {
			return R.data(processService.startProcessInstanceByKey(processDefKey, businessKey, body));
		} catch (Exception e) {
			return R.fail(e.getLocalizedMessage());
		}
	}

	@GetMapping("todoList")
	@Operation(summary = "待办列表（包含待签）")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcess>> todoList(@Parameter(hidden = true) WfProcess process, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		process.setStatus(WfProcessConstant.STATUS_TODO);
		return R.data(processService.selectTaskPage(process, query));
	}

	@GetMapping("doneList")
	@Operation(summary = "办结列表")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcess>> doneList(@Parameter(hidden = true) WfProcess process, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		process.setStatus(WfProcessConstant.STATUS_DONE);
		return R.data(processService.selectProcessPage(process, query));
	}

	@GetMapping("myDoneList")
	@Operation(summary = "我的已办（流程不一定办结）")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcess>> myDoneList(@Parameter(hidden = true) WfProcess process, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		process.setStatus(WfProcessConstant.STATUS_DONE);
		return R.data(processService.selectTaskPage(process, query));
	}

	@GetMapping("sendList")
	@Operation(summary = "我的请求列表")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcess>> sendList(@Parameter(hidden = true) WfProcess process, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		process.setStatus(WfProcessConstant.STATUS_SEND);
		return R.data(processService.selectProcessPage(process, query));
	}

	@GetMapping("claimList")
	@Operation(summary = "待签列表")
	@Parameters({
		@Parameter(name = "name", description = "流程名称"),
		@Parameter(name = "key", description = "流程标识"),
		@Parameter(name = "category", description = "流程分类"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfProcess>> claimList(@Parameter(hidden = true) WfProcess process, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		process.setStatus(WfProcessConstant.STATUS_CLAIM);
		return R.data(processService.selectTaskPage(process, query));
	}

	@GetMapping("copyList")
	@Operation(summary = "抄送列表")
	@Parameters({
		@Parameter(name = "title", description = "标题"),
		@Parameter(name = "initiator", description = "发起人"),
		@Parameter(name = "current", description = "当前第几页", required = true),
		@Parameter(name = "size", description = "每页条数", required = true),
	})
	public R<IPage<WfCopy>> page(@Parameter(hidden = true) @RequestParam Map<String, Object> copy, Query query) {
		if (ObjectUtil.isAnyEmpty(query.getCurrent(), query.getSize())) {
			return R.fail("参数错误");
		}
		return R.data(copyService.page(Condition.getPage(query), Condition.getQueryWrapper(copy, WfCopy.class).eq("user_id", Long.valueOf(WfTaskUtil.getTaskUser())).orderByDesc("id")));
	}

	@GetMapping("detail")
	@Operation(summary = "流程详情")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id"),
		@Parameter(name = "processInsId", description = "流程实例id", required = true),
	})
	public R<Map<String, Object>> detail(String taskId, String processInsId) throws ExecutionException, InterruptedException {
		if (ObjectUtil.isAnyEmpty(processInsId)) {
			return R.fail("参数错误");
		}
		// 流程查看权限，参与人或者抄送人。若无需此功能请注释。
		boolean access = processService.hasProcessAccess(processInsId, WfTaskUtil.getTaskUser());
		if (!access) {
			return R.fail("暂无权限查看");
		}
		if (StringUtil.isBlank(taskId)) { // 不传taskId的情况下，取最后一个创建的任务
			List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
				.processInstanceId(processInsId)
				.orderByHistoricTaskInstanceStartTime()
				.desc()
				.list();
			if (list.isEmpty()) {
				return R.fail("查询不到任务详情");
			}
			taskId = list.get(0).getId();
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

	@GetMapping("getXmlByProcessDefId")
	@Operation(summary = "获取模型xml - 根据processDefId")
	@Parameters({
		@Parameter(name = "processDefId", description = "流程定义id", required = true),
	})
	public R getXmlByProcessDefId(String processDefId) {
		if (ObjectUtil.isAnyEmpty(processDefId)) {
			return R.fail("参数错误");
		}
		return R.data(processService.getXmlByProcessDefId(processDefId));
	}

	@GetMapping("getXmlByProcessDefKey")
	@Operation(summary = "获取模型xml - 根据processDefKey")
	@Parameters({
		@Parameter(name = "processDefKey", description = "流程定义key", required = true),
	})
	public R getXmlByProcessDefKey(String processDefKey) {
		if (ObjectUtil.isAnyEmpty(processDefKey)) {
			return R.fail("参数错误");
		}
		return R.data(processService.getXmlByProcessDefKey(processDefKey));
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
	public R completeTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R transferTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R delegateTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R claimTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R<List<WfNode>> getBackNodes(@Parameter(hidden = true) WfProcess process) {
		return R.data(processService.getBackNodes(process));
	}

	@PostMapping("rollbackTask")
	@Operation(summary = "退回到指定节点")
	@Parameters({
		@Parameter(name = "taskId", description = "任务id", required = true),
		@Parameter(name = "nodeId", description = "节点id", required = true),
		@Parameter(name = "comment", description = "评论", required = true),
	})
	public R rollbackTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R terminateProcess(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R addMultiInstance(@Parameter(hidden = true) @RequestBody WfProcess process) {
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
	public R withdrawTask(@Parameter(hidden = true) @RequestBody WfProcess process) {
		if (ObjectUtil.isAnyEmpty(process.getTaskId())) {
			return R.fail("参数错误");
		}
		return (R) processService.withdrawTask(process);
	}
}
