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
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Attachment;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractExpiryRuleMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractExpiryRule;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractStatsVO;
import org.springblade.modules.contract.service.IContractService;
import org.springblade.modules.park.mapper.RoomMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 合同主档服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService {

	private static final String DEFAULT_DEL_FLAG = "0";
	private static final String STATUS_PENDING = "0";
	private static final String STATUS_ACTIVE = "1";
	private static final String STATUS_RENEWED = "3";
	private static final String STATUS_TERMINATED = "4";
	private static final String STATUS_PENDING_SEAL = "5";
	private static final String STATUS_TERMINATION_HANDOVER = "7";
	private static final String STATUS_ROOM_REVIEW_RUNNING = "8";
	private static final String PROCESS_STATUS_APPROVED = "approved";
	private static final String BUSINESS_TYPE_CONTRACT_TERMINATION = "contract_termination";
	private static final String BUSINESS_TYPE_CONTRACT_PAYMENT = "contract_payment";
	private static final String BUSINESS_TYPE_CONTRACT_ROOM_REVIEW = "contract_room_review";
	private static final String FEE_TYPE_DEPOSIT_REFUND = "deposit_refund";
	private static final String PAY_STATUS_UNPAID = "0";
	private static final String PAY_STATUS_PAID = "1";
	private static final String REMIND_STATUS_REMINDED = "1";
	private static final String ROOM_STATUS_VACANT = "0";

	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;
	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
	private final ContractExpiryRuleMapper contractExpiryRuleMapper;
	private final RoomMapper roomMapper;
	private final TaskService taskService;

	@Override
	public IPage<Contract> selectContractPage(IPage<Contract> page, Contract contract) {
		return page.setRecords(baseMapper.selectContractPage(page, contract));
	}

	@Override
	public Contract selectContractById(Long contractId) {
		return baseMapper.selectContractById(contractId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitContract(Contract contract) {
		if (Func.isEmpty(contract.getContractId())) {
			return createContract(contract);
		}
		return updateContract(contract);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteContracts(String ids) {
		List<Long> idList = Func.toLongList(ids);
		if (idList.isEmpty()) {
			return false;
		}
		return baseMapper.deleteContractByIds(idList) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean renewContract(Long contractId, Contract newContract) {
		Contract oldContract = requireContract(contractId);
		oldContract.setContractStatus(STATUS_RENEWED);
		oldContract.setUpdateBy(currentUserName());
		oldContract.setUpdateTime(DateUtil.now());
		baseMapper.updateById(oldContract);
		addLog(contractId, "renew", "发起续签");

		newContract.setContractId(null);
		newContract.setParkId(oldContract.getParkId());
		newContract.setParentContractId(contractId);
		newContract.setContractStatus(STATUS_PENDING);
		boolean result = createContract(newContract);
		addLog(newContract.getContractId(), "create", "续签新建合同，源合同ID：" + contractId);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean terminateContract(Long contractId) {
		Contract contract = requireContract(contractId);
		contract.setContractStatus(STATUS_TERMINATED);
		contract.setUpdateBy(currentUserName());
		contract.setUpdateTime(DateUtil.now());
		boolean result = baseMapper.updateById(contract) > 0;
		if (result) {
			addLog(contractId, "terminate", "合同终止");
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean uploadSignedContract(Long contractId, Contract contract) {
		Contract existing = requireContract(contractId);
		if (!STATUS_PENDING_SEAL.equals(existing.getContractStatus())) {
			throw new ServiceException("合同审批通过后才可以上传盖章合同");
		}
		if (Func.isBlank(contract.getContractFileUrl())) {
			throw new ServiceException("请上传盖章合同文件");
		}
		Contract update = new Contract();
		update.setContractId(contractId);
		update.setContractFileUrl(contract.getContractFileUrl());
		update.setContractStatus(STATUS_ACTIVE);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		boolean result = baseMapper.updateById(update) > 0;
		if (result) {
			if (contractPaymentMapper.selectByContractId(contractId).isEmpty()) {
				existing.setContractStatus(STATUS_ACTIVE);
				generatePaymentPlan(existing);
			}
			addLog(contractId, "signed", "上传盖章合同，合同生效");
		}
		return result;
	}

	@Override
	public IPage<Contract> selectExpiringPage(IPage<Contract> page, Contract contract) {
		return page.setRecords(baseMapper.selectExpiringPage(page, contract));
	}

	@Override
	public ContractStatsVO stats(Long parkId) {
		ContractStatsVO stats = baseMapper.selectStats(parkId);
		return normalizeStats(stats);
	}

	private ContractStatsVO normalizeStats(ContractStatsVO stats) {
		ContractStatsVO normalized = stats == null ? new ContractStatsVO() : stats;
		if (normalized.getTotalCount() == null) {
			normalized.setTotalCount(0L);
		}
		if (normalized.getPendingCount() == null) {
			normalized.setPendingCount(0L);
		}
		if (normalized.getActiveCount() == null) {
			normalized.setActiveCount(0L);
		}
		if (normalized.getExpiredCount() == null) {
			normalized.setExpiredCount(0L);
		}
		if (normalized.getRenewedCount() == null) {
			normalized.setRenewedCount(0L);
		}
		if (normalized.getTerminatedCount() == null) {
			normalized.setTerminatedCount(0L);
		}
		if (normalized.getMonthlyRentTotal() == null) {
			normalized.setMonthlyRentTotal(BigDecimal.ZERO);
		}
		if (normalized.getDepositTotal() == null) {
			normalized.setDepositTotal(BigDecimal.ZERO);
		}
		return normalized;
	}

	@Override
	public IPage<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, ContractPayment payment) {
		return page.setRecords(contractPaymentMapper.selectPaymentPage(page, payment));
	}

	@Override
	public List<ContractPayment> selectPaymentByContractId(Long contractId) {
		return contractPaymentMapper.selectByContractId(contractId);
	}

	@Override
	public ContractPayment getDepositRefundPayment(Long contractId) {
		requireContract(contractId);
		return findDepositRefundPayment(contractId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractPayment ensureDepositRefundPayment(Long contractId) {
		Contract contract = requireContract(contractId);
		if (!STATUS_TERMINATED.equals(contract.getContractStatus())) {
			throw new ServiceException("房屋验收完成后才可以发起押金退还");
		}
		if (contract.getDeposit() == null || contract.getDeposit().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("该合同未配置可退保证金");
		}
		ContractPayment existing = findDepositRefundPayment(contractId);
		if (existing != null) {
			return existing;
		}
		validateDepositRefundMaterials(contractId);
		Date now = DateUtil.now();
		ContractPayment payment = new ContractPayment();
		payment.setContractId(contractId);
		payment.setDirection("payable");
		payment.setFeeType(FEE_TYPE_DEPOSIT_REFUND);
		payment.setFeeName("押金退还");
		payment.setPeriodStart(now);
		payment.setPeriodEnd(now);
		payment.setAmountDue(contract.getDeposit());
		payment.setAmountPaid(BigDecimal.ZERO);
		payment.setPayDeadline(now);
		payment.setPayStatus(PAY_STATUS_UNPAID);
		payment.setParkId(contract.getParkId());
		payment.setRemark("退租押金退还付款单");
		payment.setCreateBy(currentUserName());
		payment.setCreateTime(now);
		contractPaymentMapper.insert(payment);
		addLog(contractId, "deposit_refund", "生成押金退还付款单");
		return contractPaymentMapper.selectById(payment.getPaymentId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractWorkflowRecord offlineRoomReview(Long contractId, Map<String, Object> formData) {
		Contract contract = requireContract(contractId);
		if (!canOfflineRoomReview(contract.getContractStatus())) {
			throw new ServiceException("退租审批通过后才可以登记验收情况");
		}
		Date now = DateUtil.now();
		Map<String, Object> snapshot = normalizeOfflineForm(formData);
		snapshot.putIfAbsent("acceptanceDate", DateUtil.format(now, DateUtil.PATTERN_DATE));
		snapshot.putIfAbsent("acceptanceResult", "验收通过");
		if (Func.isBlank(textValue(snapshot, "returnDate"))) {
			snapshot.put("returnDate", snapshot.get("acceptanceDate"));
		}
		if (Func.isBlank(textValue(snapshot, "handoverResult"))) {
			snapshot.put("handoverResult", firstNotBlank(textValue(snapshot, "acceptanceSituation"), textValue(snapshot, "acceptanceResult")));
		}

		ContractWorkflowRecord record = new ContractWorkflowRecord();
		record.setParkId(contract.getParkId());
		record.setBusinessType(BUSINESS_TYPE_CONTRACT_ROOM_REVIEW);
		record.setBusinessKey(String.valueOf(contractId));
		record.setProcessDefKey("offline-room-review");
		record.setProcessName("线下房屋验收");
		record.setProcessStatus(PROCESS_STATUS_APPROVED);
		record.setCurrentNodeKey("offline_room_review");
		record.setCurrentNode("线下验收完成");
		record.setContractId(contractId);
		record.setCustomerId(contract.getCustomerId());
		record.setRoomIds(resolveContractRoomIds(contract));
		record.setTemplateKey("room-review");
		record.setFormKey("return");
		record.setFormDataJson(JsonUtil.toJson(snapshot));
		record.setAttachmentJson(JsonUtil.toJson(resolveAttachmentSnapshot(snapshot)));
		record.setPrintFileUrl("/blade-contract/print/room-review/" + contractId);
		record.setApprovalTime(now);
		record.setRemark(limitText(firstNotBlank(textValue(snapshot, "acceptanceSituation"), textValue(snapshot, "remark"), "线下验收登记"), 500));
		record.setDelFlag(DEFAULT_DEL_FLAG);
		record.setCreateBy(currentUserName());
		record.setCreateTime(now);
		contractWorkflowRecordMapper.insert(record);

		Contract update = new Contract();
		update.setContractId(contractId);
		update.setContractStatus(STATUS_TERMINATED);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		baseMapper.updateById(update);
		releaseRooms(contract);
		addLog(contractId, "room_review", "线下房屋验收完成");
		return contractWorkflowRecordMapper.selectById(record.getRecordId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractPayment offlineDepositRefund(Long contractId, Map<String, Object> formData) {
		Contract contract = requireContract(contractId);
		if (!STATUS_TERMINATED.equals(contract.getContractStatus())) {
			throw new ServiceException("房屋验收完成后才可以上传支付凭证");
		}
		ContractPayment payment = findDepositRefundPayment(contractId);
		if (payment == null) {
			throw new ServiceException("请先发起付款申请");
		}
		if (PAY_STATUS_PAID.equals(payment.getPayStatus())) {
			throw new ServiceException("该押金退还已完成");
		}
		if (!PROCESS_STATUS_APPROVED.equals(payment.getPaymentApprovalStatus())) {
			throw new ServiceException("付款申请审批完成后才可以上传支付凭证");
		}
		validateDepositRefundMaterials(contractId);
		Date now = DateUtil.now();
		Map<String, Object> snapshot = normalizeOfflineForm(formData);
		BigDecimal amountPaid = decimalValue(snapshot.get("amountPaid"), payment.getAmountDue());
		if (amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("支付金额必须大于0");
		}
		snapshot.putIfAbsent("amountPaid", amountPaid);
		snapshot.putIfAbsent("payTime", DateUtil.format(now, DateUtil.PATTERN_DATETIME));

		ContractPayment update = new ContractPayment();
		update.setPaymentId(payment.getPaymentId());
		update.setAmountPaid(amountPaid);
		update.setPayStatus(PAY_STATUS_PAID);
		update.setPayTime(now);
		update.setRemark(limitText(firstNotBlank(textValue(snapshot, "remark"), textValue(snapshot, "paymentRemark"), "线下押金退还"), 500));
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		contractPaymentMapper.updateById(update);

		ContractWorkflowRecord record = new ContractWorkflowRecord();
		record.setParkId(contract.getParkId());
		record.setBusinessType(BUSINESS_TYPE_CONTRACT_PAYMENT);
		record.setBusinessKey(String.valueOf(payment.getPaymentId()));
		record.setProcessDefKey("offline-deposit-refund");
		record.setProcessName("线下押金退还");
		record.setProcessStatus(PROCESS_STATUS_APPROVED);
		record.setCurrentNodeKey("offline_deposit_refund");
		record.setCurrentNode("线下支付完成");
		record.setContractId(contractId);
		record.setPaymentId(payment.getPaymentId());
		record.setCustomerId(contract.getCustomerId());
		record.setRoomIds(resolveContractRoomIds(contract));
		record.setTemplateKey("payment-notice");
		record.setFormKey("pay");
		record.setFormDataJson(JsonUtil.toJson(snapshot));
		record.setAttachmentJson(JsonUtil.toJson(resolveAttachmentSnapshot(snapshot)));
		record.setPrintFileUrl("/blade-contract/print/payment-notice/" + payment.getPaymentId());
		record.setApprovalTime(now);
		record.setRemark(limitText(firstNotBlank(textValue(snapshot, "remark"), textValue(snapshot, "paymentRemark"), "线下支付凭证登记"), 500));
		record.setDelFlag(DEFAULT_DEL_FLAG);
		record.setCreateBy(currentUserName());
		record.setCreateTime(now);
		contractWorkflowRecordMapper.insert(record);

		addLog(contractId, "deposit_refund", "上传押金退还支付凭证");
		return contractPaymentMapper.selectById(payment.getPaymentId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean confirmPayment(Long paymentId, ContractPayment payment) {
		ContractPayment existing = contractPaymentMapper.selectById(paymentId);
		if (existing == null) {
			throw new ServiceException("缴费记录不存在");
		}
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setAmountPaid(payment.getAmountPaid());
		update.setRemark(payment.getRemark());
		update.setPayStatus(PAY_STATUS_PAID);
		update.setPayTime(DateUtil.now());
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			addLog(existing.getContractId(), "payment", "确认缴费，费用：" + existing.getFeeName());
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remindPayment(Long paymentId) {
		ContractPayment existing = contractPaymentMapper.selectById(paymentId);
		if (existing == null) {
			throw new ServiceException("缴费记录不存在");
		}
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setRemindStatus(REMIND_STATUS_REMINDED);
		update.setRemindTime(DateUtil.now());
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			addLog(existing.getContractId(), "remind", "发起催缴提醒");
		}
		return result;
	}

	@Override
	public List<ContractLog> selectLogByContractId(Long contractId) {
		return contractLogMapper.selectByContractId(contractId);
	}

	private boolean createContract(Contract contract) {
		validateNewRelations(contract);
		Date now = DateUtil.now();
		if (Func.isBlank(contract.getContractNo())) {
			contract.setContractNo(generateContractNo());
		}
		if (Func.isBlank(contract.getContractStatus())) {
			contract.setContractStatus(STATUS_PENDING);
		}
		if (Func.isBlank(contract.getPaymentCycle())) {
			contract.setPaymentCycle("monthly");
		}
		contract.setRenewalRemindDays(resolveRenewalRemindDays(contract));
		contract.setDelFlag(DEFAULT_DEL_FLAG);
		contract.setCreateBy(currentUserName());
		contract.setCreateTime(now);
		boolean result = save(contract);
		if (result) {
			addLog(contract.getContractId(), "create", "创建合同");
		}
		return result;
	}

	private boolean updateContract(Contract contract) {
		Contract oldContract = requireContract(contract.getContractId());
		validateNewRelations(contract);
		contract.setParkId(oldContract.getParkId());
		contract.setRenewalRemindDays(resolveRenewalRemindDays(contract));
		contract.setDelFlag(DEFAULT_DEL_FLAG);
		contract.setUpdateBy(currentUserName());
		contract.setUpdateTime(DateUtil.now());
		boolean result = updateById(contract);
		if (result) {
			addLog(contract.getContractId(), "update", "更新合同信息");
		}
		return result;
	}

	private Contract requireContract(Long contractId) {
		Contract contract = selectContractById(contractId);
		if (contract == null) {
			throw new ServiceException("合同不存在");
		}
		return contract;
	}

	private ContractPayment findDepositRefundPayment(Long contractId) {
		return contractPaymentMapper.selectByContractId(contractId).stream()
			.filter(payment -> FEE_TYPE_DEPOSIT_REFUND.equals(payment.getFeeType()))
			.findFirst()
			.orElse(null);
	}

	private void validateNewRelations(Contract contract) {
		contract.setCustomerName(Func.toStr(contract.getCustomerName(), "").trim());
		if (Func.isBlank(contract.getCustomerName())) {
			throw new ServiceException("请输入乙方企业");
		}
		if (contract.getParkId() == null || contract.getParkId() <= 0) {
			throw new ServiceException("请选择所属园区");
		}
		if (emptyCount(baseMapper.existsPark(contract.getParkId()))) {
			throw new ServiceException("所属园区不存在");
		}
		if (contract.getCustomerId() != null) {
			validateCustomer(contract.getCustomerId());
		}
		if (contract.getBuildingId() != null && emptyCount(baseMapper.existsBuilding(contract.getBuildingId()))) {
			throw new ServiceException("楼宇不存在");
		}
		if (contract.getRoomId() != null && emptyCount(baseMapper.existsRoom(contract.getRoomId()))) {
			throw new ServiceException("房源不存在");
		}
	}

	private Integer resolveRenewalRemindDays(Contract contract) {
		List<Long> buildingIds = resolveContractBuildingIds(contract);
		if (buildingIds.isEmpty()) {
			return null;
		}
		return contractExpiryRuleMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<ContractExpiryRule>lambdaQuery()
				.eq(ContractExpiryRule::getDelFlag, DEFAULT_DEL_FLAG))
			.stream()
			.filter(rule -> matchesAnyBuilding(rule, buildingIds))
			.map(ContractExpiryRule::getRemindDays)
			.filter(Func::isNotEmpty)
			.max(Integer::compareTo)
			.orElse(null);
	}

	private List<Long> resolveContractBuildingIds(Contract contract) {
		List<Long> buildingIds = new java.util.ArrayList<>();
		if (contract == null) {
			return buildingIds;
		}
		if (contract.getBuildingId() != null) {
			buildingIds.add(contract.getBuildingId());
		}
		Func.toLongList(Func.toStr(contract.getBuildingIds(), "")).stream()
			.filter(Func::isNotEmpty)
			.filter(id -> !buildingIds.contains(id))
			.forEach(buildingIds::add);
		return buildingIds;
	}

	private boolean matchesAnyBuilding(ContractExpiryRule rule, List<Long> buildingIds) {
		if (rule == null || Func.isBlank(rule.getBuildingIds())) {
			return false;
		}
		List<Long> ruleBuildingIds = Func.toLongList(rule.getBuildingIds());
		return buildingIds.stream().anyMatch(ruleBuildingIds::contains);
	}

	private void validateCustomer(Long customerId) {
		if (emptyCount(baseMapper.existsSettledCustomer(customerId))) {
			throw new ServiceException("客户不存在或尚未完成入驻审批");
		}
	}

	private boolean emptyCount(Long count) {
		return count == null || count == 0;
	}

	private boolean canOfflineRoomReview(String contractStatus) {
		return STATUS_TERMINATION_HANDOVER.equals(contractStatus)
			|| STATUS_ROOM_REVIEW_RUNNING.equals(contractStatus)
			|| STATUS_TERMINATED.equals(contractStatus);
	}

	private void validateDepositRefundMaterials(Long contractId) {
		ContractWorkflowRecord terminationRecord = contractWorkflowRecordMapper.selectLatest(contractId, BUSINESS_TYPE_CONTRACT_TERMINATION);
		if (terminationRecord == null || !PROCESS_STATUS_APPROVED.equals(terminationRecord.getProcessStatus())) {
			throw new ServiceException("退租审批完成后才可以发起付款申请");
		}
		List<Map<String, Object>> attachmentSources = new java.util.ArrayList<>();
		attachmentSources.add(resolveWorkflowAttachments(terminationRecord));
		ContractWorkflowRecord roomReviewRecord = contractWorkflowRecordMapper.selectLatest(contractId, BUSINESS_TYPE_CONTRACT_ROOM_REVIEW);
		if (roomReviewRecord != null) {
			attachmentSources.add(resolveWorkflowAttachments(roomReviewRecord));
			if (Func.isNotBlank(roomReviewRecord.getPrintFileUrl())) {
				attachmentSources.add(Map.of("roomReviewFileUrl", roomReviewRecord.getPrintFileUrl()));
			}
		}
		List<String> missingMaterials = missingDepositRefundMaterials(attachmentSources);
		if (!missingMaterials.isEmpty()) {
			throw new ServiceException("请先补齐退租资料：" + String.join("、", missingMaterials));
		}
	}

	private Map<String, Object> resolveWorkflowAttachments(ContractWorkflowRecord record) {
		Map<String, Object> attachments = parseAttachmentJson(record == null ? null : record.getAttachmentJson());
		if (record == null || Func.isBlank(record.getProcessInsId())) {
			return attachments;
		}
		List<Object> materials = new java.util.ArrayList<>(materialFiles(attachments.get("materials")));
		try {
			List<Attachment> processAttachments = taskService.getProcessInstanceAttachments(record.getProcessInsId());
			if (processAttachments != null) {
				processAttachments.forEach(attachment -> {
					if (attachment != null && Func.isNotBlank(attachment.getUrl())) {
						materials.add(buildWorkflowMaterial(attachment, record.getBusinessType()));
					}
				});
			}
		} catch (Exception ignored) {
			// 流程附件快照失败不影响已有业务附件校验
		}
		if (!materials.isEmpty()) {
			attachments.put("materials", materials);
		}
		return attachments;
	}

	private Map<String, Object> buildWorkflowMaterial(Attachment attachment, String businessType) {
		Map<String, Object> file = new LinkedHashMap<>();
		String materialName = BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType) ? "房屋验收资料" : "审批资料";
		String fileUrl = attachment.getUrl();
		file.put("fileUrl", fileUrl);
		file.put("fileName", Func.isBlank(attachment.getName()) ? fileUrl.substring(fileUrl.lastIndexOf('/') + 1) : attachment.getName());
		file.put("materialType", BUSINESS_TYPE_CONTRACT_ROOM_REVIEW.equals(businessType) ? "room_acceptance" : "approval");
		file.put("materialName", materialName);
		file.put("remark", "审批流程附件");
		return file;
	}

	private List<String> missingDepositRefundMaterials(List<Map<String, Object>> attachmentSources) {
		List<String> missing = new java.util.ArrayList<>();
		if (!hasMaterial(attachmentSources, List.of("room_acceptance"), List.of("房屋验收", "房屋验收资料"), List.of("acceptanceFileUrl", "roomAcceptanceFiles", "roomReviewFileUrl", "room-review"))) {
			missing.add("房屋验收");
		}
		if (!hasMaterial(attachmentSources, List.of("termination_agreement", "signed_termination_agreement"), List.of("租赁合同解除补充协议", "已盖章解除补充协议"), List.of("termination-agreement", "terminationAgreementUrl"))) {
			missing.add("租赁合同解除补充协议");
		}
		if (!hasMaterial(attachmentSources, List.of("handover_file"), List.of("退租交接资料"), List.of("fileList", "handoverFileUrl"))) {
			missing.add("退租交接资料");
		}
		return missing;
	}

	private boolean hasMaterial(List<Map<String, Object>> attachmentSources, List<String> materialTypes, List<String> materialNames, List<String> attachmentKeys) {
		return attachmentSources.stream().anyMatch(attachments -> hasMaterial(attachments, materialTypes, materialNames, attachmentKeys));
	}

	private boolean hasMaterial(Map<String, Object> attachments, List<String> materialTypes, List<String> materialNames, List<String> attachmentKeys) {
		for (String key : attachmentKeys) {
			if (hasAttachmentValue(attachments.get(key))) {
				return true;
			}
		}
		for (Map<String, Object> file : materialFiles(attachments.get("materials"))) {
			String materialType = textValue(file, "materialType");
			String category = textValue(file, "category");
			String materialName = firstNotBlank(textValue(file, "materialName"), textValue(file, "categoryName"), category);
			if (materialTypes.contains(materialType) || materialTypes.contains(category)) {
				return true;
			}
			if (materialNames.stream().anyMatch(materialName::contains)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasAttachmentValue(Object value) {
		if (value == null) {
			return false;
		}
		if (value instanceof Collection<?> collection) {
			return !collection.isEmpty();
		}
		if (value instanceof Map<?, ?> map) {
			return !map.isEmpty();
		}
		return Func.isNotBlank(Func.toStr(value, ""));
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> materialFiles(Object value) {
		List<Map<String, Object>> files = new java.util.ArrayList<>();
		if (value instanceof Collection<?> collection) {
			collection.forEach(item -> {
				if (item instanceof Map<?, ?> map) {
					files.add(new LinkedHashMap<>((Map<String, Object>) map));
				}
			});
		} else if (value instanceof Map<?, ?> map) {
			files.add(new LinkedHashMap<>((Map<String, Object>) map));
		}
		return files;
	}

	private Map<String, Object> parseAttachmentJson(String attachmentJson) {
		if (Func.isBlank(attachmentJson)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> attachment = JsonUtil.readMap(attachmentJson);
			return attachment == null ? new LinkedHashMap<>() : new LinkedHashMap<>(attachment);
		} catch (Exception ignored) {
			return new LinkedHashMap<>();
		}
	}

	private Map<String, Object> normalizeOfflineForm(Map<String, Object> formData) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		if (formData != null) {
			snapshot.putAll(formData);
		}
		snapshot.putIfAbsent("operator", currentUserName());
		snapshot.putIfAbsent("operateTime", DateUtil.format(DateUtil.now(), DateUtil.PATTERN_DATETIME));
		return snapshot;
	}

	private Map<String, Object> resolveAttachmentSnapshot(Map<String, Object> snapshot) {
		Map<String, Object> attachments = new LinkedHashMap<>();
		putIfPresent(attachments, "acceptanceFileUrl", snapshot.get("acceptanceFileUrl"));
		putIfPresent(attachments, "acceptanceFileName", snapshot.get("acceptanceFileName"));
		putIfPresent(attachments, "paymentVoucherUrl", snapshot.get("paymentVoucherUrl"));
		putIfPresent(attachments, "paymentVoucherName", snapshot.get("paymentVoucherName"));
		putIfPresent(attachments, "fileList", snapshot.get("fileList"));
		return attachments;
	}

	private void putIfPresent(Map<String, Object> target, String key, Object value) {
		if (value != null && Func.isNotBlank(Func.toStr(value, ""))) {
			target.put(key, value);
		}
	}

	private BigDecimal decimalValue(Object value, BigDecimal defaultValue) {
		if (value == null || Func.isBlank(Func.toStr(value, ""))) {
			return defaultValue == null ? BigDecimal.ZERO : defaultValue;
		}
		try {
			return new BigDecimal(Func.toStr(value, "0"));
		} catch (NumberFormatException exception) {
			throw new ServiceException("支付金额格式不正确");
		}
	}

	private String textValue(Map<String, Object> source, String key) {
		return source == null ? "" : Func.toStr(source.get(key), "");
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (Func.isNotBlank(value)) {
				return value;
			}
		}
		return "";
	}

	private String limitText(String text, int maxLength) {
		String value = Func.toStr(text, "");
		if (value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}

	private String resolveContractRoomIds(Contract contract) {
		String roomIds = contract == null ? "" : Func.toStr(contract.getRoomIds(), "");
		if (Func.isNotBlank(roomIds)) {
			return roomIds;
		}
		return contract != null && contract.getRoomId() != null ? String.valueOf(contract.getRoomId()) : "";
	}

	private void releaseRooms(Contract contract) {
		if (contract == null) {
			return;
		}
		for (Long roomId : Func.toLongList(resolveContractRoomIds(contract))) {
			roomMapper.updateRoomStatus(roomId, ROOM_STATUS_VACANT, currentUserName());
		}
	}

	private void generatePaymentPlan(Contract contract) {
		if (contract.getStartDate() == null || contract.getEndDate() == null) {
			return;
		}
		BigDecimal monthlyRent = contract.getMonthlyRent();
		if (monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}
		int monthsPerPeriod = switch (Func.toStr(contract.getPaymentCycle(), "monthly")) {
			case "quarterly" -> 3;
			case "halfYear" -> 6;
			case "yearly" -> 12;
			default -> 1;
		};

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(contract.getStartDate());
		Date endDate = contract.getEndDate();
		while (calendar.getTime().before(endDate)) {
			Date periodStart = calendar.getTime();
			calendar.add(Calendar.MONTH, monthsPerPeriod);
			Date naturalPeriodEnd = calendar.getTime();
			Date periodEnd = naturalPeriodEnd.after(endDate) ? endDate : naturalPeriodEnd;
			BigDecimal billableMonths = naturalPeriodEnd.after(endDate)
				? calculatePartialMonths(periodStart, periodEnd)
				: BigDecimal.valueOf(monthsPerPeriod);

			insertPeriodPayment(contract, "rent", "租金", periodStart, periodEnd, calculatePeriodAmount(monthlyRent, billableMonths));
			if (contract.getPropertyFee() != null && contract.getRentArea() != null && contract.getPropertyFee().compareTo(BigDecimal.ZERO) > 0) {
				insertPeriodPayment(contract, "property", "物业费", periodStart, periodEnd, calculatePeriodAmount(contract.getPropertyFee().multiply(contract.getRentArea()), billableMonths));
			}
			if (contract.getManagementFee() != null && contract.getManagementFee().compareTo(BigDecimal.ZERO) > 0) {
				insertPeriodPayment(contract, "management", "管业管理费", periodStart, periodEnd, calculatePeriodAmount(contract.getManagementFee(), billableMonths));
			}
			if (contract.getPublicFee() != null && contract.getPublicFee().compareTo(BigDecimal.ZERO) > 0) {
				insertPeriodPayment(contract, "public", "公摊费", periodStart, periodEnd, calculatePeriodAmount(contract.getPublicFee(), billableMonths));
			}
		}
	}

	private void insertPeriodPayment(Contract contract, String feeType, String feeName, Date periodStart, Date periodEnd, BigDecimal amountDue) {
		ContractPayment payment = new ContractPayment();
		payment.setContractId(contract.getContractId());
		payment.setDirection("receivable");
		payment.setFeeType(feeType);
		payment.setFeeName(feeName);
		payment.setPeriodStart(periodStart);
		payment.setPeriodEnd(periodEnd);
		payment.setAmountDue(amountDue);
		payment.setAmountPaid(BigDecimal.ZERO);
		payment.setPayDeadline(periodStart);
		payment.setPayStatus(PAY_STATUS_UNPAID);
		payment.setParkId(contract.getParkId());
		payment.setCreateBy(currentUserName());
		payment.setCreateTime(DateUtil.now());
		contractPaymentMapper.insert(payment);
	}

	private BigDecimal calculatePeriodAmount(BigDecimal monthlyAmount, BigDecimal billableMonths) {
		return monthlyAmount.multiply(billableMonths).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal calculatePartialMonths(Date periodStart, Date periodEnd) {
		LocalDate start = toLocalDate(periodStart);
		LocalDate end = toLocalDate(periodEnd);
		if (!end.isAfter(start)) {
			return BigDecimal.ZERO;
		}
		long fullMonths = ChronoUnit.MONTHS.between(start, end);
		LocalDate cursor = start.plusMonths(fullMonths);
		BigDecimal months = BigDecimal.valueOf(fullMonths);
		if (cursor.isBefore(end)) {
			long partialDays = ChronoUnit.DAYS.between(cursor, end);
			months = months.add(BigDecimal.valueOf(partialDays).divide(BigDecimal.valueOf(cursor.lengthOfMonth()), 6, RoundingMode.HALF_UP));
		}
		return months;
	}

	private LocalDate toLocalDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private String generateContractNo() {
		return "HT" + DateUtil.format(DateUtil.now(), "yyyyMMdd") + String.format("%04d", new Random().nextInt(10000));
	}

	private void addLog(Long contractId, String action, String actionDesc) {
		ContractLog contractLog = new ContractLog();
		contractLog.setContractId(contractId);
		contractLog.setAction(action);
		contractLog.setActionDesc(actionDesc);
		contractLog.setOperator(currentUserName());
		contractLog.setOperateTime(DateUtil.now());
		contractLogMapper.insert(contractLog);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return Func.isBlank(userName) ? "system" : userName;
	}

}
