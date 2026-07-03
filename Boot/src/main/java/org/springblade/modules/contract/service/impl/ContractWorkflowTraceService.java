/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.approval.service.impl.WorkflowApprovalTraceService;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 合同流程审批轨迹回流服务.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class ContractWorkflowTraceService {

	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
	private final WorkflowApprovalTraceService workflowApprovalTraceService;

	public ContractWorkflowRecord latestRecord(String businessType, Long contractId, Long paymentId) {
		return latestRecord(null, businessType, contractId, paymentId);
	}

	public ContractWorkflowRecord latestRecord(String noticeType, String businessType, Long contractId, Long paymentId) {
		if (StringUtil.isBlank(businessType) || contractId == null) {
			return null;
		}
		List<ContractWorkflowRecord> records = contractWorkflowRecordMapper.selectByContractId(contractId);
		if (records == null || records.isEmpty()) {
			return null;
		}
		return records.stream()
			.filter(record -> Objects.equals(businessType, record.getBusinessType()))
			.filter(record -> paymentId == null || Objects.equals(paymentId, record.getPaymentId()))
			.filter(record -> matchesNoticeType(noticeType, record))
			.findFirst()
			.orElse(null);
	}

	public Map<String, String> approvalFields(String businessType, Long contractId, Long paymentId) {
		return approvalFields(null, businessType, contractId, paymentId);
	}

	public Map<String, String> approvalFields(String noticeType, String businessType, Long contractId, Long paymentId) {
		ContractWorkflowRecord record = latestRecord(noticeType, businessType, contractId, paymentId);
		return approvalFields(record);
	}

	public Map<String, String> approvalFields(ContractWorkflowRecord record) {
		if (record == null || StringUtil.isBlank(record.getProcessInsId())) {
			return Collections.emptyMap();
		}
		return workflowApprovalTraceService.approvalFields(record.getProcessInsId());
	}

	private boolean matchesNoticeType(String noticeType, ContractWorkflowRecord record) {
		if (StringUtil.isBlank(noticeType)) {
			return true;
		}
		if (IContractNoticeService.NOTICE_INVOICE.equals(noticeType)) {
			return isInvoiceWorkflow(record);
		}
		if (IContractNoticeService.NOTICE_PAYMENT.equals(noticeType)) {
			return !isInvoiceWorkflow(record);
		}
		return true;
	}

	private boolean isInvoiceWorkflow(ContractWorkflowRecord record) {
		if (record == null) {
			return false;
		}
		return containsIgnoreCase(record.getProcessDefKey(), "invoice")
			|| containsIgnoreCase(record.getFormKey(), "invoice")
			|| containsIgnoreCase(record.getTemplateKey(), IContractNoticeService.NOTICE_INVOICE)
			|| containsIgnoreCase(record.getPrintFileUrl(), "/invoice-apply/")
			|| containsIgnoreCase(record.getFormDataJson(), "\"templateKey\":\"" + IContractNoticeService.NOTICE_INVOICE + "\"")
			|| containsIgnoreCase(record.getFormDataJson(), "\"formKey\":\"invoice\"");
	}

	private boolean containsIgnoreCase(String source, String search) {
		return StringUtil.isNotBlank(source)
			&& StringUtil.isNotBlank(search)
			&& source.toLowerCase().contains(search.toLowerCase());
	}

}
