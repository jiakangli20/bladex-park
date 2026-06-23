package org.springblade.plugin.workflow.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityImpl;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.springblade.core.mp.support.Query;
import org.springblade.core.redis.lock.RedisLock;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.cache.WfProcessCache;
import org.springblade.plugin.workflow.core.constant.WfExtendConstant;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.user.WfTokenUser;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfModelUtil;
import org.springblade.plugin.workflow.core.utils.WfSearchUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.design.service.IWfFormVariableService;
import org.springblade.plugin.workflow.design.service.IWfSerialService;
import org.springblade.plugin.workflow.process.dto.WfCopyDTO;
import org.springblade.plugin.workflow.process.entity.WfCopy;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.model.WfNode;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.process.model.WfTaskUser;
import org.springblade.plugin.workflow.process.service.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.lang.Nullable;
import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfProcessService implements IWfProcessService {

	private final RuntimeService runtimeService;
	private final IdentityService identityService;
	private final HistoryService historyService;
	private final TaskService taskService;
	private final RepositoryService repositoryService;

	private final IWfCopyService wfCopyService;
	private final IWfSerialService wfSerialService;
	private final IWfNoticeService wfNoticeService;
	private final IWfDraftService wfDraftService;
	private final IWfExpressionService wfExpressionService;
	private final IWfTaskService wfTaskService;
	private final IWfFormVariableService wfFormVariableService;

	// 流程图或流转信息的人名假如超过几个人后显示 xx 共xx人
	// 用户1/用户2/用户3/用户4/用户5/用户6 => 用户1/用户2/用户3 等共6人
	private static final int FLOW_ASSIGNEE_NAME_LIMIT = 3;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String startProcessInstanceById(String processDefId, Map<String, Object> variables) {
		ProcessDefinition definition = WfProcessCache.getProcessDefinition(processDefId);
		if (definition == null) {
			throw new RuntimeException("查询不到此部署的流程");
		}
		String userId = WfTaskUtil.getTaskUser();
		variables.put(WfProcessConstant.TASK_VARIABLE_APPLY_USER, userId);
		// 启动流程
		identityService.setAuthenticatedUserId(userId);
		ProcessInstance processInstance = runtimeService.startProcessInstanceById(processDefId, definition.getKey(), variables);
		identityService.setAuthenticatedUserId(null);
		return handleProcessInstance(processInstance, variables);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public String startProcessInstanceByKey(String processDefKey, String businessKey, Map<String, Object> variables) {
		String tenantId = WfTaskUtil.getTenantId();
		String userId = WfTaskUtil.getTaskUser();
		variables.put(WfProcessConstant.TASK_VARIABLE_APPLY_USER, userId);
		// 启动流程
		identityService.setAuthenticatedUserId(userId);
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKeyAndTenantId(processDefKey, businessKey, variables, tenantId);
		identityService.setAuthenticatedUserId(null);
		return handleProcessInstance(processInstance, variables);
	}

	private String handleProcessInstance(ProcessInstance processInstance, @Nullable Map<String, Object> variables) {
		processInstance = runtimeService.createProcessInstanceQuery()
			.processInstanceId(processInstance.getId())
			.singleResult();
		if (processInstance == null) { // 流程已结束，无需处理以下逻辑。节点全部跳过时会出现此情况。
			return null;
		}
		String userId = WfTaskUtil.getTaskUser();
		String processDefId = processInstance.getProcessDefinitionId();
		if (variables == null) {
			variables = new HashMap<>();
		}
		// 业务变量
		Map<String, Object> customVariables = new HashMap<>();
		customVariables.put("_FLOWABLE_SKIP_EXPRESSION_ENABLED", true); // 打开流程表达式跳过

		WfUser user = WfUserCache.getUser(Long.parseLong(userId));
		customVariables.put(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, user.getName());
		// 流水号
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
		String sn = wfSerialService.getNextSN(bpmnModel, processInstance.getDeploymentId());
		if (StringUtil.isNotBlank(sn)) {
			customVariables.put(WfProcessConstant.TASK_VARIABLE_SN, sn);
		}
		runtimeService.setVariables(processInstance.getId(), customVariables);

		// 修改流程实例名称，方便查询。只有流程列表可查询，任务列表只可查询流程部署名称。
		try {
			variables.putAll(customVariables);
			runtimeService.setProcessInstanceName(processInstance.getId(), wfExpressionService.getProcessTitle(bpmnModel, processInstance, variables));
		} catch (Exception ignore) {
			return processInstance.getId();
		}
		// 自动跳过第一节点
		String skip = WfModelUtil.getProcessExtensionAttribute(bpmnModel, WfExtendConstant.SKIP_FIRST_NODE);
		if (StringUtil.isNotBlank(skip) && "true".equals(skip)) {
			List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
			Map<String, Object> skipVariables = new HashMap<>();
			skipVariables.put(WfProcessConstant.TASK_VARIABLE_SKIP, true);
			taskList.forEach(task -> taskService.complete(task.getId(), skipVariables, true));
		}
		// 指定下一步审核人
		int num = this.handleNextNodeAssignee(processInstance.getId(), variables.get(WfProcessConstant.TASK_VARIABLE_ASSIGNEE));
		if (num > 0) {
			throw new RuntimeException("需指定 " + num + " 个审核人");
		}

		// 处理抄送
		Object copyUser = variables.get(WfProcessConstant.TASK_VARIABLE_COPY_USER);
		if (ObjectUtil.isNotEmpty(copyUser)) {
			List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
			if (!taskList.isEmpty()) {
				Task task = taskList.get(0);
				wfCopyService.resolveCopyUser((WfCopyDTO) new WfCopyDTO()
						.setCopyUser(copyUser.toString())
						.setTask(task)
						.setProcessInstance(processInstance)
                );
			}
		}
		// 删除草稿箱
		wfDraftService.deleteByProcessDefId(processDefId, Long.valueOf(WfTaskUtil.getTaskUser()), variables.get(WfProcessConstant.TASK_VARIABLE_PLATFORM));
		// 保存开始节点表单变量
		wfFormVariableService.saveFormVariable(processInstance.getId(), null, WfModelUtil.getStartEvent(bpmnModel), variables, user.getId());
		return processInstance.getId();
	}

	@Override
	public IPage<WfProcess> selectTaskPage(WfProcess process, Query query) {
		IPage<WfProcess> page = new Page<>();

		String taskUser = WfTaskUtil.getTaskUser();
		String taskGroup = WfTaskUtil.getCandidateGroup();

		long count;
		switch (process.getStatus()) {
			case WfProcessConstant.STATUS_TODO:  // 待办或待签 runtimeService
			case WfProcessConstant.STATUS_CLAIM:
				TaskQuery taskQuery = taskService.createTaskQuery()
					.orderByTaskCreateTime()
					.desc()
					.taskTenantId(WfTaskUtil.getTenantId())
					.active();
				if (WfProcessConstant.STATUS_TODO.equals(process.getStatus())) { // 待办
					taskQuery
//						.taskAssignee(taskUser)
						.taskCandidateOrAssigned(taskUser)
						.taskCandidateGroupIn(Func.toStrList(taskGroup))
					;
				} else { // 待签
					taskQuery.taskCandidateUser(taskUser)
						.taskCandidateGroupIn(Func.toStrList(taskGroup));
				}
				WfSearchUtil.buildSearchQuery(taskQuery, process); // 搜索条件
				count = taskQuery.count();
				if (count > 0) {
					List<WfProcess> list = new LinkedList<>();
					buildTaskList(list, taskQuery, query, WfProcessConstant.STATUS_TODO);
					page.setRecords(list);
				}
				break;
			case WfProcessConstant.STATUS_DONE:  // 已办 historyService
				HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery()
					.orderByTaskCreateTime()
					.desc()
					.taskAssignee(taskUser)
					.finished()
					.taskTenantId(WfTaskUtil.getTenantId());
				WfSearchUtil.buildSearchQuery(historicTaskInstanceQuery, process); // 搜索条件
				count = historicTaskInstanceQuery.count();
				if (count > 0) {
					List<WfProcess> list = new LinkedList<>();
					buildTaskList(list, historicTaskInstanceQuery, query, WfProcessConstant.STATUS_DONE);
					page.setRecords(list);
				}
				break;
			default:
				return page;
		}
		page.setTotal(count);
		page.setCurrent(query.getCurrent());
		page.setSize(query.getSize());

		return page;
	}

	@Override
	public IPage<WfProcess> selectProcessPage(WfProcess process, Query query) {
		IPage<WfProcess> page = new Page<>();

		String taskUser = WfTaskUtil.getTaskUser();
		HistoricProcessInstanceQuery historyQuery = historyService.createHistoricProcessInstanceQuery()
			.processInstanceTenantId(WfTaskUtil.getTenantId())
			.orderByProcessInstanceStartTime()
			.desc();

		if (StringUtil.isNotBlank(process.getStatus())) {
			switch (process.getStatus()) {
				case WfProcessConstant.STATUS_SEND: // 我的请求
					historyQuery.startedBy(taskUser);
					break;
				case WfProcessConstant.STATUS_DONE: // 办结
					historyQuery.involvedUser(taskUser).finished();
					break;
				case WfProcessConstant.STATUS_FINISH: // 结束
					historyQuery
						.processInstanceTenantId(WfTaskUtil.getTenantId())
						.finished();
					break;
			}
		}
		WfSearchUtil.buildSearchQuery(historyQuery, process); // 搜索条件

		long count = historyQuery.count();
		if (count > 0) {
			List<WfProcess> list = new LinkedList<>();
			buildProcessList(list, historyQuery, query);
			page.setRecords(list);
		}
		page.setTotal(count);
		page.setCurrent(query.getCurrent());
		page.setSize(query.getSize());
		return page;
	}

	@Async
	@Override
	public Future<List<WfProcess>> historyFlowList(String processInstanceId, String startActivityId, String endActivityId, WfTokenUser tokenUser) {
		String tenantId = tokenUser.getTenantId();
		String userId = tokenUser.getUserId() + "";
		List<WfProcess> flowList = new LinkedList<>();
		List<HistoricActivityInstance> historicActivityInstanceList = historyService.createHistoricActivityInstanceQuery()
			.processInstanceId(processInstanceId)
			.orderByHistoricActivityInstanceStartTime().asc()
			.orderByHistoricActivityInstanceEndTime().asc()
			.list();
		List<Comment> commentList = taskService.getProcessInstanceComments(processInstanceId);
		List<Attachment> attachmentList = taskService.getProcessInstanceAttachments(processInstanceId);
		boolean start = false;
		Map<String, Integer> activityMap = new HashMap<>(16);
		for (int i = 0; i < historicActivityInstanceList.size(); i++) {
			HistoricActivityInstance historicActivityInstance = historicActivityInstanceList.get(i);
			// 过滤开始节点前的节点
			if (StringUtil.isNotBlank(startActivityId) && startActivityId.equals(historicActivityInstance.getActivityId())) {
				start = true;
			}
			if (StringUtil.isNotBlank(startActivityId) && !start) {
				continue;
			}
			// 显示开始节点和结束节点，并且执行人不为空的任务
			if (Arrays.asList(
				WfProcessConstant.USER_TASK,
				WfProcessConstant.START_EVENT,
				WfProcessConstant.END_EVENT,
				WfProcessConstant.SEQUENCE_FLOW,
				WfProcessConstant.HTTP_SERVICE_TASK,
				WfProcessConstant.SHELL_SERVICE_TASK,
				WfProcessConstant.MAIL_SERVICE_TASK,
				"intermediateCatchEvent"
			).contains(historicActivityInstance.getActivityType())
				&& !"Delete MI execution".equals(historicActivityInstance.getDeleteReason())) {
				// 给节点增加序号
				activityMap.computeIfAbsent(historicActivityInstance.getActivityId(), k -> activityMap.size());
				WfProcess flow = new WfProcess();
				flow.setHistoryActivityId(historicActivityInstance.getActivityId());
				flow.setHistoryActivityName(historicActivityInstance.getActivityName());
				flow.setHistoryActivityType(historicActivityInstance.getActivityType());
				flow.setCreateTime(historicActivityInstance.getStartTime());
				flow.setEndTime(historicActivityInstance.getEndTime());
				String durationTime = DateUtil.secondToTime(Func.toLong(historicActivityInstance.getDurationInMillis(), 0L) / 1000);
				flow.setHistoryActivityDurationTime(durationTime);
				// 获取流程发起人名称
				if (WfProcessConstant.START_EVENT.equals(historicActivityInstance.getActivityType())) {
					List<HistoricProcessInstance> processInstanceList = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).orderByProcessInstanceStartTime().asc().list();
					if (processInstanceList.size() > 0) {
						if (StringUtil.isNotBlank(processInstanceList.get(0).getStartUserId())) {
							String taskUser = processInstanceList.get(0).getStartUserId();
							WfUser user = WfUserCache.getUser(WfTaskUtil.getUserId(taskUser), tenantId);
							if (user != null) {
								flow.setAssignee(historicActivityInstance.getAssignee());
								flow.setAssigneeName(user.getName());
							}
						}
					}
				} else if (WfProcessConstant.USER_TASK.equals(historicActivityInstance.getActivityType())) {
					// 获取任务执行人名称
					if (StringUtil.isNotBlank(historicActivityInstance.getAssignee())) {
						WfUser user = WfUserCache.getUser(WfTaskUtil.getUserId(historicActivityInstance.getAssignee()), tenantId);
						if (user != null) {
							flow.setAssignee(historicActivityInstance.getAssignee());
							flow.setAssigneeName(user.getName());
						}
					} else {
						WfTaskUser taskUser = wfTaskService.getTaskUser(historicActivityInstance.getProcessDefinitionId(), historicActivityInstance.getProcessInstanceId(), historicActivityInstance.getActivityId(), userId);
						List<WfUser> userList = taskUser.getUserList();
						if (ObjectUtil.isNotEmpty(userList)) {
							if (userList.size() == 1) {
								flow.setAssignee(String.valueOf(userList.get(0).getId()));
								flow.setAssigneeName(userList.get(0).getName());
							} else {
								String assigneeName;
								if (userList.size() > FLOW_ASSIGNEE_NAME_LIMIT) {
									assigneeName = userList.subList(0, FLOW_ASSIGNEE_NAME_LIMIT).stream().map(WfUser::getName).collect(Collectors.joining("/")) + " 等共" + userList.size() + "人";
								} else {
									assigneeName = userList.stream().map(WfUser::getName).collect(Collectors.joining("/"));
								}
								flow.setAssigneeName(assigneeName);
							}
						}
					}
				}

				// 获取意见评论内容/附件
				if (StringUtil.isNotBlank(historicActivityInstance.getTaskId())) {
					List<Comment> comments = new ArrayList<>();
					for (Comment comment : commentList) {
						if (comment.getTaskId().equals(historicActivityInstance.getTaskId())) {
							comments.add(comment);
						}
					}
					flow.setComments(comments);

					List<Attachment> attachments = new ArrayList<>();
					for (Attachment attachment : attachmentList) {
						if (attachment.getTaskId().equals(historicActivityInstance.getTaskId())) {
							attachments.add(attachment);
						}
					}
					flow.setAttachments(attachments);
				}
				flowList.add(flow);
			}
			// 过滤结束节点后的节点
			if (StringUtils.isNotBlank(endActivityId) && endActivityId.equals(historicActivityInstance.getActivityId())) {
				boolean temp = false;
				Integer activityNum = activityMap.get(historicActivityInstance.getActivityId());
				// 该活动节点，后续节点是否在结束节点之前，在后续节点中是否存在
				for (int j = i + 1; j < historicActivityInstanceList.size(); j++) {
					HistoricActivityInstance hi = historicActivityInstanceList.get(j);
					Integer activityNumA = activityMap.get(hi.getActivityId());
					boolean numberTemp = activityNumA != null && activityNumA < activityNum;
					boolean equalsTemp = StringUtils.equals(hi.getActivityId(), historicActivityInstance.getActivityId());
					if (numberTemp || equalsTemp) {
						temp = true;
					}
				}
				if (!temp) {
					break;
				}
			}
		}
		// 处理未流转到的节点
		if (historicActivityInstanceList.size() > 0) {
			String processDefId = historicActivityInstanceList.get(0).getProcessDefinitionId();
			String processInsId = historicActivityInstanceList.get(0).getProcessInstanceId();
			HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInsId).singleResult();
			if (processInstance.getEndTime() == null) { // 流程结束后不再处理未流转到的节点
				BpmnModel model = repositoryService.getBpmnModel(processDefId);

				List<FlowElement> elements = new ArrayList<>();
				model.getMainProcess().getFlowElements().forEach(flowElement -> {
					if (flowElement instanceof UserTask) {
						WfProcess wfProcess = flowList.stream().filter(flow -> flow.getHistoryActivityId().equals(flowElement.getId())).findFirst().orElse(null);
						if (wfProcess == null) {
							elements.add(flowElement);
						}
					}
				});
				if (elements.size() > 0) {
					elements.forEach(element -> {
						WfProcess flow = new WfProcess();
						flow.setHistoryActivityId(element.getId());
						flow.setHistoryActivityName(element.getName());
						flow.setHistoryActivityType(WfProcessConstant.CANDIDATE);
						List<WfUser> userList = wfTaskService.getTaskUser(processDefId, processInsId, element.getId(), userId).getUserList();
						if (ObjectUtil.isNotEmpty(userList)) {
							String assigneeName;
							if (userList.size() > FLOW_ASSIGNEE_NAME_LIMIT) {
								assigneeName = userList.subList(0, FLOW_ASSIGNEE_NAME_LIMIT).stream().map(WfUser::getName).collect(Collectors.joining("/")) + " 等共" + userList.size() + "人";
							} else {
								assigneeName = userList.stream().map(WfUser::getName).collect(Collectors.joining("/"));
							}
							flow.setAssigneeName(assigneeName);
						}
						flowList.add(flow);
					});
				}
			}
		}
		return new AsyncResult<>(flowList);
	}

	@Async
	@Override
	public Future<WfProcess> detail(String taskId, String assignee, String candidateGroup) {
		WfProcess process = new WfProcess();

		HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery() // 是否待办
			.taskId(taskId)
			.includeProcessVariables()
//			.includeTaskLocalVariables()
			.includeIdentityLinks()
			.singleResult();
		if (task == null) {
			return new AsyncResult<>(process);
		}
		if (StringUtil.isNotBlank(task.getAssignee())) { // 有审核人
			if (assignee.equals(task.getAssignee())) { // 我的任务
				if (task.getEndTime() == null) {
					process.setStatus(WfProcessConstant.STATUS_TODO);
				} else {
					process.setStatus(WfProcessConstant.STATUS_DONE);
				}
			} else {
				process.setStatus(WfProcessConstant.STATUS_DONE);
			}
		} else { // 候选或者已办
			List<? extends IdentityLinkInfo> identityLinks = task.getIdentityLinks();
			// 候选组
			List<String> roles = new ArrayList<>();
			// 候选人
			List<String> userIds = new ArrayList<>();
			identityLinks.forEach(link -> {
				if (StringUtil.isNotBlank(link.getGroupId())) {
					roles.add(link.getGroupId());
				}
				if (StringUtil.isNotBlank(link.getUserId())) {
					userIds.add(link.getUserId());
				}
			});
			List<String> candidateGroups = Arrays.asList(candidateGroup.split(","));
			if ((userIds.contains(assignee) || roles.stream().anyMatch(candidateGroups::contains)) && task.getEndTime() == null) { // 是否选人或候选组
				process.setStatus(WfProcessConstant.STATUS_TODO);
			} else {
				process.setStatus(WfProcessConstant.STATUS_DONE);
			}
		}

		process.setIsMultiInstance(wfTaskService.isMultiInstance(task.getTaskDefinitionKey(), task.getProcessDefinitionId()));
		process.setTaskId(task.getId());
		process.setTaskDefinitionKey(task.getTaskDefinitionKey());
		process.setTaskName(task.getName());
		process.setAssignee(task.getAssignee());
		process.setCreateTime(task.getCreateTime());
		process.setExecutionId(task.getExecutionId());
		process.setHistoryTaskEndTime(task.getEndTime());
		Map<String, Object> variables = task.getProcessVariables();
		variables.putAll(task.getTaskLocalVariables());

		// 删除历史固定变量字段，防止提交时重复序列化
		if (variables.containsKey(WfProcessConstant.TASK_VARIABLE_FORM_OPTION)) {
			variables.remove(WfProcessConstant.TASK_VARIABLE_FORM_OPTION);
			try {
				runtimeService.removeVariable(task.getExecutionId(), WfProcessConstant.TASK_VARIABLE_FORM_OPTION);
			} catch (Exception ignore) {
			}
		}
		if (variables.containsKey(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE)) {
			variables.remove(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE);
			try {
				runtimeService.removeVariable(task.getExecutionId(), WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE);
			} catch (Exception ignore) {
			}
		}

		process.setVariables(variables);
		process.setProcessInstanceId(task.getProcessInstanceId());

		ProcessDefinition processDefinition = WfProcessCache.getProcessDefinition(task.getProcessDefinitionId());

		process.setProcessDefinitionId(processDefinition.getId());
		process.setProcessDefinitionName(processDefinition.getName());
		process.setProcessDefinitionKey(processDefinition.getKey());
		process.setProcessDefinitionVersion(processDefinition.getVersion());
		process.setCategory(processDefinition.getCategory());

		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(task.getProcessInstanceId())
			.singleResult();
		WfUser starter = WfUserCache.getUser(NumberUtils.toLong(processInstance.getStartUserId()));
		if (starter != null) {
			process.setStartUsername(starter.getName());
		}
		process.setIsOwner(assignee.equals(processInstance.getStartUserId()));
		process.setIsReturnable(this.isReturnable(processInstance.getId(), assignee));
		process.setBusinessId(processInstance.getBusinessKey());

		// 流程状态
		this.setProcessStatus(process, processInstance);

		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());

		process.setXml(new String(new BpmnXMLConverter().convertToXML(bpmnModel)));
		// BpmnModel转换为xml，上面的办法会丢失互斥网关中间的icon。下面的办法修改模型后会导致旧版本部署的流程流程图也变。自行取舍。
