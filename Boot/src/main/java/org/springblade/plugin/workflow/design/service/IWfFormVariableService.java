package org.springblade.plugin.workflow.design.service;

import org.flowable.bpmn.model.StartEvent;
import org.flowable.task.api.Task;
import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import org.springblade.plugin.workflow.design.vo.WfFormVariableVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;

import org.springframework.lang.Nullable;
import java.util.Map;

/**
 * 流程表单 - 历史变量 服务类
 *
 * @author ssc
 */
public interface IWfFormVariableService extends BaseService<WfFormVariable> {

	/**
	 * 自定义分页
	 */
	IPage<WfFormVariableVO> selectWfFormVariablePage(IPage<WfFormVariableVO> page, WfFormVariableVO wfFormVariable);

	/**
	 * 保存表单变量
	 *
	 * @param processInsId 流程实例id
	 * @param task         任务实例
	 * @param startEvent   开始节点
	 * @param variables    变量
	 * @param createUser   创建人
	 */
	void saveFormVariable(String processInsId, @Nullable Task task, StartEvent startEvent, Map<String, Object> variables, Long createUser);
}
