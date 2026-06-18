package org.springblade.plugin.workflow.core.listener;

import lombok.RequiredArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.delegate.event.FlowableProcessStartedEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.enums.WfProcessStatusEnum;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.service.IWfNoticeService;
import org.springblade.plugin.workflow.process.service.IWfTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * flowable全局监听
 *
 * @author ssc
 */
@Component
@RequiredArgsConstructor
public class WfGlobalListener extends AbstractFlowableEngineEventListener {

    private final IWfNoticeService wfNoticeService;
    private final IWfTaskService wfTaskService;

    /**
     * 任务创建时的监听
     */
    @Override
    protected void taskCreated(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof TaskEntity task) {
            Map<String, Object> variables = task.getVariables();

            WfNotice notice = new WfNotice();
            Object status = variables.get(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE);
            if (status != null) { // 上个节点非审核通过，驳回/撤回等
                notice.setType(WfNotice.getTypeByCode(WfProcessStatusEnum.getCodeByStatus(status.toString())));
            } else { // 上个节点审核通过
                notice.setType(WfNotice.Type.TASK_CREATE);

				Object assigner = variables.get(WfProcessConstant.TASK_VARIABLE_ASSIGNEE);
				String appointUser = null;
				if (task.getAssignee() == null && assigner != null && !assigner.toString().contains(",")) { // 指定唯一审批人
					appointUser = assigner.toString();
				}
                Boolean skip = wfTaskService.checkIsSkip(task, appointUser);
                if (skip) { // 跳过的节点不触发后续消息/业务
                    return;
                }
            }
            wfNoticeService.sendNotice(notice
                    .setTask(task)
                    .setProcessInsId(task.getProcessInstanceId())
                    .setVariables(variables));
        }
    }

    /**
     * 任务正常结束
     *
     * @param event {@link FlowableEngineEntityEvent}
     */
    @Override
    protected void taskCompleted(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof TaskEntity task) {
            // 有跳过标记的任务不触发消息/业务
            Map<String, Object> variables = task.getVariablesLocal();
            if (variables.containsKey(WfProcessConstant.TASK_VARIABLE_SKIP)) {
                return;
            }
            wfNoticeService.sendNotice(new WfNotice()
                    .setType(WfNotice.Type.TASK_COMPLETE)
                    .setTask(task)
                    .setProcessInsId(task.getProcessInstanceId())
                    .setVariables(task.getVariables()));
        }
    }

    /**
     * 流程创建
     *
     * @param event {@link FlowableProcessStartedEvent}
     */
    @Override
    protected void processStarted(FlowableProcessStartedEvent event) {
        if (event.getEntity() instanceof ExecutionEntityImpl executionEntity) {
            wfNoticeService.sendNotice(new WfNotice()
                    .setType(WfNotice.Type.START)
                    .setProcessInstance(executionEntity.getProcessInstance())
                    .setVariables(executionEntity.getVariables()));
        }
    }

    /**
     * 流程结束
     *
     * @param event {@link FlowableEngineEntityEvent}
     */
    @Override
    protected void processCompleted(FlowableEngineEntityEvent event) {
        if (event.getEntity() instanceof ProcessInstance processInstance) {
            Map<String, Object> variables = processInstance.getProcessVariables();

            WfNotice notice = new WfNotice();
            Object status = variables.get(WfProcessConstant.TASK_VARIABLE_PROCESS_TERMINATE);
            if (status != null) { // 非正常结束，撤销/终结等
                notice.setType(WfNotice.getTypeByCode(WfProcessStatusEnum.getCodeByStatus(status.toString())));
            } else { // 正常结束
                notice.setType(WfNotice.Type.FINISH);
            }
            wfNoticeService.sendNotice(notice
                    .setProcessInstance(processInstance)
                    .setVariables(variables));
        }
    }

    /**
     * 流程删除，逻辑删除时触发
     *
     * @param event {@link FlowableCancelledEvent}
     */
    @Override
    protected void processCancelled(FlowableCancelledEvent event) {
        wfNoticeService.sendNotice(new WfNotice()
                .setType(WfNotice.Type.DELETE_PROCESS)
                .setProcessInsId(event.getProcessInstanceId()));
    }

}
