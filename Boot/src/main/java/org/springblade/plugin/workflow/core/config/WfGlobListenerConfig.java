package org.springblade.plugin.workflow.core.config;

import lombok.AllArgsConstructor;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.engine.RuntimeService;
import org.springblade.plugin.workflow.core.listener.WfGlobalListener;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * flowable全局监听配置
 *
 * @author ssc
 */
@Configuration
@AllArgsConstructor
public class WfGlobListenerConfig implements ApplicationListener<ContextRefreshedEvent> {

	private final WfGlobalListener wfGlobalListener;
	private final RuntimeService runtimeService;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// 任务创建时
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.TASK_CREATED);
		// 任务到审核人时
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.TASK_ASSIGNED);
		// 任务完成时
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.TASK_COMPLETED);
		// 流程启动
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.PROCESS_STARTED);
		// 流程结束
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.PROCESS_COMPLETED);
		// 流程删除 - 逻辑删除
		runtimeService.addEventListener(wfGlobalListener, FlowableEngineEventType.PROCESS_CANCELLED);
	}
}
