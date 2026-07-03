/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springblade.modules.resource.builder.OssBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 合同通知文书服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class ContractNoticeServiceImpl implements IContractNoticeService {

	private static final String TEMPLATE_CONTRACT_APPROVAL = "君联大厦招商管理办法2023/附件二：合同会签审批表.docx";
	private static final String TEMPLATE_PAYMENT = "君联大厦招商管理办法2023/附件四：君联大厦付款通知单.docx";
	private static final String TEMPLATE_REMINDER = "君联大厦招商管理办法2023/附件五：催款通知书.docx";
	private static final String TEMPLATE_OVERDUE = "君联大厦招商管理办法2023/附件六：租金逾期处理通知书.docx";
	private static final String TEMPLATE_INVOICE = "君联大厦招商管理办法2023/开票申请.docx";
	private static final String TEMPLATE_PROJECT_APPROVAL = "君联大厦招商管理办法2023/附件七：项目审批表.docx";
	private static final String TEMPLATE_TERMINATION_APPROVAL = "君联大厦招商管理办法2023/附件八：退租审批表.docx";
	private static final String TEMPLATE_TERMINATION_AGREEMENT = "君联大厦招商管理办法2023/附件九：关于君联大厦租赁合同解除之补充协议.docx";
	private static final String TEMPLATE_ROOM_REVIEW = "君联大厦招商管理办法2023/附件15：房屋退租交接验收单（思锐泰）.xlsx";
	private static final String TEMPLATE_CONTRACT_FIXED = "君联合同/科技服务中心租赁合同（固定租金）202508版 - 解锁.docx";
	private static final String TEMPLATE_CONTRACT_FLOATING = "君联合同/科技服务中心租赁合同（浮动租金）202508版 - 解锁.docx";
	private static final String EXACT_REPLACEMENT_PREFIX = "__exact__:";

	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractMapper contractMapper;
	private final CustomerMapper customerMapper;
	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
	private final OssBuilder ossBuilder;
	private final IContractTemplateRenderService contractTemplateRenderService;
	private final ContractDocumentPreviewService contractDocumentPreviewService;

	@Override
	public ContractNoticeFileVO buildNotice(String noticeType, Long paymentId, Long contractId) {
		return buildNotice(noticeType, paymentId, contractId, Collections.emptyMap());
	}

	private ContractNoticeFileVO buildNotice(String noticeType, Long paymentId, Long contractId, Map<String, Object> formData) {
		NoticeContext context = resolveContext(noticeType, paymentId, contractId, formData);
		return switch (normalizeNoticeType(noticeType)) {
			case NOTICE_PAYMENT -> buildPaymentNotice(context);
			case NOTICE_REMINDER -> buildReminderNotice(context);
			case NOTICE_INVOICE -> buildInvoiceApply(context);
			case NOTICE_CONTRACT_APPROVAL -> buildContractApproval(context);
			case NOTICE_CONTRACT_FIXED -> buildContractText(context, false);
			case NOTICE_CONTRACT_FLOATING -> buildContractText(context, true);
			case NOTICE_PROJECT_APPROVAL -> buildProjectApproval(context);
			case NOTICE_OVERDUE -> buildOverdueNotice(context);
			case NOTICE_LEGAL -> buildLegalLetter(context);
			case NOTICE_MOVE_OUT -> buildMoveOutNotice(context);
			case NOTICE_TERMINATION -> buildTerminationApproval(context);
			case NOTICE_TERMINATION_AGREEMENT -> buildTerminationAgreement(context);
			case NOTICE_ROOM_REVIEW -> buildRoomReview(context);
			default -> throw new ServiceException("不支持的通知类型");
		};
	}

	@Override
	@SneakyThrows
	public ContractNoticeFileVO uploadNotice(String noticeType, Long paymentId, Long contractId) {
		return uploadDocument(buildNotice(noticeType, paymentId, contractId));
	}

	@SneakyThrows
	private ContractNoticeFileVO uploadDocument(ContractNoticeFileVO document) {
		BladeFile bladeFile = ossBuilder.template().putFile(document.getFileName(), new ByteArrayInputStream(document.getFileBytes()));
		document.setFileUrl(bladeFile.getLink());
		document.setFileBytes(null);
		return document;
	}

	@Override
	public Map<String, String> uploadContractApprovalPackage(Long contractId) {
		Map<String, String> fileMap = new LinkedHashMap<>();
		ContractNoticeFileVO approval = uploadNotice(NOTICE_CONTRACT_APPROVAL, null, contractId);
		fileMap.put(NOTICE_CONTRACT_APPROVAL, approval.getFileUrl());
		ContractNoticeFileVO fixedContract = uploadNotice(NOTICE_CONTRACT_FIXED, null, contractId);
		fileMap.put(NOTICE_CONTRACT_FIXED, fixedContract.getFileUrl());
		ContractNoticeFileVO floatingContract = uploadNotice(NOTICE_CONTRACT_FLOATING, null, contractId);
		fileMap.put(NOTICE_CONTRACT_FLOATING, floatingContract.getFileUrl());
		return fileMap;
	}

	@Override
	public Map<String, String> uploadPaymentPackage(Long paymentId) {
		Map<String, String> fileMap = new LinkedHashMap<>();
		ContractNoticeFileVO paymentNotice = uploadNotice(NOTICE_PAYMENT, paymentId, null);
		fileMap.put(NOTICE_PAYMENT, paymentNotice.getFileUrl());
		ContractNoticeFileVO invoiceApply = uploadNotice(NOTICE_INVOICE, paymentId, null);
		fileMap.put(NOTICE_INVOICE, invoiceApply.getFileUrl());
		return fileMap;
	}

	@Override
	public Map<String, String> uploadOverduePackage(Long paymentId) {
		Map<String, String> fileMap = new LinkedHashMap<>();
		ContractNoticeFileVO projectApproval = uploadNotice(NOTICE_PROJECT_APPROVAL, paymentId, null);
		fileMap.put(NOTICE_PROJECT_APPROVAL, projectApproval.getFileUrl());
		ContractNoticeFileVO paymentNotice = uploadNotice(NOTICE_PAYMENT, paymentId, null);
		fileMap.put(NOTICE_PAYMENT, paymentNotice.getFileUrl());
		ContractNoticeFileVO reminderNotice = uploadNotice(NOTICE_REMINDER, paymentId, null);
		fileMap.put(NOTICE_REMINDER, reminderNotice.getFileUrl());
		ContractNoticeFileVO overdueNotice = uploadNotice(NOTICE_OVERDUE, paymentId, null);
		fileMap.put(NOTICE_OVERDUE, overdueNotice.getFileUrl());
		ContractNoticeFileVO legalLetter = uploadNotice(NOTICE_LEGAL, paymentId, null);
		fileMap.put(NOTICE_LEGAL, legalLetter.getFileUrl());
		ContractNoticeFileVO moveOutNotice = uploadNotice(NOTICE_MOVE_OUT, paymentId, null);
		fileMap.put(NOTICE_MOVE_OUT, moveOutNotice.getFileUrl());
		return fileMap;
	}

	@Override
	public Map<String, String> uploadTerminationPackage(Long contractId) {
		return uploadTerminationPackage(contractId, null);
	}

	@Override
	public Map<String, String> uploadTerminationPackage(Long contractId, String formDataJson) {
		Map<String, Object> formData = parseFormData(formDataJson);
		Map<String, String> fileMap = new LinkedHashMap<>();
		ContractNoticeFileVO approval = uploadDocument(buildNotice(NOTICE_TERMINATION, null, contractId, formData));
		fileMap.put(NOTICE_TERMINATION, approval.getFileUrl());
		ContractNoticeFileVO agreement = uploadDocument(buildNotice(NOTICE_TERMINATION_AGREEMENT, null, contractId, formData));
		fileMap.put(NOTICE_TERMINATION_AGREEMENT, agreement.getFileUrl());
		return fileMap;
	}

	@Override
	public Kv buildMiniAppPayload(String noticeType, Long paymentId, Long contractId) {
		String normalizedNoticeType = normalizeNoticeType(noticeType);
		NoticeContext context = resolveContext(normalizedNoticeType, paymentId, contractId);
		ContractNoticeFileVO document = buildNotice(normalizedNoticeType, paymentId, contractId);
		document.setFileUrl(buildMiniAppDownloadPath(normalizedNoticeType, context));
		return Kv.create()
			.set("noticeType", normalizedNoticeType)
			.set("noticeName", document.getNoticeName())
			.set("noticeTitle", noticeDisplayName(normalizedNoticeType))
			.set("fileName", document.getFileName())
			.set("fileUrl", document.getFileUrl())
			.set("generatedAt", document.getGeneratedAt())
			.set("sendStatus", "pending")
			.set("channel", "miniapp")
			.set("businessType", resolveMiniAppBusinessType(normalizedNoticeType))
			.set("contractId", context.contract == null ? null : context.contract.getContractId())
			.set("paymentId", context.payment == null ? null : context.payment.getPaymentId())
			.set("businessKey", context.payment == null ? context.contract == null ? null : context.contract.getContractId() : context.payment.getPaymentId())
			.set("receiver", buildMiniAppReceiver(context))
			.set("document", buildMiniAppDocument(normalizedNoticeType, context, document))
			.set("payload", buildMiniAppData(normalizedNoticeType, context, document));
	}

	@Override
	public Kv buildNoticePreview(String noticeType, Long paymentId, Long contractId, String formDataJson) {
		Map<String, Object> formData = parseFormData(formDataJson);
		NoticeContext context = resolveContext(noticeType, paymentId, contractId, formData);
		PreviewData previewData = buildPreviewData(normalizeNoticeType(noticeType), context);
		ContractNoticeFileVO document = buildNotice(noticeType, paymentId, contractId, formData);
		return Kv.create()
			.set("noticeType", normalizeNoticeType(noticeType))
			.set("noticeName", document.getNoticeName())
			.set("fileName", document.getFileName())
			.set("contentType", document.getContentType())
			.set("generatedAt", document.getGeneratedAt())
			.set("summary", previewData.summary)
			.set("fields", previewData.fields)
			.set("missingFields", previewData.missingFields)
			.set("previewMode", "document")
			.set("html", contractDocumentPreviewService.render(document, previewData.summary, previewData.missingFields));
	}

	private Map<String, Object> buildMiniAppData(String noticeType, NoticeContext context, ContractNoticeFileVO document) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("noticeType", noticeType);
		payload.put("noticeTitle", noticeDisplayName(noticeType));
		payload.put("businessScene", resolveMiniAppBusinessType(noticeType));
		payload.put("contractId", context.contract == null ? null : context.contract.getContractId());
		payload.put("paymentId", context.payment == null ? null : context.payment.getPaymentId());
		payload.put("customerId", context.customer == null ? null : context.customer.getCustomerId());
		payload.put("parkId", context.contract == null ? null : context.contract.getParkId());
		payload.put("parkName", context.parkName());
		payload.put("contractNo", context.contractNo());
		payload.put("contractName", context.contract == null ? null : context.contract.getContractName());
		payload.put("contractStatus", context.contractStatusName());
		payload.put("customerName", context.customerName());
		payload.put("roomName", context.roomName());
		payload.put("roomDisplay", context.roomDisplay());
		payload.put("buildingName", context.buildingName());
		payload.put("feeName", context.feeName());
		payload.put("periodText", context.periodText());
		payload.put("periodStart", formatDate(context.payment == null ? null : context.payment.getPeriodStart()));
		payload.put("periodEnd", formatDate(context.payment == null ? null : context.payment.getPeriodEnd()));
		payload.put("payDeadline", formatDate(context.payment == null ? null : context.payment.getPayDeadline()));
		payload.put("amountDue", formatMoney(context.payment == null ? null : context.payment.getAmountDue()));
		payload.put("amountPaid", formatMoney(context.payment == null ? null : context.payment.getAmountPaid()));
		payload.put("unpaidAmount", formatMoney(context.unpaidAmount()));
		payload.put("overdueDays", context.overdueDays());
		payload.put("contactName", context.customer == null ? null : context.customer.getContactName());
		payload.put("contactPhone", customerContactPhone(context));
		payload.put("contactEmail", context.customer == null ? null : context.customer.getContactEmail());
		payload.put("receiverOpenId", context.customer == null ? null : context.customer.getWxOpenid());
		payload.put("receiverUnionId", context.customer == null ? null : context.customer.getWxUnionid());
		payload.put("fileName", document.getFileName());
		payload.put("fileUrl", document.getFileUrl());
		payload.put("downloadPath", buildMiniAppDownloadPath(noticeType, context));
		payload.put("previewPath", buildMiniAppPreviewPath(noticeType, context));
		payload.put("pagePath", buildMiniAppPagePath(noticeType, context));
		payload.put("templateSource", resolveTemplateSource(noticeType));
		payload.put("sendMode", "reserved");
		return payload;
	}

	private Map<String, Object> buildMiniAppReceiver(NoticeContext context) {
		Map<String, Object> receiver = new LinkedHashMap<>();
		receiver.put("customerId", context.customer == null ? null : context.customer.getCustomerId());
		receiver.put("customerName", context.customerName());
		receiver.put("contactName", context.customer == null ? null : context.customer.getContactName());
		receiver.put("contactPhone", customerContactPhone(context));
		receiver.put("contactEmail", context.customer == null ? null : context.customer.getContactEmail());
		receiver.put("wxOpenid", context.customer == null ? null : context.customer.getWxOpenid());
		receiver.put("wxUnionid", context.customer == null ? null : context.customer.getWxUnionid());
		return receiver;
	}

	private Map<String, Object> buildMiniAppDocument(String noticeType, NoticeContext context, ContractNoticeFileVO document) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("noticeType", noticeType);
		data.put("noticeTitle", noticeDisplayName(noticeType));
		data.put("fileName", document.getFileName());
		data.put("fileUrl", document.getFileUrl());
		data.put("contentType", document.getContentType());
		data.put("generatedAt", document.getGeneratedAt());
		data.put("downloadPath", buildMiniAppDownloadPath(noticeType, context));
		data.put("previewPath", buildMiniAppPreviewPath(noticeType, context));
		data.put("templateSource", resolveTemplateSource(noticeType));
		return data;
	}

	private String resolveMiniAppBusinessType(String noticeType) {
		return switch (normalizeNoticeType(noticeType)) {
			case NOTICE_PAYMENT, NOTICE_INVOICE -> "contract_payment";
			case NOTICE_REMINDER, NOTICE_OVERDUE -> "contract_overdue_notice";
			case NOTICE_LEGAL, NOTICE_PROJECT_APPROVAL -> "contract_overdue_legal";
			case NOTICE_MOVE_OUT -> "contract_move_out";
			case NOTICE_TERMINATION, NOTICE_TERMINATION_AGREEMENT, NOTICE_ROOM_REVIEW -> "contract_termination";
			case NOTICE_CONTRACT_APPROVAL, NOTICE_CONTRACT_FIXED, NOTICE_CONTRACT_FLOATING -> "contract_approval";
			default -> "contract";
		};
	}

	private String buildMiniAppDownloadPath(String noticeType, NoticeContext context) {
		String normalized = normalizeNoticeType(noticeType);
		Long paymentId = context.payment == null ? null : context.payment.getPaymentId();
		Long contractId = context.contract == null ? null : context.contract.getContractId();
		return switch (normalized) {
			case NOTICE_PAYMENT -> paymentId == null ? null : "/blade-contract/print/payment-notice/" + paymentId;
			case NOTICE_REMINDER -> paymentId == null ? null : "/blade-contract/print/reminder-notice/" + paymentId;
			case NOTICE_INVOICE -> paymentId == null ? null : "/blade-contract/print/invoice-apply/" + paymentId;
			case NOTICE_CONTRACT_APPROVAL -> contractId == null ? null : "/blade-contract/print/contract-approval/" + contractId;
			case NOTICE_CONTRACT_FIXED -> contractId == null ? null : "/blade-contract/print/contract-text/fixed/" + contractId;
			case NOTICE_CONTRACT_FLOATING -> contractId == null ? null : "/blade-contract/print/contract-text/floating/" + contractId;
			case NOTICE_PROJECT_APPROVAL -> paymentId == null ? null : "/blade-contract/print/project-approval/" + paymentId;
			case NOTICE_OVERDUE -> paymentId == null ? null : "/blade-contract/print/overdue-notice/" + paymentId;
			case NOTICE_LEGAL -> paymentId == null ? null : "/blade-contract/print/legal-letter/" + paymentId;
			case NOTICE_MOVE_OUT -> paymentId != null
				? "/blade-contract/print/move-out-notice/payment/" + paymentId
				: contractId == null ? null : "/blade-contract/print/move-out-notice/" + contractId;
			case NOTICE_TERMINATION -> contractId == null ? null : "/blade-contract/print/termination-approval/" + contractId;
			case NOTICE_TERMINATION_AGREEMENT -> contractId == null ? null : "/blade-contract/print/termination-agreement/" + contractId;
			case NOTICE_ROOM_REVIEW -> contractId == null ? null : "/blade-contract/print/room-review/" + contractId;
			default -> null;
		};
	}

	private String buildMiniAppPreviewPath(String noticeType, NoticeContext context) {
		StringBuilder builder = new StringBuilder("/blade-contract/print/preview?noticeType=").append(normalizeNoticeType(noticeType));
		if (context.payment != null && context.payment.getPaymentId() != null) {
			builder.append("&paymentId=").append(context.payment.getPaymentId());
		}
		if (context.contract != null && context.contract.getContractId() != null) {
			builder.append("&contractId=").append(context.contract.getContractId());
		}
		return builder.toString();
	}

	private String buildMiniAppPagePath(String noticeType, NoticeContext context) {
		StringBuilder builder = new StringBuilder("/pages/contract/notice/detail?noticeType=").append(normalizeNoticeType(noticeType));
		if (context.payment != null && context.payment.getPaymentId() != null) {
			builder.append("&paymentId=").append(context.payment.getPaymentId());
		}
		if (context.contract != null && context.contract.getContractId() != null) {
			builder.append("&contractId=").append(context.contract.getContractId());
		}
		return builder.toString();
	}

	private String resolveTemplateSource(String noticeType) {
		return switch (normalizeNoticeType(noticeType)) {
			case NOTICE_PAYMENT -> TEMPLATE_PAYMENT;
			case NOTICE_REMINDER -> TEMPLATE_REMINDER;
			case NOTICE_INVOICE -> TEMPLATE_INVOICE;
			case NOTICE_CONTRACT_APPROVAL -> TEMPLATE_CONTRACT_APPROVAL;
			case NOTICE_CONTRACT_FIXED -> TEMPLATE_CONTRACT_FIXED;
			case NOTICE_CONTRACT_FLOATING -> TEMPLATE_CONTRACT_FLOATING;
			case NOTICE_PROJECT_APPROVAL -> TEMPLATE_PROJECT_APPROVAL;
			case NOTICE_OVERDUE, NOTICE_LEGAL, NOTICE_MOVE_OUT -> TEMPLATE_OVERDUE;
			case NOTICE_TERMINATION -> TEMPLATE_TERMINATION_APPROVAL;
			case NOTICE_TERMINATION_AGREEMENT -> TEMPLATE_TERMINATION_AGREEMENT;
			case NOTICE_ROOM_REVIEW -> TEMPLATE_ROOM_REVIEW;
			default -> "";
		};
	}

	private ContractNoticeFileVO buildPaymentNotice(NoticeContext context) {
		Map<String, String> fields = createPaymentNoticeFields(context);
		Map<String, String> replacements = createPaymentNoticeReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_PAYMENT,
			"付款通知单",
			TEMPLATE_PAYMENT,
			"付款通知单-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildReminderNotice(NoticeContext context) {
		Map<String, String> fields = createOverdueFields(context);
		Map<String, String> replacements = createReminderNoticeReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_REMINDER,
			"催款通知书",
			TEMPLATE_REMINDER,
			"催款通知书-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildOverdueNotice(NoticeContext context) {
		Map<String, String> fields = createOverdueFields(context);
		Map<String, String> replacements = createOverdueNoticeReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_OVERDUE,
			"租金逾期处理通知书",
			TEMPLATE_OVERDUE,
			"租金逾期处理通知书-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildLegalLetter(NoticeContext context) {
		Map<String, String> fields = createOverdueFields(context);
		Map<String, String> replacements = createLegalLetterReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_LEGAL,
			"律师函",
			TEMPLATE_OVERDUE,
			"律师函-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildMoveOutNotice(NoticeContext context) {
		Map<String, String> fields = createOverdueFields(context);
		Map<String, String> replacements = createMoveOutNoticeReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_MOVE_OUT,
			"限期搬离通知书",
			TEMPLATE_OVERDUE,
			"限期搬离通知书-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildTerminationApproval(NoticeContext context) {
		Map<String, String> fields = createTerminationApprovalFields(context);
		Map<String, String> replacements = createTerminationReplacements(context);
		return contractTemplateRenderService.render(
			NOTICE_TERMINATION,
			"退租审批表",
			TEMPLATE_TERMINATION_APPROVAL,
			"退租审批表-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildRoomReview(NoticeContext context) {
		Map<String, String> fields = createRoomReviewFields(context);
		Map<String, String> replacements = createRoomReviewReplacements(context);
		return contractTemplateRenderService.render(
			NOTICE_ROOM_REVIEW,
			"房屋退租交接验收单",
			TEMPLATE_ROOM_REVIEW,
			"房屋退租交接验收单-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildTerminationAgreement(NoticeContext context) {
		Map<String, String> fields = createTerminationApprovalFields(context);
		Map<String, String> replacements = createTerminationAgreementReplacements(context);
		return contractTemplateRenderService.render(
			NOTICE_TERMINATION_AGREEMENT,
			"合同解除补充协议",
			TEMPLATE_TERMINATION_AGREEMENT,
			"合同解除补充协议-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildContractApproval(NoticeContext context) {
		Map<String, String> fields = createContractApprovalFields(context);
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("{{contractNo}}", context.contractNo());
		replacements.put("{{customerName}}", context.customerName());
		replacements.put("{{roomName}}", context.roomDisplay());
		replacements.put("{{rentArea}}", formatArea(context.contract == null ? null : context.contract.getRentArea()));
		replacements.put("{{monthlyRent}}", formatMoney(context.contract == null ? null : context.contract.getMonthlyRent()));
		return contractTemplateRenderService.render(
			NOTICE_CONTRACT_APPROVAL,
			"合同会签审批表",
			TEMPLATE_CONTRACT_APPROVAL,
			"合同会签审批表-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildInvoiceApply(NoticeContext context) {
		Map<String, String> fields = createInvoiceFields(context);
		String applyDate = DateUtil.format(new Date(), "yyyy年 M月 d日");
		String applicant = Func.toStr(context.contract == null ? null : context.contract.getCreateBy(), "经办人");
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("合同号：                          申请人：赵琪", "合同号：" + context.contractNo() + "                          申请人：" + applicant);
		replacements.put("2025年 6月 17 日", applyDate);
		replacements.put("申请人：赵琪", "申请人：" + applicant);
		replacements.put("江苏弘业国际技术工程有限公司", context.customerName());
		replacements.put("91320000051830610F", fields.get("税号"));
		replacements.put("电话：52278888", "电话：" + fields.get("联系电话"));
		replacements.put("地址：中华路50号弘业大厦", "地址：" + fields.get("单位地址"));
			replacements.put("银行账号：527460884999", "银行账号：" + fields.get("银行账号"));
			replacements.put("开户行：中国银行中华路支行", "开户行：" + fields.get("开户行"));
			replacements.put("2025.3.25-2025.6.30", periodTextCompact(context));
			replacements.put(exactReplacementKey("22581.6元"), moneyText(fields.get("房租")));
			replacements.put(exactReplacementKey("3575.42元"), moneyText(fields.get("物业")));
			replacements.put(exactReplacementKey("元"), moneyText(fields.get("押金")));
			replacements.put(exactReplacementKey("（2025.3.25-2025.6.30）物业费（2025.3.25-2025.6.30）"), fields.get("开票内容及所属期"));
			return contractTemplateRenderService.render(
			NOTICE_INVOICE,
			"开票申请单",
			TEMPLATE_INVOICE,
			"开票申请单-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildContractText(NoticeContext context, boolean floatingRent) {
		Map<String, String> fields = createContractTextFields(context, floatingRent);
		Map<String, String> replacements = createContractTextReplacements(context, fields, floatingRent);
		String noticeType = floatingRent ? NOTICE_CONTRACT_FLOATING : NOTICE_CONTRACT_FIXED;
		String noticeName = floatingRent ? "合同正文浮动租金版" : "合同正文固定租金版";
		String template = floatingRent ? TEMPLATE_CONTRACT_FLOATING : TEMPLATE_CONTRACT_FIXED;
		return contractTemplateRenderService.render(
			noticeType,
			noticeName,
			template,
			noticeName + "-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private ContractNoticeFileVO buildProjectApproval(NoticeContext context) {
		Map<String, String> fields = createProjectApprovalFields(context);
		Map<String, String> replacements = createProjectApprovalReplacements(context, fields);
		return contractTemplateRenderService.render(
			NOTICE_PROJECT_APPROVAL,
			"项目审批表",
			TEMPLATE_PROJECT_APPROVAL,
			"项目审批表-" + context.contractNo(),
			fields,
			replacements
		);
	}

	private NoticeContext resolveContext(String noticeType, Long paymentId, Long contractId) {
		return resolveContext(noticeType, paymentId, contractId, Collections.emptyMap());
	}

	private NoticeContext resolveContext(String noticeType, Long paymentId, Long contractId, Map<String, Object> formData) {
		String normalized = normalizeNoticeType(noticeType);
		ContractPayment payment = null;
		Contract contract = null;
		Customer customer = null;
		if (NOTICE_TERMINATION.equals(normalized) || NOTICE_ROOM_REVIEW.equals(normalized) || Func.isNotEmpty(contractId)) {
			if (Func.isNotEmpty(contractId)) {
				contract = contractMapper.selectContractById(contractId);
			}
		}
		if (needsPayment(normalized) || Func.isNotEmpty(paymentId)) {
			if (Func.isNotEmpty(paymentId)) {
				payment = contractPaymentMapper.selectById(paymentId);
			}
			if (payment != null && contract == null) {
				contract = contractMapper.selectContractById(payment.getContractId());
			}
		}
		if (contract == null && payment != null) {
			contract = contractMapper.selectContractById(payment.getContractId());
		}
		if (contract == null) {
			throw new ServiceException("合同不存在");
		}
			if (needsPayment(normalized) && payment == null) {
				throw new ServiceException("账单不存在");
			}
			formData = resolveLatestFormData(normalized, contract, formData);
			if (contract != null && contract.getCustomerId() != null) {
				customer = customerMapper.selectCustomerById(contract.getCustomerId());
			}
			return new NoticeContext(normalized, payment, contract, customer, formData);
		}

		private Map<String, Object> resolveLatestFormData(String noticeType, Contract contract, Map<String, Object> formData) {
			if (formData != null && !formData.isEmpty()) {
				return formData;
			}
			if (contract == null || contract.getContractId() == null) {
				return Collections.emptyMap();
			}
			String businessType = noticeWorkflowBusinessType(noticeType);
			if (StringUtil.isBlank(businessType)) {
				return Collections.emptyMap();
			}
			ContractWorkflowRecord record = contractWorkflowRecordMapper.selectLatest(contract.getContractId(), businessType);
			if (record == null) {
				return Collections.emptyMap();
			}
			return parseFormData(record.getFormDataJson());
		}

		private String noticeWorkflowBusinessType(String noticeType) {
			return switch (normalizeNoticeType(noticeType)) {
				case NOTICE_TERMINATION, NOTICE_TERMINATION_AGREEMENT -> "contract_termination";
				case NOTICE_ROOM_REVIEW -> "contract_room_review";
				case NOTICE_PROJECT_APPROVAL, NOTICE_LEGAL -> "contract_overdue_legal";
				case NOTICE_CONTRACT_APPROVAL, NOTICE_CONTRACT_FIXED, NOTICE_CONTRACT_FLOATING -> "contract_approval";
				default -> null;
			};
		}

		private boolean needsPayment(String noticeType) {
			return Set.of(NOTICE_PAYMENT, NOTICE_REMINDER, NOTICE_INVOICE, NOTICE_PROJECT_APPROVAL, NOTICE_OVERDUE, NOTICE_LEGAL).contains(normalizeNoticeType(noticeType));
		}

	private Map<String, String> createCommonFields(NoticeContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("合同编号", context.contractNo());
		fields.put("企业名称", context.customerName());
		fields.put("所属园区", context.parkName());
		fields.put("房源信息", context.roomDisplay());
		fields.put("楼宇名称", context.buildingName());
		fields.put("费用类型", context.feeName());
		fields.put("账期", context.periodText());
		fields.put("应缴日期", formatDate(context.payment == null ? null : context.payment.getPayDeadline()));
		fields.put("应收金额", formatMoney(context.payment == null ? null : context.payment.getAmountDue()));
		fields.put("已缴金额", formatMoney(context.payment == null ? null : context.payment.getAmountPaid()));
		fields.put("未缴金额", formatMoney(context.unpaidAmount()));
		fields.put("逾期天数", String.valueOf(context.overdueDays()));
		fields.put("合同状态", context.contractStatusName());
		return fields;
	}

	private Map<String, String> createPaymentNoticeFields(NoticeContext context) {
		Map<String, String> fields = createCommonFields(context);
		fields.put("日期", firstNotBlank(
			formValue(context, "a17827111407976730", "付款日期", "日期"),
			formatChineseDate(DateUtil.now())
		));
		fields.put("单元", firstNotBlank(
			formValue(context, "a17827097285474047", "单元", "Unit"),
			context.roomDisplay()
		));
		fields.put("租户", firstNotBlank(
			formValue(context, "a178270977083337055", "租户", "Tenant"),
			context.customerName()
		));
		fields.put("缴费内容", context.feeName());
		fields.put("租金", firstNotBlank(
			formValue(context, "a178271064539262003", "租金 Rent", "租金"),
			amountForFee(context, "rent", "租", "房租")
		));
		fields.put("物业费", firstNotBlank(
			formValue(context, "a17827106469118400", "物业费 Proper Management Fee", "物业费"),
			amountForFee(context, "property", "物业")
		));
		fields.put("电费", firstNotBlank(
			formValue(context, "a178271065006694738", "电费 Power Rate", "电费"),
			amountForFee(context, "electric", "电")
		));
		fields.put("计费周期", firstNotBlank(
			formValue(context, "a178271130194976385", "计费周期"),
			context.periodText()
		));
		fields.put("总计", firstNotBlank(
			formValue(context, "a178271012553233941", "总计(RMB)", "总计"),
			formatMoney(context.payment == null ? null : context.payment.getAmountDue())
		));
		fields.put("账户信息", firstNotBlank(
			formValue(context, "a178271014325490480", "账户信息", "BankInformation"),
			"-"
		));
		fields.put("支付时间", firstNotBlank(
			formValue(context, "a178271015785294970", "支付时间", "Payment"),
			formatDate(context.payment == null ? null : context.payment.getPayDeadline())
		));
		fields.put("租户签收", firstNotBlank(
			formValue(context, "a178271018429016785", "租户签收", "TenantSign"),
			"-"
		));
		fields.put("备注", firstNotBlank(
			formValue(context, "a178271019302862626", "备注", "Note"),
			"合同编号：" + context.contractNo() + "，" + context.feeName() + "：" + context.periodText()
		));
		return fields;
	}

	private Map<String, String> createPaymentNoticeReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("日期（Date）:", "日期（Date）: " + fields.get("日期"));
		replacements.put("名称：苏州市吴中金融招商服务有限公司", "名称：苏州市吴中金融招商服务有限公司");
		replacements.put("务必在转账时写明款项用途及时间，例产业资本中心***室*年*月*日 / 租金 / 保证金 /物业服务费",
			"务必在转账时写明款项用途及时间，例如：" + context.roomDisplay() + " / " + context.feeName() + " / " + context.periodText());
		return replacements;
	}

	private Map<String, String> createOverdueFields(NoticeContext context) {
		Map<String, String> fields = createCommonFields(context);
		fields.put("致", context.customerName());
		fields.put("承租物业", context.roomDisplay());
		fields.put("期数", periodTextCompact(context));
		fields.put("到期日", formatChineseDate(context.payment == null ? null : context.payment.getPayDeadline()));
		fields.put("费用期间", context.periodText());
		fields.put("项目名称", context.feeName());
		fields.put("应收金额", formatMoney(context.payment == null ? null : context.payment.getAmountDue()));
		fields.put("应付日期", formatDate(context.payment == null ? null : context.payment.getPayDeadline()));
		fields.put("违约金", estimateLateFee(context));
		fields.put("总计", formatMoney(context.unpaidAmount().add(parseMoney(estimateLateFee(context)))));
		fields.put("合计人民币", formatMoney(context.unpaidAmount().add(parseMoney(estimateLateFee(context)))));
		fields.put("通知日期", formatChineseDate(DateUtil.now()));
		fields.put("催款通知送达日期", formatChineseDate(DateUtil.now()));
		fields.put("支付期限天数", "5");
		return fields;
	}

	private Map<String, String> createProjectApprovalFields(NoticeContext context) {
		Map<String, String> fields = createOverdueFields(context);
		Contract contract = context.contract;
		String applyTime = firstNotBlank(
			formValue(context, "a178228601087185682", "申请时间", "applyTime"),
			formatDate(DateUtil.now())
		);
		String applicant = firstNotBlank(
			formValue(context, "a178228616434960037", "经办人", "applicant"),
			contract == null ? null : firstNotBlank(contract.getFollowUser(), contract.getCreateBy())
		);
		String applicantDept = firstNotBlank(
			formValue(context, "a178228619901214332", "部门", "applicantDept"),
			"-"
		);
		String applyContent = firstNotBlank(
			formValue(context, "a17822862555375219", "申请内容", "remark"),
			"因" + context.customerName() + "未按期缴纳" + context.feeName()
				+ "，欠费期间为" + context.periodText() + "，未缴金额" + formatMoney(context.unpaidAmount())
				+ "元，已逾期" + context.overdueDays() + "天，申请启动催款、租金逾期处理通知及律师函审批。"
		);
		fields.put("项目名称", "逾期费用处理");
		fields.put("申请单位", context.customerName());
		fields.put("申请单位（个人）", firstNotBlank(
			formValue(context, "a178228600913530003", "申请单位（个人）", "申请单位"),
			context.customerName()
		));
		fields.put("申请时间", applyTime);
		fields.put("合同编号", context.contractNo());
		fields.put("租赁楼层", firstNotBlank(
			formValue(context, "a178228601343895801", "租赁楼层"),
			context.roomDisplay()
		));
		fields.put("租赁面积", firstNotBlank(
			formValue(context, "a178228601442020074", "租赁面积"),
			formatArea(contract == null ? null : contract.getRentArea())
		));
		fields.put("租赁楼层、面积", fields.get("租赁楼层") + " " + fields.get("租赁面积"));
		fields.put("合同有效期", firstNotBlank(
			formValue(context, "a178228603001881228", "合同有效期"),
			formatDate(contract == null ? null : contract.getStartDate()) + " 至 " + formatDate(contract == null ? null : contract.getEndDate())
		));
		fields.put("月租金（元）", firstNotBlank(
			formValue(context, "a178228612023052511", "月租金（元）", "单价（元）"),
			formatMoney(contract == null ? null : contract.getMonthlyRent())
		));
		fields.put("保证金（元）", firstNotBlank(
			formValue(context, "a178228613003148864", "保证金（元）"),
			formatMoney(contract == null ? null : contract.getDeposit())
		));
		fields.put("经办人", Func.toStr(applicant, "-"));
		fields.put("部门", applicantDept);
		fields.put("申请内容", applyContent);
		fields.put("备注", "账单ID：" + (context.payment == null ? "-" : context.payment.getPaymentId()));
		return fields;
	}

	private Map<String, String> createProjectApprovalReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("{{projectName}}", fields.get("项目名称"));
		replacements.put("{{customerName}}", context.customerName());
		replacements.put("{{contractNo}}", context.contractNo());
		replacements.put("{{applyContent}}", fields.get("申请内容"));
		replacements.put("{{amount}}", fields.get("未缴金额"));
		replacements.put("{{overdueDays}}", fields.get("逾期天数"));
		return replacements;
	}

	private Map<String, String> createReminderNoticeReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("致：", "致：" + context.customerName());
		replacements.put(exactReplacementKey("贵司承租的位于苏州市吴中区石湖西路140号君联大厦     办公物业第   期租金已于   年  月  日到期，我司特向贵司发出本催款通知书，请贵司在收到本通知书的10个工作日内足额及时缴纳租金及其他相关费用。"),
			"贵司承租的位于" + context.roomDisplay() + "办公物业" + context.feeName() + "已于" + fields.get("到期日")
				+ "到期，我司特向贵司发出本催款通知书，请贵司在收到本通知书的10个工作日内足额及时缴纳租金及其他相关费用。");
		replacements.put("贵司承租的位于苏州市吴中区石湖西路140号君联大厦 办公物业第 期租金已于 年 月 日到期，我司特向贵司发出本催款通知书，请贵司在收到本通知书的10个工作日内足额及时缴纳租金及其他相关费用。",
			"贵司承租的位于" + context.roomDisplay() + "办公物业" + context.feeName() + "已于" + fields.get("到期日")
				+ "到期，我司特向贵司发出本催款通知书，请贵司在收到本通知书的10个工作日内足额及时缴纳租金及其他相关费用。");
		replacements.put("年 月 日— 年 月 日", context.periodText());
		replacements.put("第一期租金", context.feeName());
		replacements.put("合计人民币", "合计人民币 " + fields.get("合计人民币") + " 元");
		replacements.put(exactReplacementKey("苏州市吴中金融招商服务有限公司\n年 月 日"), "苏州市吴中金融招商服务有限公司\n" + fields.get("通知日期"));
		replacements.put(exactReplacementKey("年 月 日"), fields.get("通知日期"));
		return replacements;
	}

	private Map<String, String> createOverdueNoticeReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = new LinkedHashMap<>();
		String overdueIntro = "贵司承租的位于" + context.roomDisplay() + "办公物业" + context.feeName() + "于" + fields.get("到期日")
			+ "到期，我司已经于" + fields.get("催款通知送达日期") + "送达了《催款通知书》，但贵司在《催款通知书》指定的期间内仍未进行支付。";
		replacements.put(exactReplacementKey("致： _____________ ："), "致：" + context.customerName() + "：");
		replacements.put(exactReplacementKey("致：  ："), "致：" + context.customerName() + "：");
		replacements.put("致： _____________ ：", "致：" + context.customerName() + "：");
		replacements.put(exactReplacementKey("贵司承租的位于苏州市吴中区石湖西路140号君联大厦室办公物业第期租金于2021年月日到期，我司已经于年月日送达了《催款通知书》，但贵司在《催款通知书》指定的期间内仍未进行支付。"),
			overdueIntro);
		replacements.put(exactReplacementKey("贵司承租的位于苏州市吴中区石湖西路140号君联大厦___室办公物业第期租金于2021年月日到期，我司已经于_____年__月日送达了《催款通知书》，但贵司在《催款通知书》指定的期间内仍未进行支付。"),
			overdueIntro);
		replacements.put(exactReplacementKey("贵司承租的位于苏州市吴中区石湖西路140号君联大厦 ___室办公物业第  期租金于2021年  月 日到期，我司已经于_____年_ _月   日送达了《催款通知书》，但贵司在《催款通知书》指定的期间内仍未进行支付。"),
			overdueIntro);
		replacements.put(exactReplacementKey("鉴于上述情况，我司特向贵司进行如下通知："), "鉴于上述情况，我司特向贵司进行如下通知：");
		String paymentDeadlineNotice = "限贵司于收到本《租金逾期处理通知书》后" + fields.get("支付期限天数")
			+ "个工作日内足额支付" + fields.get("费用期间") + "期间欠付" + context.feeName()
			+ fields.get("应收金额") + "元；截至本通知生成日，预估违约金" + fields.get("违约金")
			+ "元，合计应付" + fields.get("总计") + "元。";
		replacements.put(exactReplacementKey("限贵司于收到本《租金逾期处理通知书》后5个工作日内足额支付欠付租金。"),
			paymentDeadlineNotice);
		replacements.put(exactReplacementKey("限贵司于收到本《租金逾期处理通知书》后个工作日内足额支付欠付租金。"),
			paymentDeadlineNotice);
		replacements.put("贵司承租的位于苏州市吴中区石湖西路140号君联大厦 ___室办公物业第 期租金于2021年 月 日到期，我司已经于_____年_ _月 日送达了《催款通知书》，但贵司在《催款通知书》指定的期间内仍未进行支付。",
			overdueIntro);
		replacements.put(exactReplacementKey("苏州市吴中金融招商服务有限公司\n年 月 日"), "苏州市吴中金融招商服务有限公司\n" + fields.get("通知日期"));
		replacements.put(exactReplacementKey("年 月 日"), fields.get("通知日期"));
		replacements.put(exactReplacementKey("年  月  日"), fields.get("通知日期"));
		return replacements;
	}

	private Map<String, String> createMoveOutNoticeReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = createOverdueNoticeReplacements(context, fields);
		replacements.put("租金逾期处理通知书", "限期搬离通知书");
		replacements.put(exactReplacementKey("鉴于上述情况，我司特向贵司进行如下通知："), "鉴于贵司逾期未清偿欠款并拒不返还租赁物业，我司特向贵司进行如下通知：");
		replacements.put("鉴于上述情况，我司特向贵司进行如下通知：", "鉴于贵司逾期未清偿欠款并拒不返还租赁物业，我司特向贵司进行如下通知：");
		replacements.put("限贵司于收到本《租金逾期处理通知书》后5个工作日内足额支付欠付租金。", "限贵司于收到本《限期搬离通知书》后5个工作日内完成搬离、交还钥匙及相关资料。");
		replacements.put("限贵司于收到本《租金逾期处理通知书》后 5 个工作日内足额支付欠付租金。", "限贵司于收到本《限期搬离通知书》后 5 个工作日内完成搬离、交还钥匙及相关资料。");
		replacements.put("4、贵司在收到本租金逾期处理通知书后5个工作日内仍未足额支付欠付租金，贵司同我司签订的租赁合同将自动解除，且无需另行书面通知。", "4、贵司逾期仍拒不搬离的，我司将按照租赁合同及园区管理办法继续启动退租、交接及追偿程序。");
		return replacements;
	}

	private Map<String, String> createLegalLetterReplacements(NoticeContext context, Map<String, String> fields) {
		Map<String, String> replacements = createOverdueNoticeReplacements(context, fields);
		replacements.put("租金逾期处理通知书", "律师函");
		replacements.put(exactReplacementKey("鉴于上述情况，我司特向贵司进行如下通知："), "鉴于贵司逾期未支付合同项下相关费用，经我司催告仍未结清，现正式函告如下：");
		replacements.put("鉴于上述情况，我司特向贵司进行如下通知：", "鉴于贵司逾期未支付合同项下相关费用，经我司催告仍未结清，现正式函告如下：");
		replacements.put("限贵司于收到本《租金逾期处理通知书》后5个工作日内足额支付欠付租金。", "请贵司于收到本函后立即结清全部欠付费用、违约金及相关损失。");
		replacements.put("限贵司于收到本《租金逾期处理通知书》后 5 个工作日内足额支付欠付租金。", "请贵司于收到本函后立即结清全部欠付费用、违约金及相关损失。");
		replacements.put("依照租赁合同的约定，我司将从贵司支付的履约保证金中扣除逾期支付违约金，金额为自本期租金逾期之日起，至贵司支付本期租金之日或合同解除之日的期间内每日逾期租金的万分之五。", "若贵司继续拒不履行付款义务，我司将依据租赁合同及相关法律规定启动追偿程序，并保留通过诉讼、仲裁等方式追究责任的权利。");
		replacements.put("从即日起禁止贵司继续使用所承租物业，并依照租赁合同的约定对贵司采取停电、停水措施，贵司在足额支付相关租金及违约金后可恢复对租赁物业的正常使用。", "由此产生的诉讼费、律师费、保全费、差旅费等维权费用，将依法由贵司承担。");
		replacements.put("4、贵司在收到本租金逾期处理通知书后5个工作日内仍未足额支付欠付租金，贵司同我司签订的租赁合同将自动解除，且无需另行书面通知。", "4、请贵司认真对待并尽快与我司联系处理。");
		return replacements;
	}

	private Map<String, String> createTerminationApprovalFields(NoticeContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		Contract contract = context.contract;
		String applyDate = firstNotBlank(
			formValue(context, "a178228909793494547", "a178228601087185682", "applyTime", "申请时间", "applyDate"),
			formatDate(DateUtil.now())
		);
		String terminationDate = firstNotBlank(
			formValue(context, "expectedTerminationDate", "退租日期", "申请退租日期", "terminationDate"),
			formatDate(DateUtil.now())
		);
		fields.put("退租申请单位（个人）", firstNotBlank(
			formValue(context, "a17822890872424035", "退租申请单位（个人）"),
			context.customerName()
		));
		fields.put("申请时间", applyDate);
		fields.put("申请人联系方式", firstNotBlank(
			formValue(context, "a178228912328564023", "申请人联系方式", "联系方式"),
			customerContactPhone(context),
			"-"
		));
		fields.put("租赁楼层、面积", firstNotBlank(
			formValue(context, "a178228920866788220", "租赁楼层、面积"),
			context.roomDisplay() + " " + formatArea(contract == null ? null : contract.getRentArea())
		));
		fields.put("合同有效期", firstNotBlank(
			formValue(context, "a178228924615174011", "合同有效期"),
			formatDate(contract == null ? null : contract.getStartDate()) + " 至 " + formatDate(contract == null ? null : contract.getEndDate())
		));
		fields.put("月租金（元）", firstNotBlank(
			formValue(context, "a178228926731876751", "月租金（元）"),
			formatMoney(contract == null ? null : contract.getMonthlyRent())
		));
		fields.put("保证金（元）", firstNotBlank(
			formValue(context, "a178228928604722363", "保证金（元）"),
			formatMoney(contract == null ? null : contract.getDeposit())
		));
		fields.put("经办人", firstNotBlank(
			formValue(context, "a178228930092178582", "经办人", "applicant"),
			contract == null ? null : Func.toStr(firstNotBlank(contract.getFollowUser(), contract.getCreateBy()), "-"),
			"-"
		));
		fields.put("部门", firstNotBlank(
			formValue(context, "a178228936466669312", "applicantDept", "申请部门", "送审部门", "部门"),
			"-"
		));
		fields.put("申请退租日期", terminationDate);
		fields.put("退租类型", firstNotBlank(formValue(context, "terminationTypeName", "退租类型"), "正常退租"));
		fields.put("违约金（元）", firstNotBlank(formValue(context, "breachPenalty", "违约金"), "0"));
		fields.put("申请内容", firstNotBlank(
			formValue(context, "a178228940119047948", "申请内容", "terminationReason", "退租原因", "reason"),
			buildTerminationSummary(context)
		));
		return fields;
	}

	private Map<String, String> createRoomReviewFields(NoticeContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		Contract contract = context.contract;
		fields.put("承租单位", context.customerName());
		fields.put("联系人", firstNotBlank(context.customer == null ? null : context.customer.getContactName(), "-"));
			fields.put("联系电话", firstNotBlank(customerContactPhone(context), "-"));
			fields.put("房屋地址", context.roomDisplay());
			fields.put("合同期限", formatChineseDate(contract == null ? null : contract.getStartDate()) + "至" + formatChineseDate(contract == null ? null : contract.getEndDate()));
			fields.put("申请退租日期", formatChineseDateText(firstNotBlank(
				formValue(context, "returnDate", "expectedTerminationDate", "申请退租日期", "退租日期"),
				formatDate(DateUtil.now())
			)));
			fields.put("退租理由", firstNotBlank(
				formValue(context, "handoverResult", "terminationReason", "退租理由", "退租原因"),
				"退租审批通过，进入房屋退租交接验收"
			));
			fields.put("应退租赁押金", formatMoney(contract == null ? null : contract.getDeposit()));
			return fields;
		}

	private Map<String, String> createTerminationReplacements(NoticeContext context) {
		Map<String, String> replacements = new LinkedHashMap<>();
		Contract contract = context.contract;
		String terminationDate = firstNotBlank(formValue(context, "expectedTerminationDate", "退租日期", "申请退租日期", "terminationDate"), formatDate(DateUtil.now()));
		replacements.put("contractNo", context.contractNo());
		replacements.put("customerName", context.customerName());
		replacements.put("roomName", context.roomDisplay());
		replacements.put("applyDate", firstNotBlank(formValue(context, "applyTime", "申请时间", "applyDate"), formatDate(DateUtil.now())));
		replacements.put("terminationDate", terminationDate);
		replacements.put("terminationSummary", buildTerminationSummary(context));
		String roomArea = context.roomDisplay() + " " + formatArea(contract == null ? null : contract.getRentArea());
		replacements.put("　 楼     m2", roomArea);
		replacements.put("楼     m2", roomArea);
		replacements.put(exactReplacementKey("楼m2"), roomArea);
		replacements.put(exactReplacementKey("自年月日至年月日止"), "自 " + formatChineseDate(contract == null ? null : contract.getStartDate()) + " 至 " + formatChineseDate(contract == null ? null : contract.getEndDate()) + "止");
		return replacements;
	}

	private Map<String, String> createRoomReviewReplacements(NoticeContext context) {
		Contract contract = context.contract;
		Map<String, String> replacements = new LinkedHashMap<>();
			replacements.put("苏州思锐泰企业管理咨询有限公司", context.customerName());
			replacements.put("苏州市吴中区石湖西路 168号\n科技服务中心大楼 911 室", context.roomDisplay());
			replacements.put("合同期限：   2025 年 3  月 11 日至2026  年3 月10   日", "合同期限：" + formatChineseDate(contract == null ? null : contract.getStartDate()) + "至" + formatChineseDate(contract == null ? null : contract.getEndDate()));
			replacements.put("申请退租日期：    年  月  日", "申请退租日期：" + formatChineseDateText(firstNotBlank(
				formValue(context, "returnDate", "expectedTerminationDate", "申请退租日期", "退租日期"),
				formatDate(DateUtil.now())
			)));
			replacements.put("应退租赁押金：", "应退租赁押金：" + formatMoney(contract == null ? null : contract.getDeposit()));
			return replacements;
		}

	private Map<String, String> createTerminationAgreementReplacements(NoticeContext context) {
		Contract contract = context.contract;
		String signDate = formatChineseDate(contract == null ? null : contract.getSignDate());
		String startDate = formatChineseDate(contract == null ? null : contract.getStartDate());
		String endDate = formatChineseDate(contract == null ? null : contract.getEndDate());
		String applyDate = formatChineseDateText(firstNotBlank(formValue(context, "a178228909793494547", "applyTime", "申请时间", "applyDate"), formatDate(DateUtil.now())));
		String terminationDate = formatChineseDateText(firstNotBlank(formValue(context, "expectedTerminationDate", "退租日期", "申请退租日期", "terminationDate"), formatDate(DateUtil.now())));
		String reason = firstNotBlank(formValue(context, "terminationReason", "退租原因", "reason", "a178228940119047948", "申请内容"), "退租申请");
		String terminationType = firstNotBlank(formValue(context, "terminationType", "退租类型编码"), "normal");
		String terminationClause = buildTerminationAgreementClause(applyDate, reason, terminationType);
		String contractClause = "1、" + signDate + "，甲乙双方签订《君联大厦租赁合同》。根据合同约定，由乙方承租甲方位于"
			+ context.roomDisplay() + "物业。租赁期限为" + contractMonths(contract) + "个月，自" + startDate + "起至" + endDate + "止。";
		String releaseClause = "1、 双方一致确认：《君联大厦租赁合同》于" + terminationDate + "解除；";
		String returnClause = "2、 乙方承诺于" + terminationDate + "前将租赁物业腾空归还甲方，同时在" + terminationDate
			+ "前完成注册地址迁出手续。若乙方按期完成腾房、迁址的，甲方按合同及审批结果结算租金、物业服务费；";
		String overdueClause = "若乙方逾期腾房或迁址的，甲方有权向乙方收取" + terminationDate
			+ "至实际腾房/迁址之日（以后到为准）的房租、物业服务费并有权按照租赁合同8.2.2条约定向乙方主张通知不足2个月的补足租金和3个月租金的违约金。";
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put("乙方（承租方）：", "乙方（承租方）：" + context.customerName());
		replacements.put("乙方：                        （盖章）", "乙方：" + context.customerName() + "（盖章）");
		replacements.put(exactReplacementKey("1、年月日，甲乙双方签订《君联大厦租赁合同》。根据合同约定，由乙方承租甲方位于苏州市吴中区石湖西路140号君联大厦室物业。租赁期限为个月，自年月日起至年月日止。"), contractClause);
		replacements.put("1、年月日，甲乙双方签订《君联大厦租赁合同》。根据合同约定，由乙方承租甲方位于苏州市吴中区石湖西路140号君联大厦室物业。租赁期限为个月，自年月日起至年月日止。", contractClause);
		replacements.put("1、   年 月 日，甲乙双方签订", "1、" + signDate + "，甲乙双方签订");
		replacements.put("君联 大厦 室物业", context.roomDisplay() + "物业");
		replacements.put("租赁期限为 个月", "租赁期限为 " + contractMonths(contract) + " 个月");
		replacements.put("自 年 月 日起至 年 月 日止", "自 " + startDate + "起至 " + endDate + "止");
		replacements.put(exactReplacementKey("2、年月日，因原因，乙方向甲方申请提前退租、解除租赁合同，已构成违约。"), terminationClause);
		replacements.put("2、年月日，因原因，乙方向甲方申请提前退租、解除租赁合同，已构成违约。", terminationClause);
		replacements.put(exactReplacementKey("1、双方一致确认：《君联大厦租赁合同》于年月日解除；"), releaseClause);
		replacements.put("1、双方一致确认：《君联大厦租赁合同》于年月日解除；", releaseClause);
		replacements.put(exactReplacementKey("2、乙方承诺于年月日前将租赁物业腾空归还甲方，同时在年月日前完成注册地址迁出手续。若乙方按期完成腾房、迁址的，甲方收取的租金、物业服务费；"), returnClause);
		replacements.put("2、乙方承诺于年月日前将租赁物业腾空归还甲方，同时在年月日前完成注册地址迁出手续。若乙方按期完成腾房、迁址的，甲方收取的租金、物业服务费；", returnClause);
		replacements.put(exactReplacementKey("若乙方逾期腾房或迁址的，甲方有权向乙方收取年月日至实际腾房/迁址之日（以后到为准）的房租、物业服务费并有权按照租赁合同8.2.2条约定向乙方主张通知不足2个月的补足租金和3个月租金的违约金。"), overdueClause);
		replacements.put("若乙方逾期腾房或迁址的，甲方有权向乙方收取年月日至实际腾房/迁址之日（以后到为准）的房租、物业服务费并有权按照租赁合同8.2.2条约定向乙方主张通知不足2个月的补足租金和3个月租金的违约金。", overdueClause);
		replacements.put("于 年 月 日解除", "于 " + terminationDate + "解除");
		replacements.put("于 年 月 日 前 将", "于 " + terminationDate + " 前将");
		replacements.put("在 年 月 日 前完成", "在 " + terminationDate + " 前完成");
		replacements.put("甲方收取 的租金、物业服务费", "甲方按合同及审批结果结算租金、物业服务费");
		replacements.put("年 月 日至实际腾房/迁址之日", terminationDate + "至实际腾房/迁址之日");
		replacements.put(exactReplacementKey("年月日"), formatChineseDate(DateUtil.now()));
		return replacements;
	}

	private Map<String, String> createContractApprovalFields(NoticeContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		Contract contract = context.contract;
		fields.put("申请单位（个人）", context.customerName());
		fields.put("申请单位", context.customerName());
		fields.put("申请时间", firstNotBlank(
			formValue(context, "applyTime", "申请时间"),
			formatDate(contract == null ? null : contract.getCreateTime())
		));
		fields.put("合同编号", firstNotBlank(
			formValue(context, "a178229178559539772", "合同编号"),
			context.contractNo()
		));
		fields.put("合同名称", firstNotBlank(
			formValue(context, "a178229184751475686", "合同名称"),
			contract == null ? "-" : Func.toStr(contract.getContractName(), "-")
		));
		fields.put("送审部门", firstNotBlank(
			formValue(context, "a178229267333284477", "送审部门", "applicantDept"),
			"-"
		));
		fields.put("部门", fields.get("送审部门"));
		fields.put("合同甲方", firstNotBlank(
			formValue(context, "a178229185420560527", "合同甲方"),
			"苏州市吴中金融招商服务有限公司"
		));
		fields.put("合同乙方", firstNotBlank(
			formValue(context, "a1782291855806355", "a178229185640367241", "合同乙方"),
			context.customerName()
		));
			fields.put("合同事项", firstNotBlank(
				formValue(context, "a178229189162468518", "合同事项"),
				buildCompactContractSummary(contract)
			));
		fields.put("项目编号", contract == null || contract.getProjectId() == null ? "-" : String.valueOf(contract.getProjectId()));
		fields.put("所属园区", context.parkName());
		fields.put("租赁楼层", context.roomDisplay());
		fields.put("租赁面积", formatArea(contract == null ? null : contract.getRentArea()));
		fields.put("租赁楼层、面积", context.roomDisplay() + " " + formatArea(contract == null ? null : contract.getRentArea()));
		fields.put("合同有效期", formatDate(contract == null ? null : contract.getStartDate()) + " 至 " + formatDate(contract == null ? null : contract.getEndDate()));
		fields.put("月租金（元）", formatMoney(contract == null ? null : contract.getMonthlyRent()));
		fields.put("月租金", formatMoney(contract == null ? null : contract.getMonthlyRent()));
		fields.put("单价（元）", formatMoney(contract == null ? null : contract.getRentPrice()));
		fields.put("单价", formatMoney(contract == null ? null : contract.getRentPrice()));
		fields.put("保证金（元）", formatMoney(contract == null ? null : contract.getDeposit()));
		fields.put("保证金", formatMoney(contract == null ? null : contract.getDeposit()));
		fields.put("经办人", firstNotBlank(
			formValue(context, "applicant", "经办人"),
			contract == null ? "-" : Func.toStr(firstNotBlank(contract.getFollowUser(), contract.getCreateBy()), "-")
		));
		fields.put("管理费", formatMoney(contract == null ? null : contract.getManagementFee()));
		fields.put("公摊费", formatMoney(contract == null ? null : contract.getPublicFee()));
		fields.put("租金递增", contract == null ? "-" : Func.toStr(contract.getRentIncreaseNode(), "-"));
		fields.put("滞纳金", lateFeeText(contract));
			fields.put("申请内容", firstNotBlank(
				formValue(context, "a178229189162468518", "申请内容"),
				buildCompactContractSummary(contract)
			));
		fields.put("部门经理", firstNotBlank(formValue(context, "a178229195669534563", "部门经理"), "-"));
		fields.put("风控审核", firstNotBlank(formValue(context, "a178229196550349962", "风控审核"), "-"));
		fields.put("律师意见", firstNotBlank(formValue(context, "a178229196627971409", "律师意见"), "-"));
		fields.put("综合管理部", firstNotBlank(formValue(context, "a178229196800255673", "综合管理部"), "-"));
		fields.put("分管领导", firstNotBlank(formValue(context, "a178229196922319221", "分管领导"), "-"));
		fields.put("总经理审批", firstNotBlank(formValue(context, "a17822920239039332", "总经理审批"), "-"));
		fields.put("备注", contract == null ? "-" : Func.toStr(contract.getRemark(), "-"));
		return fields;
	}

	private Map<String, String> createContractTextFields(NoticeContext context, boolean floatingRent) {
		Map<String, String> fields = new LinkedHashMap<>();
		Contract contract = context.contract;
		fields.put("合同编号", context.contractNo());
		fields.put("签订日期", formatChineseDate(contract == null ? null : firstNotNull(contract.getSignDate(), DateUtil.now())));
		fields.put("乙方", context.customerName());
		fields.put("承租方", context.customerName());
		fields.put("租赁房源", context.roomDisplay());
		fields.put("租赁地址", contractLeaseAddress(context));
		fields.put("房间号", context.roomName());
		fields.put("租赁面积", formatAreaNumber(contract == null ? null : contract.getRentArea()));
		fields.put("租赁月数", String.valueOf(contractMonths(contract)));
		fields.put("起租日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("结束日期", formatChineseDate(contract == null ? null : contract.getEndDate()));
		fields.put("交付日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("免租开始日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("免租结束日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("租金单价", formatMoney(contract == null ? null : contract.getRentPrice()));
		fields.put("月租金", formatMoney(contract == null ? null : contract.getMonthlyRent()));
		fields.put("月租金大写", moneyUpper(contract == null ? null : contract.getMonthlyRent()));
		fields.put("付款周期", paymentCycleMonths(contract));
		fields.put("第一期付款日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("第二期付款日期", formatChineseDate(nextPaymentDate(contract)));
		fields.put("物业费付款周期", paymentCycleMonths(contract));
		fields.put("第一期物业费付款日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
		fields.put("第二期物业费付款日期", formatChineseDate(nextPaymentDate(contract)));
		fields.put("保证金月数", "3");
		fields.put("保证金", formatMoney(contract == null ? null : contract.getDeposit()));
		fields.put("保证金大写", moneyUpper(contract == null ? null : contract.getDeposit()));
		if (floatingRent) {
			fields.put("第一阶段开始日期", formatChineseDate(contract == null ? null : contract.getStartDate()));
			fields.put("第一阶段结束日期", formatChineseDate(midRentEndDate(contract)));
			fields.put("第一阶段租金单价", formatMoney(contract == null ? null : contract.getRentPrice()));
			fields.put("第一阶段月租金", formatMoney(contract == null ? null : contract.getMonthlyRent()));
			fields.put("第一阶段月租金大写", moneyUpper(contract == null ? null : contract.getMonthlyRent()));
			fields.put("第二阶段开始日期", formatChineseDate(midRentStartDate(contract)));
			fields.put("第二阶段结束日期", formatChineseDate(contract == null ? null : contract.getEndDate()));
			fields.put("第二阶段租金单价", formatMoney(adjustRentPrice(contract)));
			fields.put("第二阶段月租金", formatMoney(adjustMonthlyRent(contract)));
			fields.put("第二阶段月租金大写", moneyUpper(adjustMonthlyRent(contract)));
		}
		return fields;
	}

	private Map<String, String> createContractTextReplacements(NoticeContext context, Map<String, String> fields, boolean floatingRent) {
		Map<String, String> replacements = new LinkedHashMap<>();
		replacements.put(exactReplacementKey("本租赁合同由下列双方于年月日签订:"), "本租赁合同由下列双方于" + fields.get("签订日期") + "签订:");
		replacements.put(exactReplacementKey("本租赁合同由下列双方于签订:"), "本租赁合同由下列双方于" + fields.get("签订日期") + "签订:");
		replacements.put(exactReplacementKey("乙方（承租方）：（以下简称“承租方”）"), "乙方（承租方）：" + fields.get("乙方") + "（以下简称“承租方”）");
		replacements.put(exactReplacementKey("1.1出租方已经取得产权方同意，按照本合同项下的条款将位于苏州市吴中区石湖西路168号科技服务中心大楼室（以下称“该物业”）岀租给承租方。"),
			"1.1出租方已经取得产权方同意，按照本合同项下的条款将位于" + fields.get("租赁地址") + "（以下称“该物业”）出租给承租方。");
		replacements.put(exactReplacementKey("1.1出租方已经取得产权方同意，按照本合同项下的条款将位于苏州市吴中区石湖西路168号科技服务中心大楼（以下称“该物业”）岀租给承租方。"),
			"1.1出租方已经取得产权方同意，按照本合同项下的条款将位于" + fields.get("租赁地址") + "（以下称“该物业”）出租给承租方。");
		replacements.put(exactReplacementKey("1.2本租赁合同的租赁面积为平方米。"), "1.2本租赁合同的租赁面积为" + fields.get("租赁面积") + "平方米。");
		replacements.put(exactReplacementKey("2.1租赁期为个月，自年月日起至年月日止。"),
			"2.1租赁期为" + fields.get("租赁月数") + "个月，自" + fields.get("起租日期") + "起至" + fields.get("结束日期") + "止。");
		replacements.put(exactReplacementKey("2.1租赁期为，自起至止。"),
			"2.1租赁期为" + fields.get("租赁月数") + "个月，自" + fields.get("起租日期") + "起至" + fields.get("结束日期") + "止。");
		replacements.put(exactReplacementKey("2.2出租方应于年月日前将该物业交付承租方，其中在年月日至年月日属于出租方提供给承租方的免租装修期。免租装修期内，承租方无需支付租金，但承租方应承担免租装修期间该物业的物业管理费和水电费等实际发生的费用。"),
			"2.2出租方应于" + fields.get("交付日期") + "前将该物业交付承租方，其中在" + fields.get("免租开始日期") + "至" + fields.get("免租结束日期") + "属于出租方提供给承租方的免租装修期。免租装修期内，承租方无需支付租金，但承租方应承担免租装修期间该物业的物业管理费和水电费等实际发生的费用。");
		replacements.put(exactReplacementKey("2.2出租方应于前将该物业交付承租方，其中属于出租方提供给承租方的免租装修期。免租装修期内，承租方无需支付租金，但承租方应承担免租装修期间该物业的物业管理费和水电费等实际发生的费用。"),
			"2.2出租方应于" + fields.get("交付日期") + "前将该物业交付承租方，其中在" + fields.get("免租开始日期") + "至" + fields.get("免租结束日期") + "属于出租方提供给承租方的免租装修期。免租装修期内，承租方无需支付租金，但承租方应承担免租装修期间该物业的物业管理费和水电费等实际发生的费用。");
		replacements.put("合同编号：", "合同编号：" + fields.get("合同编号"));
		replacements.put("本租赁合同由下列双方于 年 月 日签订:", "本租赁合同由下列双方于" + fields.get("签订日期") + "签订:");
		replacements.put("乙方（承租方）： （以下简称“承租方”）", "乙方（承租方）：" + fields.get("乙方") + "（以下简称“承租方”）");
		replacements.put("苏州市吴中区石湖西路168号科技服务中心大楼 室", "苏州市吴中区石湖西路168号科技服务中心大楼 " + fields.get("房间号"));
		replacements.put("本租赁合同的租赁面积为 平方米。", "本租赁合同的租赁面积为 " + fields.get("租赁面积") + " 平方米。");
		replacements.put("租赁期为 个月，自 年 月 日起至 年 月 日止。", "租赁期为 " + fields.get("租赁月数") + " 个月，自" + fields.get("起租日期") + "起至" + fields.get("结束日期") + "止。");
		replacements.put("出租方应于 年 月 日前将该物业交付承租方，其中在 年 月 日至 年 月 日属于出租方提供给承租方的免租装修期。", "出租方应于" + fields.get("交付日期") + "前将该物业交付承租方，其中在" + fields.get("免租开始日期") + "至" + fields.get("免租结束日期") + "属于出租方提供给承租方的免租装修期。");
		if (floatingRent) {
			replacements.put(exactReplacementKey("4.1.1该物业自年月日至年月日租金单价为元/M2/月（以租赁面积计，下同），合计元/月（大写：人民币）。自年月日至年月日租金单价为元/ M2/月（以租赁面积计，下同），合计元/月（大写：人民币）。"),
				"4.1.1该物业自" + fields.get("第一阶段开始日期") + "至" + fields.get("第一阶段结束日期") + "租金单价为" + fields.get("第一阶段租金单价")
					+ "元/M2/月（以租赁面积计，下同），合计" + fields.get("第一阶段月租金") + "元/月（大写：人民币" + fields.get("第一阶段月租金大写")
					+ "）。自" + fields.get("第二阶段开始日期") + "至" + fields.get("第二阶段结束日期") + "租金单价为" + fields.get("第二阶段租金单价")
					+ "元/M2/月（以租赁面积计，下同），合计" + fields.get("第二阶段月租金") + "元/月（大写：人民币" + fields.get("第二阶段月租金大写") + "）。");
			replacements.put(exactReplacementKey("4.1.1该物业租金单价为元/M2/月（以租赁面积计，下同），合计元/月（大写：人民币）。租金单价为元/ M2/月（以租赁面积计，下同），合计元/月（大写：人民币）。"),
				"4.1.1该物业自" + fields.get("第一阶段开始日期") + "至" + fields.get("第一阶段结束日期") + "租金单价为" + fields.get("第一阶段租金单价")
					+ "元/M2/月（以租赁面积计，下同），合计" + fields.get("第一阶段月租金") + "元/月（大写：人民币" + fields.get("第一阶段月租金大写")
					+ "）。自" + fields.get("第二阶段开始日期") + "至" + fields.get("第二阶段结束日期") + "租金单价为" + fields.get("第二阶段租金单价")
					+ "元/M2/月（以租赁面积计，下同），合计" + fields.get("第二阶段月租金") + "元/月（大写：人民币" + fields.get("第二阶段月租金大写") + "）。");
			replacements.put("该物业自 年 月 日至 年 月 日租金单价为 元/M2/月（以租赁面积计，下同），合计 元/月（大写：人民币 ）。自 年 月 日至 年 月 日租金单价为 元/ M2/月（以租赁面积计，下同），合计 元/月（大写：人民币 ）。",
				"该物业自" + fields.get("第一阶段开始日期") + "至" + fields.get("第一阶段结束日期") + "租金单价为" + fields.get("第一阶段租金单价")
					+ "元/M2/月（以租赁面积计，下同），合计" + fields.get("第一阶段月租金") + "元/月（大写：人民币" + fields.get("第一阶段月租金大写")
					+ "）。自" + fields.get("第二阶段开始日期") + "至" + fields.get("第二阶段结束日期") + "租金单价为" + fields.get("第二阶段租金单价")
					+ "元/ M2/月（以租赁面积计，下同），合计" + fields.get("第二阶段月租金") + "元/月（大写：人民币" + fields.get("第二阶段月租金大写") + "）。");
		} else {
			replacements.put(exactReplacementKey("4.1.1该物业租金单价为元/M2/月（以租赁面积计，下同），合计元/月（大写：人民币）。"),
				"4.1.1该物业租金单价为" + fields.get("租金单价") + "元/M2/月（以租赁面积计，下同），合计" + fields.get("月租金") + "元/月（大写：人民币" + fields.get("月租金大写") + "）。");
			replacements.put("该物业租金单价为 元/M2/月（以租赁面积计，下同），合计 元/月（大写：人民币 ）。",
				"该物业租金单价为 " + fields.get("租金单价") + " 元/M2/月（以租赁面积计，下同），合计 " + fields.get("月租金") + " 元/月（大写：人民币 " + fields.get("月租金大写") + "）。");
		}
		replacements.put(exactReplacementKey("4.1.2租金支付方式为先付后用，租金应按每个月为一期支付，承租方应在年月日或之前支付第一期租金；年月日或之前支付第二期租金；以后以此类推，每次提前30日支付下一期租金。"),
			"4.1.2租金支付方式为先付后用，租金应按每" + fields.get("付款周期") + "个月为一期支付，承租方应在" + fields.get("第一期付款日期")
				+ "或之前支付第一期租金；" + fields.get("第二期付款日期") + "或之前支付第二期租金；以后以此类推，每次提前30日支付下一期租金。");
		replacements.put(exactReplacementKey("4.1.2租金支付方式为先付后用，租金应按为一期支付，承租方应在或之前支付第一期租金；或之前支付第二期租金；以后以此类推，每次提前30日支付下一期租金。"),
			"4.1.2租金支付方式为先付后用，租金应按每" + fields.get("付款周期") + "个月为一期支付，承租方应在" + fields.get("第一期付款日期")
				+ "或之前支付第一期租金；" + fields.get("第二期付款日期") + "或之前支付第二期租金；以后以此类推，每次提前30日支付下一期租金。");
		replacements.put(exactReplacementKey("4.2.2物业服务费支付方式为先付后用，物业服务费应按每个月为一期支付，承租方应在"),
			"4.2.2物业服务费支付方式为先付后用，物业服务费应按每" + fields.get("物业费付款周期") + "个月为一期支付，承租方应在");
		replacements.put(exactReplacementKey("4.2.2物业服务费支付方式为先付后用，物业服务费应按为一期支付，承租方应在"),
			"4.2.2物业服务费支付方式为先付后用，物业服务费应按每" + fields.get("物业费付款周期") + "个月为一期支付，承租方应在");
		replacements.put(exactReplacementKey("年月日或之前支付第一期物业服务费；年月日或之前支付第二期物业费；以后以此类推，每次提前30日支付下一期物业服务费。"),
			fields.get("第一期物业费付款日期") + "或之前支付第一期物业服务费；" + fields.get("第二期物业费付款日期") + "或之前支付第二期物业费；以后以此类推，每次提前30日支付下一期物业服务费。");
		replacements.put(exactReplacementKey("或之前支付第一期物业服务费；或之前支付第二期物业费；以后以此类推，每次提前30日支付下一期物业服务费。"),
			fields.get("第一期物业费付款日期") + "或之前支付第一期物业服务费；" + fields.get("第二期物业费付款日期") + "或之前支付第二期物业费；以后以此类推，每次提前30日支付下一期物业服务费。");
		replacements.put(exactReplacementKey("4.3.1在年月日前，承租方应向出租方支付相当于个月房租的履约保证金"),
			"4.3.1在" + fields.get("第一期付款日期") + "前，承租方应向出租方支付相当于" + fields.get("保证金月数") + "个月房租的履约保证金");
		replacements.put(exactReplacementKey("4.3.1在前，承租方应向出租方支付相当于个月房租的履约保证金"),
			"4.3.1在" + fields.get("第一期付款日期") + "前，承租方应向出租方支付相当于" + fields.get("保证金月数") + "个月房租的履约保证金");
		replacements.put(exactReplacementKey("（即元，大写：人民币）。"), "（即" + fields.get("保证金") + "元，大写：人民币" + fields.get("保证金大写") + "）。");
		replacements.put(exactReplacementKey("（即元，大写：）。"), "（即" + fields.get("保证金") + "元，大写：人民币" + fields.get("保证金大写") + "）。");
		replacements.put("租金应按每 个月为一期支付，承租方应在 年 月 日或之前支付第一期租金； 年 月 日或之前支付第二期租金；", "租金应按每 " + fields.get("付款周期") + " 个月为一期支付，承租方应在 " + fields.get("第一期付款日期") + " 或之前支付第一期租金； " + fields.get("第二期付款日期") + " 或之前支付第二期租金；");
		replacements.put("在 年 月 日前，承租方应向出租方支付相当于 个月房租的履约保证金\n（即 元，大写：人民币 ）。", "在 " + fields.get("第一期付款日期") + " 前，承租方应向出租方支付相当于 " + fields.get("保证金月数") + " 个月房租的履约保证金\n（即 " + fields.get("保证金") + " 元，大写：人民币 " + fields.get("保证金大写") + "）。");
		replacements.put(exactReplacementKey("以下为编号《科技服务中心租赁合同》签字盖章页"), "以下为编号" + fields.get("合同编号") + "《科技服务中心租赁合同》签字盖章页");
		replacements.put("以下为编号《科技服务中心租赁合同》签字盖章页", "以下为编号" + fields.get("合同编号") + "《科技服务中心租赁合同》签字盖章页");
		replacements.put("以下为编号 《科技服务中心租赁合同》签字盖章页", "以下为编号 " + fields.get("合同编号") + "《科技服务中心租赁合同》签字盖章页");
		return replacements;
	}

	private Map<String, String> createInvoiceFields(NoticeContext context) {
		Map<String, String> fields = new LinkedHashMap<>();
		String amount = formatMoney(context.payment == null ? null : context.payment.getAmountDue());
		fields.put("日期", firstNotBlank(
			formValue(context, "a178229020187351487", "日期", "applyTime"),
			formatDate(DateUtil.now())
		));
		fields.put("合同号", firstNotBlank(
			formValue(context, "a17822901960254579", "合同号"),
			context.contractNo()
		));
		fields.put("申请人", firstNotBlank(
			formValue(context, "a178229026327048309", "申请人", "applicant"),
			context.contract == null ? "-" : Func.toStr(context.contract.getCreateBy(), "-")
		));
		fields.put("房租", firstNotBlank(
			formValue(context, "a178229043562386124"),
			amountForFee(context, "rent", "租", "房租")
		));
		fields.put("物业", firstNotBlank(
			formValue(context, "a178229053048579216"),
			amountForFee(context, "property", "物业")
		));
		fields.put("押金", firstNotBlank(
			formValue(context, "a178229053161649966"),
			amountForFee(context, "deposit", "押金", "保证金")
		));
		fields.put("单位名称", firstNotBlank(
			formValue(context, "a178229058644646132", "单位名称"),
			context.customerName()
		));
		fields.put("税号", firstNotBlank(
			formValue(context, "a178229059800215745", "税号"),
			customerCreditCode(context),
			"-"
		));
		fields.put("联系电话", firstNotBlank(customerContactPhone(context), "-"));
		fields.put("单位地址", firstNotBlank(
			formValue(context, "a178229061296035254", "单位地址"),
			customerRegisteredAddress(context),
			"-"
		));
		fields.put("单位地址电话", joinAddressAndPhone(customerRegisteredAddress(context), customerContactPhone(context)));
		fields.put("开户行", firstNotBlank(
			formValue(context, "a17822906220499539", "开户行", "bankName", "depositBank"),
			"-"
		));
		fields.put("银行账号", firstNotBlank(
			formValue(context, "bankAccount"),
			extractBankAccount(formValue(context, "a17822906220499539", "开户行及账号")),
			"-"
		));
		fields.put("开户行及账号", joinBankInfo(
			firstNotBlank(formValue(context, "a17822906220499539", "开户行及账号", "开户行", "bankName", "depositBank"), null),
			firstNotBlank(formValue(context, "bankAccount"), extractBankAccount(formValue(context, "a17822906220499539", "开户行及账号")), null)
		));
		fields.put("开票内容及所属期", firstNotBlank(
			formValue(context, "a17822906368403586", "开票内容及所属期"),
			context.feeName() + "（" + periodTextCompact(context) + "）"
		));
		fields.put("备注", firstNotBlank(
			formValue(context, "a178229065580035292", "备注"),
			"合同编号：" + context.contractNo() + "，账单：" + context.feeName()
		));
		fields.put("分管领导", firstNotBlank(formValue(context, "a17822907200094136", "分管领导"), "-"));
		fields.put("部门经理", firstNotBlank(formValue(context, "a178229072168040692", "部门经理"), "-"));
		if (StringUtil.isBlank(fields.get("房租")) && StringUtil.isBlank(fields.get("物业")) && StringUtil.isBlank(fields.get("押金"))) {
			fields.put(context.feeName(), amount);
		}
		return fields;
	}

	private String extractBankAccount(String value) {
		if (StringUtil.isBlank(value)) {
			return null;
		}
		String text = value.trim();
		String[] segments = text.split("[\\s/：:]+");
		for (int index = segments.length - 1; index >= 0; index--) {
			String segment = segments[index];
			if (segment.matches(".*\\d{6,}.*")) {
				return segment.replaceAll("[^0-9]", "");
			}
		}
		return null;
	}

	private String amountForFee(NoticeContext context, String... keywords) {
		if (context.payment == null || keywords == null || keywords.length == 0) {
			return "";
		}
		String feeType = Func.toStr(context.payment.getFeeType(), "").toLowerCase(Locale.ROOT);
		String feeName = Func.toStr(context.payment.getFeeName(), "");
		for (String keyword : keywords) {
			String normalized = Func.toStr(keyword, "");
			if (StringUtil.isBlank(normalized)) {
				continue;
			}
			if (feeType.contains(normalized.toLowerCase(Locale.ROOT)) || feeName.contains(normalized)) {
				return formatMoney(context.payment.getAmountDue());
			}
		}
		return "";
	}

	private String estimateLateFee(NoticeContext context) {
		if (context == null || context.payment == null || context.contract == null || context.contract.getLateFeeRatio() == null) {
			return "0.00";
		}
		BigDecimal ratio = context.contract.getLateFeeRatio();
		BigDecimal amount = context.unpaidAmount();
		long overdueDays = context.overdueDays();
		if (overdueDays <= 0 || amount.compareTo(BigDecimal.ZERO) <= 0) {
			return "0.00";
		}
		String unit = Func.toStr(context.contract.getLateFeeUnit(), "");
		BigDecimal result;
		if (unit.contains("amount") || unit.contains("元") || unit.contains("yuan")) {
			result = ratio.multiply(BigDecimal.valueOf(overdueDays));
		} else {
			result = amount.multiply(ratio).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(overdueDays));
		}
		if (context.contract.getLateFeeCap() != null && result.compareTo(context.contract.getLateFeeCap()) > 0) {
			result = context.contract.getLateFeeCap();
		}
		return formatMoney(result);
	}

	private BigDecimal parseMoney(String value) {
		if (StringUtil.isBlank(value)) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(value.replace(",", "").replace("元", "").trim());
		} catch (Exception ignored) {
			return BigDecimal.ZERO;
		}
	}

	private String buildContractSummary(Contract contract) {
		if (contract == null) {
			return "-";
		}
		List<String> parts = new ArrayList<>();
		if (Func.isNotBlank(contract.getCustomerName())) {
			parts.add(contract.getCustomerName());
		}
		if (Func.isNotBlank(contract.getRoomName())) {
			parts.add("租赁房源：" + contract.getRoomName());
		}
		if (contract.getRentArea() != null) {
			parts.add("面积：" + formatArea(contract.getRentArea()));
		}
		if (contract.getStartDate() != null || contract.getEndDate() != null) {
			parts.add("合同期：" + formatDate(contract.getStartDate()) + " 至 " + formatDate(contract.getEndDate()));
		}
		if (contract.getMonthlyRent() != null) {
			parts.add("月租金：" + formatMoney(contract.getMonthlyRent()) + "元");
		}
		return parts.isEmpty() ? "-" : String.join("；", parts);
	}

	private String buildCompactContractSummary(Contract contract) {
		if (contract == null) {
			return "-";
		}
		List<String> parts = new ArrayList<>();
		if (Func.isNotBlank(contract.getRoomName())) {
			String room = contract.getRoomName();
			if (Func.isNotBlank(contract.getBuildingName())) {
				room = room + "/" + contract.getBuildingName();
			}
			parts.add(room);
		}
		if (contract.getRentArea() != null) {
			parts.add(formatArea(contract.getRentArea()));
		}
		if (contract.getStartDate() != null || contract.getEndDate() != null) {
			parts.add(formatDate(contract.getStartDate()) + "至" + formatDate(contract.getEndDate()));
		}
		if (contract.getMonthlyRent() != null) {
			parts.add("月租" + formatMoney(contract.getMonthlyRent()) + "元");
		}
		return parts.isEmpty() ? "-" : String.join("，", parts);
	}

	private String buildTerminationSummary(NoticeContext context) {
		Contract contract = context.contract;
		List<String> parts = new ArrayList<>();
		parts.add("申请退租并办理合同解除、费用结算、房屋交接及押金退还手续");
		String terminationType = firstNotBlank(formValue(context, "terminationTypeName", "退租类型"), null);
		if (StringUtil.isNotBlank(terminationType)) {
			parts.add("退租类型：" + terminationType);
		}
		String expectedTerminationDate = firstNotBlank(formValue(context, "expectedTerminationDate", "退租日期", "申请退租日期", "terminationDate"), null);
		if (StringUtil.isNotBlank(expectedTerminationDate)) {
			parts.add("申请退租日期：" + expectedTerminationDate);
		}
		String reason = firstNotBlank(formValue(context, "terminationReason", "退租原因", "reason", "a178228940119047948", "申请内容"), null);
		if (StringUtil.isNotBlank(reason)) {
			parts.add("退租原因：" + reason);
		}
		String breachPenalty = firstNotBlank(formValue(context, "breachPenalty", "违约金"), null);
		if (StringUtil.isNotBlank(breachPenalty) && !"0".equals(breachPenalty) && !"0.00".equals(breachPenalty)) {
			parts.add("违约金：" + breachPenalty + "元");
		}
		if (contract != null && Func.isNotBlank(contract.getContractNo())) {
			parts.add("合同编号：" + contract.getContractNo());
		}
		if (contract != null && Func.isNotBlank(contract.getRoomName())) {
			parts.add("退租房源：" + contract.getRoomName());
		}
		if (contract != null && contract.getDeposit() != null) {
			parts.add("保证金：" + formatMoney(contract.getDeposit()) + "元");
		}
		return String.join("；", parts);
	}

	private String buildTerminationAgreementClause(String applyDate, String reason, String terminationType) {
		if ("normal".equals(terminationType)) {
			return "2、" + applyDate + "，因" + reason + "，乙方向甲方申请退租、解除租赁合同。";
		}
		if ("special".equals(terminationType)) {
			return "2、" + applyDate + "，因" + reason + "，乙方向甲方申请退租、解除租赁合同，双方按特殊情况一事一议结果执行。";
		}
		return "2、" + applyDate + "，因" + reason + "，乙方向甲方申请提前退租、解除租赁合同，相关违约责任按租赁合同及审批结果执行。";
	}

	private Map<String, Object> parseFormData(String formDataJson) {
		if (StringUtil.isBlank(formDataJson)) {
			return Collections.emptyMap();
		}
		try {
			return JsonUtil.readMap(formDataJson);
		} catch (Exception ignored) {
			return Collections.emptyMap();
		}
	}

	private String formValue(NoticeContext context, String... keys) {
		if (context == null || context.formData == null || context.formData.isEmpty() || keys == null || keys.length == 0) {
			return null;
		}
		for (String key : keys) {
			String value = findFormValue(context.formData, key);
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}

	private String findFormValue(Object source, String key) {
		if (source == null || StringUtil.isBlank(key)) {
			return null;
		}
		if (source instanceof Map<?, ?> map) {
			String value = findDirectFormValue(map, key);
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
			for (Object item : map.values()) {
				value = findFormValue(item, key);
				if (StringUtil.isNotBlank(value)) {
					return value;
				}
			}
		} else if (source instanceof Collection<?> collection) {
			for (Object item : collection) {
				String value = findFormValue(item, key);
				if (StringUtil.isNotBlank(value)) {
					return value;
				}
			}
		}
		return null;
	}

	private String findDirectFormValue(Map<?, ?> map, String key) {
		Object directValue = map.get(key);
		if (directValue != null && !isNested(directValue)) {
			return Func.toStr(directValue, "");
		}
		if (matchesFormKey(map, key)) {
			return Func.toStr(firstNotNull(map.get("value"), map.get("modelValue"), map.get("defaultValue")), "");
		}
		return null;
	}

	private boolean matchesFormKey(Map<?, ?> map, String key) {
		String normalizedKey = normalizeFormKey(key);
		return normalizedKey.equals(normalizeFormKey(Func.toStr(map.get("label"), "")))
			|| normalizedKey.equals(normalizeFormKey(Func.toStr(map.get("title"), "")))
			|| normalizedKey.equals(normalizeFormKey(Func.toStr(map.get("name"), "")))
			|| normalizedKey.equals(normalizeFormKey(Func.toStr(map.get("field"), "")))
			|| normalizedKey.equals(normalizeFormKey(Func.toStr(map.get("prop"), "")));
	}

	private String normalizeFormKey(String key) {
		return Func.toStr(key, "")
			.replace("：", "")
			.replace(":", "")
			.replace(" ", "")
			.trim()
			.toLowerCase(Locale.ROOT);
	}

	private boolean isNested(Object value) {
		return value instanceof Map<?, ?> || value instanceof Collection<?>;
	}

	private PreviewData buildPreviewData(String noticeType, NoticeContext context) {
		Map<String, String> fields = switch (noticeType) {
			case NOTICE_PAYMENT -> createPaymentNoticeFields(context);
			case NOTICE_REMINDER, NOTICE_OVERDUE, NOTICE_LEGAL, NOTICE_MOVE_OUT -> createOverdueFields(context);
			case NOTICE_INVOICE -> createInvoiceFields(context);
			case NOTICE_CONTRACT_APPROVAL -> createContractApprovalFields(context);
			case NOTICE_CONTRACT_FIXED -> createContractTextFields(context, false);
			case NOTICE_CONTRACT_FLOATING -> createContractTextFields(context, true);
			case NOTICE_PROJECT_APPROVAL -> createProjectApprovalFields(context);
			case NOTICE_TERMINATION, NOTICE_TERMINATION_AGREEMENT -> createTerminationApprovalFields(context);
			case NOTICE_ROOM_REVIEW -> createRoomReviewFields(context);
			default -> new LinkedHashMap<>();
		};
		Map<String, String> summary = buildPreviewSummary(noticeType, context, fields);
		List<String> missingFields = fields.entrySet().stream()
			.filter(entry -> isMissingValue(entry.getValue()))
			.map(Map.Entry::getKey)
			.toList();
		return new PreviewData(summary, fields, missingFields);
	}

	private Map<String, String> buildPreviewSummary(String noticeType, NoticeContext context, Map<String, String> fields) {
		Map<String, String> summary = new LinkedHashMap<>();
		summary.put("文书类型", noticeDisplayName(noticeType));
		summary.put("合同编号", context.contractNo());
		summary.put("企业名称", context.customerName());
		summary.put("房源信息", context.roomDisplay());
		if (context.payment != null) {
			summary.put("费用类型", context.feeName());
			summary.put("账期", context.periodText());
			summary.put("应收金额", formatMoney(context.payment.getAmountDue()));
		}
		if (NOTICE_CONTRACT_FIXED.equals(noticeType) || NOTICE_CONTRACT_FLOATING.equals(noticeType)) {
			summary.put("合同版本", NOTICE_CONTRACT_FLOATING.equals(noticeType) ? "浮动租金版" : "固定租金版");
		}
		if (NOTICE_TERMINATION.equals(noticeType) || NOTICE_TERMINATION_AGREEMENT.equals(noticeType)) {
			summary.put("退租日期", firstNotBlank(
				formValue(context, "expectedTerminationDate", "退租日期", "申请退租日期", "terminationDate"),
				fields.get("申请退租日期"),
				"-"
			));
		}
		summary.put("缺失字段数", String.valueOf(fields.entrySet().stream().filter(entry -> isMissingValue(entry.getValue())).count()));
		return summary;
	}

	private String noticeDisplayName(String noticeType) {
		return switch (noticeType) {
			case NOTICE_PAYMENT -> "付款通知单";
			case NOTICE_REMINDER -> "催款通知书";
			case NOTICE_INVOICE -> "开票申请单";
			case NOTICE_CONTRACT_APPROVAL -> "合同会签审批表";
			case NOTICE_CONTRACT_FIXED -> "合同正文固定租金版";
			case NOTICE_CONTRACT_FLOATING -> "合同正文浮动租金版";
			case NOTICE_PROJECT_APPROVAL -> "项目审批表";
			case NOTICE_OVERDUE -> "租金逾期处理通知书";
			case NOTICE_LEGAL -> "律师函";
			case NOTICE_MOVE_OUT -> "限期搬离通知书";
			case NOTICE_TERMINATION -> "退租审批表";
			case NOTICE_TERMINATION_AGREEMENT -> "合同解除补充协议";
			case NOTICE_ROOM_REVIEW -> "房屋退租交接验收单";
			default -> "审批文件";
		};
	}

	private boolean isMissingValue(String value) {
		if (StringUtil.isBlank(value)) {
			return true;
		}
		String normalized = value.trim();
		return "-".equals(normalized)
			|| "/".equals(normalized)
			|| "    年  月  日".equals(normalized);
	}

	private String exactReplacementKey(String key) {
		return EXACT_REPLACEMENT_PREFIX + key;
	}

	private String buildPreviewHtml(String title, Map<String, String> summary, Map<String, String> fields, List<String> missingFields) {
		String summaryHtml = summary.entrySet().stream()
			.map(entry -> "<div class=\"summary-item\"><span>" + escapeHtml(entry.getKey()) + "</span><strong>"
				+ escapeHtml(entry.getValue()) + "</strong></div>")
			.reduce("", String::concat);
		String fieldRows = fields.entrySet().stream()
			.map(entry -> "<tr><th>" + escapeHtml(entry.getKey()) + "</th><td>" + escapeHtml(entry.getValue()) + "</td></tr>")
			.reduce("", String::concat);
		String missingHtml = missingFields.isEmpty()
			? "<div class=\"notice-success\">字段已完整回填</div>"
			: "<div class=\"notice-warning\"><strong>待补字段：</strong>" + escapeHtml(String.join("、", missingFields)) + "</div>";
		return "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + escapeHtml(title)
			+ "</title><style>"
			+ "body{margin:0;padding:24px;background:#f6f8fb;color:#303133;font-family:Arial,\"Microsoft YaHei\",sans-serif;}"
			+ ".sheet{max-width:980px;margin:0 auto;background:#fff;border:1px solid #e5e7eb;border-radius:12px;padding:24px;}"
			+ "h2{margin:0 0 18px;text-align:center;font-size:24px;color:#1f2937;}"
			+ ".summary-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:18px;}"
			+ ".summary-item{padding:12px 14px;border:1px solid #e5e7eb;border-radius:10px;background:#f8fafc;display:flex;flex-direction:column;gap:6px;}"
			+ ".summary-item span{font-size:12px;color:#6b7280;}.summary-item strong{font-size:15px;color:#111827;}"
			+ ".notice-success,.notice-warning{margin-bottom:16px;padding:12px 14px;border-radius:10px;font-size:13px;}"
			+ ".notice-success{background:#f0fdf4;border:1px solid #bbf7d0;color:#15803d;}"
			+ ".notice-warning{background:#fff7ed;border:1px solid #fdba74;color:#c2410c;}"
			+ "table{width:100%;border-collapse:collapse;font-size:13px;}th,td{border:1px solid #dcdfe6;padding:10px 12px;text-align:left;vertical-align:top;}"
			+ "th{width:220px;background:#f8fafc;color:#4b5563;font-weight:600;}td{color:#1f2937;word-break:break-word;}"
			+ "@media (max-width: 900px){body{padding:12px;}.sheet{padding:16px;}.summary-grid{grid-template-columns:1fr;}}"
			+ "</style></head><body><div class=\"sheet\"><h2>" + escapeHtml(title) + "</h2>"
			+ "<div class=\"summary-grid\">" + summaryHtml + "</div>" + missingHtml
			+ "<table><tbody>" + fieldRows + "</tbody></table></div></body></html>";
	}

	private String escapeHtml(String value) {
		String safeValue = Func.toStr(value, "");
		return safeValue
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

	private String customerCreditCode(NoticeContext context) {
		return context.customer == null ? null : context.customer.getCreditCode();
	}

	private String customerContactPhone(NoticeContext context) {
		return context.customer == null ? null : context.customer.getContactPhone();
	}

	private String customerRegisteredAddress(NoticeContext context) {
		return context.customer == null ? null : context.customer.getRegisteredAddress();
	}

	private String contractLeaseAddress(NoticeContext context) {
		String roomDisplay = context == null ? null : context.roomDisplay();
		if (StringUtil.isBlank(roomDisplay) || "-".equals(roomDisplay)) {
			return "苏州市吴中区石湖西路168号科技服务中心大楼";
		}
		String normalizedRoom = roomDisplay.replace(" / ", " ");
		if (normalizedRoom.contains("苏州市") || normalizedRoom.contains("石湖西路")) {
			return normalizedRoom;
		}
		if (normalizedRoom.contains("科技服务中心")) {
			return "苏州市吴中区石湖西路168号" + normalizedRoom;
		}
		return "苏州市吴中区石湖西路168号科技服务中心大楼 " + normalizedRoom;
	}

	private String joinAddressAndPhone(String address, String phone) {
		String safeAddress = firstNotBlank(address, null);
		String safePhone = firstNotBlank(phone, null);
		if (safeAddress == null && safePhone == null) {
			return "-";
		}
		if (safeAddress == null) {
			return safePhone;
		}
		if (safePhone == null) {
			return safeAddress;
		}
		return safeAddress + " / " + safePhone;
	}

	private String joinBankInfo(String bankName, String bankAccount) {
		String safeBankName = firstNotBlank(bankName, null);
		String safeBankAccount = firstNotBlank(bankAccount, null);
		if (safeBankName == null && safeBankAccount == null) {
			return "-";
		}
		if (safeBankName == null) {
			return safeBankAccount;
		}
		if (safeBankAccount == null) {
			return safeBankName;
		}
		return safeBankName + " / " + safeBankAccount;
	}

	private String lateFeeText(Contract contract) {
		if (contract == null || contract.getLateFeeRatio() == null) {
			return "-";
		}
		Map<String, String> unitMap = Map.of(
			"percentPerDay", "%/日",
			"percentPerMonth", "%/月",
			"amountPerDay", "元/日",
			"percent_day", "%/天",
			"yuan_day", "元/天"
		);
		return formatMoney(contract.getLateFeeRatio()) + unitMap.getOrDefault(contract.getLateFeeUnit(), contract.getLateFeeUnit() == null ? "" : contract.getLateFeeUnit())
			+ (contract.getLateFeeCap() == null ? "" : "，上限 " + formatMoney(contract.getLateFeeCap()) + " 元");
	}

	private String normalizeNoticeType(String noticeType) {
		String normalized = Func.toStr(noticeType, "").trim().toLowerCase(Locale.ROOT);
		return switch (normalized) {
			case "project-approval-law" -> NOTICE_PROJECT_APPROVAL;
			default -> normalized;
		};
	}

	private String buildFileName(String title, NoticeContext context) {
		String base = String.join("-",
			filterPart(title),
			filterPart(context.contractNo()),
			filterPart(context.customerName()),
			DateUtil.format(new Date(), "yyyyMMddHHmmss")
		);
		return base + ".docx";
	}

	private String filterPart(String value) {
		if (StringUtil.isBlank(value)) {
			return "未命名";
		}
		return value.replaceAll("[\\\\/:*?\"<>|]", "-");
	}

	private String formatMoney(BigDecimal value) {
		if (value == null) {
			return "-";
		}
		return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private String moneyText(String value) {
		if (StringUtil.isBlank(value) || "-".equals(value.trim())) {
			return "0.00元";
		}
		String normalized = value.trim();
		return normalized.endsWith("元") ? normalized : normalized + "元";
	}

	private String formatAreaNumber(BigDecimal value) {
		if (value == null) {
			return "-";
		}
		return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private String formatArea(BigDecimal value) {
		if (value == null) {
			return "-";
		}
		return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "㎡";
	}

	private String formatDate(Date date) {
		return date == null ? "-" : DateUtil.format(date, DateUtil.PATTERN_DATE);
	}

	private String formatDateTime(Date date) {
		return date == null ? "-" : DateUtil.format(date, DateUtil.PATTERN_DATETIME);
	}

	private String formatChineseDate(Date date) {
		return date == null ? "    年  月  日" : DateUtil.format(date, "yyyy 年 M 月 d 日");
	}

	private Date nextPaymentDate(Contract contract) {
		if (contract == null || contract.getStartDate() == null) {
			return null;
		}
		int months = parsePaymentCycle(contract);
		LocalDate date = contract.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(months);
		return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private Date midRentEndDate(Contract contract) {
		if (contract == null || contract.getStartDate() == null || contract.getEndDate() == null) {
			return contract == null ? null : contract.getEndDate();
		}
		long months = Math.max(contractMonths(contract), 1L);
		LocalDate start = contract.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate midEnd = start.plusMonths(Math.max(months / 2, 1L)).minusDays(1);
		return Date.from(midEnd.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private Date midRentStartDate(Contract contract) {
		Date midEnd = midRentEndDate(contract);
		if (midEnd == null) {
			return null;
		}
		LocalDate date = midEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(1);
		return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
	}

	private String paymentCycleMonths(Contract contract) {
		return String.valueOf(parsePaymentCycle(contract));
	}

	private int parsePaymentCycle(Contract contract) {
		if (contract == null || StringUtil.isBlank(contract.getPaymentCycle())) {
			return 3;
		}
		String value = contract.getPaymentCycle().trim();
		if (value.matches("\\d+")) {
			return Math.max(Integer.parseInt(value), 1);
		}
		if (value.contains("月")) {
			String number = value.replaceAll("[^0-9]", "");
			return StringUtil.isBlank(number) ? 3 : Math.max(Integer.parseInt(number), 1);
		}
		if ("quarter".equalsIgnoreCase(value) || "季付".equals(value)) {
			return 3;
		}
		if ("halfYear".equalsIgnoreCase(value) || "半年付".equals(value)) {
			return 6;
		}
		if ("year".equalsIgnoreCase(value) || "年付".equals(value)) {
			return 12;
		}
		return 3;
	}

	private BigDecimal adjustRentPrice(Contract contract) {
		if (contract == null || contract.getRentPrice() == null) {
			return null;
		}
		BigDecimal rate = contract.getRentIncreaseRate();
		if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
			return contract.getRentPrice();
		}
		if ("amount".equals(contract.getRentIncreaseUnit())) {
			return contract.getRentPrice().add(rate);
		}
		return contract.getRentPrice().multiply(BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
	}

	private BigDecimal adjustMonthlyRent(Contract contract) {
		if (contract == null || contract.getMonthlyRent() == null) {
			return null;
		}
		BigDecimal rate = contract.getRentIncreaseRate();
		if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
			return contract.getMonthlyRent();
		}
		if ("amount".equals(contract.getRentIncreaseUnit())) {
			BigDecimal area = contract.getRentArea() == null ? BigDecimal.ONE : contract.getRentArea();
			return contract.getMonthlyRent().add(rate.multiply(area));
		}
		return contract.getMonthlyRent().multiply(BigDecimal.ONE.add(rate.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)));
	}

	private String moneyUpper(BigDecimal amount) {
		if (amount == null) {
			return "";
		}
		String[] cnNums = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
		String[] cnIntRadice = {"", "拾", "佰", "仟"};
		String[] cnIntUnits = {"", "万", "亿", "兆"};
		String[] cnDecUnits = {"角", "分"};
		BigDecimal money = amount.setScale(2, RoundingMode.HALF_UP).abs();
		String[] parts = money.toPlainString().split("\\.");
		String integer = parts[0];
		String decimal = parts.length > 1 ? parts[1] : "00";
		StringBuilder result = new StringBuilder();
		if ("0".equals(integer)) {
			result.append("零元");
		} else {
			int zeroCount = 0;
			for (int index = 0; index < integer.length(); index++) {
				int position = integer.length() - index - 1;
				int quotient = position / 4;
				int modulus = position % 4;
				int number = integer.charAt(index) - '0';
				if (number == 0) {
					zeroCount++;
				} else {
					if (zeroCount > 0) {
						result.append(cnNums[0]);
					}
					zeroCount = 0;
					result.append(cnNums[number]).append(cnIntRadice[modulus]);
				}
				if (modulus == 0 && zeroCount < 4) {
					result.append(cnIntUnits[quotient]);
				}
			}
			result.append("元");
		}
		int jiao = decimal.length() > 0 ? decimal.charAt(0) - '0' : 0;
		int fen = decimal.length() > 1 ? decimal.charAt(1) - '0' : 0;
		if (jiao == 0 && fen == 0) {
			result.append("整");
		} else {
			if (jiao > 0) {
				result.append(cnNums[jiao]).append(cnDecUnits[0]);
			}
			if (fen > 0) {
				result.append(cnNums[fen]).append(cnDecUnits[1]);
			}
		}
		return result.toString();
	}

	private String formatChineseDateText(String value) {
		if (StringUtil.isBlank(value)) {
			return formatChineseDate(null);
		}
		String dateValue = value.length() >= 10 ? value.substring(0, 10) : value;
		try {
			return DateUtil.format(DateUtil.parse(dateValue, DateUtil.PATTERN_DATE), "yyyy 年 M 月 d 日");
		} catch (Exception ignored) {
			return value;
		}
	}

	private long contractMonths(Contract contract) {
		if (contract == null || contract.getStartDate() == null || contract.getEndDate() == null) {
			return 0L;
		}
		LocalDate start = contract.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		LocalDate end = contract.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
		if (!end.isAfter(start)) {
			return 0L;
		}
		long months = ChronoUnit.MONTHS.between(start, end);
		return start.plusMonths(months).isBefore(end) ? months + 1 : months;
	}

	private String periodTextCompact(NoticeContext context) {
		if (context == null || context.payment == null) {
			return "-";
		}
		return formatDateCompact(context.payment.getPeriodStart()) + "-" + formatDateCompact(context.payment.getPeriodEnd());
	}

	private String formatDateCompact(Date date) {
		return date == null ? "-" : DateUtil.format(date, "yyyy.M.d");
	}

	private String firstNotBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}

	@SafeVarargs
	private final <T> T firstNotNull(T... values) {
		if (values == null) {
			return null;
		}
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private static class NoticeContext {
		private final String noticeType;
		private final ContractPayment payment;
		private final Contract contract;
		private final Customer customer;
		private final Map<String, Object> formData;

		private NoticeContext(String noticeType, ContractPayment payment, Contract contract, Customer customer, Map<String, Object> formData) {
			this.noticeType = noticeType;
			this.payment = payment;
			this.contract = contract;
			this.customer = customer;
			this.formData = formData == null ? Collections.emptyMap() : formData;
		}

		private String contractNo() {
			return contract == null ? "-" : Func.toStr(contract.getContractNo(), "-");
		}

		private String customerName() {
			return contract == null ? "-" : Func.toStr(contract.getCustomerName(), "-");
		}

		private String parkName() {
			return contract == null ? "-" : Func.toStr(contract.getParkName(), "-");
		}

		private String roomName() {
			return contract == null ? "-" : Func.toStr(contract.getRoomName(), "-");
		}

		private String buildingName() {
			return contract == null ? "-" : Func.toStr(contract.getBuildingName(), "-");
		}

		private String roomDisplay() {
			List<String> parts = new ArrayList<>();
			if (contract != null) {
				if (Func.isNotBlank(contract.getRoomName())) {
					parts.add(contract.getRoomName());
				}
				if (Func.isNotBlank(contract.getBuildingName()) && !Objects.equals(contract.getBuildingName(), contract.getRoomName())) {
					parts.add(contract.getBuildingName());
				}
			}
			if (parts.isEmpty()) {
				return "-";
			}
			return String.join(" / ", parts);
		}

		private String feeName() {
			return payment == null ? "-" : Func.toStr(payment.getFeeName(), "-");
		}

		private String periodText() {
			if (payment == null) {
				return "-";
			}
			return formatDateValue(payment.getPeriodStart()) + " 至 " + formatDateValue(payment.getPeriodEnd());
		}

		private BigDecimal unpaidAmount() {
			if (payment == null) {
				return BigDecimal.ZERO;
			}
			BigDecimal due = payment.getAmountDue() == null ? BigDecimal.ZERO : payment.getAmountDue();
			BigDecimal paid = payment.getAmountPaid() == null ? BigDecimal.ZERO : payment.getAmountPaid();
			return due.subtract(paid).max(BigDecimal.ZERO);
		}

		private long overdueDays() {
			if (payment == null || payment.getPayDeadline() == null || PAY_STATUS_PAID.equals(payment.getPayStatus())) {
				return 0L;
			}
			LocalDate deadline = payment.getPayDeadline().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			long days = ChronoUnit.DAYS.between(deadline, LocalDate.now());
			return Math.max(days, 0L);
		}

		private String contractStatusName() {
			if (contract == null) {
				return "-";
			}
			String status = Func.toStr(contract.getContractStatus(), "");
			return switch (status) {
				case "0" -> "待签约";
				case "1" -> "履约中";
				case "2" -> "已到期";
				case "3" -> "已续签";
				case "4" -> "已终止";
				case "5" -> "审批通过待盖章";
				case "6" -> "退租审批中";
				case "7" -> "退租待交接";
				case "8" -> "房屋验收中";
				default -> contract.getContractStatusName() == null ? "-" : contract.getContractStatusName();
			};
		}
	}

	private static final String PAY_STATUS_PAID = "1";

	private static String formatDateValue(Date date) {
		return date == null ? "-" : DateUtil.format(date, DateUtil.PATTERN_DATE);
	}

	private record PreviewData(Map<String, String> summary, Map<String, String> fields, List<String> missingFields) {
	}

}
