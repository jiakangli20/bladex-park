package org.springblade.modules.home.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springblade.core.mp.support.Query;
import org.springblade.modules.business.service.IPolicyServiceService;
import org.springblade.modules.home.mapper.HomeMapper;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.process.service.IWfProcessService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeServiceImplTest {

	@Test
	void workflowTodoCountReadsEveryPageBeforeFilteringByPark() {
		IWfProcessService workflowService = mock(IWfProcessService.class);
		HomeServiceImpl service = new HomeServiceImpl(
			mock(HomeMapper.class),
			mock(IPaymentService.class),
			mock(IPolicyServiceService.class),
			workflowService,
			mock(IParkPermissionService.class));

		Page<WfProcess> firstPage = new Page<>(1, 500, 501);
		firstPage.setRecords(List.of(process(1L), process(9L)));
		Page<WfProcess> secondPage = new Page<>(2, 500, 501);
		secondPage.setRecords(List.of(process(1L)));
		when(workflowService.selectTaskPage(any(WfProcess.class), any(Query.class)))
			.thenReturn(firstPage, secondPage);

		Long count = ReflectionTestUtils.invokeMethod(service, "countWorkflowTodos", List.of(1L));

		assertEquals(2L, count);
		verify(workflowService, times(2)).selectTaskPage(any(WfProcess.class), any(Query.class));
	}

	private WfProcess process(Long parkId) {
		WfProcess process = new WfProcess();
		process.setVariables(Map.of("parkId", parkId));
		return process;
	}
}