//		try {
//			WfModel model = wfModelService.selectOne(new LambdaQueryWrapper<WfModel>().eq(WfModel::getModelKey, processDefinition.getKey()));
//			if (model == null) throw new RuntimeException();
//			process.setXml(model.getXml());
//		} catch (Exception e) {
//			process.setXml(new String(new BpmnXMLConverter().convertToXML(bpmnModel)));
//		}

		if (process.getStatus().equals(WfProcessConstant.STATUS_TODO)) { // 待办再查询扩展属性
			String taskKey = task.getTaskDefinitionKey();

			// 隐藏评论附件选项
			String hideAttachment = WfModelUtil.getUserTaskExtensionAttribute(taskKey, bpmnModel, WfExtendConstant.HIDE_ATTACHMENT);
			if (StringUtil.isNotBlank(hideAttachment) && "true".equals(hideAttachment)) {
				process.setHideAttachment(true);
			}

			// 隐藏抄送人选项
			String hideCopy = WfModelUtil.getUserTaskExtensionAttribute(taskKey, bpmnModel, WfExtendConstant.HIDE_COPY);
			if (StringUtil.isNotBlank(hideCopy) && "true".equals(hideCopy)) {
				process.setHideCopy(true);
			}

			// 隐藏下一步审核人选项
			String hideExamine = WfModelUtil.getUserTaskExtensionAttribute(taskKey, bpmnModel, WfExtendConstant.HIDE_EXAMINE);
			if (StringUtil.isNotBlank(hideExamine) && "true".equals(hideExamine)) {
				process.setHideExamine(true);
			}

			// 默认抄送人
			List<ExtensionElement> copyUsers = WfModelUtil.getUserTaskExtensionElements(taskKey, bpmnModel, WfExtendConstant.COPY_USER);
			if (copyUsers != null && copyUsers.size() > 0) {
				List<String> values = new ArrayList<>();
				List<String> texts = new ArrayList<>();
				copyUsers.forEach(copyUser -> {
					String value = copyUser.getAttributes().get("value").get(0).getValue();
					String text = copyUser.getAttributes().get("text").get(0).getValue();
					if (StringUtil.isNoneBlank(value, text)) {
						values.add(value);
						texts.add(text);
					}
				});
				if (values.size() > 0 && texts.size() > 0) {
					process.setCopyUser(String.join(",", values));
					process.setCopyUserName(String.join(",", texts));
				}
			}
		}

		return new AsyncResult<>(process);
	}

	@Override
	public String getXmlByProcessDefId(String processDefId) {
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
		if (bpmnModel != null) {
			return new String(new BpmnXMLConverter().convertToXML(bpmnModel));
		}
		return null;
	}

	@Override
	public String getXmlByProcessDefKey(String processDefKey) {
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
			.processDefinitionKey(processDefKey)
			.latestVersion()
			.singleResult();
		if (processDefinition != null) {
			return getXmlByProcessDefId(processDefinition.getId());
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object completeTask(WfProcess process) {
		String taskId = process.getTaskId();

		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
//		taskService.setVariable(task.getId(), WfProcessConstant.PASS_KEY, process.isPass());
		process.setTaskName(process.getProcessDefinitionName() + "-" + task.getName());
		process.setAssigneeName(WfTaskUtil.getNickName());
		process.setTaskDefinitionKey(task.getTaskDefinitionKey());

		// 最后操作人
		taskService.setVariable(taskId, WfProcessConstant.TASK_VARIABLE_LATEST_TASK_ASSIGNEE, WfTaskUtil.getTaskUser());
		if (process.isPass()) { // 审核通过
			this.passTask(process, task);
		} else { // 审核不通过
			this.rejectTask(process);
		}

		// 处理抄送
		if (StringUtil.isNotBlank(process.getCopyUser())) {
			wfCopyService.resolveCopyUser((WfCopyDTO) new WfCopyDTO()
					.setCopyUser(process.getCopyUser())
					.setTask(task));
		}
		// 删除草稿箱
		wfDraftService.deleteByTaskId(taskId, WfTaskUtil.getTaskUser());

		return R.success("操作成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object transferTask(WfProcess process) {
		String taskId = process.getTaskId();
		String acceptUser = process.getAssignee();
		String comment = process.getComment();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		if (StringUtil.isNotBlank(comment)) {
			WfUser fromUser = WfUserCache.getUser(Long.valueOf(WfTaskUtil.getTaskUser()));
			WfUser toUser = WfUserCache.getUser(Long.valueOf(acceptUser));
			if (fromUser != null && toUser != null) {
				comment = fromUser.getName() + "→" + toUser.getName() + "：" + comment;
			}
			taskService.addComment(taskId, task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_TRANSFER, comment);
		}
		// 评论附件
		List<AttachmentEntityImpl> attachment = process.getAttachment();
		if (ObjectUtil.isNotEmpty(attachment)) {
			String finalComment = comment;
			attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_TRANSFER, taskId, task.getProcessInstanceId(), att.getName(), finalComment, att.getUrl())));
		}
		taskService.removeVariable(taskId, WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE); // 删除撤回/驳回标记
		if (StringUtil.isBlank(task.getOwner())) taskService.setOwner(taskId, WfTaskUtil.getTaskUser());
		taskService.setAssignee(taskId, acceptUser);

		// 处理抄送
		if (StringUtil.isNotBlank(process.getCopyUser())) {
			wfCopyService.resolveCopyUser((WfCopyDTO) new WfCopyDTO()
					.setCopyUser(process.getCopyUser())
					.setTask(task));
		}

		// 处理消息
		wfNoticeService.sendNotice(new WfNotice()
				.setFromUserId(WfTaskUtil.getTaskUser())
				.setToUserId(acceptUser)
				.setTask(task)
				.setProcessInsId(task.getProcessInstanceId())
				.setComment(comment)
				.setType(WfNotice.Type.TRANSFER));
		return R.success("转办成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object delegateTask(WfProcess process) {
		String taskId = process.getTaskId();
		String acceptUser = process.getAssignee();
		String comment = process.getComment();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		if (StringUtil.isNotBlank(comment)) {
			WfUser fromUser = WfUserCache.getUser(Long.valueOf(WfTaskUtil.getTaskUser()));
			WfUser toUser = WfUserCache.getUser(Long.valueOf(acceptUser));
			if (fromUser != null && toUser != null) {
				comment = fromUser.getName() + "→" + toUser.getName() + "：" + comment;
			}
			taskService.addComment(taskId, task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_DELEGATE, comment);
		}
		// 评论附件
		List<AttachmentEntityImpl> attachment = process.getAttachment();
		if (ObjectUtil.isNotEmpty(attachment)) {
			String finalComment = comment;
			attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_DELEGATE, taskId, task.getProcessInstanceId(), att.getName(), finalComment, att.getUrl())));
		}
		taskService.removeVariable(taskId, WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE); // 删除撤回/驳回标记
		if (StringUtil.isBlank(task.getOwner())) taskService.setOwner(taskId, WfTaskUtil.getTaskUser());
		taskService.delegateTask(taskId, acceptUser);

		// 处理抄送
		if (StringUtil.isNotBlank(process.getCopyUser())) {
			wfCopyService.resolveCopyUser((WfCopyDTO) new WfCopyDTO()
					.setCopyUser(process.getCopyUser())
					.setTask(task));
		}

		// 处理消息
		wfNoticeService.sendNotice(new WfNotice()
				.setFromUserId(WfTaskUtil.getTaskUser())
				.setToUserId(acceptUser)
				.setProcessInsId(task.getProcessInstanceId())
				.setTask(task)
				.setComment(comment)
				.setType(WfNotice.Type.DELEGATE));
		return R.success("委托成功");
	}

	@Override
	@RedisLock(value = "WfTaskLock", param = "#taskId")
	public Object claimTask(String taskId) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		taskService.claim(taskId, WfTaskUtil.getTaskUser());
		return R.success("签收成功");
	}

	@Override
	public List<WfNode> getBackNodes(WfProcess wfProcess) {
		String taskId = wfProcess.getTaskId();
		HistoricTaskInstance task = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
		if (task == null) {
			return new ArrayList<>();
		}
		String processInstanceId = task.getProcessInstanceId();
		String currActId = task.getTaskDefinitionKey();
		String processDefinitionId = task.getProcessDefinitionId();
		Process process = repositoryService.getBpmnModel(processDefinitionId).getMainProcess();
		boolean isSubProcess = false;
		FlowNode currentFlowElement = (FlowNode) process.getFlowElement(currActId, false);
		if (currentFlowElement == null) {
			isSubProcess = true;
		}
		List<HistoricActivityInstance> activities = historyService
				.createHistoricActivityInstanceQuery()
				.processInstanceId(processInstanceId)
				.finished()
				.orderByHistoricActivityInstanceStartTime().asc()
				.orderByHistoricActivityInstanceEndTime().asc()
				.list();
		List<String> activityIds =
			activities.stream().filter(activity -> activity.getActivityType().equals(BpmnXMLConstants.ELEMENT_TASK_USER) || activity.getActivityType().equals(BpmnXMLConstants.ELEMENT_EVENT_START)).map(HistoricActivityInstance::getActivityId).filter(activityId -> !activityId.equals(currActId)).distinct().collect(Collectors.toList());
		List<WfNode> result = new ArrayList<>();
		for (String activityId : activityIds) {
			FlowNode toBackFlowElement = (FlowNode) process.getFlowElement(activityId, true);
			if (toBackFlowElement != null && (isSubProcess || ExecutionGraphUtil.isReachable(process, toBackFlowElement, currentFlowElement, Sets.newHashSet()))) {
				WfNode vo = new WfNode();
				vo.setNodeName(toBackFlowElement.getName());
				vo.setNodeId(activityId);
				result.add(vo);
			}
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object rollbackTask(WfProcess process) {
		String taskId = process.getTaskId();
		String nodeId = process.getNodeId();
		String comment = process.getComment();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		if (StringUtil.isBlank(task.getAssignee())) {
			taskService.claim(taskId, WfTaskUtil.getTaskUser());
		}

		ActivityInstance targetRealActivityInstance = runtimeService
			.createActivityInstanceQuery()
			.processInstanceId(task.getProcessInstanceId())
			.activityId(nodeId).list().get(0);
		if (targetRealActivityInstance.getActivityType().equals(BpmnXMLConstants.ELEMENT_EVENT_START)) {
			process.setProcessInstanceId(task.getProcessInstanceId());
			this.terminateProcess(process);
		} else {
			if (StringUtil.isNoneBlank(comment)) { // 增加评论
				taskService.addComment(taskId, task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_ROLLBACK, comment);
			}
			List<AttachmentEntityImpl> attachment = process.getAttachment();
			if (ObjectUtil.isNotEmpty(attachment)) { // 增加评论附件
				attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_ROLLBACK, taskId, task.getProcessInstanceId(), att.getName(), comment, att.getUrl())));
			}
			taskService.setVariable(taskId, WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_REJECT); // 添加驳回标记
			// 最后操作人
			taskService.setVariable(taskId, WfProcessConstant.TASK_VARIABLE_LATEST_TASK_ASSIGNEE, WfTaskUtil.getTaskUser());

			BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());

			// 被驳回的节点是否配置了 重新提交回到驳回人
			if (model != null) {
				String backToRejecter = WfModelUtil.getUserTaskExtensionAttribute(process.getNodeId(), model, WfExtendConstant.BACK_TO_REJECTER);
				if (StringUtil.isNotBlank(backToRejecter) && "true".equals(backToRejecter)) {
					taskService.setVariable(taskId, WfExtendConstant.BACK_TO_REJECTER, task.getTaskDefinitionKey());
				}
			}

