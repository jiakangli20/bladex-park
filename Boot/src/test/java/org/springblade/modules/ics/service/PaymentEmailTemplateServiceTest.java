package org.springblade.modules.ics.service;

import org.junit.jupiter.api.Test;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentEmailTemplateServiceTest {

	private final PaymentEmailTemplateService service = new PaymentEmailTemplateService();

	@Test
	void buildsDefaultSubjectAndContent() {
		PaymentNoticeVO detail = new PaymentNoticeVO();
		detail.setCustomerName("测试租客");
		detail.setContractNo("HT-001");
		detail.setAmountDue(new BigDecimal("1200.50"));
		detail.setAmountPaid(new BigDecimal("200.00"));

		assertEquals("收款通知-HT-001", service.subject("收款通知", detail));
		assertEquals("租客名称：测试租客\n合同号：HT-001\n未收金额：¥1000.50", service.content(detail));
	}

}
