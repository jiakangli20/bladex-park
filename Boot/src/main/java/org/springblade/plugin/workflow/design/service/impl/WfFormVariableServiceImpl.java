package org.springblade.plugin.workflow.design.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.jetbrains.annotations.Nullable;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import org.springblade.plugin.workflow.design.mapper.WfFormVariableMapper;
import org.springblade.plugin.workflow.design.service.IWfFormVariableService;
import org.springblade.plugin.workflow.design.vo.WfFormVariableVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 流程表单 - 历史变量 服务实现类
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfFormVariableServiceImpl extends BaseServiceImpl<WfFormVariableMapper, WfFormVariable> implements IWfFormVariableService {

	private final HistoryService historyService;
	private final RuntimeService runtimeService;

	@Override
	public IPage<WfFormVariableVO> selectWfFormVariablePage(IPage<WfFormVariableVO> page, WfFormVariableVO wfFormVariable) {
		return page.setRecords(baseMapper.selectWfFormVariablePage(page, wfFormVariable));
	}

	@Async
	@Override
	public void saveFormVariable(String processInsId, @Nullable Task task, @Nullable StartEvent startEvent, Map<String, Object> variables, Long createUser) {
		if (variables == null) {
			return;
		}
		Object formOption = variables.get(WfProcessConstant.TASK_VARIABLE_FORM_OPTION);
		Object formVariable = variables.get(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE);
		if (formOption == null || formVariable == null) {
			return;
		}
		String deploymentId;
		HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(processInsId)
			.singleResult();
		if (historicProcessInstance == null) {
			ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
				.processInstanceId(processInsId)
				.singleResult();
			if (processInstance == null) {
				return;
			} else {
				deploymentId = processInstance.getDeploymentId();
			}
		} else {
			deploymentId = historicProcessInstance.getDeploymentId();
		}

		WfFormVariable variable = new WfFormVariable();
		variable.setProcessInsId(processInsId);
		variable.setDeploymentId(deploymentId);
		if (task != null) {
			variable.setTaskId(task.getId());
			variable.setTaskDefKey(task.getTaskDefinitionKey());
			variable.setTaskName(task.getName());
		} else if (startEvent != null) {
			variable.setTaskDefKey(startEvent.getId());
			variable.setTaskName(startEvent.getName());
		}
		variable.setFormOption(formOption.toString());
		variable.setFormVariable(formVariable.toString());
		variable.setCreateUser(createUser);
		this.save(variable);

		try {
			// 流程结束时移除变量忽略异常
			runtimeService.removeVariable(processInsId, WfProcessConstant.TASK_VARIABLE_FORM_OPTION);
		} catch (Exception ignore) {
		}
	}
}
