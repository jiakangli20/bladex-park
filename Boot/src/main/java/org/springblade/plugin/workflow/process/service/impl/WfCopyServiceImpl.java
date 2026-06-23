package org.springblade.plugin.workflow.process.service.impl;

import lombok.AllArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfExtendConstant;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.WfModelUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.process.dto.WfCopyDTO;
import org.springblade.plugin.workflow.process.entity.WfCopy;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.service.IWfNoticeService;
import org.springblade.plugin.workflow.process.mapper.WfCopyMapper;
import org.springblade.plugin.workflow.process.service.IWfCopyService;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程抄送 服务实现类
 *
 * @author ssc
 */
@Service
@AllArgsConstructor
public class WfCopyServiceImpl extends BaseServiceImpl<WfCopyMapper, WfCopy> implements IWfCopyService {

	private final IWfNoticeService wfNoticeService;
	private final RepositoryService repositoryService;
	private final RuntimeService runtimeService;
	private final HistoryService historyService;

	private ExecutionEntityImpl getExecutionEntity(HistoricProcessInstance historicProcessInstance) {
		if (historicProcessInstance == null) {
			return null;
		}
		ExecutionEntityImpl executionEntity = new ExecutionEntityImpl();
		executionEntity.setId(historicProcessInstance.getId());
		executionEntity.setName(historicProcessInstance.getName());
		executionEntity.setStartUserId(historicProcessInstance.getStartUserId());
		executionEntity.setProcessDefinitionId(historicProcessInstance.getProcessDefinitionId());
		executionEntity.setProcessDefinitionKey(historicProcessInstance.getProcessDefinitionKey());
		executionEntity.setProcessDefinitionName(historicProcessInstance.getProcessDefinitionName());
		executionEntity.setDeploymentId(historicProcessInstance.getDeploymentId());
		return executionEntity;
	}

	@Override
	public void resolveCopyUser(WfCopyDTO dto) {
		Task task = dto.getTask();
		ProcessInstance processInstance = dto.getProcessInstance();
		if (processInstance == null) {
			processInstance = runtimeService.createProcessInstanceQuery()
					.processInstanceId(task.getProcessInstanceId())
					.singleResult();
			if (processInstance == null) {
				HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
					.processInstanceId(task.getProcessInstanceId())
					.singleResult();
				processInstance = getExecutionEntity(historicProcessInstance);
				if (processInstance == null) {
					System.err.println("处理抄送失败");
					return;
				}
			}
		}
        String taskId = task.getId();
		String taskDefKey = task.getTaskDefinitionKey();
        String processInstanceId = processInstance.getId();
        String processDefId = processInstance.getProcessDefinitionId();
		String deploymentId = processInstance.getDeploymentId();

        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefId);
        String exFormKey = WfModelUtil.getUserTaskExtensionAttribute(taskDefKey, bpmnModel, WfExtendConstant.EX_FORM_KEY);
        String exFormUrl = WfModelUtil.getUserTaskExtensionAttribute(taskDefKey, bpmnModel, WfExtendConstant.EX_FORM_URL);

        String copyUser = dto.getCopyUser();
        if (StringUtil.isNotBlank(copyUser)) {
            String[] ids = copyUser.split(",");
            for (String id : ids) {
                WfCopy copy = new WfCopy();
                copy.setTaskId(taskId);
                copy.setProcessId(processInstanceId);
                copy.setUserId(Long.parseLong(id));
                String processTitle = StringUtil.isBlank(processInstance.getName()) ? processInstance.getProcessDefinitionName() : processInstance.getName();
                copy.setTitle(processTitle + " - " + task.getName());
                copy.setInitiator(WfTaskUtil.getNickName());
                if (StringUtil.isNotBlank(exFormKey)) {
                    copy.setFormKey(WfProcessConstant.EX_FORM_PREFIX + exFormKey);
                }
                copy.setFormUrl(exFormUrl);
                copy.setDeploymentId(deploymentId);
                this.save(copy);

                wfNoticeService.sendNotice(new WfNotice()
                        .setFromUserId(WfTaskUtil.getTaskUser())
                        .setToUserId(id)
                        .setTask(task)
                        .setProcessInstance(processInstance)
                        .setType(WfNotice.Type.COPY));
            }
        }
	}

}
