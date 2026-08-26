package org.springblade.plugin.workflow.process.service.impl;

import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springblade.plugin.workflow.design.service.IWfFormVariableService;
import org.springblade.plugin.workflow.design.service.IWfSerialService;
import org.springblade.plugin.workflow.process.service.IWfCopyService;
import org.springblade.plugin.workflow.process.service.IWfDraftService;
import org.springblade.plugin.workflow.process.service.IWfExpressionService;
import org.springblade.plugin.workflow.process.service.IWfNoticeService;
import org.springblade.plugin.workflow.process.service.IWfTaskService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WfProcessServiceParkScopeTest {

	private final IParkPermissionService parkPermissionService = mock(IParkPermissionService.class);
	private final WfProcessService service = new WfProcessService(
		mock(RuntimeService.class),
		mock(IdentityService.class),
		mock(HistoryService.class),
		mock(TaskService.class),
		mock(RepositoryService.class),
		mock(IWfCopyService.class),
		mock(IWfSerialService.class),
		mock(IWfNoticeService.class),
		mock(IWfDraftService.class),
		mock(IWfExpressionService.class),
		mock(IWfTaskService.class),
		mock(IWfFormVariableService.class),
		parkPermissionService
	);

	@Test
	void taskQueryKeepsGlobalProcessesAndAuthorizedParkProcesses() {
		TaskQuery query = mock(TaskQuery.class, Answers.RETURNS_SELF);
		when(parkPermissionService.authorizedParkIds()).thenReturn(List.of(2L));

		service.applyParkScope(query);

		verify(query).or();
		verify(query).processVariableNotExists("businessType");
		verify(query).processVariableValueEquals("parkId", 2L);
		verify(query).processVariableValueEquals("parkId", "2");
		verify(query).endOr();
	}

	@Test
	void administratorDoesNotRestrictWorkflowQuery() {
		TaskQuery query = mock(TaskQuery.class, Answers.RETURNS_SELF);
		when(parkPermissionService.authorizedParkIds()).thenReturn(null);

		service.applyParkScope(query);

		verify(query, never()).or();
	}

	@Test
	void detailRequiresPermissionForStringParkId() {
		service.requireParkAccess(Map.of("parkId", "2"));

		verify(parkPermissionService).requirePark(2L);
	}

	@Test
	void detailAllowsWorkflowWithoutParkOwnership() {
		service.requireParkAccess(Collections.emptyMap());

		verify(parkPermissionService, never()).requirePark(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void detailRejectsMalformedParkId() {
		assertThrows(ServiceException.class, () -> service.requireParkAccess(Map.of("parkId", "not-a-number")));
	}
}
