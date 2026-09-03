package org.springblade.modules.ai.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.ai.mapper.AiEnterpriseReportMapper;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiEnterpriseReportRequest;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEnterpriseReportServiceImplTest {

	@Test
	void persistsGeneratedHtmlWithCapturedTenantAndUser() {
		AiEnterpriseReportMapper mapper = mock(AiEnterpriseReportMapper.class);
		CustomerMapper customerMapper = mock(CustomerMapper.class);
		ICustomerService customerService = mock(ICustomerService.class);
		AiEnterpriseReportDomainHandler handler = mock(AiEnterpriseReportDomainHandler.class);
		AiEnterpriseReportHtmlRenderer renderer = mock(AiEnterpriseReportHtmlRenderer.class);
		AiEnterpriseCustomerResolver resolver = new AiEnterpriseCustomerResolver(customerMapper, customerService);
		Customer customer = new Customer();
		customer.setCustomerId(12L);
		customer.setParkId(2001L);
		customer.setEnterpriseName("测试企业有限公司");
		when(handler.normalizeRequest(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(handler.supports(any())).thenReturn(true);
		when(customerMapper.selectCustomerList(any(Customer.class), any())).thenReturn(List.of(customer));
		AiEnterpriseReportDomainHandler.ReportAnalysis analysis =
			new AiEnterpriseReportDomainHandler.ReportAnalysis("企业概述", "风险分析");
		when(handler.analyze(any(), any())).thenReturn(analysis);
		when(renderer.render(any(), any(), any())).thenReturn("<html>报告</html>");
		AiEnterpriseReportServiceImpl service = new AiEnterpriseReportServiceImpl(mapper, resolver, handler, renderer);
		AiEnterpriseReportRequest request = new AiEnterpriseReportRequest();
		request.setRequestContent("帮我查询测试企业的经营状态，并出一份分析报告");

		service.generate(request, new AiAccessContext(100L, "tenant-a", List.of(2001L)));

		ArgumentCaptor<AiEnterpriseReport> captor = ArgumentCaptor.forClass(AiEnterpriseReport.class);
		verify(mapper).insert(captor.capture());
		assertEquals("tenant-a", captor.getValue().getTenantId());
		assertEquals(100L, captor.getValue().getUserId());
		assertEquals(12L, captor.getValue().getCustomerId());
		assertEquals("测试企业有限公司", captor.getValue().getEnterpriseName());
		assertEquals("<html>报告</html>", captor.getValue().getHtmlContent());
		verify(customerMapper).selectCustomerList(any(Customer.class), eq(List.of(2001L)));
	}

	@Test
	void rejectsCustomerOutsideCapturedParkScopeBeforeGenerating() {
		AiEnterpriseReportMapper mapper = mock(AiEnterpriseReportMapper.class);
		CustomerMapper customerMapper = mock(CustomerMapper.class);
		ICustomerService customerService = mock(ICustomerService.class);
		AiEnterpriseReportDomainHandler handler = mock(AiEnterpriseReportDomainHandler.class);
		AiEnterpriseReportHtmlRenderer renderer = mock(AiEnterpriseReportHtmlRenderer.class);
		AiEnterpriseCustomerResolver resolver = new AiEnterpriseCustomerResolver(customerMapper, customerService);
		Customer customer = new Customer();
		customer.setCustomerId(12L);
		customer.setParkId(3002L);
		when(handler.normalizeRequest(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(handler.supports(any())).thenReturn(true);
		when(customerService.selectCustomerById(12L)).thenReturn(customer);
		AiEnterpriseReportServiceImpl service = new AiEnterpriseReportServiceImpl(mapper, resolver, handler, renderer);
		AiEnterpriseReportRequest request = new AiEnterpriseReportRequest();
		request.setCustomerId(12L);
		request.setRequestContent("生成企业分析报告");

		assertThrows(ServiceException.class,
			() -> service.generate(request, new AiAccessContext(100L, "000000", List.of(2001L))));

		verify(handler, never()).analyze(any(), any());
		verify(mapper, never()).insert(any(AiEnterpriseReport.class));
	}
}
