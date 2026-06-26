package org.springblade.plugin.workflow.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.business.service.ITenantEntryWorkflowService;
import org.springblade.modules.contract.service.IContractWorkflowService;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserCache;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.demo.service.IWfProcessLeaveService;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.model.WfTaskUser;
import org.springblade.plugin.workflow.process.service.IWfLowcodeService;
import org.springblade.plugin.workflow.process.service.IWfNoticeService;
import org.springblade.plugin.workflow.process.service.IWfTaskService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springblade.plugin.workflow.process.entity.WfNotice.Type.*;

/**
 * 流程消息 服务实现类
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfNoticeServiceImpl implements IWfNoticeService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    private final BladeRedis redis;
    private final IWfTaskService wfTaskService;
    private final IWfProcessLeaveService leaveService;
    private final ITenantEntryWorkflowService tenantEntryWorkflowService;
    private final IContractWorkflowService contractWorkflowService;
    private final IWfLowcodeService wfLowcodeService;

    // 需要发送消息的操作
    private final List<WfNotice.Type> DEAL_WITH_NOTICE = List.of(
            TASK_CREATE, REJECT, RECALL, // 任务正常创建，上个节点驳回/撤回
            COPY, TRANSFER, DELEGATE, ASSIGNEE, SUSPEND, ACTIVE, URGE, ADD_MULTI_INSTANCE, DELETE_MULTI_INSTANCE, // 抄送、转发等不会改变当前任务节点的操作
            TERMINATE, DELETE_PROCESS, FINISH // 终结、删除、正常结束
    );

    // 需要业务处理的消息，操作对应数据表
    private final List<WfNotice.Type> DEAL_WITH_BUSINESS = List.of(
            START, // 发起，插入数据
            TASK_CREATE, // 任务创建，更新当前节点key、节点名称、状态为1（进行时）
            TASK_COMPLETE, // 任务审核通过，更新流程数据
            REJECT, RECALL, // 非审核通过，更新当前节点key、节点名称、状态
            FINISH, // 流程正常结束，更新状态
            TERMINATE, WITHDRAW, // 流程非正常结束，更新状态
            DELETE_PROCESS // 流程逻辑删除，更新is_deleted字段
    );

    /**
     * 发送消息、处理业务入口。根据实际情况处理。
     */
    private void sendNotice(WfNoticeDTO notice) {
        WfNotice.Type type = notice.getType();
        ProcessInstance processInstance = notice.getProcessInstance();

        // 消息
        if (DEAL_WITH_NOTICE.contains(type)) {
            WfUser fromUser = notice.getFromUser();
            List<WfUser> toUser = notice.getToUser();
            Task task = notice.getTask();
            Map<String, Object> variables = notice.getVariables();

            System.err.println("⬇⬇⬇⬇⬇⬇⬇⬇消息预留口⬇⬇⬇⬇⬇⬇⬇⬇");
            if (fromUser != null) {
                System.err.println("发送人：" + fromUser.getName());
            }
            System.err.println("接收人：" + Func.join(toUser.stream().map(WfUser::getName).toList()));
            System.err.println("消息类型：" + type.getName());
            System.err.println("流程标题：" + (StringUtils.isBlank(processInstance.getName()) ? processInstance.getProcessDefinitionName() : processInstance.getName()));
            System.err.println("流程发起人：" + notice.getStartUser().getName());
            if (variables != null && !variables.isEmpty()) {
                System.err.println("流程变量：" + variables);
            }
            if (task != null) {
                System.err.println("当前节点：" + task.getName());
            }
            System.err.println("⬆⬆⬆⬆⬆⬆⬆⬆消息预留口⬆⬆⬆⬆⬆⬆⬆⬆");
        }
        // 业务
        if (DEAL_WITH_BUSINESS.contains(type)) {
            // 低代码方式，！！！！！购买后可根据文档指示使用。使用此方式时请注释或删掉 下面自定义方式！！！！！
            wfLowcodeService.updateWithNotice(notice);

            // 自定义方式，此处仅为示例，不保证完全正确。如需多个流程处理，可建一个统一service处理，根据key区分流程。
            String processDefinitionKey = processInstance.getProcessDefinitionKey();
            // 以请假流程为示例
            if ("leave".equalsIgnoreCase(processDefinitionKey)) {
                leaveService.businessWithNotice(notice);
            }
            if (tenantEntryWorkflowService.supports(notice)) {
                tenantEntryWorkflowService.businessWithNotice(notice);
            }
            if (contractWorkflowService.supports(notice)) {
                contractWorkflowService.businessWithNotice(notice);
            }
        }
    }

    /**
     * 消息处理，一般无需关注此处处理逻辑。
     */
    @Override
    public void sendNotice(WfNotice notice) {
        // 发送人
        String fromUserId = notice.getFromUserId();
        WfUser fromUser = StringUtils.isNotBlank(fromUserId) ? WfUserCache.getUser(NumberUtils.toLong(fromUserId)) : null;
        // 接收人
        String toUserId = notice.getToUserId();
        WfUser toUser = StringUtils.isNotBlank(toUserId) ? WfUserCache.getUser(NumberUtils.toLong(toUserId)) : null;
        // 任务
        Task task = notice.getTask();
        if (task == null && StringUtils.isNotBlank(notice.getTaskId())) {
            task = taskService.createTaskQuery().taskId(notice.getTaskId()).singleResult();
        }
        // 流程变量
        Map<String, Object> variables = notice.getVariables();
        // 流程实例
        ProcessInstance processInstance = notice.getProcessInstance();
        if (processInstance == null && StringUtils.isNotBlank(notice.getProcessInsId())) {
            ProcessInstanceQuery processInstanceQuery = runtimeService.createProcessInstanceQuery().processInstanceId(notice.getProcessInsId());
            if (variables == null || variables.isEmpty()) {
                processInstanceQuery.includeProcessVariables();
            }
            processInstance = processInstanceQuery.singleResult();

            if (processInstance != null && (variables == null || variables.isEmpty())) {
                variables = processInstance.getProcessVariables();
            }
        }
        assert processInstance != null;

        // 流程发起人
        String startUserId = processInstance.getStartUserId();
        WfUser startUser = WfUserCache.getUser(NumberUtils.toLong(startUserId));

        // 消息类型
        WfNotice.Type type = notice.getType();

        // 发送消息
        List<WfUser> toUsers = new ArrayList<>();
        if (DEAL_WITH_NOTICE.contains(type)) {
            switch (type) {
                case TASK_CREATE: // 上个任务正常通过 / 流程运维 - 指定节点回退
                case REJECT: // 上个任务驳回
                case RECALL: // 发起人撤回
                    assert task != null;

                    // 发起人节点跳过
                    if ("发起人".equals(task.getName()) && type == TASK_CREATE) {
                        return;
                    }

                    Object assigner = variables.get(WfProcessConstant.TASK_VARIABLE_ASSIGNEE);
                    if (assigner != null) { // 指定审批人
                        String[] userIds = Func.split(assigner.toString(), ",");
                        Boolean isMultiInstance = wfTaskService.isMultiInstance(task.getTaskDefinitionKey(), task.getProcessDefinitionId());
                        int index = 1;
                        if (isMultiInstance) { // 多实例并行全部生效，否则只有第一个生效
                            index = userIds.length;

                            // 防止指定时多实例并行相同节点重复触发送消息
                            String key = "wf_notice_" + processInstance.getId() + "_" + task.getTaskDefinitionKey();
                            Object value = redis.get(key);
                            if (value == null) {
                                redis.setEx(key, "1", Duration.ofSeconds(10L));
                            } else {
                                return;
                            }
                        }
                        for (int i = 0; i < index; i++) {
                            toUsers.add(WfUserCache.getUser(NumberUtils.toLong(userIds[i])));
                        }
                    } else { // 非指定
                        String assignee = task.getAssignee();
                        if (StringUtils.isNotBlank(assignee)) { // 唯一审核人
                            toUsers.add(WfUserCache.getUser(NumberUtils.toLong(assignee)));
                        } else { // 候选人，从节点配置中读取
                            WfTaskUser taskUser = wfTaskService.getTaskUser(task.getProcessDefinitionId(), task.getProcessInstanceId(), task.getTaskDefinitionKey(), WfTaskUtil.getTaskUser());
                            String userAssignee = taskUser.getAssignee();
                            List<WfUser> userList = taskUser.getUserList();
                            if (StringUtils.isNotBlank(userAssignee)) {
                                toUsers.add(WfUserCache.getUser(NumberUtils.toLong(userAssignee)));
                            } else if (userList != null && !userList.isEmpty()) {
                                toUsers.addAll(userList);
                            }
                        }
                    }

                    // 最后操作人
                    Object latestAssignee = variables.get(WfProcessConstant.TASK_VARIABLE_LATEST_TASK_ASSIGNEE);
                    if (latestAssignee != null) {
                        fromUser = WfUserCache.getUser(NumberUtils.toLong(latestAssignee.toString()));
                    }

                    break;
                case COPY: // 抄送
                case TRANSFER: // 转发
                case DELEGATE: // 代理
                case ASSIGNEE: // 流程运维 - 变更审核人
                case SUSPEND: // 流程运维 - 任务挂起
                case ACTIVE: // 流程运维 - 任务激活
                case URGE: // 催办
                case ADD_MULTI_INSTANCE: // 多实例并行 - 添加实例
                case DELETE_MULTI_INSTANCE: // 多实例并行 - 删除实例
                    toUsers.add(toUser);
                    break;
                case TERMINATE: // 流程终止
                case DELETE_PROCESS: // 删除流程
                case FINISH: // 正常结束
                    toUsers.add(startUser);
                    break;
            }
        }
        WfNoticeDTO noticeDTO = new WfNoticeDTO();
        noticeDTO.setFromUser(fromUser);
        noticeDTO.setToUser(toUsers);
        noticeDTO.setStartUser(startUser);
        noticeDTO.setType(type);
        noticeDTO.setTask(task);
        noticeDTO.setProcessInstance(processInstance);
        noticeDTO.setVariables(variables);
        sendNotice(noticeDTO);
    }

}
