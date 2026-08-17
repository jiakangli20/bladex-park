/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.service;

import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 通知邮件默认模板服务.
 */
@Service
public class PaymentEmailTemplateService {

	public String subject(String noticeName, PaymentNoticeVO detail) {
		return noticeName + "-" + firstNotBlank(detail.getContractNo(), detail.getPaymentNo(), String.valueOf(detail.getPaymentId()));
	}

	public String content(PaymentNoticeVO detail) {
		BigDecimal amountDue = detail.getAmountDue() == null ? BigDecimal.ZERO : detail.getAmountDue();
		BigDecimal amountPaid = detail.getAmountPaid() == null ? BigDecimal.ZERO : detail.getAmountPaid();
		String unpaidAmount = amountDue.subtract(amountPaid).max(BigDecimal.ZERO).setScale(2).toPlainString();
		return "租客名称：" + firstNotBlank(detail.getCustomerName(), "-")
			+ "\n合同号：" + firstNotBlank(detail.getContractNo(), "-")
			+ "\n未收金额：¥" + unpaidAmount;
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (!StringUtil.isBlank(value)) {
				return value;
			}
		}
		return "";
	}

}
