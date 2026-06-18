package org.springblade.plugin.workflow.process.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.plugin.workflow.core.enums.WfProcessStatusEnum;
import org.springblade.plugin.workflow.design.entity.WfDeploymentForm;
import org.springblade.plugin.workflow.design.service.IWfDeploymentFormService;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.service.IWfLowcodeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static org.springblade.plugin.workflow.process.entity.WfNotice.Type.*;

//import org.springblade.plugin.lowcode.form.dto.NfBaseDto;
//import org.springblade.plugin.lowcode.form.service.INfFormService;

/**
 * 工作流对接低代码 实现类
 * <p>
 * 请在购买低码版后参照文档使用，其他情况放开以下注释无法通过编译。
 * </p>
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfLowcodeServiceImpl implements IWfLowcodeService {

	private final RuntimeService runtimeService;

	private final IWfDeploymentFormService wfDeploymentFormService;
//	private final INfFormService nfFormService;

	@Override
	public void createTable(List<WfDeploymentForm> list) {
//		if (list != null && !list.isEmpty()) {
//			nfFormService.createTable(new NfBaseDto()
//				.setTableName(list.get(0).getTableName())
//				.setTableComment(list.get(0).getTableComment())
//				.setContents(list.stream().map(WfDeploymentForm::getAppContent).toList()));
//		}
	}

	@Override
	public void updateWithNotice(WfNoticeDTO notice) {
//		WfNotice.Type type = notice.getType();
//		ProcessInstance processInstance = notice.getProcessInstance();
//		Task task = notice.getTask();
//		Map<String, Object> variables = notice.getVariables();
//
//		String processInstanceId = processInstance.getId();
//		List<WfDeploymentForm> forms = wfDeploymentFormService.list(new LambdaQueryWrapper<WfDeploymentForm>()
//			.eq(WfDeploymentForm::getDeploymentId, processInstance.getDeploymentId())
//			.isNotNull(WfDeploymentForm::getTableName));
//
//		WfDeploymentForm form;
//		if (forms == null || forms.isEmpty()) {
//			return;
//		} else if (forms.size() == 1) { // 内置表单
//			form = forms.get(0);
//		} else { // 节点独立表单
//			if (task != null) { // 找到当前task的content，若是汇总且不配置表单取第一个获取表名
//				form = forms.stream().filter(f -> f.getTaskKey().equalsIgnoreCase(task.getTaskDefinitionKey())).findFirst().orElse(forms.get(0));
//			} else { // 结束时task为null，取第一个获取表名
//				form = forms.get(0);
//			}
//		}
//		if (form == null || StringUtils.isBlank(form.getTableName())) {
//			return;
//		}
//		NfBaseDto dto = new NfBaseDto()
//			.setBusinessId(NumberUtils.toLong(processInstance.getBusinessKey()))
//			.setContent(form.getAppContent())
//			.setTableName(form.getTableName())
//			.setProcessInsId(processInstanceId);
//		// 发起流程，插入数据
//		if (START == type) {
//			long id = nfFormService.insertIntoTable(dto
//				.setVariables(variables));
//			if (id != 0) { // 业务表返回的id回填流程业务key字段
//				runtimeService.updateBusinessKey(processInstanceId, id + "");
//			}
//		}
//		// 任务正常结束，更新数据
//		else if (TASK_COMPLETE == type) {
//			nfFormService.updateTable(dto
//				.setProcessStatus(WfProcessStatusEnum.RUNNING.getCode())
//				.setVariables(variables));
//		}
//		// 流程删除，逻辑删除数据
//		else if (DELETE_PROCESS == type) {
//			nfFormService.logicDeleteTable(dto);
//		}
//		// 更新数据流程状态
//		else {
//			// 任务创建（正常创建、上节点驳回、撤回）时更新当前节点key、节点名称、状态
//			if (List.of(TASK_CREATE, REJECT, RECALL).contains(type)) {
//				assert task != null;
//				dto.setTaskKey(task.getTaskDefinitionKey())
//					.setTaskName(task.getName());
//			}
//			// 流程结束（正常结束、终结、撤销）时更新当前节点key、节点名称、状态
//			else {
//				dto.setTaskName("结束");
//				dto.setTaskKey(null);
//			}
//			nfFormService.updateTableStatus(dto
//				.setProcessStatus(type.getCode()));
//		}
	}

	@Override
	public void dropTable(List<WfDeploymentForm> list) {
//		if (list != null && !list.isEmpty()) {
//			nfFormService.dropTable(new NfBaseDto()
//				.setTableName(list.get(0).getTableName())
//				.setContents(list.stream().map(WfDeploymentForm::getAppContent).toList()), false);
//		}
	}
}
