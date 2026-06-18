package org.springblade.plugin.workflow.process.service;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.runtime.ProcessInstance;

import java.util.Map;

public interface IWfExpressionService {

	/**
	 * 获取表达式值
	 *
	 * @param processInstanceId 流程实例id
	 * @param exp               表达式
	 */
	Object getValue(String processInstanceId, String exp);

	/**
	 * 根据表达式获取流程标题
	 *
	 * @param model           BpmnModel
	 * @param processInstance 流程实例
	 * @param variables       发起时的变量
	 */
	String getProcessTitle(BpmnModel model, ProcessInstance processInstance, Map<String, Object> variables);

}
