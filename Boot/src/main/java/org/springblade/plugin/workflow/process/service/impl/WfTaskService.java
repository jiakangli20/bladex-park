package org.springblade.plugin.workflow.process.service.impl;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.jetbrains.annotations.Nullable;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfExtendConstant;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserService;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfModelUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.ops.service.IWfProxyService;
import org.springblade.plugin.workflow.process.model.WfTaskUser;
import org.springblade.plugin.workflow.process.service.IWfExpressionService;
import org.springblade.plugin.workflow.process.service.IWfTaskService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WfTaskService implements IWfTaskService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final TaskService taskService;

    private final WfUserService userSearchService;
    private final IWfExpressionService wfExpressionService;
    private final IWfProxyService wfProxyService;

    @Override
    public WfTaskUser getTaskUser(String processDefId, @Nullable String processInsId, String nodeId) {
        return getTaskUser(processDefId, processInsId, nodeId, WfTaskUtil.getTaskUser());
    }

    @Override
    public WfTaskUser getTaskUser(String processDefId, @Nullable String processInsId, String nodeId, String currentUser) {
        WfTaskUser taskUser = new WfTaskUser();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
        if (bpmnModel == null) {
            return taskUser;
        }

        List<ExtensionElement> elements;
        String nodeType;
        FlowElement flowElement = bpmnModel.getFlowElement(nodeId);
        if (flowElement instanceof UserTask) {
            nodeType = "userTask";
            elements = WfModelUtil.getUserTaskExtensionElements(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE);
        } else if (flowElement instanceof SubProcess) {
            nodeType = "subProcess";
            elements = WfModelUtil.getSubProcessExtensionElements(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE);
        } else if (flowElement instanceof CallActivity) {
            nodeType = "callActivity";
            elements = WfModelUtil.getCallActivityExtensionElements(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE);
        } else {
            return taskUser;
        }

        // 人员 - 单个 请赋值assignee，表示审核人唯一。！！赋值assignee后，候选人将失效！！
        // userList所有用户，包含组查询出来的人员和配置的人员，可用于多实例、流程图显示未到达节点的候选人。
        LinkedHashSet<WfUser> userList = new LinkedHashSet<>(); // 所有用户，包含角色、部门、岗位查询出的用户，审核人等。
        String assignee = null; // 唯一审核人

        LinkedHashSet<WfUser> roleUserList = new LinkedHashSet<>(); // 根据角色查询出的用户
        LinkedHashSet<WfUser> deptUserList = new LinkedHashSet<>(); // 根据部门查询出的用户
        LinkedHashSet<WfUser> postUserList = new LinkedHashSet<>(); // 根据岗位查询出的用户
        LinkedHashSet<WfUser> customUserList = new LinkedHashSet<>(); // 根据自定义查询出的用户

        if (elements == null || elements.isEmpty()) {
            return taskUser;
        }
        for (ExtensionElement element : elements) {
            String type = element.getAttributes().get("type").get(0).getValue();
            String value = element.getAttributes().get("value").get(0).getValue();

            switch (type) {
                case "role": // 角色
                    roleUserList.addAll(userSearchService.listByRole(Func.toLongList(value)));
                    break;
                case "dept": // 部门
                    deptUserList.addAll(userSearchService.listByDept(Func.toLongList(value)));
                    break;
                case "post": // 岗位
                    postUserList.addAll(userSearchService.listByPost(Func.toLongList(value)));
                    break;
                case "user": // 用户
                    userList.addAll(userSearchService.listByUser(Func.toLongList(value)));
                    break;
                case "custom": // 自定义
                    if (StringUtil.isBlank(processInsId)) {
                        break;
                    }

                    switch (value) {
                        case "currentUser": // 当前操作人
                            customUserList.addAll(userSearchService.listByUser(Func.toLongList(WfTaskUtil.getTaskUser())));
                            assignee = WfTaskUtil.getTaskUser();
                            break;
                        case "${assignee}":
                            assignee = "${assignee}";
                            break;
                        case "leader": // 示例，请自行修改
                            customUserList.addAll(userSearchService.listByUser(Func.toLongList("1123598821738675201")));
                            assignee = "1123598821738675201";
                            break;
                        default:
                            List<WfUser> users = new ArrayList<>();
                            if ((!value.startsWith("${") && !value.startsWith("#{") && !value.endsWith("}"))) {
                                value = String.format("${%s}", value);
                            }
                            try {
                                // flowable表达式解析
                                Object defaultVal = wfExpressionService.getValue(processInsId, value);
                                if (defaultVal != null) {
                                    // id1,id2,id3,...
                                    if (defaultVal instanceof String) {
                                        users = userSearchService.listByUser(Func.toLongList(defaultVal.toString()));
                                    }
                                    // [User1, User2, User3, ...]
                                    else if (defaultVal instanceof List && !((List<?>) defaultVal).isEmpty() && ((List<?>) defaultVal).get(0) instanceof WfUser) {
                                        users = JSON.parseArray(JSON.toJSONString(defaultVal), WfUser.class);
                                    }
                                    // String [id1, id2, id3, ...]
                                    else if (defaultVal instanceof List && !((List<?>) defaultVal).isEmpty() && ((List<?>) defaultVal).get(0) instanceof String) {
                                        List<String> ids = JSON.parseArray(JSON.toJSONString(defaultVal), String.class);
                                        users = userSearchService.listByUser(Func.toLongList(Func.join(ids)));
                                    }
                                    // Long [id1, id2, id3, ...]
                                    else if (defaultVal instanceof List && !((List<?>) defaultVal).isEmpty() && ((List<?>) defaultVal).get(0) instanceof Long) {
                                        List<Long> ids = JSON.parseArray(JSON.toJSONString(defaultVal), Long.class);
                                        users = userSearchService.listByUser(ids);
                                    }
                                    // 人员选择v2，rowKey为id的情况
                                    else if (defaultVal instanceof LinkedHashMap<?, ?> map) {
										if (map.containsKey("id")) {
                                        	users = userSearchService.listByUser(Func.toLongList(map.get("id").toString()));
										}
                                    }
									// 人员选择v3
									else if (defaultVal instanceof List && !((List<?>) defaultVal).isEmpty() && ((List<?>) defaultVal).get(0) instanceof LinkedHashMap<?, ?> map) {
										if (map.containsKey("id")) {
											users = JSON.parseArray(JSON.toJSONString(defaultVal), WfUser.class);
										}
									}
                                }
                            } catch (Exception ignore) {
                            }
                            if (ObjectUtil.isNotEmpty(users) && !users.isEmpty()) {
                                customUserList.addAll(users);
                            }
                            break;
                    }
                    break;
            }
        }
        // 人员交集/并集
        String assigneeRetain = "";
        if ("userTask".equals(nodeType)) {
            assigneeRetain = WfModelUtil.getUserTaskExtensionAttribute(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE_RETAIN);
        } else if ("subProcess".equals(nodeType)) {
            List<ExtensionElement> extensionElements = WfModelUtil.getSubProcessExtensionElements(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE_RETAIN);
            if (extensionElements != null && !extensionElements.isEmpty()) {
                assigneeRetain = extensionElements.get(0).getAttributes().get("value").get(0).getValue();
            }
        } else if ("callActivity".equals(nodeType)) {
            List<ExtensionElement> extensionElements = WfModelUtil.getCallActivityExtensionElements(nodeId, bpmnModel, WfExtendConstant.ASSIGNEE_RETAIN);
            if (extensionElements != null && !extensionElements.isEmpty()) {
                assigneeRetain = extensionElements.get(0).getAttributes().get("value").get(0).getValue();
            }
        }
        if (StringUtil.isNotBlank(assigneeRetain) && "true".equals(assigneeRetain)) { // 交集
            userList = Arrays.asList(roleUserList, deptUserList, postUserList, customUserList, userList)
                    .parallelStream()
                    .filter(elementList -> elementList != null && !elementList.isEmpty())
                    .reduce((a, b) -> {
                        a.retainAll(b);
                        return a;
                    }).orElse(new LinkedHashSet<>());
        } else { // 并集
            userList.addAll(roleUserList);
            userList.addAll(deptUserList);
            userList.addAll(postUserList);
            userList.addAll(customUserList);
        }

        // 替换代理人员
        userList = wfProxyService.getProxyUsers(userList, processInsId);
        assignee = wfProxyService.getProxyUser(assignee, processInsId);

        taskUser.setUserList(new ArrayList<>(userList));
        taskUser.setAssignee(assignee);
        taskUser.setCandidateUserIds(userList.stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toCollection(LinkedHashSet::new)));

        return taskUser;
    }

    @Override
    public Boolean isMultiInstance(String taskKey, String processDefId) {
        boolean isMultiInstance = false;

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
        Process process = bpmnModel.getMainProcess();
        FlowElement flowElement = process.getFlowElement(taskKey, true);
        if (flowElement instanceof UserTask userTask) {
            if (userTask.getBehavior() instanceof ParallelMultiInstanceBehavior behavior) {
                if (behavior.getCollectionExpression() != null) {
                    isMultiInstance = true;
                }
            }
        }
        return isMultiInstance;
    }

    @Override
    public Boolean checkIsSkip(Task task, String appointUser) {
        if ("发起人".equals(task.getName())) {
            return true;
        }

        String assignee = StringUtils.isEmpty(appointUser) ? task.getAssignee() : appointUser;
        if (StringUtils.isBlank(assignee)) {
            WfTaskUser taskUser = this.getTaskUser(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getTaskDefinitionKey(), WfTaskUtil.getTaskUser());
            if (taskUser != null) {
                assignee = taskUser.getAssignee();
                List<WfUser> userList = taskUser.getUserList();
                if (StringUtils.isBlank(assignee) && userList.size() == 1) {
                    assignee = userList.get(0).getId() + "";
                }
            }
        }
        if (StringUtils.isBlank(assignee)) { // 当前节点没有唯一审核人，无法跳过。
            return false;
        }

        boolean skip = false;
        String comment = "";
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());

        // 当前任务人为发起人时跳过
        String skipWhenStartUser = WfModelUtil.getProcessExtensionAttribute(bpmnModel, WfExtendConstant.SKIP_WHEN_START_USER);
        if (StringUtil.isNotBlank(skipWhenStartUser) && "true".equals(skipWhenStartUser)) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            String startUserId = processInstance.getStartUserId();
            if (startUserId.equals(assignee)) {
                skip = true;
                comment = "任务审核人为发起人";
            }
        }
        // 当前任务人之前处理过任务时跳过
        String skipWhenAssignee = WfModelUtil.getProcessExtensionAttribute(bpmnModel, WfExtendConstant.SKIP_WHEN_ASSIGNEE);
        if (StringUtil.isNotBlank(skipWhenAssignee) && "true".equals(skipWhenAssignee)) {
            // 事务未完成，最后完成的任务信息在runtime
            List<Task> list = taskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .list();
            if (list != null && !list.isEmpty()) {
                for (Task t : list) {
                    if (StringUtils.isNotBlank(t.getAssignee()) && t.getAssignee().equals(assignee)) {
                        skip = true;
                        comment = "任务审核人已处理过其他任务";
                        break;
                    }
                }
            }
            if (!skip) { // 上个节点之前的任务
                List<HistoricTaskInstance> historicTaskList = historyService.createHistoricTaskInstanceQuery()
                        .processInstanceId(task.getProcessInstanceId())
                        .finished()
						.taskWithoutDeleteReason()
						.taskAssigned()
                        .list();
                if (historicTaskList != null && !historicTaskList.isEmpty()) {
                    for (HistoricTaskInstance t : historicTaskList) {
                        if (StringUtils.isNotBlank(t.getAssignee()) && t.getAssignee().equals(assignee)) {
                            skip = true;
                            comment = "任务审核人已处理过其他任务";
                            break;
                        }
                    }
                }
            }
        }
        if (skip) {
            // 添加评论
            taskService.addComment(task.getId(), task.getProcessInstanceId(), WfProcessConstant.COMMENT_TYPE_SKIP_COMMENT, comment);
            // 完成任务, 添加跳过标记
            taskService.complete(task.getId(), Collections.singletonMap(WfProcessConstant.TASK_VARIABLE_SKIP, true), true);
        }
        return skip;
    }
}
