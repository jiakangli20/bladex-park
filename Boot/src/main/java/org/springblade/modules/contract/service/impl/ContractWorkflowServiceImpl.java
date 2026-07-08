/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.contract.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.task.api.Task;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springblade.modules.contract.service.IContractService;
import org.springblade.modules.contract.service.IContractWorkflowService;
import org.springblade.modules.park.mapper.RoomMapper;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springblade.plugin.workflow.process.entity.WfNotice.Type.*;

/**
 * 合同管理流程记录服务实现类
 *
 * @author Chill
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ContractWorkflowServiceImpl extends ServiceImpl<ContractWorkflowRecordMapper, ContractWorkflowRecord> implements IContractWorkflowService {

	public static final String BUSINESS_TYPE_CONTRACT_APPROVAL = "contract_approval";
	public static final String BUSINESS_TYPE_CONTRACT_PAYMENT = "contract_payment";
	public static final String BUSINESS_TYPE_CONTRACT_ROOM_REVIEW = "contract_room_review";
	public static final String BUSINESS_TYPE_CONTRACT_TERMINATION = "contract_termination";
	public static final String BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL = "contract_overdue_legal";

	private static final String DEFAULT_DEL_FLAG = "0";
	private static final String DELETED_DEL_FLAG = "1";
	private static final String STATUS_RUNNING = "running";
	private static final String STATUS_APPROVED = "approved";
	private static final String STATUS_REJECTED = "rejected";
	private static final String STATUS_CANCELED = "canceled";
	private static final String STATUS_DELETED = "deleted";
	private static final String CONTRACT_STATUS_PENDING = "0";
	private static final String CONTRACT_STATUS_ACTIVE = "1";
	private static final String CONTRACT_STATUS_EXPIRED = "2";
	private static final String CONTRACT_STATUS_TERMINATED = "4";
	private static final String CONTRACT_STATUS_PENDING_SEAL = "5";
	private static final String CONTRACT_STATUS_TERMINATION_RUNNING = "6";
	private static final String CONTRACT_STATUS_TERMINATION_HANDOVER = "7";
	private static final String CONTRACT_STATUS_ROOM_REVIEW_RUNNING = "8";
	private static final String PAY_STATUS_PAID = "1";
	private static final String PAY_STATUS_PARTIAL = "3";
	private static final String ROOM_STATUS_VACANT = "0";
	private static final String NODE_START = "流程发起";
	private static final String NODE_END = "流程结束";
	private static final Set<String> PAYMENT_AMOUNT_KEYS = Set.of(
		"amountPaid",
		"amountDue",
		"payAmount",
		"paymentAmount",
		"paidAmount",
		"actualAmount",
		"invoiceAmount",
		"a178271012553233941",
		"a178229043562386124",
		"a178229053048579216",
		"a178229053161649966",
		"实收金额",
		"付款金额",
		"缴费金额",
		"支付金额",
		"开票金额"
	);

	private static final Set<String> BUSINESS_TYPES = Set.of(
		BUSINESS_TYPE_CONTRACT_APPROVAL,
		BUSINESS_TYPE_CONTRACT_PAYMENT,
		BUSINESS_TYPE_CONTRACT_ROOM_REVIEW,
		BUSINESS_TYPE_CONTRACT_TERMINATION,
		BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL
	);

	private static final Map<String, String> PROCESS_KEY_BUSINESS_TYPE = Map.ofEntries(
		Map.entry("contract_sign", BUSINESS_TYPE_CONTRACT_APPROVAL),
		Map.entry("contract_approval", BUSINESS_TYPE_CONTRACT_APPROVAL),
		Map.entry("pay", BUSINESS_TYPE_CONTRACT_PAYMENT),
		Map.entry("invoice", BUSINESS_TYPE_CONTRACT_PAYMENT),
		Map.entry("contract_payment", BUSINESS_TYPE_CONTRACT_PAYMENT),
		Map.entry("roomreview", BUSINESS_TYPE_CONTRACT_ROOM_REVIEW),
		Map.entry("contract_room_review", BUSINESS_TYPE_CONTRACT_ROOM_REVIEW),
		Map.entry("termination", BUSINESS_TYPE_CONTRACT_TERMINATION),
		Map.entry("contract_termination", BUSINESS_TYPE_CONTRACT_TERMINATION),
		Map.entry("law", BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL),
		Map.entry("contract_overdue_legal", BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL)
	);

	private final ContractMapper contractMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;
	private final IContractNoticeService contractNoticeService;
	private final IContractService contractService;
	private final RoomMapper roomMapper;
	private final TaskService taskService;

	@Override
	public IPage<ContractWorkflowRecord> selectRecordPage(IPage<ContractWorkflowRecord> page, ContractWorkflowRecord record) {
		return page.setRecords(baseMapper.selectRecordPage(page, record).stream()
			.map(this::enrichProcessAttachments)
			.toList());
	}

	@Override
	public List<ContractWorkflowRecord> selectByContractId(Long contractId) {
		return baseMapper.selectByContractId(contractId).stream()
			.map(this::enrichProcessAttachments)
			.toList();
	}

	@Override
	public ContractWorkflowRecord selectLatest(Long contractId, String businessType) {
		return enrichProcessAttachments(baseMapper.selectLatest(contractId, businessType));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractWorkflowRecord uploadAttachment(Long recordId, Map<String, Object> payload) {
		ContractWorkflowRecord record = baseMapper.selectById(recordId);
		if (record == null || DELETED_DEL_FLAG.equals(record.getDelFlag())) {
			throw new ServiceException("退租记录不存在");
		}
		Map<String, Object> attachments = parseAttachmentJson(record.getAttachmentJson());
		List<Object> files = attachmentFiles(attachments.get("materials"));
		Map<String, Object> file = buildAttachmentFile(payload);
		files.add(file);
		attachments.put("materials", files);
		attachments.put("latestMaterial", file);

		ContractWorkflowRecord update = new ContractWorkflowRecord();
		update.setRecordId(recordId);
		update.setAttachmentJson(JsonUtil.toJson(attachments));
		update.setUpdateBy(currentUserName(null));
		update.setUpdateTime(DateUtil.now());
		baseMapper.updateById(update);
		return baseMapper.selectById(recordId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractWorkflowRecord removeAttachment(Long recordId, String fileUrl, String materialName) {
		if (StringUtil.isBlank(fileUrl)) {
			throw new ServiceException("文件地址不能为空");
		}
		ContractWorkflowRecord record = baseMapper.selectById(recordId);
		if (record == null || DELETED_DEL_FLAG.equals(record.getDelFlag())) {
			throw new ServiceException("退租记录不存在");
		}
		Map<String, Object> attachments = parseAttachmentJson(record.getAttachmentJson());
		List<Object> files = attachmentFiles(attachments.get("materials"));
		int originalSize = files.size();
		files.removeIf(file -> isSameAttachmentFile(file, fileUrl, materialName));
		if (files.size() == originalSize) {
			throw new ServiceException("资料不存在或不可删除");
		}
		if (files.isEmpty()) {
			attachments.remove("materials");
			attachments.remove("latestMaterial");
		} else {
			attachments.put("materials", files);
			attachments.put("latestMaterial", files.get(files.size() - 1));
		}
		ContractWorkflowRecord update = new ContractWorkflowRecord();
		update.setRecordId(recordId);
		update.setAttachmentJson(JsonUtil.toJson(attachments));
		update.setUpdateBy(currentUserName(null));
		update.setUpdateTime(DateUtil.now());
		baseMapper.updateById(update);
		return baseMapper.selectById(recordId);
	}

	@Override
	public boolean supports(WfNoticeDTO notice) {
		if (notice == null || notice.getProcessInstance() == null) {
			return false;
		}
		ProcessInstance processInstance = notice.getProcessInstance();
		Map<String, Object> variables = notice.getVariables();
		String businessType = resolveBusinessType(processInstance.getProcessDefinitionKey(), variables);
		if (BUSINESS_TYPES.contains(businessType)) {
			return true;
		}
		if (getLong(variables, "contractId") != null || getLong(variables, "paymentId") != null) {
			return true;
		}
		return StringUtil.isNotBlank(processInstance.getId())
			&& baseMapper.selectByProcessInsId(processInstance.getId()) != null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void businessWithNotice(WfNoticeDTO notice) {
		if (!supports(notice)) {
			return;
		}
		ContractWorkflowRecord record = resolveRecord(notice);
		if (record == null) {
			return;
		}
		WfNotice.Type type = notice.getType();
		applyNoticeState(record, notice);
		saveRecord(record, notice);
		updateBusinessState(record, type);
		addWorkflowLog(record, type, notice);
	}

	private ContractWorkflowRecord resolveRecord(WfNoticeDTO notice) {
		ProcessInstance processInstance = notice.getProcessInstance();
		Map<String, Object> variables = notice.getVariables();
		ContractWorkflowRecord record = baseMapper.selectByProcessInsId(processInstance.getId());
		if (record == null) {
			record = new ContractWorkflowRecord();
			record.setDelFlag(DEFAULT_DEL_FLAG);
			record.setCreateTime(DateUtil.now());
			record.setCreateBy(currentUserName(notice));
		}

		String businessType = firstNotBlank(record.getBusinessType(), resolveBusinessType(processInstance.getProcessDefinitionKey(), variables));
		record.setBusinessType(businessType);
		record.setBusinessKey(limit(firstNotBlank(record.getBusinessKey(), resolveBusinessKey(processInstance, variables, businessType)), 64));
		record.setProcessDefKey(limit(firstNotBlank(processInstance.getProcessDefinitionKey(), getString(variables, "processDefKey", record.getProcessDefKey())), 128));
		record.setProcessDefId(limit(processInstance.getProcessDefinitionId(), 128));
		record.setProcessName(limit(firstNotBlank(processInstance.getName(), processInstance.getProcessDefinitionName()), 200));
		record.setProcessInsId(processInstance.getId());

		Long paymentId = firstNotNull(record.getPaymentId(), getLong(variables, "paymentId"));
		record.setPaymentId(paymentId);
		Long contractId = firstNotNull(record.getContractId(), getLong(variables, "contractId"));
		if (contractId == null && BUSINESS_TYPE_CONTRACT_APPROVAL.equals(businessType)) {
			contractId = toLong(record.getBusinessKey());
		}

		ContractPayment payment = paymentId == null ? null : contractPaymentMapper.selectById(paymentId);
		if (contractId == null && payment != null) {
			contractId = payment.getContractId();
		}
		record.setContractId(contractId);

		Contract contract = contractId == null ? null : contractMapper.selectContractById(contractId);
		record.setParkId(firstNotNull(record.getParkId(), getLong(variables, "parkId"), payment == null ? null : payment.getParkId(), contract == null ? null : contract.getParkId()));
		record.setCustomerId(firstNotNull(record.getCustomerId(), getLong(variables, "customerId"), contract == null ? null : contract.getCustomerId()));
		record.setRoomIds(limit(firstNotBlank(record.getRoomIds(), getString(variables, "roomIds", null), contract == null ? null : firstNotBlank(contract.getRoomIds(), contract.getRoomId() == null ? null : String.valueOf(contract.getRoomId()))), 500));
		record.setTemplateKey(limit(firstNotBlank(record.getTemplateKey(), getString(variables, "templateKey", null), businessType), 128));
		record.setFormKey(limit(firstNotBlank(record.getFormKey(), getString(variables, "formKey", null)), 128));
		record.setFormDataJson(resolveFormDataJson(variables, record.getFormDataJson()));
		record.setAttachmentJson(resolveAttachmentJson(notice, record.getAttachmentJson(), businessType));
		return record;
	}

	private void applyNoticeState(ContractWorkflowRecord record, WfNoticeDTO notice) {
		WfNotice.Type type = notice.getType();
		Task task = notice.getTask();
		record.setProcessStatus(statusByType(type, record.getProcessStatus()));
		record.setCurrentNodeKey(task == null ? null : limit(task.getTaskDefinitionKey(), 128));
		record.setCurrentNode(limit(currentNode(type, task), 200));
		if (FINISH == type) {
			record.setApprovalTime(DateUtil.now());
			if (BUSINESS_TYPE_CONTRACT_APPROVAL.equals(record.getBusinessType()) && record.getContractId() != null) {
				Map<String, String> contractFiles = uploadContractApprovalPackage(record.getContractId());
				if (!contractFiles.isEmpty()) {
					record.setAttachmentJson(mergeAttachmentJson(record.getAttachmentJson(), contractFiles));
					record.setPrintFileUrl(limit(firstNotBlank(contractFiles.get(IContractNoticeService.NOTICE_CONTRACT_APPROVAL), record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
					return;
				}
			}
			if (BUSINESS_TYPE_CONTRACT_PAYMENT.equals(record.getBusinessType()) && record.getPaymentId() != null) {
				if (isInvoiceWorkflow(record)) {
					String invoiceUrl = uploadNotice(IContractNoticeService.NOTICE_INVOICE, record.getPaymentId(), null);
					if (StringUtil.isNotBlank(invoiceUrl)) {
						record.setAttachmentJson(mergeAttachmentJson(record.getAttachmentJson(), Map.of(IContractNoticeService.NOTICE_INVOICE, invoiceUrl)));
						record.setPrintFileUrl(limit(invoiceUrl, 500));
						return;
					}
					record.setPrintFileUrl(limit(firstNotBlank(record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
					return;
				}
				Map<String, String> paymentFiles = uploadPaymentPackage(record.getPaymentId());
				if (!paymentFiles.isEmpty()) {
					record.setAttachmentJson(mergeAttachmentJson(record.getAttachmentJson(), paymentFiles));
					record.setPrintFileUrl(limit(firstNotBlank(paymentFiles.get(IContractNoticeService.NOTICE_PAYMENT), paymentFiles.get(IContractNoticeService.NOTICE_INVOICE), record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
					return;
				}
			}
			if (BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL.equals(record.getBusinessType()) && record.getPaymentId() != null) {
				Map<String, String> noticeFiles = uploadOverduePackage(record.getPaymentId());
				if (!noticeFiles.isEmpty()) {
					record.setAttachmentJson(mergeAttachmentJson(record.getAttachmentJson(), noticeFiles));
					record.setPrintFileUrl(limit(firstNotBlank(noticeFiles.get(IContractNoticeService.NOTICE_LEGAL), record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
					return;
				}
			}
			if (BUSINESS_TYPE_CONTRACT_TERMINATION.equals(record.getBusinessType()) && record.getContractId() != null) {
				Map<String, String> noticeFiles = uploadTerminationPackage(record.getContractId(), record.getFormDataJson());
				if (!noticeFiles.isEmpty()) {
					record.setAttachmentJson(mergeAttachmentJson(record.getAttachmentJson(), noticeFiles));
					record.setPrintFileUrl(limit(firstNotBlank(noticeFiles.get(IContractNoticeService.NOTICE_TERMINATION), record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
					return;
				}
			}
			record.setPrintFileUrl(limit(firstNotBlank(record.getPrintFileUrl(), buildPrintFileUrl(record)), 500));
		}
		if (DELETE_PROCESS == type) {
			record.setDelFlag(DELETED_DEL_FLAG);
		}
	}

	private void saveRecord(ContractWorkflowRecord record, WfNoticeDTO notice) {
		record.setUpdateBy(currentUserName(notice));
		record.setUpdateTime(DateUtil.now());
		if (record.getRecordId() == null) {
			baseMapper.insert(record);
		} else {
			baseMapper.updateById(record);
		}
	}

	private void updateBusinessState(ContractWorkflowRecord record, WfNotice.Type type) {
		String businessType = record.getBusinessType();
		if (record.getContractId() == null && !BUSINESS_TYPE_CONTRACT_PAYMENT.equals(businessType)) {
			return;
		}
		if (BUSINESS_TYPE_CONTRACT_APPROVAL.equals(businessType)) {
			updateContractApprovalState(record, type);
		} else if (BUSINESS_TYPE_CONTRACT_PAYMENT.equals(businessType)) {
			if (isInvoiceWorkflow(record)) {
				return;
			}
			updatePaymentState(record, type);
		} else if (BUSINESS_TYPE_CONTRACT_TERMINATION.equals(businessType)) {
			updateTerminationState(record, type);
		} else if (BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType)) {
			updateRoomReviewState(record, type);
		}
	}

	private void updatePaymentState(ContractWorkflowRecord record, WfNotice.Type type) {
		if (FINISH != type || record.getPaymentId() == null) {
			return;
		}
		ContractPayment payment = contractPaymentMapper.selectById(record.getPaymentId());
		if (payment == null || PAY_STATUS_PAID.equals(payment.getPayStatus())) {
			return;
		}
		ContractPayment update = new ContractPayment();
		update.setPaymentId(record.getPaymentId());
		BigDecimal amountDue = payment.getAmountDue();
		BigDecimal amountPaid = firstNotNull(resolvePaymentAmount(record.getFormDataJson()), amountDue);
		if (amountPaid != null) {
			update.setAmountPaid(amountPaid);
		}
		update.setPayStatus(amountDue != null && amountPaid != null && amountPaid.compareTo(amountDue) < 0 ? PAY_STATUS_PARTIAL : PAY_STATUS_PAID);
		Date now = DateUtil.now();
		update.setPayTime(now);
		update.setUpdateBy(record.getUpdateBy());
		update.setUpdateTime(now);
		contractPaymentMapper.updateById(update);
	}

	private void updateContractApprovalState(ContractWorkflowRecord record, WfNotice.Type type) {
		if (FINISH == type) {
			updateContractStatus(record, CONTRACT_STATUS_PENDING_SEAL);
		} else if (isCanceledType(type)) {
			Contract contract = contractMapper.selectContractById(record.getContractId());
			if (contract != null && CONTRACT_STATUS_PENDING_SEAL.equals(contract.getContractStatus())) {
				updateContractStatus(record, CONTRACT_STATUS_PENDING);
			}
		}
	}

	private void updateTerminationState(ContractWorkflowRecord record, WfNotice.Type type) {
		if (START == type) {
			updateContractStatus(record, CONTRACT_STATUS_TERMINATION_RUNNING);
		} else if (FINISH == type) {
			updateContractStatus(record, CONTRACT_STATUS_TERMINATION_HANDOVER);
		} else if (isCanceledType(type)) {
			Contract contract = contractMapper.selectContractById(record.getContractId());
			if (contract != null && CONTRACT_STATUS_TERMINATION_RUNNING.equals(contract.getContractStatus())) {
				updateContractStatus(record, activeOrExpiredStatus(contract));
			}
		}
	}

	private void updateRoomReviewState(ContractWorkflowRecord record, WfNotice.Type type) {
		if (START == type) {
			updateContractStatus(record, CONTRACT_STATUS_ROOM_REVIEW_RUNNING);
		} else if (FINISH == type) {
			updateContractStatus(record, CONTRACT_STATUS_TERMINATED);
			releaseContractRooms(record);
			ensureDepositRefundPayment(record);
		} else if (isCanceledType(type)) {
			Contract contract = contractMapper.selectContractById(record.getContractId());
			if (contract != null && CONTRACT_STATUS_ROOM_REVIEW_RUNNING.equals(contract.getContractStatus())) {
				updateContractStatus(record, CONTRACT_STATUS_TERMINATION_HANDOVER);
			}
		}
	}

	private boolean isCanceledType(WfNotice.Type type) {
		return REJECT == type || RECALL == type || WITHDRAW == type || TERMINATE == type || DELETE_PROCESS == type;
	}

	private void updateContractStatus(ContractWorkflowRecord record, String contractStatus) {
		Contract update = new Contract();
		update.setContractId(record.getContractId());
		update.setContractStatus(contractStatus);
		update.setUpdateBy(record.getUpdateBy());
		update.setUpdateTime(DateUtil.now());
		contractMapper.updateById(update);
	}

	private String activeOrExpiredStatus(Contract contract) {
		Date endDate = contract == null ? null : contract.getEndDate();
		if (endDate != null && endDate.before(DateUtil.now())) {
			return CONTRACT_STATUS_EXPIRED;
		}
		return CONTRACT_STATUS_ACTIVE;
	}

	private void addWorkflowLog(ContractWorkflowRecord record, WfNotice.Type type, WfNoticeDTO notice) {
		if (record.getContractId() == null || !shouldLog(type)) {
			return;
		}
		ContractLog contractLog = new ContractLog();
		contractLog.setContractId(record.getContractId());
		contractLog.setAction(actionByType(type));
		contractLog.setActionDesc(descriptionByType(record, type));
		contractLog.setOperator(currentUserName(notice));
		contractLog.setOperateTime(DateUtil.now());
		contractLogMapper.insert(contractLog);
	}

	private String resolveBusinessType(String processDefKey, Map<String, Object> variables) {
		String businessType = getString(variables, "businessType", null);
		if (BUSINESS_TYPES.contains(businessType)) {
			return businessType;
		}
		if (StringUtil.isNotBlank(processDefKey) && PROCESS_KEY_BUSINESS_TYPE.containsKey(processDefKey)) {
			return PROCESS_KEY_BUSINESS_TYPE.get(processDefKey);
		}
		if (getLong(variables, "paymentId") != null) {
			return BUSINESS_TYPE_CONTRACT_PAYMENT;
		}
		if (getLong(variables, "contractId") != null) {
			return BUSINESS_TYPE_CONTRACT_APPROVAL;
		}
		if (StringUtil.isBlank(processDefKey)) {
			return businessType;
		}
		return PROCESS_KEY_BUSINESS_TYPE.getOrDefault(processDefKey, businessType);
	}

	private String resolveBusinessKey(ProcessInstance processInstance, Map<String, Object> variables, String businessType) {
		String businessKey = firstNotBlank(getString(variables, "businessKey", null), processInstance.getBusinessKey());
		if (StringUtil.isNotBlank(businessKey)) {
			return businessKey;
		}
		if (BUSINESS_TYPE_CONTRACT_PAYMENT.equals(businessType) || BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL.equals(businessType)) {
			return getString(variables, "paymentId", null);
		}
		return getString(variables, "contractId", null);
	}

	private String resolveFormDataJson(Map<String, Object> variables, String fallback) {
		if (variables == null || variables.isEmpty()) {
			return fallback;
		}
		Map<String, Object> snapshot = new LinkedHashMap<>();
		Object formVariable = variables.get(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE);
		if (formVariable != null && StringUtil.isNotBlank(Func.toStr(formVariable, ""))) {
			try {
				snapshot.putAll(JsonUtil.readMap(Func.toStr(formVariable, "")));
			} catch (Exception ignored) {
				snapshot.put(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE, formVariable);
			}
		}
		variables.forEach((key, value) -> {
			if (!isWorkflowInternalVariable(key) && value != null) {
				snapshot.putIfAbsent(key, value);
			}
		});
		if (snapshot.isEmpty()) {
			return firstNotBlank(fallback, JsonUtil.toJson(variables));
		}
		return JsonUtil.toJson(snapshot);
	}

	private String resolveAttachmentJson(WfNoticeDTO notice, String fallback, String businessType) {
		Map<String, Object> variables = notice == null ? null : notice.getVariables();
		Map<String, Object> attachments = parseAttachmentJson(fallback);
		List<Object> materials = attachmentFiles(attachments.get("materials"));
		appendWorkflowMaterials(materials, variables == null ? null : variables.get("attachment"), businessType);
		appendWorkflowMaterials(materials, variables == null ? null : variables.get("attachments"), businessType);
		appendFormUploadMaterials(materials, variables, businessType);
		appendProcessAttachments(materials, notice, businessType);
		if (!materials.isEmpty()) {
			attachments.put("materials", distinctMaterials(materials));
		}
		if (attachments.isEmpty()) {
			return firstNotBlank(getString(variables, "attachment", null), getString(variables, "attachments", null), fallback);
		}
		return JsonUtil.toJson(attachments);
	}

	private void appendProcessAttachments(List<Object> materials, WfNoticeDTO notice, String businessType) {
		String processInsId = notice == null || notice.getProcessInstance() == null ? null : notice.getProcessInstance().getId();
		appendProcessAttachments(materials, processInsId, businessType);
	}

	private ContractWorkflowRecord enrichProcessAttachments(ContractWorkflowRecord record) {
		if (record == null || StringUtil.isBlank(record.getProcessInsId())) {
			return record;
		}
		Map<String, Object> attachments = parseAttachmentJson(record.getAttachmentJson());
		List<Object> materials = attachmentFiles(attachments.get("materials"));
		appendProcessAttachments(materials, record.getProcessInsId(), record.getBusinessType());
		if (!materials.isEmpty()) {
			attachments.put("materials", distinctMaterials(materials));
			record.setAttachmentJson(JsonUtil.toJson(attachments));
		}
		return record;
	}

	private void appendProcessAttachments(List<Object> materials, String processInsId, String businessType) {
		if (StringUtil.isBlank(processInsId)) {
			return;
		}
		try {
			List<Attachment> attachments = taskService.getProcessInstanceAttachments(processInsId);
			if (attachments == null || attachments.isEmpty()) {
				return;
			}
			attachments.forEach(attachment -> {
				if (attachment != null && StringUtil.isNotBlank(attachment.getUrl())) {
					materials.add(buildWorkflowMaterial(attachment.getUrl(), attachment.getName(), businessType, attachment.getName()));
				}
			});
		} catch (Exception exception) {
			log.debug("同步流程审批附件失败，processInsId={}", processInsId, exception);
		}
	}

	private void appendFormUploadMaterials(List<Object> materials, Map<String, Object> variables, String businessType) {
		if (variables == null || variables.isEmpty()) {
			return;
		}
		Object formVariable = variables.get(WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE);
		Map<String, Object> formData = new LinkedHashMap<>();
		if (formVariable instanceof Map<?, ?> map) {
			map.forEach((key, value) -> formData.put(Func.toStr(key, ""), value));
		} else if (formVariable != null && StringUtil.isNotBlank(Func.toStr(formVariable, ""))) {
			try {
				formData.putAll(JsonUtil.readMap(Func.toStr(formVariable, "")));
			} catch (Exception ignored) {
				// 非标准表单快照不阻断流程记录同步
			}
		}
		variables.forEach((key, value) -> {
			if (!isWorkflowInternalVariable(key) && value != null) {
				formData.putIfAbsent(key, value);
			}
		});
		formData.forEach((key, value) -> {
			if (isUploadValue(value)) {
				appendWorkflowMaterials(materials, value, businessType, key);
			}
		});
	}

	private void appendWorkflowMaterials(List<Object> materials, Object source, String businessType) {
		appendWorkflowMaterials(materials, source, businessType, null);
	}

	@SuppressWarnings("unchecked")
	private void appendWorkflowMaterials(List<Object> materials, Object source, String businessType, String fieldKey) {
		if (source == null) {
			return;
		}
		if (source instanceof String text) {
			if (StringUtil.isBlank(text)) {
				return;
			}
			String trimmed = text.trim();
			if ((trimmed.startsWith("[") && trimmed.endsWith("]")) || (trimmed.startsWith("{") && trimmed.endsWith("}"))) {
				try {
					appendWorkflowMaterials(materials, JsonUtil.parse(trimmed, Object.class), businessType, fieldKey);
					return;
				} catch (Exception ignored) {
					// 普通文件地址继续按字符串处理
				}
			}
			if (looksLikeFileUrl(trimmed)) {
				materials.add(buildWorkflowMaterial(trimmed, null, businessType, fieldKey));
			}
			return;
		}
		if (source instanceof Collection<?> collection) {
			collection.forEach(item -> appendWorkflowMaterials(materials, item, businessType, fieldKey));
			return;
		}
		if (source instanceof Map<?, ?> map) {
			Map<String, Object> item = new LinkedHashMap<>((Map<String, Object>) map);
			String fileUrl = firstNotBlank(
				Func.toStr(firstNotNull(item.get("fileUrl"), item.get("url"), item.get("link"), item.get("value")), ""),
				""
			);
			if (StringUtil.isNotBlank(fileUrl) && looksLikeFileUrl(fileUrl)) {
				String fileName = firstNotBlank(
					Func.toStr(firstNotNull(item.get("fileName"), item.get("name"), item.get("originalName"), item.get("label")), ""),
					null
				);
				materials.add(buildWorkflowMaterial(fileUrl, fileName, businessType, fieldKey));
				return;
			}
			item.values().forEach(value -> appendWorkflowMaterials(materials, value, businessType, fieldKey));
		}
	}

	private boolean isUploadValue(Object value) {
		if (value instanceof Collection<?> collection) {
			return collection.stream().anyMatch(this::isUploadValue);
		}
		if (value instanceof Map<?, ?> map) {
			return map.containsKey("fileUrl") || map.containsKey("url") || map.containsKey("link")
				|| map.values().stream().anyMatch(this::isUploadValue);
		}
		return value instanceof String text && looksLikeFileUrl(text);
	}

	private boolean looksLikeFileUrl(String value) {
		if (StringUtil.isBlank(value)) {
			return false;
		}
		String text = value.trim();
		return text.startsWith("http://")
			|| text.startsWith("https://")
			|| text.startsWith("/")
			|| text.startsWith("blob:")
			|| text.contains("/blade-resource/")
			|| text.contains("/oss/");
	}

	private Map<String, Object> buildWorkflowMaterial(String fileUrl, String fileName, String businessType, String fieldKey) {
		Map<String, Object> file = new LinkedHashMap<>();
		String materialName = workflowMaterialName(businessType, fieldKey);
		file.put("fileUrl", fileUrl);
		file.put("fileName", firstNotBlank(fileName, fileUrl.substring(fileUrl.lastIndexOf('/') + 1), materialName));
		file.put("materialType", workflowMaterialType(businessType, fieldKey));
		file.put("materialName", materialName);
		file.put("remark", "审批流程附件");
		file.put("uploadBy", currentUserName(null));
		file.put("uploadTime", DateUtil.format(DateUtil.now(), DateUtil.PATTERN_DATETIME));
		return file;
	}

	private String workflowMaterialType(String businessType, String fieldKey) {
		String key = Func.toStr(fieldKey, "").toLowerCase();
		if (BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType) || key.contains("acceptance") || key.contains("roomreview") || key.contains("验收")) {
			return "room_acceptance";
		}
		if (key.contains("agreement") || key.contains("解除") || key.contains("补充协议")) {
			return "termination_agreement";
		}
		if (key.contains("handover") || key.contains("filelist") || key.contains("交接")) {
			return "handover_file";
		}
		return "approval";
	}

	private String workflowMaterialName(String businessType, String fieldKey) {
		String key = Func.toStr(fieldKey, "").toLowerCase();
		if (BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType) || key.contains("acceptance") || key.contains("roomreview") || key.contains("验收")) {
			return "房屋验收资料";
		}
		if (key.contains("agreement") || key.contains("解除") || key.contains("补充协议")) {
			return "租赁合同解除补充协议";
		}
		if (key.contains("handover") || key.contains("filelist") || key.contains("交接")) {
			return "退租交接资料";
		}
		return "审批资料";
	}

	private List<Object> distinctMaterials(List<Object> materials) {
		List<Object> result = new ArrayList<>();
		Set<String> seen = new java.util.LinkedHashSet<>();
		for (Object material : materials) {
			if (material instanceof Map<?, ?> map) {
				String fileUrl = Func.toStr(map.get("fileUrl"), "");
				String materialName = Func.toStr(map.get("materialName"), "");
				String key = materialName + "|" + fileUrl;
				if (StringUtil.isBlank(fileUrl) || !seen.add(key)) {
					continue;
				}
			}
			result.add(material);
		}
		return result;
	}

	private boolean isWorkflowInternalVariable(String key) {
		if (StringUtil.isBlank(key)) {
			return true;
		}
		return WfProcessConstant.TASK_VARIABLE_FORM_VARIABLE.equals(key)
			|| WfProcessConstant.TASK_VARIABLE_FORM_OPTION.equals(key)
			|| key.startsWith("wf_")
			|| "copyUser".equals(key)
			|| "assignee".equals(key);
	}

	private String statusByType(WfNotice.Type type, String fallback) {
		if (START == type || TASK_CREATE == type || TASK_COMPLETE == type) {
			return STATUS_RUNNING;
		}
		if (FINISH == type) {
			return STATUS_APPROVED;
		}
		if (REJECT == type) {
			return STATUS_REJECTED;
		}
		if (DELETE_PROCESS == type) {
			return STATUS_DELETED;
		}
		if (RECALL == type || WITHDRAW == type || TERMINATE == type) {
			return STATUS_CANCELED;
		}
		return fallback;
	}

	private String currentNode(WfNotice.Type type, Task task) {
		if (FINISH == type) {
			return NODE_END;
		}
		if (task != null && StringUtil.isNotBlank(task.getName())) {
			return task.getName();
		}
		return NODE_START;
	}

	private String buildPrintFileUrl(ContractWorkflowRecord record) {
		if (BUSINESS_TYPE_CONTRACT_APPROVAL.equals(record.getBusinessType()) && record.getContractId() != null) {
			return firstNotBlank(uploadNotice(IContractNoticeService.NOTICE_CONTRACT_APPROVAL, null, record.getContractId()), "/blade-contract/print/contract-approval/" + record.getContractId());
		}
		if (BUSINESS_TYPE_CONTRACT_PAYMENT.equals(record.getBusinessType()) && record.getPaymentId() != null) {
			if (isInvoiceWorkflow(record)) {
				return firstNotBlank(
					uploadNotice(IContractNoticeService.NOTICE_INVOICE, record.getPaymentId(), null),
					"/blade-contract/print/invoice-apply/" + record.getPaymentId()
				);
			}
			return firstNotBlank(
				uploadNotice(IContractNoticeService.NOTICE_PAYMENT, record.getPaymentId(), null),
				uploadNotice(IContractNoticeService.NOTICE_INVOICE, record.getPaymentId(), null),
				"/blade-contract/print/payment-notice/" + record.getPaymentId()
			);
		}
		if (BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL.equals(record.getBusinessType()) && record.getPaymentId() != null) {
			return firstNotBlank(uploadNotice(IContractNoticeService.NOTICE_LEGAL, record.getPaymentId(), null), "/blade-contract/print/legal-letter/" + record.getPaymentId());
		}
		if (BUSINESS_TYPE_CONTRACT_TERMINATION.equals(record.getBusinessType()) && record.getContractId() != null) {
			return firstNotBlank(uploadNotice(IContractNoticeService.NOTICE_TERMINATION, null, record.getContractId()), "/blade-contract/print/termination-approval/" + record.getContractId());
		}
		if (BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(record.getBusinessType()) && record.getContractId() != null) {
			return firstNotBlank(uploadNotice(IContractNoticeService.NOTICE_ROOM_REVIEW, null, record.getContractId()), "/blade-contract/print/room-review/" + record.getContractId());
		}
		return null;
	}

	private String uploadNotice(String noticeType, Long paymentId, Long contractId) {
		try {
			ContractNoticeFileVO file = contractNoticeService.uploadNotice(noticeType, paymentId, contractId);
			return file == null ? null : file.getFileUrl();
		} catch (Exception exception) {
			log.warn("合同流程通知文件生成失败，noticeType={}, paymentId={}, contractId={}", noticeType, paymentId, contractId, exception);
			return null;
		}
	}

	private Map<String, String> uploadContractApprovalPackage(Long contractId) {
		try {
			return contractNoticeService.uploadContractApprovalPackage(contractId);
		} catch (Exception exception) {
			log.warn("合同审批文件包生成失败，contractId={}", contractId, exception);
			return Map.of();
		}
	}

	private Map<String, String> uploadPaymentPackage(Long paymentId) {
		try {
			return contractNoticeService.uploadPaymentPackage(paymentId);
		} catch (Exception exception) {
			log.warn("付款审批文件包生成失败，paymentId={}", paymentId, exception);
			return Map.of();
		}
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

	private Map<String, String> uploadOverduePackage(Long paymentId) {
		try {
			return contractNoticeService.uploadOverduePackage(paymentId);
		} catch (Exception exception) {
			log.warn("逾期审批通知文件包生成失败，paymentId={}", paymentId, exception);
			return Map.of();
		}
	}

	private Map<String, String> uploadTerminationPackage(Long contractId, String formDataJson) {
		try {
			return contractNoticeService.uploadTerminationPackage(contractId, formDataJson);
		} catch (Exception exception) {
			log.warn("退租审批归档文件包生成失败，contractId={}", contractId, exception);
			return Map.of();
		}
	}

	private String mergeAttachmentJson(String attachmentJson, Map<String, String> generatedFiles) {
		Map<String, Object> attachments = parseAttachmentJson(attachmentJson);
		generatedFiles.forEach((key, value) -> {
			if (StringUtil.isNotBlank(value)) {
				attachments.put(key, value);
			}
		});
		return JsonUtil.toJson(attachments);
	}

	private void releaseContractRooms(ContractWorkflowRecord record) {
		if (record.getContractId() == null) {
			return;
		}
		Contract contract = contractMapper.selectContractById(record.getContractId());
		if (contract == null) {
			return;
		}
		for (Long roomId : resolveRoomIds(contract, record)) {
			roomMapper.updateRoomStatus(roomId, ROOM_STATUS_VACANT, record.getUpdateBy());
		}
	}

	private void ensureDepositRefundPayment(ContractWorkflowRecord record) {
		if (record.getContractId() == null) {
			return;
		}
		try {
			contractService.ensureDepositRefundPayment(record.getContractId());
		} catch (Exception exception) {
			log.info("房屋验收完成后未自动生成押金退还付款单，contractId={}, reason={}", record.getContractId(), exception.getMessage());
		}
	}

	private Set<Long> resolveRoomIds(Contract contract, ContractWorkflowRecord record) {
		Set<Long> roomIds = new java.util.LinkedHashSet<>();
		addRoomIds(roomIds, contract == null ? null : contract.getRoomIds());
		addRoomIds(roomIds, record == null ? null : record.getRoomIds());
		if (contract != null && contract.getRoomId() != null) {
			roomIds.add(contract.getRoomId());
		}
		return roomIds;
	}

	private void addRoomIds(Set<Long> roomIds, String source) {
		if (StringUtil.isBlank(source)) {
			return;
		}
		for (String item : source.split(",")) {
			String normalized = Func.toStr(item, "").replace("room_", "").trim();
			Long roomId = toLong(normalized);
			if (roomId != null) {
				roomIds.add(roomId);
			}
		}
	}

	private boolean shouldLog(WfNotice.Type type) {
		return START == type || FINISH == type || REJECT == type || RECALL == type
			|| WITHDRAW == type || TERMINATE == type || DELETE_PROCESS == type;
	}

	private String actionByType(WfNotice.Type type) {
		if (START == type) {
			return "workflow_start";
		}
		if (FINISH == type) {
			return "workflow_finish";
		}
		if (REJECT == type) {
			return "workflow_reject";
		}
		if (DELETE_PROCESS == type) {
			return "workflow_delete";
		}
		return "workflow_cancel";
	}

	private String descriptionByType(ContractWorkflowRecord record, WfNotice.Type type) {
		String label = businessTypeLabel(record.getBusinessType());
		if (START == type) {
			return "发起" + label;
		}
		if (FINISH == type) {
			return label + "通过";
		}
		if (REJECT == type) {
			return label + "驳回";
		}
		if (DELETE_PROCESS == type) {
			return label + "流程删除";
		}
		return label + "流程取消";
	}

	private String businessTypeLabel(String businessType) {
		if (BUSINESS_TYPE_CONTRACT_PAYMENT.equals(businessType)) {
			return "付款流程审批";
		}
		if (BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType)) {
			return "房屋验收审批";
		}
		if (BUSINESS_TYPE_CONTRACT_TERMINATION.equals(businessType)) {
			return "退租审批";
		}
		if (BUSINESS_TYPE_CONTRACT_OVERDUE_LEGAL.equals(businessType)) {
			return "逾期律师函审批";
		}
		return "合同审批";
	}

	private BigDecimal resolvePaymentAmount(String formDataJson) {
		if (StringUtil.isBlank(formDataJson)) {
			return null;
		}
		try {
			return findPaymentAmount(JsonUtil.readMap(formDataJson));
		} catch (Exception exception) {
			log.debug("解析付款审批表单金额失败，formDataJson={}", formDataJson, exception);
			return null;
		}
	}

	private BigDecimal findPaymentAmount(Object source) {
		if (source instanceof Map<?, ?> map) {
			BigDecimal namedAmount = findNamedPaymentAmount(map);
			if (namedAmount != null) {
				return namedAmount;
			}
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (isPaymentAmountKey(Func.toStr(entry.getKey(), ""))) {
					BigDecimal amount = toBigDecimal(entry.getValue());
					if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
						return amount;
					}
				}
			}
			for (Object value : map.values()) {
				BigDecimal amount = findPaymentAmount(value);
				if (amount != null) {
					return amount;
				}
			}
		} else if (source instanceof Collection<?> collection) {
			for (Object item : collection) {
				BigDecimal amount = findPaymentAmount(item);
				if (amount != null) {
					return amount;
				}
			}
		}
		return null;
	}

	private BigDecimal findNamedPaymentAmount(Map<?, ?> map) {
		String label = firstNotBlank(
			Func.toStr(map.get("label"), ""),
			Func.toStr(map.get("title"), ""),
			Func.toStr(map.get("name"), ""),
			Func.toStr(map.get("field"), ""),
			Func.toStr(map.get("prop"), "")
		);
		if (!isPaymentAmountKey(label)) {
			return null;
		}
		BigDecimal amount = toBigDecimal(firstNotNull(
			map.get("value"),
			map.get("modelValue"),
			map.get("defaultValue")
		));
		return amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? amount : null;
	}

	private boolean isPaymentAmountKey(String key) {
		if (StringUtil.isBlank(key)) {
			return false;
		}
		String normalizedKey = key.replace("：", "")
			.replace(":", "")
			.replace(" ", "")
			.trim();
		for (String amountKey : PAYMENT_AMOUNT_KEYS) {
			if (normalizedKey.equalsIgnoreCase(amountKey)) {
				return true;
			}
		}
		return false;
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		String text = Func.toStr(value, "")
			.replace(",", "")
			.replace("，", "")
			.replace("￥", "")
			.replace("元", "")
			.trim();
		if (StringUtil.isBlank(text)) {
			return null;
		}
		try {
			return new BigDecimal(text);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private Map<String, Object> parseAttachmentJson(String attachmentJson) {
		if (StringUtil.isBlank(attachmentJson)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> attachment = JsonUtil.readMap(attachmentJson);
			return attachment == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attachment);
		} catch (Exception ignored) {
			return new LinkedHashMap<>();
		}
	}

	private List<Object> attachmentFiles(Object value) {
		List<Object> files = new ArrayList<>();
		if (value instanceof Collection<?> collection) {
			files.addAll(collection);
		} else if (value instanceof Map<?, ?> map) {
			files.add(new LinkedHashMap<>(map));
		}
		return files;
	}

	private Map<String, Object> buildAttachmentFile(Map<String, Object> payload) {
		String fileUrl = firstNotBlank(
			getString(payload, "fileUrl", null),
			getString(payload, "url", null),
			getString(payload, "link", null)
		);
		if (StringUtil.isBlank(fileUrl)) {
			throw new ServiceException("文件地址不能为空");
		}
		Map<String, Object> file = new LinkedHashMap<>();
		file.put("fileUrl", fileUrl);
		file.put("fileName", firstNotBlank(getString(payload, "fileName", null), getString(payload, "name", null), "退租资料"));
		file.put("materialType", firstNotBlank(getString(payload, "materialType", null), getString(payload, "category", null), "other"));
		file.put("materialName", firstNotBlank(getString(payload, "materialName", null), getString(payload, "categoryName", null), getString(payload, "category", null), "退租资料"));
		file.put("remark", getString(payload, "remark", ""));
		file.put("uploadBy", currentUserName(null));
		file.put("uploadTime", DateUtil.format(DateUtil.now(), DateUtil.PATTERN_DATETIME));
		return file;
	}

	private boolean isSameAttachmentFile(Object source, String fileUrl, String materialName) {
		if (!(source instanceof Map<?, ?> map)) {
			return false;
		}
		String sourceUrl = firstNotBlank(Func.toStr(map.get("fileUrl"), null), Func.toStr(map.get("url"), null), Func.toStr(map.get("link"), null));
		if (!StringUtil.equals(sourceUrl, fileUrl)) {
			return false;
		}
		if (StringUtil.isBlank(materialName)) {
			return true;
		}
		String sourceName = firstNotBlank(Func.toStr(map.get("materialName"), null), Func.toStr(map.get("categoryName"), null), Func.toStr(map.get("category"), null));
		return StringUtil.isBlank(sourceName) || StringUtil.equals(sourceName, materialName);
	}

	private String currentUserName(WfNoticeDTO notice) {
		String userName = AuthUtil.getUserName();
		if (StringUtil.isNotBlank(userName)) {
			return userName;
		}
		WfUser fromUser = notice == null ? null : notice.getFromUser();
		if (fromUser != null && StringUtil.isNotBlank(fromUser.getName())) {
			return fromUser.getName();
		}
		WfUser startUser = notice == null ? null : notice.getStartUser();
		if (startUser != null && StringUtil.isNotBlank(startUser.getName())) {
			return startUser.getName();
		}
		return "system";
	}

	private Long getLong(Map<String, Object> variables, String key) {
		return toLong(getString(variables, key, null));
	}

	private Long toLong(String value) {
		if (StringUtil.isBlank(value)) {
			return null;
		}
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private String getString(Map<String, Object> variables, String key, String defaultValue) {
		if (variables == null || variables.get(key) == null) {
			return defaultValue;
		}
		return Func.toStr(variables.get(key), defaultValue);
	}

	@SafeVarargs
	private final <T> T firstNotNull(T... values) {
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}

	private String limit(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

}