//			this.dispatchTaskTo(task.getProcessInstanceId(), nodeId);

			UserTask userTask = WfModelUtil.getUserTaskByKey(task.getTaskDefinitionKey(), model);
            UserTask targetUserTask = WfModelUtil.getUserTaskByKey(nodeId, model);
            SubProcess targetSubProcess = null;
            if (targetUserTask != null) {
                targetSubProcess = targetUserTask.getSubProcess();
            }
            boolean isParallelGateway = false;
            boolean isSubProcess = false;
            if (userTask != null) {
                isParallelGateway = WfModelUtil.isInParallelGateway(userTask, model);
				if (!isParallelGateway) {
					SubProcess subProcess = userTask.getSubProcess();
					// 子流程驳回。1、子流程内部驳回，只驳回单个节点。2、子流程驳回到外部，驳回所有节点。
					if (subProcess != null) {
						// 当前任务与驳回目标节点不在子流程中或者不在同一子流程中。
						if (targetSubProcess != null && targetSubProcess.getId().equals(subProcess.getId())) {
							isSubProcess = true;
						}
					}
				}
            }
			if (isParallelGateway && !WfModelUtil.isInParallelGateway(targetUserTask, model)) {
                this.dispatchTaskTo(task.getProcessInstanceId(), nodeId);
            } else if (isSubProcess) {
				runtimeService.createChangeActivityStateBuilder()
						.moveExecutionToActivityId(task.getExecutionId(), nodeId).changeState();
			} else {
                runtimeService.createChangeActivityStateBuilder()
                        .processInstanceId(task.getProcessInstanceId())
                        .moveActivityIdTo(task.getTaskDefinitionKey(), nodeId).changeState();
            }
			// 添加指定标记，防止并行网关中指定时查询错误。
			List<Task> taskList = taskService.createTaskQuery()
				.processInstanceId(task.getProcessInstanceId())
				.taskDefinitionKey(nodeId)
				.list();
			if (!taskList.isEmpty()) {
				taskList.forEach(t -> taskService.setVariableLocal(t.getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1"));
			}
		}

		return R.success("退回成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object terminateProcess(WfProcess process) {
		String taskId = process.getTaskId();
		String comment = process.getComment();
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		EndEvent endEvent = WfModelUtil.getEndEvent(model);
		if (endEvent == null) {
			return R.fail("流程缺少结束节点");
		}
		if (StringUtil.isBlank(task.getAssignee())) {
			taskService.claim(taskId, WfTaskUtil.getTaskUser());
		}
		// 添加终止标记
		taskService.setVariable(taskId, WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, "true");
		// 最后操作人
		taskService.setVariable(taskId, WfProcessConstant.TASK_VARIABLE_LATEST_TASK_ASSIGNEE, WfTaskUtil.getTaskUser());

		// 增加评论
		if (StringUtil.isNoneBlank(task.getProcessInstanceId(), comment)) {
			taskService.addComment(taskId, task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_TERMINATE, comment);
		}
		// 评论附件
		List<AttachmentEntityImpl> attachment = process.getAttachment();
		if (ObjectUtil.isNotEmpty(attachment)) {
			attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_TERMINATE, taskId, task.getProcessInstanceId(), att.getName(), comment, att.getUrl())));
		}
		this.dispatchTaskTo(task.getProcessInstanceId(), endEvent.getId());
		return R.success("终止成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object addMultiInstance(WfProcess process) {
		String taskId = process.getTaskId();
		String comment = StringUtil.isBlank(process.getComment()) ? "" : process.getComment() + " - ";

		Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			return R.fail("查询不到此任务");
		}
		String assignee = process.getAssignee();
		String[] ids = assignee.split(",");
		List<String> usernames = new ArrayList<>();
		for (String id : ids) {
			WfUser user = WfUserCache.getUser(Long.valueOf(id));
			if (user == null) continue;

			// 执行加签
			runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), task.getProcessInstanceId(), Collections.singletonMap("assignee", id));

			usernames.add(user.getName());
			// 处理消息
			wfNoticeService.sendNotice(new WfNotice()
				.setFromUserId(WfTaskUtil.getTaskUser())
				.setToUserId(id)
				.setProcessInsId(task.getProcessInstanceId())
				.setTask(task)
				.setComment(comment)
				.setType(WfNotice.Type.ADD_MULTI_INSTANCE));
		}
		// 增加评论
		if (StringUtil.isBlank(comment) || comment.contains("管理员操作：")) {
			comment += Func.join(usernames);
			taskService.addComment(taskId, task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_ADD_MULTI_INSTANCE, comment);
		}
		// 评论附件
		List<AttachmentEntityImpl> attachment = process.getAttachment();
		if (ObjectUtil.isNotEmpty(attachment)) {
			String finalComment = comment;
			attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_ADD_MULTI_INSTANCE, taskId, task.getProcessInstanceId(), att.getName(), finalComment, att.getUrl())));
		}
		return R.success("操作成功");
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	@RedisLock(value = "WfTaskLock", param = "#process.taskId")
	public Object withdrawTask(WfProcess process) {
		String taskId = process.getTaskId();
		HistoricTaskInstance historicTaskInstance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
		if (historicTaskInstance == null) {
			return R.fail("查询不到此任务");
		}
		String currentUser = WfTaskUtil.getTaskUser();
		Boolean isRevocable = this.isReturnable(historicTaskInstance.getProcessInstanceId(), currentUser);
		if (!isRevocable) {
			return R.fail("此任务不可撤回");
		}
		List<Task> list = taskService.createTaskQuery().processInstanceId(historicTaskInstance.getProcessInstanceId()).list();
		if (list == null || list.isEmpty()) {
			return R.fail("没有可撤回的任务");
		}
		Task task = list.iterator().next();

		BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		if (WfProcessConstant.WITHDRAW_TYPE_START.equals(process.getWithdrawType())) { // 重新提交
			StartEvent startEvent = WfModelUtil.getStartEvent(model);
			if (startEvent != null) {
				List<SequenceFlow> outgoingFlows = startEvent.getOutgoingFlows();
				for (SequenceFlow outgoingFlow : outgoingFlows) {
					FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
					if (targetFlowElement instanceof UserTask) {
						UserTask userTask = (UserTask) targetFlowElement;
						WfTaskUser taskUser = wfTaskService.getTaskUser(task.getProcessDefinitionId(), task.getProcessInstanceId(), userTask.getId(), currentUser);
						List<WfUser> userList = taskUser.getUserList();
						if (userList.size() == 1 && currentUser.equals(String.valueOf(userList.get(0).getId()))) {
                            for (Task t : list) {
                                // 添加撤回重新提交标记
                                taskService.setVariable(t.getId(), WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_RECALL);
                                // 增加评论
                                taskService.addComment(t.getId(), task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_RECALL, "撤回重新提交");
                            }
							this.dispatchTaskTo(task.getProcessInstanceId(), userTask.getId());
							return R.success("撤回成功");
						}
					}
				}
			}
		}
		EndEvent endEvent = WfModelUtil.getEndEvent(model);
		if (endEvent == null) {
			return R.fail("流程缺少结束节点");
		}
        for (Task t : list) {
            // 添加撤回终止标记
            taskService.setVariable(t.getId(), WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE, WfProcessConstant.STATUS_WITHDRAW);
            // 增加评论
            taskService.addComment(t.getId(), task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_WITHDRAW, "撤销流程");
        }
		this.dispatchTaskTo(task.getProcessInstanceId(), endEvent.getId());
		return R.success("撤销成功");
	}

	@Override
	public Boolean isReturnable(String processInsId, String currentUser) {
		ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInsId).singleResult();
		if (processInstance == null) {
			return false;
		}
		if (!currentUser.equals(processInstance.getStartUserId())) { // 流程发起人才可撤回
			return false;
		}
		HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery()
			.processInstanceId(processInsId)
			.taskWithoutDeleteReason()
			.finished();

		// ======start======只允许撤回一次，若需无限撤回可注释此段代码======
		long count = query.count();
		if (count > 1) {
			return false;
		}
		// ======end======

		List<HistoricTaskInstance> list = query.list();
		for (HistoricTaskInstance t : list) { // 判断已完成的任务是否都是当前登录人处理的，若是则可撤回
			if (StringUtil.isNotBlank(t.getAssignee())) {
				if (!currentUser.equals(t.getAssignee())) {
					return false;
				}
			} else {
				WfTaskUser taskUser = wfTaskService.getTaskUser(t.getProcessDefinitionId(), t.getProcessInstanceId(), t.getTaskDefinitionKey(), currentUser);
				List<WfUser> userList = taskUser.getUserList();
				if (userList.size() != 1) { // 无处理人/不是唯一处理人
					return false;
				}
				if (!currentUser.equals(String.valueOf(userList.get(0).getId()))) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void dispatchTaskTo(String processInsId, String nodeId) {
		List<Execution> executions = runtimeService.createExecutionQuery().parentId(processInsId).list();
		List<String> executionIds = new ArrayList<>();
		executions.forEach(execution -> executionIds.add(execution.getId()));
		runtimeService.createChangeActivityStateBuilder().moveExecutionsToSingleActivityId(executionIds, nodeId).changeState();
	}

	@Override
	public void setProcessStatus(WfProcess process, HistoricProcessInstance processInstance) {
		if (processInstance.getEndTime() == null) {
			process.setProcessIsFinished(WfProcessConstant.STATUS_UNFINISHED);
		} else {
			process.setProcessIsFinished(WfProcessConstant.STATUS_FINISHED);
		}
		if (processInstance.getDeleteReason() == null) { // 未逻辑删除
			HistoricVariableInstanceQuery variableInstanceQuery = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId());
			HistoricVariableInstance terminate = variableInstanceQuery.variableName(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE).singleResult();
			if (ObjectUtil.isNotEmpty(terminate) && ObjectUtil.isNotEmpty(terminate.getValue())) {
				String processIsFinished = terminate.getValue().toString();
				if ("true".equals(processIsFinished)) processIsFinished = WfProcessConstant.STATUS_TERMINATE;
				process.setProcessIsFinished(processIsFinished);
			}
		} else {
			process.setProcessIsFinished(WfProcessConstant.STATUS_DELETED);
		}
	}

	@Override
	public boolean hasProcessAccess(String processInsId, String userId) {
		// 管理员权限
		if (AuthUtil.isAdmin() || AuthUtil.isAdministrator()) {
			return true;
		}
		// 流程参与者
		long processInstance = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(processInsId)
			.involvedUser(userId)
			.count();
		// 抄送
		long copy = wfCopyService.count(new LambdaQueryWrapper<WfCopy>()
			.eq(WfCopy::getProcessId, processInsId)
			.eq(WfCopy::getUserId, NumberUtils.toLong(userId)));
		return processInstance != 0 || copy != 0;
	}

	/**
	 * 审核通过
	 */
	private void passTask(WfProcess process, Task task) {
		String taskId = task.getId();
		String processInstanceId = process.getProcessInstanceId();
		String processDefinitionId = process.getProcessDefinitionId();
		String assignee = process.getAssignee();
		String comment = process.getComment();

		// 创建变量
		Map<String, Object> variables = process.getVariables();
		if (variables == null) {
			variables = new HashMap<>();
		}
		variables.put(WfProcessConstant.PASS_KEY, true);
		variables.put(WfProcessConstant.TASK_VARIABLE_ASSIGNEE, assignee);

		if (StringUtil.isNoneBlank(processInstanceId, comment)) { // 增加评论
			taskService.addComment(taskId, processInstanceId, comment);
		}
		List<AttachmentEntityImpl> attachment = process.getAttachment();
		if (ObjectUtil.isNotEmpty(attachment)) { // 增加评论附件
			String finalComment = comment;
			attachment.forEach(att -> taskService.saveAttachment(taskService.createAttachment(WfProcessConstant.COMMENT_TYPE_COMMENT, taskId, task.getProcessInstanceId(), att.getName(), finalComment, att.getUrl())));
		}

		taskService.removeVariable(taskId, WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE); // 删除撤回/驳回标记

		boolean needComplete = true;
		// 重新提交回到驳回人
		Object nodeId = runtimeService.getVariable(processInstanceId, WfExtendConstant.BACK_TO_REJECTER);
		if (ObjectUtil.isNotEmpty(nodeId) && StringUtil.isNotBlank(processDefinitionId)) {
			BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
			String backToRejecter = WfModelUtil.getUserTaskExtensionAttribute(task.getTaskDefinitionKey(), bpmnModel, WfExtendConstant.BACK_TO_REJECTER);
			if (StringUtil.isNotBlank(backToRejecter) && "true".equals(backToRejecter)) {
				taskService.setVariables(taskId, variables);

//				this.dispatchTaskTo(processInstanceId, nodeId.toString());

				runtimeService.createChangeActivityStateBuilder()
					.processInstanceId(task.getProcessInstanceId())
					.moveActivityIdTo(task.getTaskDefinitionKey(), nodeId.toString()).changeState();

				runtimeService.removeVariable(processInstanceId, WfExtendConstant.BACK_TO_REJECTER);
				needComplete = false;
			}
		}

		if (needComplete) {
			if (StringUtil.isNotBlank(task.getOwner())) { // 转办/委托设置了owner
				DelegationState delegationState = task.getDelegationState();
				if (delegationState != null) {
					switch (delegationState) {
						case PENDING: // 委托任务先处理，处理完成后会回到委派人的任务中，再执行完成
							if (StringUtil.isBlank(comment)) { // 默认委托处理意见，防止被委托人处理完后委托人无法理解
								comment = WfTaskUtil.getNickName() + "：同意";
								taskService.addComment(taskId, processInstanceId, comment);
							}
							taskService.resolveTask(taskId, variables);
//						taskService.complete(taskId, variables);
							break;
						case RESOLVED: // 已处理委托
						default: // 无委托
							taskService.complete(taskId, variables);
							break;
					}
				} else {
					taskService.complete(taskId, variables);
				}
			} else if (StringUtil.isBlank(task.getAssignee())) { // 待签任务，先签收
				this.claimTask(taskId);
				taskService.complete(taskId, variables);
			} else {
				taskService.complete(taskId, variables);
			}
		}

		// 指定下一步审批人
		int num = this.handleNextNodeAssignee(processInstanceId, assignee);
		if (num > 0) {
			throw new RuntimeException("需指定 " + num + " 个审核人");
		}

		// 保存流程表单变量
		wfFormVariableService.saveFormVariable(processInstanceId, task, null, variables, Long.valueOf(WfTaskUtil.getTaskUser()));
	}

	/**
	 * 审核不通过
	 */
	@Transactional(rollbackFor = Exception.class)
    protected void rejectTask(WfProcess process) {
		String taskKey = process.getTaskDefinitionKey();
		String processDefinitionId = process.getProcessDefinitionId();
		String rollbackNode = null;

		if (StringUtil.isNotBlank(processDefinitionId)) {
			BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);

			// 判断节点上是否配置了驳回节点，若没有配置则使用流程上配置的驳回节点，若流程也没有配置则驳回到上一节点
			List<ExtensionAttribute> attributes;
			rollbackNode = WfModelUtil.getUserTaskExtensionAttribute(taskKey, bpmnModel, WfExtendConstant.ROLLBACK_NODE);
			if (StringUtil.isBlank(rollbackNode)) {
				attributes = WfModelUtil.getProcessExtensionAttributes(bpmnModel, WfExtendConstant.ROLLBACK_NODE);
				if (attributes != null) {
					rollbackNode = attributes.get(0).getValue();
				}
			}
		}
		List<WfNode> backNodes = this.getBackNodes(process);
		if (backNodes.size() > 0) {
			String finalRollbackNode = rollbackNode;
			// 配置了默认退回节点并且可退回节点中包含配置的退回节点
			if (StringUtil.isNotBlank(rollbackNode) && backNodes.stream().filter(wfNode -> wfNode.getNodeId().equals(finalRollbackNode)).findAny().orElse(null) != null) {
				process.setNodeId(rollbackNode);
			} else {
				WfNode node = backNodes.get(backNodes.size() - 1);
				process.setNodeId(node.getNodeId());
			}
			this.rollbackTask(process);
		}
	}

	/**
	 * 构建流程列表
	 */
	private void buildProcessList(List<WfProcess> wfProcessList, HistoricProcessInstanceQuery historyQuery, Query query) {
		// 查询列表
		List<HistoricProcessInstance> historyList;
		if (query == null || query.getSize() == -1) {
			historyList = historyQuery.list();
		} else {
			historyList = historyQuery.listPage(Func.toInt((query.getCurrent() - 1) * query.getSize()), Func.toInt(query.getSize()));
		}

		historyList.forEach(historicProcessInstance -> {
			WfProcess process = new WfProcess();
			// historicProcessInstance
			process.setCreateTime(historicProcessInstance.getStartTime());
			process.setEndTime(historicProcessInstance.getEndTime());
			process.setProcessInstanceId(historicProcessInstance.getId());
			// Variables
			process.setVariables(buildTaskVariables(WfProcessConstant.STATUS_DONE, historicProcessInstance.getId(), process));
			// ProcessDefinition
			ProcessDefinition processDefinition = WfProcessCache.getProcessDefinition(historicProcessInstance.getProcessDefinitionId());
			process.setProcessDefinitionId(processDefinition.getId());
			process.setProcessDefinitionName(StringUtil.isNotBlank(historicProcessInstance.getName()) ? historicProcessInstance.getName() : processDefinition.getName());
			process.setProcessDefinitionKey(processDefinition.getKey());
			process.setCategory(processDefinition.getCategory());
			// HistoricTaskInstance
			List<HistoricTaskInstance> historyTasks = historyService.createHistoricTaskInstanceQuery()
				.processInstanceId(historicProcessInstance.getId())
				.orderByTaskCreateTime().desc()
				.orderByHistoricTaskInstanceEndTime().desc()
				.list();
			if (Func.isNotEmpty(historyTasks)) {
				HistoricTaskInstance historyTask = historyTasks.iterator().next();

				Set<String> taskNames = new LinkedHashSet<>();
				Set<String> assignees = new LinkedHashSet<>();
				historyTasks.stream().filter(historicTaskInstance -> historicTaskInstance.getEndTime() == null).forEach(task -> {
					taskNames.add(task.getName());
					if (task.getAssignee() != null) {
						WfUser assignee = WfUserCache.getUser(Long.valueOf(task.getAssignee()));
						if (assignee != null) {
							assignees.add(assignee.getName());
						}
					}
				});
                // 管理员查看表单字段权限
                if (AuthUtil.isAdmin() || AuthUtil.isAdministrator()) {
                    process.setTaskId(historyTask.getId());
                } else { // 其他人若有已办，表单字段以已办为准。没有以最新任务为准。
                    String taskUser = WfTaskUtil.getTaskUser();
                    HistoricTaskInstance taskInstance = historyTasks
                            .stream()
                            .filter(historicTaskInstance -> taskUser.equals(historicTaskInstance.getAssignee()))
                            .findFirst()
                            .orElse(null);
                    if (taskInstance != null) {
                        process.setTaskId(taskInstance.getId());
                    } else {
                        process.setTaskId(historyTask.getId());
                    }
                }

				process.setTaskName(Func.join(taskNames, " / "));
				process.setAssigneeName(Func.join(assignees, " / "));

				// ExForm
				buildExForm(historyTask.getProcessDefinitionId(), historyTask.getTaskDefinitionKey(), process);
			}
			// Status
			if (historicProcessInstance.getEndActivityId() != null) {
				process.setTaskName("结束");
				process.setAssigneeName("无");
			}
			this.setProcessStatus(process, historicProcessInstance);
			wfProcessList.add(process);
		});
	}

	/**
	 * 构建任务列表
	 */
	private void buildTaskList(List<WfProcess> wfProcessList, org.flowable.common.engine.api.query.Query<?, ?> taskQuery, Query query, String status) {
		List<?> taskList = taskQuery.listPage(Func.toInt((query.getCurrent() - 1) * query.getSize()), Func.toInt(query.getSize()));

		taskList.forEach(task -> {
			HistoricTaskInstance t1 = null;
			Task t2 = null;
			if (task instanceof HistoricTaskInstance) {
				t1 = (HistoricTaskInstance) task;
			} else {
				t2 = (Task) task;
			}
			String taskId = t1 == null ? t2.getId() : t1.getId(); // 任务id
			String taskDefKey = t1 == null ? t2.getTaskDefinitionKey() : t1.getTaskDefinitionKey(); // 任务key
			String taskName = t1 == null ? t2.getName() : t1.getName(); // 任务名称
			Date taskCreateTime = t1 == null ? t2.getCreateTime() : t1.getCreateTime(); // 任务创建时间
			String processInsId = t1 == null ? t2.getProcessInstanceId() : t1.getProcessInstanceId(); // 流程实例id
			String processDefId = t1 == null ? t2.getProcessDefinitionId() : t1.getProcessDefinitionId(); // 流程定义id

			WfProcess process = new WfProcess();
			process.setTaskId(taskId);
			process.setTaskDefinitionKey(taskDefKey);
			process.setTaskName(taskName);
			process.setCreateTime(taskCreateTime);
			process.setStatus(status);
			process.setProcessInstanceId(processInsId);
			// Variables
			process.setVariables(buildTaskVariables(status, processInsId, process));
			// ProcessDefinition
			ProcessDefinition processDefinition = WfProcessCache.getProcessDefinition(processDefId);
			process.setCategory(processDefinition.getCategory());
			process.setProcessDefinitionId(processDefinition.getId());
			process.setProcessDefinitionKey(processDefinition.getKey());
			// ProcessInstance
			HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInsId)
				.singleResult();
			process.setProcessDefinitionName(StringUtil.isNotBlank(historicProcessInstance.getName()) ? historicProcessInstance.getName() : processDefinition.getName());

			// ExForm
			buildExForm(processDefId, taskDefKey, process);

			wfProcessList.add(process);
		});
	}

	/**
	 * 构建业务变量
	 */
	private Map<String, Object> buildTaskVariables(String status, String processInsId, WfProcess process) {
		Map<String, Object> variables = new HashMap<>();
		if (WfProcessConstant.STATUS_TODO.equals(status)) { // 进行中实例
			Map<String, Object> runtimeVariables = runtimeService.getVariables(processInsId);
			if (ObjectUtil.isNotEmpty(runtimeVariables)) {
				variables.putAll(runtimeVariables);
			}
			Object applyUsername = runtimeService.getVariable(processInsId, WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME);
			// 发起人
			if (ObjectUtil.isNotEmpty(applyUsername)) {
				variables.put(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, applyUsername.toString());
				process.setStartUsername(applyUsername.toString());
			}
			// 流水号
			Object sn = runtimeService.getVariable(processInsId, WfProcessConstant.TASK_VARIABLE_SN);
			if (ObjectUtil.isNotEmpty(sn)) {
				variables.put(WfProcessConstant.TASK_VARIABLE_SN, sn.toString());
			}
		} else { // 历史实例
			HistoricVariableInstanceQuery variableInstanceQuery = historyService.createHistoricVariableInstanceQuery()
				.processInstanceId(processInsId);
			List<HistoricVariableInstance> variableInstanceList = variableInstanceQuery.list();
			if (ObjectUtil.isNotEmpty(variableInstanceList)) {
				variableInstanceList.forEach(item -> variables.put(item.getVariableName(), item.getValue()));
			}
			// 发起人
			HistoricVariableInstance applyUsername = variableInstanceQuery.variableName(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME).singleResult();
			if (ObjectUtil.isNotEmpty(applyUsername)) {
				variables.put(WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, applyUsername.getValue());
				process.setStartUsername(applyUsername.getValue().toString());
			}
			// 流水号
			HistoricVariableInstance sn = variableInstanceQuery.variableName(WfProcessConstant.TASK_VARIABLE_SN).singleResult();
			if (ObjectUtil.isNotEmpty(sn)) {
				variables.put(WfProcessConstant.TASK_VARIABLE_SN, sn.getValue());
			}
		}

		return variables;
	}

	/**
	 * 构建外置表单url
	 */
	private void buildExForm(String processDefId, String taskDefKey, WfProcess process) {
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
		String exFormKey = WfModelUtil.getUserTaskExtensionAttribute(taskDefKey, bpmnModel, WfExtendConstant.EX_FORM_KEY);
		if (StringUtil.isNotBlank(exFormKey)) {
			process.setFormKey(WfProcessConstant.EX_FORM_PREFIX + exFormKey);
		}

		String exFormUrl = WfModelUtil.getUserTaskExtensionAttribute(taskDefKey, bpmnModel, WfExtendConstant.EX_FORM_URL);
		if (StringUtil.isNotBlank(exFormUrl)) {
			process.setFormUrl(exFormUrl);
		}

		String exAppFormUrl = WfModelUtil.getUserTaskExtensionAttribute(taskDefKey, bpmnModel, WfExtendConstant.EX_APP_FORM_URL);
		if (StringUtil.isNotBlank(exAppFormUrl)) {
			process.setAppFormUrl(exAppFormUrl);
		}
	}

	/**
	 * 处理指定下一步审核人
	 *
	 * @param processInsId 流程实例id
	 * @param assignee     下一步审核人
	 */
	private int handleNextNodeAssignee(String processInsId, Object assignee) {
		List<Task> list = taskService.createTaskQuery()
			.processInstanceId(processInsId)
			.taskVariableNotExists(WfProcessConstant.TASK_VARIABLE_APPOINT)
			.list();
		if (list.size() == 0) return 0;
		if (ObjectUtil.isNotEmpty(assignee)) {
			String[] ids = assignee.toString().split(",");
			List<String> taskKeys = list.stream().map(Task::getTaskDefinitionKey).distinct().collect(Collectors.toList());
			if (taskKeys.size() > 1) { // 并行网关
				if (ids.length != taskKeys.size()) {
					return taskKeys.size();
				}
				for (int i = 0; i < taskKeys.size(); i++) {
					String taskKey = taskKeys.get(i);
					List<Task> ls = list.stream().filter(task -> task.getTaskDefinitionKey().equals(taskKey)).collect(Collectors.toList());
					if (ls.size() == 0) continue;
					if (ls.size() > 1) { // 包含多实例并行节点
						this.addOrDelMultiInstance(ls, ids, processInsId);
					} else { // 普通用户任务节点
						Task task = ls.get(0);
						taskService.setAssignee(task.getId(), ids[i]);
						taskService.setVariableLocal(task.getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
					}
				}
			} else { // 多实例并行节点
				this.addOrDelMultiInstance(list, ids, processInsId);
			}
		} else {
			for (Task task : list) {
				taskService.setVariableLocal(task.getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
			}
		}
		return 0;
	}

	/**
	 * 加签或减签
	 *
	 * @param list         taskList
	 * @param ids          选择的审核人
	 * @param processInsId 流程实例id
	 */
	private void addOrDelMultiInstance(List<Task> list, String[] ids, String processInsId) {
		if (list.size() > 1 || (list.size() == 1 && wfTaskService.isMultiInstance(list.get(0).getTaskDefinitionKey(), list.get(0).getProcessDefinitionId()))) { // 多实例
			int index = 0;
			if (list.size() == ids.length) { // 实例数量等于指定数量，循环赋值
				for (Task task : list) {
					taskService.setAssignee(task.getId(), ids[index]);
					taskService.setVariableLocal(task.getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
					index++;
				}
			} else if (list.size() > ids.length) { // 实例数量大于指定数量，需动态减签
				for (int i = 0; i < ids.length; i++) {
					taskService.setAssignee(list.get(i).getId(), ids[i]);
					taskService.setVariableLocal(list.get(i).getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
				}
				list.subList(ids.length, list.size()).forEach(task -> runtimeService.deleteMultiInstanceExecution(task.getExecutionId(), false));
			} else { // 实例数量小于指定数量，需动态加签
				for (int i = 0; i < list.size(); i++) {
					taskService.setAssignee(list.get(i).getId(), ids[i]);
					taskService.setVariableLocal(list.get(i).getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
				}
				for (int i = 0; i < ids.length - list.size(); i++) {
					Task task = list.get(0);
					runtimeService.addMultiInstanceExecution(task.getTaskDefinitionKey(), task.getProcessInstanceId(), Collections.singletonMap("assignee", ids[i + list.size()]));
				}
				list = taskService.createTaskQuery()
					.processInstanceId(processInsId)
					.taskVariableNotExists(WfProcessConstant.TASK_VARIABLE_APPOINT)
					.list();
				list.forEach(t -> taskService.setVariableLocal(t.getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1"));
			}
		} else {
			taskService.setAssignee(list.get(0).getId(), ids[0]);
			taskService.setVariableLocal(list.get(0).getId(), WfProcessConstant.TASK_VARIABLE_APPOINT, "1");
		}
	}
}
