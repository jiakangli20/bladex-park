package org.springblade.plugin.workflow.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.plugin.workflow.core.utils.WfModelUtil;
import org.springblade.plugin.workflow.process.service.IWfExpressionService;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WfExpressionServiceImpl implements IWfExpressionService {

	private final ProcessEngineConfigurationImpl processEngineConfiguration;
	private final ManagementService managementService;
	private final RuntimeService runtimeService;

	@Override
	public Object getValue(String processInstanceId, String exp) {
		Expression expression = processEngineConfiguration.getExpressionManager().createExpression(exp);
		return managementService.executeCommand((commandContext) ->
			expression.getValue((ExecutionEntity) runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()));
	}

	@Override
	public String getProcessTitle(BpmnModel model, ProcessInstance processInstance, Map<String, Object> variables) {
		String processDefId = "processDefId"; // 部署id
		String processDefName = "processDefName"; // 部署名称
		String processDefKey = "processDefKey"; // 部署key
		if (variables == null) {
			variables = new HashMap<>();
		}
		variables.put(processDefId, processInstance.getProcessDefinitionId());
		variables.put(processDefName, processInstance.getProcessDefinitionName());
		variables.put(processDefKey, processInstance.getProcessDefinitionKey());

		// 默认流程标题表达式
		String defaultTitleExp = "${#processDefName}";
		String titleExp = WfModelUtil.getProcessExtensionAttribute(model, "title");
		if (titleExp == null) {
			titleExp = defaultTitleExp;
		}
		ExpressionParser parser = new SpelExpressionParser();
		StandardEvaluationContext context = new StandardEvaluationContext();
		// 注册方法，如需注册其他方法请在此处添加
		try {
			context.registerFunction("dateFormat", this.getClass().getDeclaredMethod("dateFormat", String.class));
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
		// 添加变量
		context.setVariables(variables);

		// 表达式解析
		TemplateParserContext templateParserContext = new TemplateParserContext("${", "}");
		String value;
		try {
			value = parser.parseExpression(titleExp, templateParserContext).getValue(context, String.class);
		} catch (Exception ignore) { // 表达式解析失败时使用默认表达式
			value = parser.parseExpression(defaultTitleExp, templateParserContext).getValue(context, String.class);
		}
		return value;
	}

	/**
	 * 格式化当前时间日期
	 */
	private static String dateFormat(String pattern) {
		return DateUtil.format(new Date(), pattern);
	}

}
