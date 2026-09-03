package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.business.pojo.entity.Customer;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiEnterpriseReportDomainHandlerTest {

	@Test
	void fallsBackToCustomerSnapshotAndEscapesHtml() {
		DeepSeekChatClient deepSeek = mock(DeepSeekChatClient.class);
		when(deepSeek.classifyEnterpriseReportRequest(anyString())).thenReturn(Optional.empty());
		when(deepSeek.complete(anyString(), anyList(), anyString())).thenReturn(Optional.empty());
		AiEnterpriseReportDomainHandler handler = new AiEnterpriseReportDomainHandler(deepSeek, new ObjectMapper());
		Customer customer = customer();

		assertTrue(handler.supports("生成这家企业的经营和风险分析报告"));
		AiEnterpriseReportDomainHandler.ReportAnalysis analysis = handler.analyze(customer, "重点分析经营风险");
		String html = new AiEnterpriseReportHtmlRenderer().render(customer, analysis, new Date());

		assertTrue(analysis.companyOverview().contains("制造业"));
		assertTrue(analysis.riskAnalysis().contains("法律风险"));
		assertTrue(html.contains("&lt;script&gt;测试企业&lt;/script&gt;"));
		assertTrue(html.contains("Content-Security-Policy"));
		assertFalse(html.contains("<script>"));
	}

	private Customer customer() {
		Customer customer = new Customer();
		customer.setCustomerId(12L);
		customer.setParkId(2001L);
		customer.setEnterpriseName("<script>测试企业</script>");
		customer.setIndustry("制造业");
		customer.setOperatingStatus("在业");
		customer.setScale("中型");
		customer.setMainBusiness("智能设备制造");
		customer.setRegisteredCapital(new BigDecimal("1200"));
		customer.setLegalRiskFlag("1");
		customer.setExecutiveRiskFlag("0");
		customer.setShareholderRiskFlag("0");
		customer.setMajorIllegalFlag("0");
		customer.setDishonestFlag("0");
		customer.setIndustryPenaltyFlag("0");
		return customer;
	}
}
