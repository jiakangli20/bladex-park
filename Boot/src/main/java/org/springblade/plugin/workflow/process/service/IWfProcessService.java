package org.springblade.plugin.workflow.process.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.flowable.engine.history.HistoricProcessInstance;
import org.springblade.plugin.workflow.core.user.WfTokenUser;
import org.springblade.plugin.workflow.process.model.WfNode;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.core.mp.support.Query;
import org.springblade.plugin.workflow.process.model.WfTaskUser;

import org.springframework.lang.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IWfProcessService {

	/**
	 * 发起流程 - 根据流程定义id
	 *
	 * @param processDefId 流程定义id
	 * @param variables    表单参数
	 * @return processInsId
	 */
	String startProcessInstanceById(String processDefId, Map<String, Object> variables);

	/**
	 * 发起流程 - 根据流程定义key
	 *
	 * @param processDefKey 流程定义key
	 * @param variables     表单参数
	 * @return processInsId
	 */
	String startProcessInstanceByKey(String processDefKey, String businessKey, Map<String, Object> variables);

	/**
	 * 待办/待签/我的已办列表
	 */
	IPage<WfProcess> selectTaskPage(WfProcess process, Query query);

	/**
	 * 我的请求/办结列表
	 */
	IPage<WfProcess> selectProcessPage(WfProcess process, Query query);

	/**
	 * 获取流转历史列表
	 */
	Future<List<WfProcess>> historyFlowList(String processInstanceId, String startActivityId, String endActivityId, WfTokenUser tokenUser);

	/**
	 * 获取流程详情
	 */
	Future<WfProcess> detail(String taskId, String assignee, String candidateGroup);

	/**
	 * 获取模型xml - 根据模型定义id
	 */
	String getXmlByProcessDefId(String processDefId);

	/**
	 * 获取模型xml - 根据模型定义key
	 */
	String getXmlByProcessDefKey(String processDefKey);

	/**
	 * 完成任务
	 */
	Object completeTask(WfProcess process);

	/**
	 * 转办任务
	 */
	Object transferTask(WfProcess process);

	/**
	 * 委托任务
	 */
	Object delegateTask(WfProcess process);

	/**
	 * 签收任务
	 */
	Object claimTask(String taskId);

	/**
	 * 获取可退回节点
	 */
	List<WfNode> getBackNodes(WfProcess process);

	/**
	 * 退回到指定节点
	 */
	Object rollbackTask(WfProcess process);

	/**
	 * 终止流程
	 */
	Object terminateProcess(WfProcess process);

	/**
	 * 加签
	 */
	Object addMultiInstance(WfProcess process);

	/**
	 * 撤回
	 */
	Object withdrawTask(WfProcess process);

	/**
	 * 是否可撤回
	 * 默认只可撤回一次，若需无限撤回可注释相关代码块。
	 * 判断当前流程已完成的任务的处理人是否都是当前登录人，若是则可撤回
	 *
	 * @param processInsId 流程实例id
	 * @param currentUser  当前登录人
	 */
	Boolean isReturnable(String processInsId, String currentUser);

	/**
	 * 调度流程实例到指定节点
	 *
	 * @param processInsId 流程实例id
	 * @param nodeId       节点id
	 */
	void dispatchTaskTo(String processInsId, String nodeId);

	/**
	 * 获取流程瞬时状态 进行中/已结束/已撤回/已撤销/已驳回/...
	 */
	void setProcessStatus(WfProcess process, HistoricProcessInstance processInstance);

	/**
	 * 是否具有流程实例访问权限
	 *
	 * @param processInsId 流程实例id
	 * @param userId       用户id
	 * @return boolean
	 */
	boolean hasProcessAccess(String processInsId, String userId);
}
