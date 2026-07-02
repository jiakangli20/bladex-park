package org.springblade.plugin.workflow.core.handler;

import lombok.AllArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.process.model.WfTaskUser;
import org.springblade.plugin.workflow.process.service.IWfProcessService;
import org.springblade.plugin.workflow.process.service.IWfTaskService;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 多实例人员配置处理
 *
 * @author ssc
 */
@Component
@AllArgsConstructor
public class WfMultiInstanceHandler {

	private final IWfTaskService wfTaskService;

	public LinkedHashSet<String> getList(DelegateExecution execution) {
		LinkedHashSet<String> candidateUserIds = new LinkedHashSet<>();

		WfTaskUser taskUser = wfTaskService.getTaskUser(execution.getProcessDefinitionId(), execution.getProcessInstanceId(), execution.getCurrentActivityId(), WfTaskUtil.getTaskUser());
		List<WfUser> userList = taskUser.getUserList();
		String assignee = taskUser.getAssignee();
		LinkedHashSet<String> userIds = taskUser.getCandidateUserIds();
		if (ObjectUtil.isNotEmpty(userList)) {
			userList.stream()
				.filter(Objects::nonNull)
				.map(WfUser::getId)
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.forEach(candidateUserIds::add);
		}
		if (ObjectUtil.isNotEmpty(candidateUserIds)) {
			return candidateUserIds;
		}
		if (ObjectUtil.isNotEmpty(userIds)) {
			candidateUserIds.addAll(userIds);
		} else if (StringUtil.isNotBlank(assignee)) {
			candidateUserIds.add(assignee);
		}
		return candidateUserIds;
	}
}
