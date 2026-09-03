package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.ai.service.AiDomainHandler;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.ai.service.IAiEnterpriseReportService;
import org.springblade.modules.business.pojo.entity.Customer;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiEnterpriseDomainHandlerTest {

	private final AiAccessContext context = new AiAccessContext(100L, "tenant-a", List.of(2001L));

	@Test
	void ordinaryEnterpriseQuestionReturnsTextWithoutReportAttachment() {
		AiEnterpriseCustomerResolver resolver = mock(AiEnterpriseCustomerResolver.class);
		IAiEnterpriseReportService reportService = mock(IAiEnterpriseReportService.class);
		DeepSeekChatClient deepSeek = mock(DeepSeekChatClient.class);
		Customer customer = customer();
		when(resolver.resolve(any(), anyString(), any())).thenReturn(customer);
		when(deepSeek.complete(anyString(), anyList(), anyString())).thenReturn(Optional.of("该企业当前经营状态为在业。"));
		AiEnterpriseDomainHandler handler = new AiEnterpriseDomainHandler(resolver, reportService, deepSeek, new ObjectMapper());

		AiDomainHandler.DomainAnswer answer = handler.answer("查询测试企业的经营状态", List.of(), context);

		assertEquals("该企业当前经营状态为在业。", answer.content());
		assertNull(answer.reportId());
		verify(reportService, never()).generateForCustomer(any(), anyString(), any());
	}

	@Test
	void explicitReportRequestReturnsPersistedReportAttachment() {
		AiEnterpriseCustomerResolver resolver = mock(AiEnterpriseCustomerResolver.class);
		IAiEnterpriseReportService reportService = mock(IAiEnterpriseReportService.class);
		DeepSeekChatClient deepSeek = mock(DeepSeekChatClient.class);
		Customer customer = customer();
		AiEnterpriseReport report = new AiEnterpriseReport();
		report.setId(9001L);
		report.setTitle("测试企业有限公司企业综合信息报告");
		when(resolver.resolve(any(), anyString(), any())).thenReturn(customer);
		when(reportService.generateForCustomer(customer, "为测试企业生成一份分析报告", context)).thenReturn(report);
		AiEnterpriseDomainHandler handler = new AiEnterpriseDomainHandler(resolver, reportService, deepSeek, new ObjectMapper());

		AiDomainHandler.DomainAnswer answer = handler.answer("为测试企业生成一份分析报告", List.of(), context);

		assertEquals(9001L, answer.reportId());
		assertEquals(true, answer.content().contains("可在下方查看或下载 HTML 文件"));
		verify(reportService).generateForCustomer(customer, "为测试企业生成一份分析报告", context);
	}

	private Customer customer() {
		Customer customer = new Customer();
		customer.setCustomerId(12L);
		customer.setParkId(2001L);
		customer.setEnterpriseName("测试企业有限公司");
		customer.setOperatingStatus("在业");
		return customer;
	}
}
