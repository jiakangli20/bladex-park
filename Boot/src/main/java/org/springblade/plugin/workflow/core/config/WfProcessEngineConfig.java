package org.springblade.plugin.workflow.core.config;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.SqlSessionFactory;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.ProcessEngineConfigurationConfigurer;
import org.springblade.plugin.workflow.core.factory.WfActivityBehaviorFactory;
import org.springblade.plugin.workflow.core.function.WfVariableDateGreaterThanExpressionFunction;
import org.springblade.plugin.workflow.core.function.WfVariableDateGreaterThanOrEqualExpressionFunction;
import org.springblade.plugin.workflow.core.function.WfVariableDateLessThanExpressionFunction;
import org.springblade.plugin.workflow.core.function.WfVariableDateLessThanOrEqualExpressionFunction;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * flowable配置
 *
 * @author ssc
 */
@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
public class WfProcessEngineConfig implements ProcessEngineConfigurationConfigurer {

	private static final String WF_PROCESS_DEFINITION_MAPPER =
		"org/springblade/plugin/workflow/process/mapper/WfProcessDefinition.xml";
	private static final String WF_PROCESS_DEFINITION_MAPPER_RESOURCE = WF_PROCESS_DEFINITION_MAPPER + "#workflow-plugin";
	private static final String WF_PROCESS_DEFINITION_NAMESPACE =
		"org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl";
	private static final String WF_PROCESS_DEFINITION_COUNT_STATEMENT =
		WF_PROCESS_DEFINITION_NAMESPACE + ".selectWfProcessDefinitionCountByQueryCriteria";
	private static final String WF_PROCESS_DEFINITION_LIST_STATEMENT =
		WF_PROCESS_DEFINITION_NAMESPACE + ".selectWfProcessDefinitionsByQueryCriteria";

	@Override
	public void configure(SpringProcessEngineConfiguration springProcessEngineConfiguration) {
		// 设置节点人员托管
		springProcessEngineConfiguration.setActivityBehaviorFactory(new WfActivityBehaviorFactory());

		// 设置自定义函数
		List<FlowableFunctionDelegate> functionDelegates = new ArrayList<>();
		if (springProcessEngineConfiguration.getCustomFlowableFunctionDelegates() != null) {
			functionDelegates.addAll(springProcessEngineConfiguration.getCustomFlowableFunctionDelegates());
		}
		functionDelegates.addAll(List.of(
			new WfVariableDateGreaterThanExpressionFunction(),
			new WfVariableDateGreaterThanOrEqualExpressionFunction(),
			new WfVariableDateLessThanExpressionFunction(),
			new WfVariableDateLessThanOrEqualExpressionFunction()
		));
		springProcessEngineConfiguration.setCustomFlowableFunctionDelegates(functionDelegates);

		// 注册流程定义扩展查询，供部署管理分页/计数查询使用
		Set<String> customMybatisXMLMappers = new LinkedHashSet<>();
		if (springProcessEngineConfiguration.getCustomMybatisXMLMappers() != null) {
			customMybatisXMLMappers.addAll(springProcessEngineConfiguration.getCustomMybatisXMLMappers());
		}
		customMybatisXMLMappers.add(WF_PROCESS_DEFINITION_MAPPER);
		springProcessEngineConfiguration.setCustomMybatisXMLMappers(customMybatisXMLMappers);
	}

	@Bean
	public ApplicationRunner wfProcessDefinitionMapperChecker(SpringProcessEngineConfiguration configuration) {
		return args -> {
			SqlSessionFactory sqlSessionFactory = configuration.getSqlSessionFactory();
			if (sqlSessionFactory == null) {
				throw new IllegalStateException("Flowable SqlSessionFactory 未初始化，无法注册工作流流程定义扩展查询");
			}

			org.apache.ibatis.session.Configuration mybatisConfiguration = sqlSessionFactory.getConfiguration();
			if (!mybatisConfiguration.hasStatement(WF_PROCESS_DEFINITION_COUNT_STATEMENT, false)
				|| !mybatisConfiguration.hasStatement(WF_PROCESS_DEFINITION_LIST_STATEMENT, false)) {
				try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(WF_PROCESS_DEFINITION_MAPPER)) {
					if (inputStream == null) {
						throw new IllegalStateException("未找到 Flowable 自定义 Mapper: " + WF_PROCESS_DEFINITION_MAPPER);
					}
					XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(
						inputStream,
						mybatisConfiguration,
						WF_PROCESS_DEFINITION_MAPPER_RESOURCE,
						mybatisConfiguration.getSqlFragments()
					);
					xmlMapperBuilder.parse();
				}
			}

			if (!mybatisConfiguration.hasStatement(WF_PROCESS_DEFINITION_COUNT_STATEMENT, false)
				|| !mybatisConfiguration.hasStatement(WF_PROCESS_DEFINITION_LIST_STATEMENT, false)) {
				throw new IllegalStateException("Flowable 工作流流程定义扩展查询注册失败: " + WF_PROCESS_DEFINITION_MAPPER);
			}
		};
	}
}
