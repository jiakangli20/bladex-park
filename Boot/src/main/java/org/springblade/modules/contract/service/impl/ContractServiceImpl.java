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
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Attachment;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.mapper.ContractChangeMapper;
import org.springblade.modules.contract.mapper.ContractExpiryRuleMapper;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractPaymentRecordMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractChange;
import org.springblade.modules.contract.pojo.entity.ContractExpiryRule;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractPaymentRecord;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractStatsVO;
import org.springblade.modules.contract.pojo.vo.ContractExpirySummaryVO;
import org.springblade.modules.contract.service.ContractParkAccessService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
	private static final String STATUS_TERMINATION_RUNNING = "6";
	private static final String STATUS_TERMINATION_HANDOVER = "7";
	private static final String STATUS_ROOM_REVIEW_RUNNING = "8";
	private static final String CHANGE_TYPE_RENT = "租金变更";
	private static final String CHANGE_TYPE_TERM = "租期变更";
	private static final String CHANGE_TYPE_RENT_AND_TERM = "租金及租期变更";
	private static final String CHANGE_TYPE_OTHER = "其他";
	private static final String CHANGE_STATUS_COMPLETED = "completed";
	private static final String PROCESS_STATUS_RUNNING = "running";
	private static final String PROCESS_STATUS_APPROVED = "approved";
	private static final String BUSINESS_TYPE_CONTRACT_APPROVAL = "contract_approval";
	private static final String BUSINESS_TYPE_CONTRACT_TERMINATION = "contract_termination";
	private static final String BUSINESS_TYPE_CONTRACT_ROOM_REVIEW = "contract_room_review";
	private static final String FEE_TYPE_DEPOSIT_REFUND = "deposit_refund";
	private static final String PAY_STATUS_UNPAID = "0";
	private static final String PAY_STATUS_PAID = "1";
	private static final String PAY_STATUS_PARTIAL = "3";
	private static final String ACCEPTANCE_PASSED = "验收通过";
	private static final String ACCEPTANCE_RECTIFICATION = "需整改";

	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractPaymentRecordMapper contractPaymentRecordMapper;
	private final ContractLogMapper contractLogMapper;
	private final ContractChangeMapper contractChangeMapper;
	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
	private final ContractExpiryRuleMapper contractExpiryRuleMapper;
	private final RoomMapper roomMapper;
	private final TaskService taskService;
	private final ContractParkAccessService contractParkAccessService;

	@Override
	public IPage<Contract> selectContractPage(IPage<Contract> page, Contract contract) {
		contract.setParkId(contractParkAccessService.scopedParkId(contract.getParkId()));
		return page.setRecords(baseMapper.selectContractPage(page, contract));
	}

	@Override
	public Contract selectContractById(Long contractId) {
		Contract contract = baseMapper.selectContractById(contractId);
		if (contract != null) {
			contractParkAccessService.assertAccessible(contract.getParkId());
		}
		return contract;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitContract(Contract contract) {
		contract.setParkId(contractParkAccessService.scopedParkId(contract.getParkId()));
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
		idList.forEach(this::requireContract);
		return baseMapper.deleteContractByIds(idList) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean renewContract(Long contractId, Contract newContract) {
		Contract oldContract = requireContract(contractId);

		newContract.setContractId(null);
		newContract.setParkId(oldContract.getParkId());
		newContract.setParentContractId(contractId);
		newContract.setContractStatus(STATUS_PENDING);
		boolean result = createContract(newContract, contractId);
		addLog(contractId, "renew", "发起续签，旧合同状态保持不变，待新合同生效后回写");
		addLog(newContract.getContractId(), "create", "续签新建合同，源合同ID：" + contractId);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractChange applyContractChange(ContractChange change) {
		if (change == null || change.getContractId() == null) {
			throw new ServiceException("合同ID不能为空");
		}
		Contract contract = requireContract(change.getContractId());
		validateContractCanChange(contract);
		String changeType = Func.toStr(change.getChangeType(), "").trim();
		String reason = Func.toStr(change.getReason(), "").trim();
		if (!isSupportedChangeType(changeType)) {
			throw new ServiceException("请选择正确的变更类型");
		}
		if (Func.isBlank(reason)) {
			throw new ServiceException("请输入变更原因");
		}

		validatePositiveAmount(change.getNewRentPrice(), "新租金单价");
		validatePositiveAmount(change.getNewMonthlyRent(), "新月租金");
		if (change.getNewEndDate() != null && contract.getStartDate() != null
			&& !change.getNewEndDate().after(contract.getStartDate())) {
			throw new ServiceException("新结束日期必须晚于合同开始日期");
		}

		boolean rentPriceChanged = amountChanged(contract.getRentPrice(), change.getNewRentPrice());
		boolean monthlyRentChanged = amountChanged(contract.getMonthlyRent(), change.getNewMonthlyRent());
		boolean endDateChanged = dateChanged(contract.getEndDate(), change.getNewEndDate());
		validateChangeContent(changeType, rentPriceChanged, monthlyRentChanged, endDateChanged);

		Date now = DateUtil.now();
		String userName = currentUserName();
		change.setChangeId(null);
		change.setChangeNo(generateChangeNo());
		change.setContractNo(contract.getContractNo());
		change.setContractName(contract.getContractName());
		change.setCustomerName(contract.getCustomerName());
		change.setChangeType(changeType);
		change.setOldRentPrice(rentPriceChanged ? contract.getRentPrice() : null);
		change.setNewRentPrice(rentPriceChanged ? change.getNewRentPrice() : null);
		change.setOldMonthlyRent(monthlyRentChanged ? contract.getMonthlyRent() : null);
		change.setNewMonthlyRent(monthlyRentChanged ? change.getNewMonthlyRent() : null);
		change.setOldEndDate(endDateChanged ? contract.getEndDate() : null);
		change.setNewEndDate(endDateChanged ? change.getNewEndDate() : null);
		change.setReason(reason);
		change.setApprovalStatus(CHANGE_STATUS_COMPLETED);
		change.setApprovalOpinion("无需审批，登记后生效");
		change.setDelFlag(DEFAULT_DEL_FLAG);
		change.setCreateBy(userName);
		change.setCreateTime(now);
		if (contractChangeMapper.insert(change) <= 0) {
			throw new ServiceException("合同变更记录保存失败");
		}

		Contract update = new Contract();
		update.setContractId(contract.getContractId());
		if (rentPriceChanged) {
			update.setRentPrice(change.getNewRentPrice());
		}
		if (monthlyRentChanged) {
			update.setMonthlyRent(change.getNewMonthlyRent());
		}
		if (endDateChanged) {
			update.setEndDate(change.getNewEndDate());
		}
		update.setUpdateBy(userName);
		update.setUpdateTime(now);
		if (baseMapper.updateById(update) <= 0) {
			throw new ServiceException("合同信息更新失败");
		}
		addLog(contract.getContractId(), "change", "合同变更已登记并生效：" + changeType + "，" + reason);
		return contractChangeMapper.selectById(change.getChangeId());
	}

	@Override
	public List<ContractChange> selectContractChanges(Long contractId) {
		requireContract(contractId);
		return contractChangeMapper.selectList(Wrappers.<ContractChange>lambdaQuery()
			.eq(ContractChange::getContractId, contractId)
			.eq(ContractChange::getDelFlag, DEFAULT_DEL_FLAG)
			.orderByDesc(ContractChange::getCreateTime)
			.orderByDesc(ContractChange::getChangeId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean terminateContract(Long contractId) {
		Contract contract = requireContract(contractId);
		validateContractCanVoid(contract);
		contract.setContractStatus(STATUS_TERMINATED);
		contract.setUpdateBy(currentUserName());
		contract.setUpdateTime(DateUtil.now());
		boolean result = baseMapper.updateById(contract) > 0;
		if (result) {
			addLog(contractId, "void", "作废待审批合同");
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
			if (existing.getParentContractId() != null) {
				Contract parent = requireContract(existing.getParentContractId());
				Contract parentUpdate = new Contract();
				parentUpdate.setContractId(parent.getContractId());
				parentUpdate.setContractStatus(STATUS_RENEWED);
				parentUpdate.setUpdateBy(currentUserName());
				parentUpdate.setUpdateTime(DateUtil.now());
				if (baseMapper.updateById(parentUpdate) <= 0) {
					throw new ServiceException("续租合同已生效，但旧合同状态回写失败");
				}
				addLog(parent.getContractId(), "renewed", "续租合同已盖章生效，新合同ID：" + contractId);
			}
			addLog(contractId, "signed", "上传盖章合同，合同生效");
		}
		return result;
	}

	@Override
	public IPage<Contract> selectExpiringPage(IPage<Contract> page, Contract contract) {
		contract.setParkId(contractParkAccessService.scopedParkId(contract.getParkId()));
		return page.setRecords(baseMapper.selectExpiringPage(page, contract));
	}

	@Override
	public ContractExpirySummaryVO expiringSummary(Contract contract) {
		contract.setParkId(contractParkAccessService.scopedParkId(contract.getParkId()));
		ContractExpirySummaryVO summary = baseMapper.selectExpiringSummary(contract);
		return summary == null ? new ContractExpirySummaryVO() : summary;
	}

	@Override
	public ContractStatsVO stats(Long parkId) {
		ContractStatsVO stats = baseMapper.selectStats(contractParkAccessService.scopedParkId(parkId));
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
		payment.setParkId(contractParkAccessService.scopedParkId(payment.getParkId()));
		return page.setRecords(contractPaymentMapper.selectPaymentPage(page, payment));
	}

	@Override
	public List<ContractPayment> selectPaymentByContractId(Long contractId) {
		requireContract(contractId);
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
		Contract contract = requireContractForUpdate(contractId);
		if (!STATUS_TERMINATED.equals(contract.getContractStatus())) {
			throw new ServiceException("房屋验收完成后才可以发起押金退还");
		}
		validateDepositRefundMaterials(contractId);
		ContractPayment existing = findDepositRefundPayment(contractId);
		if (existing != null && (PAY_STATUS_PAID.equals(existing.getPayStatus())
			|| "termination_settlement".equals(existing.getSpecialBillType()))) {
			return existing;
		}
		TerminationSettlement settlement = settleTerminationPayments(contract);
		return existing == null
			? createDepositRefundPayment(contract, settlement)
			: refreshDepositRefundPayment(existing, settlement);
	}

	private ContractPayment createDepositRefundPayment(Contract contract, TerminationSettlement settlement) {
		Date now = DateUtil.now();
		ContractPayment payment = new ContractPayment();
		payment.setContractId(contract.getContractId());
		payment.setDirection("payable");
		payment.setFeeType(FEE_TYPE_DEPOSIT_REFUND);
		payment.setFeeName("押金退还");
		payment.setPeriodStart(now);
		payment.setPeriodEnd(now);
		payment.setAmountDue(settlement.refundableAmount());
		payment.setAmountPaid(BigDecimal.ZERO);
		payment.setPayDeadline(now);
		payment.setPayStatus(settlement.refundableAmount().compareTo(BigDecimal.ZERO) > 0 ? PAY_STATUS_UNPAID : PAY_STATUS_PAID);
		payment.setPayTime(settlement.refundableAmount().compareTo(BigDecimal.ZERO) > 0 ? null : now);
		payment.setParkId(contract.getParkId());
		payment.setSpecialBillType("termination_settlement");
		payment.setRemark(settlement.remark());
		payment.setCreateBy(currentUserName());
		payment.setCreateTime(now);
		contractPaymentMapper.insert(payment);
		addLog(contract.getContractId(), "deposit_refund", "生成押金退还付款单：" + settlement.remark());
		return findDepositRefundPayment(contract.getContractId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractWorkflowRecord offlineRoomReview(Long contractId, Map<String, Object> formData) {
		Contract contract = requireContractForUpdate(contractId);
		if (!canOfflineRoomReview(contract.getContractStatus())) {
			throw new ServiceException("退租审批通过后才可以登记验收情况");
		}
		Date now = DateUtil.now();
		Map<String, Object> snapshot = normalizeOfflineForm(formData);
		snapshot.putIfAbsent("acceptanceDate", DateUtil.format(now, DateUtil.PATTERN_DATE));
		snapshot.putIfAbsent("acceptanceResult", ACCEPTANCE_PASSED);
		String acceptanceResult = textValue(snapshot, "acceptanceResult");
		if (!ACCEPTANCE_PASSED.equals(acceptanceResult) && !ACCEPTANCE_RECTIFICATION.equals(acceptanceResult)) {
			throw new ServiceException("请选择正确的验收结果");
		}
		BigDecimal deductionAmount = validateNonNegativeAmount(snapshot.get("deductionAmount"), "其他扣款");
		if (deductionAmount.compareTo(BigDecimal.ZERO) > 0
			&& Func.isBlank(textValue(snapshot, "deductionRemark"))) {
			throw new ServiceException("请填写其他扣款说明");
		}
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

		completeRoomReviewInternal(contract);
		return contractWorkflowRecordMapper.selectById(record.getRecordId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean completeRoomReview(Long contractId) {
		return completeRoomReviewInternal(requireContractForUpdate(contractId));
	}

	@Override
	public List<ContractLog> selectLogByContractId(Long contractId) {
		requireContract(contractId);
		return contractLogMapper.selectByContractId(contractId);
	}

	private boolean createContract(Contract contract) {
		return createContract(contract, null);
	}

	private boolean createContract(Contract contract, Long parentContractId) {
		contract.setParkId(contractParkAccessService.scopedParkId(contract.getParkId()));
		validateNewRelations(contract);
		Date now = DateUtil.now();
		if (Func.isBlank(contract.getContractNo())) {
			contract.setContractNo(generateContractNo());
		}
		contract.setContractStatus(STATUS_PENDING);
		if (Func.isBlank(contract.getPaymentCycle())) {
			contract.setPaymentCycle("monthly");
		}
		normalizeRentIncreaseNode(contract);
		contract.setContractFileUrl(null);
		contract.setParentContractId(parentContractId);
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
		Contract update = editableContract(contract);
		update.setContractId(oldContract.getContractId());
		update.setParkId(oldContract.getParkId());
		validateNewRelations(update);
		update.setRenewalRemindDays(resolveRenewalRemindDays(update));
		normalizeRentIncreaseNode(update);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		boolean result = updateById(update);
		if (result) {
			addLog(contract.getContractId(), "update", "更新合同信息");
		}
		return result;
	}

	/**
	 * 通用编辑接口只允许修改合同业务字段，生命周期和归档字段必须由专用业务动作维护.
	 */
	private Contract editableContract(Contract source) {
		Contract update = new Contract();
		update.setContractNo(source.getContractNo());
		update.setContractName(source.getContractName());
		update.setCustomerId(source.getCustomerId());
		update.setCustomerName(source.getCustomerName());
		update.setRoomId(source.getRoomId());
		update.setRoomIds(source.getRoomIds());
		update.setRoomName(source.getRoomName());
		update.setBuildingId(source.getBuildingId());
		update.setBuildingIds(source.getBuildingIds());
		update.setBuildingName(source.getBuildingName());
		update.setRentPrice(source.getRentPrice());
		update.setRentArea(source.getRentArea());
		update.setMonthlyRent(source.getMonthlyRent());
		update.setPropertyFee(source.getPropertyFee());
		update.setDeposit(source.getDeposit());
		update.setFollowUser(source.getFollowUser());
		update.setRentIncreaseNode(source.getRentIncreaseNode());
		update.setRentIncreaseRate(source.getRentIncreaseRate());
		update.setRentIncreaseUnit(source.getRentIncreaseUnit());
		update.setLateFeeRatio(source.getLateFeeRatio());
		update.setLateFeeUnit(source.getLateFeeUnit());
		update.setLateFeeCap(source.getLateFeeCap());
		update.setManagementFee(source.getManagementFee());
		update.setPublicFee(source.getPublicFee());
		update.setStartDate(source.getStartDate());
		update.setEndDate(source.getEndDate());
		update.setSignDate(source.getSignDate());
		update.setPaymentCycle(source.getPaymentCycle());
		update.setRemark(source.getRemark());
		return update;
	}

	/**
	 * 递增节点字段只保存短枚举，详细分阶段租金放在备注中，兼容历史短字段长度.
	 */
	private void normalizeRentIncreaseNode(Contract contract) {
		String node = Func.toStr(contract.getRentIncreaseNode(), "").trim();
		if (node.length() <= 32) {
			contract.setRentIncreaseNode(node);
			return;
		}
		String detail = "租金递增：" + node;
		String remark = Func.toStr(contract.getRemark(), "");
		if (!remark.contains(detail)) {
			contract.setRemark(limitText(Func.isBlank(remark) ? detail : remark + "\n" + detail, 500));
		}
		contract.setRentIncreaseNode("custom");
	}

	private Contract requireContract(Long contractId) {
		Contract contract = selectContractById(contractId);
		if (contract == null) {
			throw new ServiceException("合同不存在");
		}
		return contract;
	}

	private Contract requireContractForUpdate(Long contractId) {
		if (contractId == null) {
			throw new ServiceException("合同ID不能为空");
		}
		Contract contract = baseMapper.selectContractByIdForUpdate(contractId);
		if (contract == null) {
			throw new ServiceException("合同不存在");
		}
		contractParkAccessService.assertAccessible(contract.getParkId());
		return contract;
	}

	private void validateContractCanVoid(Contract contract) {
		if (!STATUS_PENDING.equals(contract.getContractStatus())) {
			throw new ServiceException("仅待审批合同可以作废，生效合同请走退租流程");
		}
		ContractWorkflowRecord approvalRecord = contractWorkflowRecordMapper.selectLatest(
			contract.getContractId(),
			BUSINESS_TYPE_CONTRACT_APPROVAL
		);
		if (approvalRecord != null && (PROCESS_STATUS_RUNNING.equals(approvalRecord.getProcessStatus())
			|| PROCESS_STATUS_APPROVED.equals(approvalRecord.getProcessStatus()))) {
			throw new ServiceException(PROCESS_STATUS_RUNNING.equals(approvalRecord.getProcessStatus())
				? "合同审批正在进行中，请先撤回或终止审批后再作废"
				: "合同审批已通过，不能作废，请按业务状态继续盖章或退租");
		}
	}

	private ContractPayment findDepositRefundPayment(Long contractId) {
		return contractPaymentMapper.selectByContractId(contractId).stream()
			.filter(payment -> FEE_TYPE_DEPOSIT_REFUND.equals(payment.getFeeType()))
			.findFirst()
			.orElse(null);
	}

	private ContractPayment refreshDepositRefundPayment(ContractPayment existing, TerminationSettlement settlement) {
		if (PAY_STATUS_PAID.equals(existing.getPayStatus())) {
			return existing;
		}
		BigDecimal currentAmount = nullToZero(existing.getAmountDue());
		BigDecimal targetAmount = settlement.refundableAmount();
		if (currentAmount.compareTo(targetAmount) != 0
			&& (nullToZero(existing.getAmountPaid()).compareTo(BigDecimal.ZERO) > 0
			|| PROCESS_STATUS_RUNNING.equals(existing.getPaymentApprovalStatus())
			|| PROCESS_STATUS_APPROVED.equals(existing.getPaymentApprovalStatus()))) {
			throw new ServiceException("退租结算金额已变化，请先撤回原付款审批后重新发起");
		}
		if (currentAmount.compareTo(targetAmount) == 0 && java.util.Objects.equals(existing.getRemark(), settlement.remark())) {
			return existing;
		}
		ContractPayment update = new ContractPayment();
		update.setPaymentId(existing.getPaymentId());
		update.setAmountDue(targetAmount);
		update.setSpecialBillType("termination_settlement");
		update.setRemark(settlement.remark());
		update.setPayStatus(targetAmount.compareTo(BigDecimal.ZERO) > 0 ? PAY_STATUS_UNPAID : PAY_STATUS_PAID);
		if (targetAmount.compareTo(BigDecimal.ZERO) == 0) {
			update.setPayTime(DateUtil.now());
		}
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		contractPaymentMapper.updateById(update);
		return findDepositRefundPayment(existing.getContractId());
	}

	private boolean completeRoomReviewInternal(Contract contract) {
		validateTerminationApproved(contract.getContractId());
		ContractWorkflowRecord roomReviewRecord = requireApprovedRoomReview(contract.getContractId());
		if (requiresRectification(roomReviewRecord.getFormDataJson())) {
			if (STATUS_ROOM_REVIEW_RUNNING.equals(contract.getContractStatus())) {
				updateContractStatus(contract.getContractId(), STATUS_TERMINATION_HANDOVER);
			}
			addLog(contract.getContractId(), "room_review_rectification", "房屋验收结果为需整改，合同保持退租交接中");
			return false;
		}
		if (!STATUS_TERMINATION_HANDOVER.equals(contract.getContractStatus())
			&& !STATUS_ROOM_REVIEW_RUNNING.equals(contract.getContractStatus())
			&& !STATUS_TERMINATED.equals(contract.getContractStatus())) {
			throw new ServiceException("当前合同状态不能完成房屋验收");
		}
		ContractPayment existingRefund = findDepositRefundPayment(contract.getContractId());
		if (existingRefund == null) {
			createDepositRefundPayment(contract, settleTerminationPayments(contract));
		} else if (!PAY_STATUS_PAID.equals(existingRefund.getPayStatus())
			&& !"termination_settlement".equals(existingRefund.getSpecialBillType())) {
			refreshDepositRefundPayment(existingRefund, settleTerminationPayments(contract));
		}
		boolean newlyTerminated = !STATUS_TERMINATED.equals(contract.getContractStatus());
		if (newlyTerminated) {
			updateContractStatus(contract.getContractId(), STATUS_TERMINATED);
		}
		releaseRooms(contract);
		if (newlyTerminated) {
			addLog(contract.getContractId(), "room_review", "房屋验收通过，退租结算完成并安全释放房源");
		}
		return true;
	}

	private TerminationSettlement settleTerminationPayments(Contract contract) {
		ContractWorkflowRecord roomReviewRecord = requireApprovedRoomReview(contract.getContractId());
		if (requiresRectification(roomReviewRecord.getFormDataJson())) {
			throw new ServiceException("房屋验收尚需整改，不能进行退租结算");
		}
		Date settlementDate = resolveSettlementDate(roomReviewRecord);
		BigDecimal futurePaidCredit = nullToZero(contractPaymentMapper.sumFuturePaidCredit(contract.getContractId(), settlementDate));
		int closedFutureBills = contractPaymentMapper.closeFutureReceivables(contract.getContractId(), settlementDate, currentUserName());
		List<ContractPayment> unsettledPayments = contractPaymentMapper.selectUnsettledReceivablesForUpdate(contract.getContractId(), settlementDate);
		BigDecimal unsettledReceivable = unsettledPayments.stream()
			.map(payment -> nullToZero(payment.getAmountDue()).subtract(nullToZero(payment.getAmountPaid())).max(BigDecimal.ZERO))
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal manualDeduction = resolveManualDeduction(roomReviewRecord.getFormDataJson());
		BigDecimal deposit = nullToZero(contract.getDeposit());
		BigDecimal availableCredit = deposit.add(futurePaidCredit).subtract(manualDeduction).max(BigDecimal.ZERO);
		BigDecimal offsetAmount = applyReceivableOffsets(unsettledPayments, availableCredit, settlementDate, contract);
		BigDecimal remainingReceivable = unsettledReceivable.subtract(offsetAmount).max(BigDecimal.ZERO);
		BigDecimal refundableAmount = deposit.add(futurePaidCredit)
			.subtract(unsettledReceivable)
			.subtract(manualDeduction)
			.max(BigDecimal.ZERO)
			.setScale(2, RoundingMode.HALF_UP);
		String remark = "退租结算：押金" + money(deposit)
			+ " + 未来账期预收" + money(futurePaidCredit)
			+ " - 未结应收" + money(unsettledReceivable)
			+ " - 其他扣款" + money(manualDeduction)
			+ " = 应退" + money(refundableAmount)
			+ "；押金抵扣应收" + money(offsetAmount)
			+ "，剩余未结应收" + money(remainingReceivable)
			+ "，已终止未来账单" + closedFutureBills + "笔";
		return new TerminationSettlement(settlementDate, unsettledReceivable, futurePaidCredit,
			manualDeduction, refundableAmount, closedFutureBills, remark);
	}

	private BigDecimal applyReceivableOffsets(List<ContractPayment> payments, BigDecimal availableCredit,
											 Date settlementDate, Contract contract) {
		BigDecimal remainingCredit = availableCredit;
		BigDecimal totalOffset = BigDecimal.ZERO;
		for (ContractPayment payment : payments) {
			if (remainingCredit.compareTo(BigDecimal.ZERO) <= 0) {
				break;
			}
			BigDecimal currentPaid = nullToZero(payment.getAmountPaid());
			BigDecimal remainingDue = nullToZero(payment.getAmountDue()).subtract(currentPaid).max(BigDecimal.ZERO);
			BigDecimal offset = remainingCredit.min(remainingDue).setScale(2, RoundingMode.HALF_UP);
			if (offset.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			BigDecimal cumulativeAmount = currentPaid.add(offset);
			ContractPayment update = new ContractPayment();
			update.setPaymentId(payment.getPaymentId());
			update.setAmountPaid(cumulativeAmount);
			update.setPayStatus(cumulativeAmount.compareTo(nullToZero(payment.getAmountDue())) >= 0 ? PAY_STATUS_PAID : PAY_STATUS_PARTIAL);
			update.setPayTime(settlementDate);
			update.setUpdateBy(currentUserName());
			update.setUpdateTime(DateUtil.now());
			if (contractPaymentMapper.updateById(update) <= 0) {
				throw new ServiceException("退租押金抵扣应收账单失败");
			}

			ContractPaymentRecord record = new ContractPaymentRecord();
			record.setPaymentId(payment.getPaymentId());
			record.setContractId(contract.getContractId());
			record.setPaymentAmount(offset);
			record.setCumulativeAmount(cumulativeAmount);
			record.setPaymentTime(settlementDate);
			record.setRemark("退租结算押金抵扣");
			record.setOperatorUserId(AuthUtil.getUserId());
			record.setOperatorAccount(AuthUtil.getUserName());
			record.setOperatorName(currentUserName());
			record.setParkId(contract.getParkId());
			record.setDelFlag(DEFAULT_DEL_FLAG);
			record.setCreateBy(currentUserName());
			record.setCreateTime(DateUtil.now());
			contractPaymentRecordMapper.insert(record);
			remainingCredit = remainingCredit.subtract(offset);
			totalOffset = totalOffset.add(offset);
		}
		return totalOffset;
	}

	private void validateTerminationApproved(Long contractId) {
		ContractWorkflowRecord terminationRecord = contractWorkflowRecordMapper.selectLatest(contractId, BUSINESS_TYPE_CONTRACT_TERMINATION);
		if (terminationRecord == null || !PROCESS_STATUS_APPROVED.equals(terminationRecord.getProcessStatus())) {
			throw new ServiceException("退租审批完成后才可以办理房屋验收");
		}
	}

	private ContractWorkflowRecord requireApprovedRoomReview(Long contractId) {
		ContractWorkflowRecord record = contractWorkflowRecordMapper.selectLatest(contractId, BUSINESS_TYPE_CONTRACT_ROOM_REVIEW);
		if (record == null || !PROCESS_STATUS_APPROVED.equals(record.getProcessStatus())) {
			throw new ServiceException("房屋验收完成后才可以进行退租结算");
		}
		return record;
	}

	private boolean requiresRectification(String formDataJson) {
		Map<String, Object> formData = parseFormData(formDataJson);
		String result = firstNotBlank(textValue(formData, "acceptanceResult"), textValue(formData, "验收结果"));
		return ACCEPTANCE_RECTIFICATION.equals(result) || result.contains("整改") || result.contains("不通过");
	}

	private Date resolveSettlementDate(ContractWorkflowRecord record) {
		Map<String, Object> formData = parseFormData(record.getFormDataJson());
		String value = firstNotBlank(textValue(formData, "acceptanceDate"), textValue(formData, "returnDate"), textValue(formData, "验收日期"));
		if (Func.isNotBlank(value)) {
			try {
				LocalDate date = LocalDate.parse(value.substring(0, Math.min(value.length(), 10)));
				return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
			} catch (Exception ignored) {
				// 历史流程日期格式不统一时，使用审批完成时间兜底.
			}
		}
		return record.getApprovalTime() == null ? DateUtil.now() : record.getApprovalTime();
	}

	private BigDecimal resolveManualDeduction(String formDataJson) {
		Map<String, Object> formData = parseFormData(formDataJson);
		Object value = firstNotNull(
			formData.get("deductionAmount"), formData.get("deductAmount"), formData.get("totalDeductionAmount"),
			formData.get("其他扣款"), formData.get("扣款金额"), formData.get("应扣金额")
		);
		BigDecimal amount = toBigDecimal(value);
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException("退租扣款金额不能小于0");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private Map<String, Object> parseFormData(String formDataJson) {
		if (Func.isBlank(formDataJson)) {
			return new LinkedHashMap<>();
		}
		try {
			Map<String, Object> formData = JsonUtil.readMap(formDataJson);
			return formData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(formData);
		} catch (Exception ignored) {
			return new LinkedHashMap<>();
		}
	}

	private BigDecimal toBigDecimal(Object value) {
		if (value == null || Func.isBlank(Func.toStr(value, ""))) {
			return BigDecimal.ZERO;
		}
		try {
			return new BigDecimal(Func.toStr(value, "0").replace(",", "").trim());
		} catch (NumberFormatException exception) {
			throw new ServiceException("退租扣款金额格式不正确");
		}
	}

	private BigDecimal validateNonNegativeAmount(Object value, String fieldName) {
		BigDecimal amount = toBigDecimal(value).setScale(2, RoundingMode.HALF_UP);
		if (amount.compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException(fieldName + "不能小于0");
		}
		return amount;
	}

	private String money(BigDecimal amount) {
		return nullToZero(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private BigDecimal nullToZero(BigDecimal amount) {
		return amount == null ? BigDecimal.ZERO : amount;
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

	private void updateContractStatus(Long contractId, String status) {
		Contract update = new Contract();
		update.setContractId(contractId);
		update.setContractStatus(status);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		if (baseMapper.updateById(update) <= 0) {
			throw new ServiceException("合同状态更新失败");
		}
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
		if (contract.getBuildingId() != null && emptyCount(baseMapper.existsBuildingInPark(contract.getBuildingId(), contract.getParkId()))) {
			throw new ServiceException("楼宇不存在或不属于所选园区");
		}
		for (Long buildingId : Func.toLongList(Func.toStr(contract.getBuildingIds(), "").replace("building_", ""))) {
			if (emptyCount(baseMapper.existsBuildingInPark(buildingId, contract.getParkId()))) {
				throw new ServiceException("存在不属于所选园区的楼宇");
			}
		}
		if (contract.getRoomId() != null && emptyCount(baseMapper.existsRoomInPark(contract.getRoomId(), contract.getParkId()))) {
			throw new ServiceException("房源不存在或不属于所选园区");
		}
		for (Long roomId : Func.toLongList(Func.toStr(contract.getRoomIds(), "").replace("room_", ""))) {
			if (emptyCount(baseMapper.existsRoomInPark(roomId, contract.getParkId()))) {
				throw new ServiceException("存在不属于所选园区的房源");
			}
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
			|| STATUS_ROOM_REVIEW_RUNNING.equals(contractStatus);
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
		putIfPresent(attachments, "fileList", snapshot.get("fileList"));
		return attachments;
	}

	private void putIfPresent(Map<String, Object> target, String key, Object value) {
		if (value != null && Func.isNotBlank(Func.toStr(value, ""))) {
			target.put(key, value);
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
		if (contract == null) {
			return "";
		}
		Set<Long> roomIds = new LinkedHashSet<>(Func.toLongList(
			Func.toStr(contract.getRoomIds(), "").replace("room_", "")
		));
		if (contract.getRoomId() != null) {
			roomIds.add(contract.getRoomId());
		}
		return roomIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
	}

	private void releaseRooms(Contract contract) {
		if (contract == null) {
			return;
		}
		for (Long roomId : Func.toLongList(resolveContractRoomIds(contract))) {
			int released = roomMapper.releaseRoomIfUnoccupied(roomId, contract.getParkId(), contract.getContractId(), currentUserName());
			if (released == 0) {
				addLog(contract.getContractId(), "room_release_skipped",
					"房源" + roomId + "不存在、园区不一致或仍被其他未终止合同占用，未释放为空置");
			}
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

	private String generateChangeNo() {
		return "BG" + DateUtil.format(DateUtil.now(), "yyyyMMddHHmmss") + String.format("%04d", new Random().nextInt(10000));
	}

	private void validateContractCanChange(Contract contract) {
		String status = contract.getContractStatus();
		if (STATUS_TERMINATED.equals(status)) {
			throw new ServiceException("已退租合同不能办理合同变更");
		}
		if (STATUS_TERMINATION_RUNNING.equals(status)
			|| STATUS_TERMINATION_HANDOVER.equals(status)
			|| STATUS_ROOM_REVIEW_RUNNING.equals(status)) {
			throw new ServiceException("退租办理中的合同不能办理合同变更");
		}
	}

	private boolean isSupportedChangeType(String changeType) {
		return CHANGE_TYPE_RENT.equals(changeType)
			|| CHANGE_TYPE_TERM.equals(changeType)
			|| CHANGE_TYPE_RENT_AND_TERM.equals(changeType)
			|| CHANGE_TYPE_OTHER.equals(changeType);
	}

	private void validatePositiveAmount(BigDecimal value, String fieldName) {
		if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException(fieldName + "必须大于0");
		}
	}

	private boolean amountChanged(BigDecimal oldValue, BigDecimal newValue) {
		return newValue != null && (oldValue == null || oldValue.compareTo(newValue) != 0);
	}

	private boolean dateChanged(Date oldValue, Date newValue) {
		if (newValue == null) {
			return false;
		}
		return oldValue == null || !DateUtil.format(oldValue, DateUtil.PATTERN_DATE)
			.equals(DateUtil.format(newValue, DateUtil.PATTERN_DATE));
	}

	private void validateChangeContent(String changeType, boolean rentPriceChanged,
									   boolean monthlyRentChanged, boolean endDateChanged) {
		boolean rentChanged = rentPriceChanged || monthlyRentChanged;
		if (!rentChanged && !endDateChanged) {
			throw new ServiceException("请至少填写一项与原合同不同的变更内容");
		}
		if (CHANGE_TYPE_RENT.equals(changeType) && !rentChanged) {
			throw new ServiceException("租金变更需填写新的租金单价或月租金");
		}
		if (CHANGE_TYPE_TERM.equals(changeType) && !endDateChanged) {
			throw new ServiceException("租期变更需填写新的合同结束日期");
		}
		if (CHANGE_TYPE_RENT_AND_TERM.equals(changeType) && (!rentChanged || !endDateChanged)) {
			throw new ServiceException("租金及租期变更需同时填写新租金和新结束日期");
		}
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

	private record TerminationSettlement(Date settlementDate,
									 BigDecimal unsettledReceivable,
									 BigDecimal futurePaidCredit,
									 BigDecimal manualDeduction,
									 BigDecimal refundableAmount,
									 int closedFutureBills,
									 String remark) {
	}

}
