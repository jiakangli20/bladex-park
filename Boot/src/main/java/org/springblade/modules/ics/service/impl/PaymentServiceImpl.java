/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractPaymentRecordMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractPaymentRecord;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springblade.modules.ics.mapper.PaymentMapper;
import org.springblade.modules.ics.mapper.PaymentNoticeMapper;
import org.springblade.modules.ics.mapper.OverdueInternalNoticeMapper;
import org.springblade.modules.ics.pojo.dto.OverdueNoticeSendDTO;
import org.springblade.modules.ics.pojo.dto.LegalLetterSendDTO;
import org.springblade.modules.ics.pojo.entity.OverdueInternalNotice;
import org.springblade.modules.ics.pojo.entity.OverdueReminderRecord;
import org.springblade.modules.ics.pojo.entity.PaymentNotice;
import org.springblade.modules.ics.pojo.vo.OverdueDisposalDetailVO;
import org.springblade.modules.ics.pojo.vo.OverdueInternalNoticeVO;
import org.springblade.modules.ics.pojo.vo.OverdueNoticeRecipientVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.system.pojo.entity.Dept;
import org.springblade.modules.system.pojo.entity.Role;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IDeptService;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 财务缴费服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

	private static final String SCOPE_OVERDUE = "overdue";
	private static final String SCOPE_OVERDUE_HISTORY = "overdue_history";
	private static final String PAY_STATUS_PAID = "1";
	private static final String FEE_TYPE_DEPOSIT_REFUND = "deposit_refund";
	private static final String PAY_STATUS_PARTIAL = "3";
	private static final String PAY_STATUS_UNPAID = "0";
	private static final String REMIND_STATUS_NONE = "0";
	private static final String DIRECTION_RECEIVABLE = "receivable";
	private static final String DIRECTION_PAYABLE = "payable";
	private static final String SPECIAL_BILL_REGULAR = "regular";
	private static final String DEFAULT_COMPANY_NAME = "吴中金融招商服务有限公司";
	private static final String REMIND_STATUS_REMINDED = "1";
	private static final String NOTICE_TYPE_RECEIPT = IContractNoticeService.NOTICE_PAYMENT;
	private static final String NOTICE_STATUS_PENDING = "pending";
	private static final String NOTICE_STATUS_SUCCESS = "success";
	private static final String NOTICE_STATUS_FAILED = "failed";
	private static final String NOTICE_STATUS_RESERVED = "reserved";
	private static final String NOTICE_TYPE_REMINDER = IContractNoticeService.NOTICE_REMINDER;
	private static final String NOTICE_TYPE_OVERDUE = IContractNoticeService.NOTICE_OVERDUE;
	private static final String NOTICE_READ_UNREAD = "0";
	private static final String DEFAULT_DEL_FLAG = "0";

	private final PaymentMapper paymentMapper;
	private final PaymentNoticeMapper paymentNoticeMapper;
	private final OverdueInternalNoticeMapper overdueInternalNoticeMapper;
	private final ContractMapper contractMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractPaymentRecordMapper contractPaymentRecordMapper;
	private final ContractLogMapper contractLogMapper;
	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
	private final IContractNoticeService contractNoticeService;
	private final IUserService userService;
	private final IDeptService deptService;
	private final IRoleService roleService;

	@Override
	public IPage<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, ContractPayment payment, String scope) {
		ContractPayment query = normalizeQuery(payment);
		page.setRecords(paymentMapper.selectPaymentPage(
			page,
			query,
			SCOPE_OVERDUE.equals(scope),
			SCOPE_OVERDUE_HISTORY.equals(scope)
		));
		return page;
	}

	@Override
	public ContractPayment selectPaymentById(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		payment.setPaymentRecords(contractPaymentRecordMapper.selectList(
			Wrappers.<ContractPaymentRecord>lambdaQuery()
				.eq(ContractPaymentRecord::getPaymentId, paymentId)
				.eq(ContractPaymentRecord::getDelFlag, DEFAULT_DEL_FLAG)
				.orderByAsc(ContractPaymentRecord::getPaymentTime)
				.orderByAsc(ContractPaymentRecord::getRecordId)
		));
		return payment;
	}

	@Override
	public List<ContractPayment> selectPaymentByContract(Long contractId) {
		if (Func.isEmpty(contractId)) {
			throw new ServiceException("合同ID不能为空");
		}
		ContractPayment query = new ContractPayment();
		query.setContractId(contractId);
		query = normalizeQuery(query);
		return paymentMapper.selectPaymentPage(null, query, false, false);
	}

	@Override
	public PaymentSummaryVO summary(ContractPayment payment) {
		PaymentSummaryVO summary = paymentMapper.selectSummary(normalizeQuery(payment), false);
		return summary == null ? new PaymentSummaryVO() : summary;
	}

	@Override
	public PaymentSummaryVO overdueReminderSummary(ContractPayment payment) {
		PaymentSummaryVO summary = paymentMapper.selectSummary(normalizeQuery(payment), true);
		return summary == null ? new PaymentSummaryVO() : summary;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractPayment create(ContractPayment payment) {
		if (payment == null || Func.isEmpty(payment.getContractId())) {
			throw new ServiceException("请选择关联合同");
		}
		Contract contract = contractMapper.selectContractById(payment.getContractId());
		if (contract == null) {
			throw new ServiceException("关联合同不存在");
		}
		String direction = normalizeDirection(payment.getDirection());
		if (DIRECTION_PAYABLE.equals(direction)
			&& FEE_TYPE_DEPOSIT_REFUND.equals(Func.toStr(payment.getFeeType(), "").trim())) {
			throw new ServiceException("押金退还付款账单由退租流程自动生成，请勿重复创建");
		}
		validateCreatePayment(payment);
		PaymentRoomSelection roomSelection = resolvePaymentRoomSelection(contract, payment.getSelectedRoomIds());

		Date now = DateUtil.now();
		ContractPayment created = new ContractPayment();
		created.setContractId(contract.getContractId());
		created.setDirection(direction);
		created.setFeeType(payment.getFeeType().trim());
		created.setFeeName(payment.getFeeName().trim());
		created.setPeriodStart(payment.getPeriodStart());
		created.setPeriodEnd(payment.getPeriodEnd());
		created.setAmountDue(payment.getAmountDue());
		created.setAmountPaid(BigDecimal.ZERO);
		created.setPayDeadline(payment.getPayDeadline());
		created.setPayStatus(PAY_STATUS_UNPAID);
		created.setRemindStatus(REMIND_STATUS_NONE);
		created.setRemark(payment.getRemark());
		created.setParkId(contract.getParkId());
		created.setTaxRate(nullToZero(payment.getTaxRate()));
		created.setSpecialBillType(Func.toStr(payment.getSpecialBillType(), SPECIAL_BILL_REGULAR));
		created.setLateFeeStartDays(payment.getLateFeeStartDays());
		created.setLateFeeRatio(payment.getLateFeeRatio());
		created.setLateFeeCap(payment.getLateFeeCap());
		created.setCompanyName(Func.toStr(payment.getCompanyName(), DEFAULT_COMPANY_NAME));
		created.setAttachmentName(payment.getAttachmentName());
		created.setAttachmentUrl(payment.getAttachmentUrl());
		created.setSelectedRoomIds(roomSelection.roomIds);
		created.setSelectedRoomName(roomSelection.roomName);
		created.setSelectedBuildingName(roomSelection.buildingName);
		created.setCreateBy(currentUserName());
		created.setCreateTime(now);
		if (contractPaymentMapper.insert(created) <= 0) {
			throw new ServiceException("账单创建失败");
		}
		addLog(contract.getContractId(), "payment_create", "创建" + directionName(direction) + "账单，费用：" + created.getFeeName());
		return paymentMapper.selectPaymentById(created.getPaymentId());
	}

	@Override
	public List<Contract> contractOptions(String keyword) {
		List<Contract> contracts = paymentMapper.selectContractOptions(StringUtil.isBlank(keyword) ? null : keyword.trim(), null);
		attachContractRooms(contracts);
		return contracts;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean confirm(Long paymentId, ContractPayment payment) {
		ContractPayment existing = requirePaymentForUpdate(paymentId);
		assertAccessible(existing);
		boolean payable = DIRECTION_PAYABLE.equals(normalizeDirection(existing.getDirection()));
		if (payable) {
			validatePayableConfirmation(existing, payment);
		} else {
			validateReceivableConfirmation(existing, payment);
		}
		BigDecimal submittedAmount = payment == null ? existing.getAmountDue() : payment.getAmountPaid();
		if (submittedAmount == null) {
			submittedAmount = existing.getAmountDue();
		}
		if (submittedAmount == null || submittedAmount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException(payable ? "实付金额必须大于0" : "实收金额必须大于0");
		}
		BigDecimal existingPaid = nullToZero(existing.getAmountPaid());
		BigDecimal paymentAmount;
		BigDecimal amountPaid = submittedAmount;
		if (payable) {
			paymentAmount = submittedAmount;
			BigDecimal remainingAmount = nullToZero(existing.getAmountDue()).subtract(existingPaid).max(BigDecimal.ZERO);
			if (paymentAmount.compareTo(remainingAmount) > 0) {
				throw new ServiceException("本次实付金额不能大于剩余应付金额");
			}
			amountPaid = existingPaid.add(paymentAmount);
		} else {
			if (amountPaid.compareTo(existingPaid) <= 0) {
				throw new ServiceException("累计实收金额必须大于当前已收金额");
			}
			if (amountPaid.compareTo(nullToZero(existing.getAmountDue())) > 0) {
				throw new ServiceException("累计实收金额不能大于应收金额");
			}
			paymentAmount = amountPaid.subtract(existingPaid);
		}
		String payStatus = amountPaid.compareTo(nullToZero(existing.getAmountDue())) < 0 ? PAY_STATUS_PARTIAL : PAY_STATUS_PAID;
		Date now = DateUtil.now();
		Date paymentTime = payment != null && payment.getPayTime() != null ? payment.getPayTime() : now;
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setAmountPaid(amountPaid);
		update.setPayStatus(payStatus);
		update.setPayTime(paymentTime);
		update.setRemark(payment == null ? null : payment.getRemark());
		update.setPaymentVoucherName(payment.getPaymentVoucherName());
		update.setPaymentVoucherUrl(payment.getPaymentVoucherUrl());
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			String feeName = StringUtil.isBlank(existing.getFeeName()) ? "-" : existing.getFeeName();
			ContractPaymentRecord record = new ContractPaymentRecord();
			record.setPaymentId(paymentId);
			record.setContractId(existing.getContractId());
			record.setPaymentAmount(paymentAmount);
			record.setCumulativeAmount(amountPaid);
			record.setPaymentTime(paymentTime);
			record.setVoucherName(payment.getPaymentVoucherName());
			record.setVoucherUrl(payment.getPaymentVoucherUrl());
			record.setRemark(payment.getRemark());
			record.setOperatorUserId(AuthUtil.getUserId());
			record.setOperatorAccount(AuthUtil.getUserName());
			record.setOperatorName(currentOperatorName());
			record.setParkId(existing.getParkId());
			record.setDelFlag(DEFAULT_DEL_FLAG);
			record.setCreateBy(currentUserName());
			record.setCreateTime(now);
			contractPaymentRecordMapper.insert(record);
			String amountText = "，本次" + (payable ? "付款：" : "收款：")
				+ paymentAmount.stripTrailingZeros().toPlainString() + "元";
			addLog(existing.getContractId(), "payment", (payable ? "确认付款" : "确认收款") + "，费用：" + feeName + amountText);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deletePaymentVoucher(Long paymentId, Long recordId) {
		ContractPayment payment = requirePaymentForUpdate(paymentId);
		assertAccessible(payment);
		boolean payable = DIRECTION_PAYABLE.equals(normalizeDirection(payment.getDirection()));
		String actionName = payable ? "付款" : "收款";
		String paidLabel = payable ? "已付" : "已收";
		if (recordId == null) {
			throw new ServiceException("历史" + actionName + "缺少逐笔记录，无法安全撤回金额");
		}
		ContractPaymentRecord record = contractPaymentRecordMapper.selectOne(
			Wrappers.<ContractPaymentRecord>lambdaQuery()
				.eq(ContractPaymentRecord::getRecordId, recordId)
				.eq(ContractPaymentRecord::getDelFlag, DEFAULT_DEL_FLAG)
		);
		if (record == null || !Objects.equals(record.getPaymentId(), paymentId)) {
			throw new ServiceException(actionName + "记录不存在或不属于当前账单");
		}
		if (record.getPaymentAmount() == null || record.getPaymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException(actionName + "记录金额异常，无法撤回");
		}
		contractPaymentRecordMapper.rollbackFollowingCumulativeAmount(
			paymentId, recordId, record.getPaymentTime(), record.getPaymentAmount());
		int deleted = contractPaymentRecordMapper.update(null,
			Wrappers.<ContractPaymentRecord>lambdaUpdate()
				.eq(ContractPaymentRecord::getRecordId, recordId)
				.eq(ContractPaymentRecord::getPaymentId, paymentId)
				.eq(ContractPaymentRecord::getDelFlag, DEFAULT_DEL_FLAG)
				.set(ContractPaymentRecord::getDelFlag, "1")
		);
		if (deleted <= 0) {
			throw new ServiceException(actionName + "记录撤回失败，请刷新后重试");
		}
		ContractPaymentRecord latestRecord = contractPaymentRecordMapper.selectOne(
			Wrappers.<ContractPaymentRecord>lambdaQuery()
				.eq(ContractPaymentRecord::getPaymentId, paymentId)
				.eq(ContractPaymentRecord::getDelFlag, DEFAULT_DEL_FLAG)
				.orderByDesc(ContractPaymentRecord::getPaymentTime)
				.orderByDesc(ContractPaymentRecord::getRecordId)
				.last("LIMIT 1")
		);
		BigDecimal amountPaid = nullToZero(payment.getAmountPaid())
			.subtract(record.getPaymentAmount()).max(BigDecimal.ZERO);
		BigDecimal amountDue = nullToZero(payment.getAmountDue());
		String payStatus = amountPaid.compareTo(BigDecimal.ZERO) == 0
			? PAY_STATUS_UNPAID
			: (amountPaid.compareTo(amountDue) < 0 ? PAY_STATUS_PARTIAL : PAY_STATUS_PAID);
		Date now = DateUtil.now();
		int updated = contractPaymentMapper.update(null,
			Wrappers.<ContractPayment>lambdaUpdate()
				.eq(ContractPayment::getPaymentId, paymentId)
				.set(ContractPayment::getAmountPaid, amountPaid)
				.set(ContractPayment::getPayStatus, payStatus)
				.set(ContractPayment::getPayTime,
					latestRecord == null ? null : latestRecord.getPaymentTime())
				.set(ContractPayment::getPaymentVoucherName,
					latestRecord == null ? null : latestRecord.getVoucherName())
				.set(ContractPayment::getPaymentVoucherUrl,
					latestRecord == null ? null : latestRecord.getVoucherUrl())
				.set(ContractPayment::getUpdateBy, currentUserName())
				.set(ContractPayment::getUpdateTime, now)
		);
		if (updated > 0) {
			addLog(payment.getContractId(), "payment", "撤回" + actionName + "："
				+ record.getPaymentAmount().stripTrailingZeros().toPlainString()
				+ "元，累计" + paidLabel + "调整为" + amountPaid.stripTrailingZeros().toPlainString() + "元");
		}
		return updated > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remind(Long paymentId, String source) {
		ContractPayment existing = requirePayment(paymentId);
		assertAccessible(existing);
		Date now = DateUtil.now();
		validateOverdueReceivable(existing, now);
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setRemindStatus(REMIND_STATUS_REMINDED);
		update.setRemindTime(now);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			OverdueReminderRecord record = new OverdueReminderRecord();
			record.setPaymentId(existing.getPaymentId());
			record.setContractId(existing.getContractId());
			record.setOperatorUserId(AuthUtil.getUserId());
			record.setOperatorAccount(AuthUtil.getUserName());
			record.setOperatorName(currentOperatorName());
			record.setSource(normalizeRemindSource(source));
			record.setRemindTime(now);
			record.setParkId(existing.getParkId());
			record.setDelFlag(DEFAULT_DEL_FLAG);
			record.setCreateBy(currentUserName());
			record.setCreateTime(now);
			overdueInternalNoticeMapper.insertReminderRecord(record);
			addLog(existing.getContractId(), "remind", "发起催缴提醒，账单ID：" + existing.getPaymentId());
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateDeadline(Long paymentId, Date payDeadline) {
		ContractPayment existing = requirePayment(paymentId);
		assertAccessible(existing);
		if (payDeadline == null) {
			throw new ServiceException("账单日期不能为空");
		}
		Date now = DateUtil.now();
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setPayDeadline(payDeadline);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			addLog(existing.getContractId(), "payment_deadline", "调整账单日期：" + DateUtil.format(payDeadline, DateUtil.PATTERN_DATE));
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractPayment updateAttachment(Long paymentId, ContractPayment payment) {
		ContractPayment existing = requirePayment(paymentId);
		assertAccessible(existing);
		if (payment == null || StringUtil.isBlank(payment.getAttachmentUrl())) {
			throw new ServiceException("附件地址不能为空");
		}
		Date now = DateUtil.now();
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setAttachmentName(Func.toStr(payment.getAttachmentName(), "账单附件"));
		update.setAttachmentUrl(payment.getAttachmentUrl());
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		if (contractPaymentMapper.updateById(update) <= 0) {
			throw new ServiceException("账单附件保存失败");
		}
		addLog(existing.getContractId(), "payment_attachment", "上传账单附件：" + update.getAttachmentName());
		return paymentMapper.selectPaymentById(paymentId);
	}

	@Override
	public List<ContractLog> logList(Long contractId) {
		if (Func.isEmpty(contractId)) {
			throw new ServiceException("合同ID不能为空");
		}
		ContractPayment query = new ContractPayment();
		query.setContractId(contractId);
		List<ContractPayment> payments = paymentMapper.selectPaymentPage(null, normalizeQuery(query), false, false);
		if (payments.isEmpty()) {
			return List.of();
		}
		return contractLogMapper.selectByContractId(contractId);
	}

	@Override
	public OverdueDisposalDetailVO overdueDisposalDetail(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		validateOverdueHistoryReceivable(payment, DateUtil.now());
		OverdueDisposalDetailVO detail = new OverdueDisposalDetailVO();
		detail.setPaymentNotice(paymentNoticeMapper.selectNoticeByPaymentId(paymentId, NOTICE_TYPE_OVERDUE));
		List<ContractLog> logs = payment.getContractId() == null ? List.of() : contractLogMapper.selectByContractId(payment.getContractId());
		detail.setDocumentRecords(logs.stream().filter(log -> isDocumentRecord(log, paymentId)).toList());
		detail.setMiniAppRecords(logs.stream().filter(log -> isMiniAppRecord(log, paymentId)).toList());
		detail.setLegalSendRecords(logs.stream().filter(log -> isLegalSendRecord(log, paymentId)).toList());
		detail.setWorkflowRecords(payment.getContractId() == null
			? List.of()
			: contractWorkflowRecordMapper.selectByContractId(payment.getContractId()).stream()
					.filter(record -> isOverdueWorkflowRecord(record, paymentId))
					.toList());
		detail.setInternalNotices(overdueInternalNoticeMapper.selectByPaymentId(paymentId));
		return detail;
	}

	@Override
	public Long unreadOverdueNoticeCount() {
		Long userId = AuthUtil.getUserId();
		if (Func.isEmpty(userId)) {
			return 0L;
		}
		Long count = overdueInternalNoticeMapper.countUnread(userId);
		return count == null ? 0L : count;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean readOverdueNotice(Long paymentId) {
		if (Func.isEmpty(paymentId) || Func.isEmpty(AuthUtil.getUserId())) {
			return false;
		}
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		// 标记已读需要保持幂等：通知已读或当前账号不是收件人时，更新行数为 0
		// 也不应导致打开逾期处置抽屉提示“操作失败”。
		overdueInternalNoticeMapper.markRead(paymentId, AuthUtil.getUserId());
		return true;
	}

	@Override
	public List<OverdueInternalNotice> overdueInternalNotices(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		return overdueInternalNoticeMapper.selectByPaymentId(paymentId);
	}

	@Override
	public List<OverdueNoticeRecipientVO> overdueNoticeRecipients(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		validateOverdueReceivable(payment, DateUtil.now());
		Contract contract = contractMapper.selectContractById(payment.getContractId());
		if (contract == null) {
			throw new ServiceException("账单关联合同不存在");
		}
		RecipientContext context = loadRecipientContext();
		Map<Long, NoticeRecipient> suggested = resolveSuggestedRecipients(contract, context);
		Set<Long> sentUserIds = overdueInternalNoticeMapper.selectByPaymentId(paymentId).stream()
			.map(OverdueInternalNotice::getRecipientUserId)
			.filter(Objects::nonNull)
			.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
		List<OverdueNoticeRecipientVO> result = new ArrayList<>();
		for (User user : context.users) {
			OverdueNoticeRecipientVO recipient = new OverdueNoticeRecipientVO();
			recipient.setUserId(user.getId());
			recipient.setAccount(user.getAccount());
			recipient.setUserName(firstNotBlank(user.getRealName(), user.getName(), user.getAccount()));
			recipient.setDeptName(userDeptNames(user, context.deptMap));
			recipient.setRoleNames(userRoleNames(user, context.roleMap));
			NoticeRecipient suggestedRecipient = suggested.get(user.getId());
			recipient.setSuggestedRoles(suggestedRecipient == null ? "" : String.join("、", suggestedRecipient.roles));
			recipient.setDefaultSelected(suggestedRecipient != null);
			recipient.setAlreadySent(sentUserIds.contains(user.getId()));
			result.add(recipient);
		}
		result.sort((left, right) -> {
			int selectedCompare = Boolean.compare(Boolean.TRUE.equals(right.getDefaultSelected()), Boolean.TRUE.equals(left.getDefaultSelected()));
			if (selectedCompare != 0) {
				return selectedCompare;
			}
			int deptCompare = Func.toStr(left.getDeptName()).compareTo(Func.toStr(right.getDeptName()));
			return deptCompare != 0 ? deptCompare : Func.toStr(left.getUserName()).compareTo(Func.toStr(right.getUserName()));
		});
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public int sendOverdueNotice(OverdueNoticeSendDTO dto) {
		if (dto == null || Func.isEmpty(dto.getPaymentId())) {
			throw new ServiceException("账单ID不能为空");
		}
		List<Long> selectedUserIds = dto.getRecipientUserIds() == null ? List.of() : dto.getRecipientUserIds().stream()
			.filter(Objects::nonNull)
			.distinct()
			.toList();
		if (selectedUserIds.isEmpty()) {
			throw new ServiceException("请至少选择一名接收人");
		}
		ContractPayment payment = requirePayment(dto.getPaymentId());
		assertAccessible(payment);
		Date now = DateUtil.now();
		validateOverdueReceivable(payment, now);
		Contract contract = contractMapper.selectContractById(payment.getContractId());
		if (contract == null) {
			throw new ServiceException("账单关联合同不存在");
		}
		RecipientContext context = loadRecipientContext();
		Map<Long, User> userMap = new LinkedHashMap<>();
		context.users.forEach(user -> userMap.put(user.getId(), user));
		List<Long> invalidUserIds = selectedUserIds.stream().filter(userId -> !userMap.containsKey(userId)).toList();
		if (!invalidUserIds.isEmpty()) {
			throw new ServiceException("所选接收人不存在或已停用，请重新选择");
		}
		Map<Long, NoticeRecipient> suggested = resolveSuggestedRecipients(contract, context);
		int inserted = 0;
		List<String> insertedRecipients = new ArrayList<>();
		for (Long userId : selectedUserIds) {
			User user = userMap.get(userId);
			NoticeRecipient suggestedRecipient = suggested.get(userId);
			String responsibilities = suggestedRecipient == null || suggestedRecipient.roles.isEmpty()
				? "指定接收人"
				: String.join("、", suggestedRecipient.roles);
			OverdueInternalNotice notice = new OverdueInternalNotice();
			notice.setPaymentId(payment.getPaymentId());
			notice.setContractId(payment.getContractId());
			notice.setRecipientUserId(user.getId());
			notice.setRecipientAccount(user.getAccount());
			notice.setRecipientName(firstNotBlank(user.getRealName(), user.getName(), user.getAccount()));
			notice.setRecipientRoles(responsibilities);
			notice.setNoticeTitle("合同账单催缴通知");
			notice.setNoticeContent(buildOverdueInternalNoticeContent(contract, payment));
			notice.setReadStatus(NOTICE_READ_UNREAD);
			notice.setParkId(payment.getParkId());
			notice.setDelFlag(DEFAULT_DEL_FLAG);
			notice.setCreateBy(currentUserName());
			notice.setCreateTime(now);
			int affected = overdueInternalNoticeMapper.insertIgnore(notice);
			inserted += affected;
			if (affected > 0) {
				insertedRecipients.add(notice.getRecipientName() + "（" + responsibilities + "）");
			}
		}
		if (!insertedRecipients.isEmpty()) {
			addLog(payment.getContractId(), "overdue_internal_notice", "发送首次逾期通知：" + String.join("、", insertedRecipients));
		}
		return inserted;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean registerLegalLetterSend(LegalLetterSendDTO dto) {
		if (dto == null || Func.isEmpty(dto.getPaymentId())) {
			throw new ServiceException("账单ID不能为空");
		}
		ContractPayment payment = requirePayment(dto.getPaymentId());
		assertAccessible(payment);
		validateOverdueHistoryReceivable(payment, DateUtil.now());
		ContractWorkflowRecord legalRecord = contractWorkflowRecordMapper.selectByContractId(payment.getContractId()).stream()
			.filter(record -> "contract_overdue_legal".equals(record.getBusinessType()))
			.filter(record -> Objects.equals(record.getPaymentId(), payment.getPaymentId()))
			.findFirst()
			.orElse(null);
		if (legalRecord == null || !"approved".equals(legalRecord.getProcessStatus())) {
			throw new ServiceException("律师函审批通过后才能登记发送");
		}
		String channel = Func.toStr(dto.getChannel(), "").trim();
		String recipient = Func.toStr(dto.getRecipient(), "").trim();
		if (StringUtil.isBlank(channel)) {
			throw new ServiceException("请选择发送方式");
		}
		if (StringUtil.isBlank(recipient)) {
			throw new ServiceException("请填写收件人");
		}
		Date sendTime = dto.getSendTime() == null ? DateUtil.now() : dto.getSendTime();
		StringBuilder desc = new StringBuilder("登记律师函发送（账单ID：")
			.append(payment.getPaymentId())
			.append("）：方式=").append(channel)
			.append("；收件人=").append(recipient)
			.append("；发送时间=").append(DateUtil.format(sendTime, DateUtil.PATTERN_DATETIME));
		if (!StringUtil.isBlank(dto.getDestination())) {
			desc.append("；送达信息=").append(dto.getDestination().trim());
		}
		if (!StringUtil.isBlank(dto.getProofUrl())) {
			desc.append("；凭证=").append(dto.getProofUrl().trim());
		}
		if (!StringUtil.isBlank(dto.getRemark())) {
			desc.append("；备注=").append(dto.getRemark().trim());
		}
		addLog(payment.getContractId(), "legal_letter_send", desc.toString());
		return true;
	}

	@Override
	public IPage<OverdueInternalNoticeVO> overdueNoticePage(IPage<OverdueInternalNoticeVO> page, String customerName,
																		String readStatus, String recordType) {
		Long userId = AuthUtil.getUserId();
		if (Func.isEmpty(userId)) {
			page.setRecords(List.of());
			return page;
		}
		String normalizedRecordType = "notice".equals(recordType) || "reminder".equals(recordType) ? recordType : "";
		page.setRecords(overdueInternalNoticeMapper.selectNoticePage(page, userId, customerName, readStatus, normalizedRecordType));
		return page;
	}

	@Override
	public PaymentNoticePlaceholderVO noticePlaceholder() {
		return new PaymentNoticePlaceholderVO(
			"通知管理",
			"通知管理统一承载收款、催款和逾期三类通知，律师函审批和发送归入逾期处理。",
			"短信、邮箱通道当前未接入真实发送服务，重发时记录为发送失败；站内信与小程序发送记录会同步生成。"
		);
	}

	@Override
	public IPage<PaymentNoticeVO> selectNoticePage(IPage<PaymentNoticeVO> page, PaymentNoticeVO query) {
		PaymentNoticeVO normalized = normalizeNoticeQuery(query);
		page.setRecords(paymentNoticeMapper.selectNoticePage(page, normalized));
		return page;
	}

	@Override
	public PaymentNoticeSummaryVO noticeSummary(PaymentNoticeVO query) {
		PaymentNoticeSummaryVO summary = paymentNoticeMapper.selectNoticeSummary(normalizeNoticeQuery(query));
		return summary == null ? new PaymentNoticeSummaryVO() : summary;
	}

	@Override
	public List<String> noticeBuildingOptions(PaymentNoticeVO query) {
		return paymentNoticeMapper.selectBuildingOptions(normalizeNoticeQuery(query));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO resendNotice(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		validateReceivableNoticePayment(payment);
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId, NOTICE_TYPE_RECEIPT);
		PaymentNotice notice = getOrCreateNotice(paymentId, NOTICE_TYPE_RECEIPT);
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(NOTICE_TYPE_RECEIPT, paymentId, null);
		contractNoticeService.buildMiniAppPayload(NOTICE_TYPE_RECEIPT, paymentId, null);
		Date now = DateUtil.now();
		notice.setNoticeType(NOTICE_TYPE_RECEIPT);
		notice.setInboxStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappStatus(NOTICE_STATUS_RESERVED);
		notice.setMiniappSendTime(null);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setFileName(file.getFileName());
		notice.setFileUrl(file.getFileUrl());
		notice.setRemark(buildNoticeRemark(detail));
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice", "重新发送收款通知");
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId, NOTICE_TYPE_RECEIPT);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractNoticeFileVO generatePaymentNoticeFile(Long paymentId, String noticeType) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		String normalizedType = normalizeNoticeType(noticeType);
		validateNoticePayment(payment, normalizedType);
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(normalizedType, paymentId, null);
		PaymentNotice notice = getOrCreateNotice(paymentId, normalizedType);
		notice.setNoticeType(normalizedType);
		notice.setFileName(file.getFileName());
		notice.setFileUrl(file.getFileUrl());
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(DateUtil.now());
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "notice_generate", noticeTypeName(normalizedType) + "文件已生成，账单ID：" + paymentId);
		return file;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendMiniAppNotice(Long paymentId, String noticeType) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		String normalizedType = normalizeNoticeType(noticeType);
		validateNoticePayment(payment, normalizedType);
		PaymentNotice notice = getOrCreateNotice(paymentId, normalizedType);
		contractNoticeService.buildMiniAppPayload(normalizedType, paymentId, null);
		Date now = DateUtil.now();
		notice.setMiniappStatus(NOTICE_STATUS_RESERVED);
		notice.setMiniappSendTime(null);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_miniapp", noticeTypeName(normalizedType) + "小程序通知，发送通道待接入，账单ID：" + paymentId);
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId, normalizedType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendSmsNotice(Long paymentId, String noticeType) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		String normalizedType = normalizeNoticeType(noticeType);
		validateNoticePayment(payment, normalizedType);
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId, normalizedType);
		PaymentNotice notice = getOrCreateNotice(paymentId, normalizedType);
		Date now = DateUtil.now();
		notice.setSmsStatus(NOTICE_STATUS_FAILED);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setRemark(hasText(detail == null ? null : detail.getContactPhone()) ? "短信通道未接入，发送失败" : "缺少手机号，短信发送失败");
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_sms", noticeTypeName(normalizedType) + "：" + notice.getRemark() + "，账单ID：" + paymentId);
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId, normalizedType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendEmailNotice(Long paymentId, String noticeType) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		String normalizedType = normalizeNoticeType(noticeType);
		validateNoticePayment(payment, normalizedType);
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId, normalizedType);
		PaymentNotice notice = getOrCreateNotice(paymentId, normalizedType);
		Date now = DateUtil.now();
		notice.setEmailStatus(NOTICE_STATUS_FAILED);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setRemark(hasText(detail == null ? null : detail.getContactEmail()) ? "邮箱通道未接入，发送失败" : "缺少邮箱，邮件发送失败");
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_email", noticeTypeName(normalizedType) + "：" + notice.getRemark() + "，账单ID：" + paymentId);
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId, normalizedType);
	}

	private RecipientContext loadRecipientContext() {
		String tenantId = AuthUtil.getTenantId();
		List<User> users = userService.list(Wrappers.<User>lambdaQuery()
			.eq(User::getTenantId, tenantId)
			.eq(User::getStatus, 1));
		List<Role> roles = roleService.list(Wrappers.<Role>lambdaQuery()
			.eq(Role::getTenantId, tenantId)
			.eq(Role::getStatus, 1));
		List<Dept> depts = deptService.list(Wrappers.<Dept>lambdaQuery()
			.eq(Dept::getTenantId, tenantId)
			.eq(Dept::getStatus, 1));
		Map<Long, Role> roleMap = new LinkedHashMap<>();
		roles.forEach(role -> roleMap.put(role.getId(), role));
		Map<Long, Dept> deptMap = new LinkedHashMap<>();
		depts.forEach(dept -> deptMap.put(dept.getId(), dept));
		return new RecipientContext(users, roleMap, deptMap);
	}

	private Map<Long, NoticeRecipient> resolveSuggestedRecipients(Contract contract, RecipientContext context) {
		List<User> users = context.users;
		Map<Long, Role> roleMap = context.roleMap;
		Map<Long, Dept> deptMap = context.deptMap;
		Map<Long, NoticeRecipient> recipients = new LinkedHashMap<>();
		User followUser = findUser(users, firstNotBlank(contract.getFollowUser(), contract.getCreateBy()));
		if (followUser == null) {
			followUser = users.stream()
				.filter(user -> userHasKeywordDept(user, deptMap, List.of("招商"))
					|| userHasKeywordRole(user, roleMap, List.of("招商")))
				.findFirst()
				.orElse(null);
		}
		if (followUser == null) {
			followUser = findUser(users, contract.getCreateBy());
		}
		addRecipient(recipients, followUser, "招商员");

		List<User> deptLeaders = resolveDepartmentLeaders(users, followUser, deptMap, roleMap);
		deptLeaders.forEach(user -> addRecipient(recipients, user, "部门领导"));

		List<User> supervisingLeaders = users.stream()
			.filter(user -> userHasKeywordRole(user, roleMap, List.of("分管领导", "主管领导", "总经理", "老板", "boss")))
			.toList();
		supervisingLeaders.forEach(user -> addRecipient(recipients, user, "分管领导"));

		List<User> financeUsers = users.stream()
			.filter(user -> userHasKeywordDept(user, deptMap, List.of("财务"))
				|| userHasKeywordRole(user, roleMap, List.of("财务")))
			.toList();
		financeUsers.forEach(user -> addRecipient(recipients, user, "财务跟进"));

		if (deptLeaders.isEmpty() && !supervisingLeaders.isEmpty()) {
			addRecipient(recipients, supervisingLeaders.get(0), "部门领导");
		}
		return recipients;
	}

	private String userDeptNames(User user, Map<Long, Dept> deptMap) {
		return Func.toLongList(user.getDeptId()).stream()
			.map(deptMap::get)
			.filter(Objects::nonNull)
			.map(dept -> firstNotBlank(dept.getDeptName(), dept.getFullName()))
			.filter(StringUtil::isNotBlank)
			.distinct()
			.reduce((left, right) -> left + "、" + right)
			.orElse("");
	}

	private String userRoleNames(User user, Map<Long, Role> roleMap) {
		return Func.toLongList(user.getRoleId()).stream()
			.map(roleMap::get)
			.filter(Objects::nonNull)
			.map(role -> firstNotBlank(role.getRoleName(), role.getRoleAlias()))
			.filter(StringUtil::isNotBlank)
			.distinct()
			.reduce((left, right) -> left + "、" + right)
			.orElse("");
	}

	private List<User> resolveDepartmentLeaders(List<User> users, User followUser, Map<Long, Dept> deptMap, Map<Long, Role> roleMap) {
		if (followUser == null) {
			return List.of();
		}
		Set<Long> leaderIds = new LinkedHashSet<>(Func.toLongList(followUser.getLeaderId()));
		List<Long> followDeptIds = Func.toLongList(followUser.getDeptId());
		for (Long deptId : followDeptIds) {
			Dept dept = deptMap.get(deptId);
			if (dept != null) {
				leaderIds.addAll(Func.toLongList(dept.getLeaderId()));
			}
		}
		List<User> configured = users.stream().filter(user -> leaderIds.contains(user.getId())).toList();
		if (!configured.isEmpty()) {
			return configured;
		}
		return users.stream()
			.filter(user -> Func.toLongList(user.getDeptId()).stream().anyMatch(followDeptIds::contains))
			.filter(user -> userHasKeywordRole(user, roleMap, List.of("部门领导", "经理", "老板", "manager", "boss")))
			.toList();
	}

	private User findUser(List<User> users, String accountOrName) {
		if (StringUtil.isBlank(accountOrName)) {
			return null;
		}
		String target = accountOrName.trim();
		return users.stream()
			.filter(user -> target.equals(user.getAccount()) || target.equals(user.getRealName()) || target.equals(user.getName()))
			.findFirst()
			.orElse(null);
	}

	private boolean userHasKeywordRole(User user, Map<Long, Role> roleMap, List<String> keywords) {
		return Func.toLongList(user.getRoleId()).stream()
			.map(roleMap::get)
			.filter(Objects::nonNull)
			.anyMatch(role -> containsAny(firstNotBlank(role.getRoleName(), "") + " " + firstNotBlank(role.getRoleAlias(), ""), keywords));
	}

	private boolean userHasKeywordDept(User user, Map<Long, Dept> deptMap, List<String> keywords) {
		return Func.toLongList(user.getDeptId()).stream()
			.map(deptMap::get)
			.filter(Objects::nonNull)
			.anyMatch(dept -> containsAny(firstNotBlank(dept.getDeptName(), dept.getFullName(), ""), keywords));
	}

	private boolean containsAny(String source, List<String> keywords) {
		String normalized = Func.toStr(source, "").toLowerCase();
		return keywords.stream().anyMatch(keyword -> normalized.contains(keyword.toLowerCase()));
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (!StringUtil.isBlank(value)) {
				return value;
			}
		}
		return "";
	}

	private void addRecipient(Map<Long, NoticeRecipient> recipients, User user, String role) {
		if (user == null || user.getId() == null) {
			return;
		}
		recipients.computeIfAbsent(user.getId(), ignored -> new NoticeRecipient(user)).roles.add(role);
	}

	private String buildOverdueInternalNoticeContent(Contract contract, ContractPayment payment) {
		BigDecimal amountDue = payment.getAmountDue() == null ? BigDecimal.ZERO : payment.getAmountDue();
		BigDecimal amountPaid = payment.getAmountPaid() == null ? BigDecimal.ZERO : payment.getAmountPaid();
		String payDeadline = payment.getPayDeadline() == null ? "-" : DateUtil.format(payment.getPayDeadline(), DateUtil.PATTERN_DATE);
		return "合同" + firstNotBlank(contract.getContractNo(), "-") + "，企业" + firstNotBlank(contract.getCustomerName(), "-")
			+ "，" + firstNotBlank(payment.getFeeName(), "费用") + "未缴金额" + amountDue.subtract(amountPaid).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString()
			+ "元，应缴日期" + payDeadline + "。";
	}

	private boolean isOverdue(ContractPayment payment, Date now) {
		return payment.getPayDeadline() != null
			&& DateUtil.format(payment.getPayDeadline(), DateUtil.PATTERN_DATE).compareTo(DateUtil.format(now, DateUtil.PATTERN_DATE)) < 0;
	}

	private void validateOverdueReceivable(ContractPayment payment, Date now) {
		if (!DIRECTION_RECEIVABLE.equals(normalizeDirection(payment == null ? null : payment.getDirection()))) {
			throw new ServiceException("付款账单不允许进入逾期催缴");
		}
		if (PAY_STATUS_PAID.equals(payment.getPayStatus())) {
			throw new ServiceException("已缴账单无需催缴");
		}
		if (!isOverdue(payment, now)) {
			throw new ServiceException("当前账单尚未逾期");
		}
	}

	private void validateReceivableConfirmation(ContractPayment payment, ContractPayment confirmation) {
		if (!DIRECTION_RECEIVABLE.equals(normalizeDirection(payment == null ? null : payment.getDirection()))) {
			throw new ServiceException("付款账单需先完成付款申请审批，不能直接确认缴费");
		}
		if (PAY_STATUS_PAID.equals(payment.getPayStatus())) {
			throw new ServiceException("当前账单已缴费，无需重复确认");
		}
		if (confirmation == null || confirmation.getPayTime() == null) {
			throw new ServiceException("请选择收款时间");
		}
		if (StringUtil.isBlank(confirmation.getPaymentVoucherUrl())) {
			throw new ServiceException("请上传收款凭证");
		}
	}

	private void validatePayableConfirmation(ContractPayment payment, ContractPayment confirmation) {
		if (!DIRECTION_PAYABLE.equals(normalizeDirection(payment == null ? null : payment.getDirection()))) {
			throw new ServiceException("当前账单不是付款账单");
		}
		if (!"approved".equals(payment.getPaymentApprovalStatus())) {
			throw new ServiceException("付款申请审批通过后才可以确认付款");
		}
		if (payment.getPaymentApprovalTime() == null) {
			throw new ServiceException("付款申请缺少审批完成时间，请检查审批记录");
		}
		if (confirmation == null || confirmation.getPayTime() == null) {
			throw new ServiceException("请选择付款时间");
		}
		if (confirmation != null && confirmation.getPayTime() != null
			&& confirmation.getPayTime().before(payment.getPaymentApprovalTime())) {
			throw new ServiceException("付款时间不能早于付款申请审批完成时间");
		}
		if (PAY_STATUS_PAID.equals(payment.getPayStatus())) {
			throw new ServiceException("当前账单已完成付款，无需重复确认");
		}
		if (confirmation == null || StringUtil.isBlank(confirmation.getPaymentVoucherUrl())) {
			throw new ServiceException("请上传付款凭证");
		}
	}

	private void validateReceivableNoticePayment(ContractPayment payment) {
		if (!DIRECTION_RECEIVABLE.equals(normalizeDirection(payment == null ? null : payment.getDirection()))) {
			throw new ServiceException("付款账单不能生成收款通知");
		}
	}

	private void validateNoticePayment(ContractPayment payment, String noticeType) {
		if (NOTICE_TYPE_REMINDER.equals(noticeType) || NOTICE_TYPE_OVERDUE.equals(noticeType)) {
			validateOverdueReceivable(payment, DateUtil.now());
			return;
		}
		validateReceivableNoticePayment(payment);
	}

	private static class NoticeRecipient {

		private final User user;
		private final Set<String> roles = new LinkedHashSet<>();

		private NoticeRecipient(User user) {
			this.user = user;
		}

	}

	private static class RecipientContext {

		private final List<User> users;
		private final Map<Long, Role> roleMap;
		private final Map<Long, Dept> deptMap;

		private RecipientContext(List<User> users, Map<Long, Role> roleMap, Map<Long, Dept> deptMap) {
			this.users = users;
			this.roleMap = roleMap;
			this.deptMap = deptMap;
		}

	}

	private void attachContractRooms(List<Contract> contracts) {
		if (contracts == null || contracts.isEmpty()) {
			return;
		}
		Set<Long> allRoomIds = new LinkedHashSet<>();
		for (Contract contract : contracts) {
			allRoomIds.addAll(contractRoomIds(contract));
		}
		if (allRoomIds.isEmpty()) {
			return;
		}
		Map<Long, Room> roomMap = roomMap(new ArrayList<>(allRoomIds));
		for (Contract contract : contracts) {
			List<Room> rooms = new ArrayList<>();
			for (Long roomId : contractRoomIds(contract)) {
				Room room = roomMap.get(roomId);
				if (room != null) {
					rooms.add(room);
				}
			}
			contract.setRooms(rooms);
		}
	}

	private PaymentRoomSelection resolvePaymentRoomSelection(Contract contract, String requestedRoomIds) {
		List<Long> contractRoomIds = contractRoomIds(contract);
		List<Long> selectedRoomIds = parseRoomIds(requestedRoomIds);
		if (selectedRoomIds.isEmpty()) {
			selectedRoomIds = contractRoomIds;
		}
		if (!contractRoomIds.isEmpty()) {
			Set<Long> allowedRoomIds = new LinkedHashSet<>(contractRoomIds);
			for (Long roomId : selectedRoomIds) {
				if (!allowedRoomIds.contains(roomId)) {
					throw new ServiceException("所选房源不属于当前合同");
				}
			}
		} else if (!selectedRoomIds.isEmpty()) {
			throw new ServiceException("当前合同未配置可选房源");
		}
		if (selectedRoomIds.isEmpty()) {
			return new PaymentRoomSelection("", contract == null ? "" : Func.toStr(contract.getRoomName(), ""),
				contract == null ? "" : Func.toStr(contract.getBuildingName(), ""));
		}
		Map<Long, Room> roomMap = roomMap(selectedRoomIds);
		List<Room> selectedRooms = new ArrayList<>();
		for (Long roomId : selectedRoomIds) {
			Room room = roomMap.get(roomId);
			if (room == null) {
				throw new ServiceException("所选房源不存在或已被删除");
			}
			selectedRooms.add(room);
		}
		return new PaymentRoomSelection(joinLongs(selectedRoomIds), joinRoomNames(selectedRooms), joinBuildingNames(selectedRooms));
	}

	private Map<Long, Room> roomMap(List<Long> roomIds) {
		Map<Long, Room> roomMap = new LinkedHashMap<>();
		if (roomIds == null || roomIds.isEmpty()) {
			return roomMap;
		}
		for (Room room : paymentMapper.selectRoomsByIds(roomIds)) {
			if (room.getId() != null) {
				roomMap.put(room.getId(), room);
			}
		}
		return roomMap;
	}

	private List<Long> contractRoomIds(Contract contract) {
		LinkedHashSet<Long> roomIds = new LinkedHashSet<>();
		if (contract == null) {
			return new ArrayList<>();
		}
		if (contract.getRoomId() != null) {
			roomIds.add(contract.getRoomId());
		}
		roomIds.addAll(parseRoomIds(contract.getRoomIds()));
		return new ArrayList<>(roomIds);
	}

	private List<Long> parseRoomIds(String source) {
		LinkedHashSet<Long> roomIds = new LinkedHashSet<>();
		if (StringUtil.isBlank(source)) {
			return new ArrayList<>();
		}
		for (String item : source.split("[,，、]")) {
			String normalized = Func.toStr(item, "").replace("room_", "").trim();
			if (StringUtil.isBlank(normalized)) {
				continue;
			}
			try {
				roomIds.add(Long.valueOf(normalized));
			} catch (NumberFormatException ignored) {
				// Ignore invalid historical room id fragments.
			}
		}
		return new ArrayList<>(roomIds);
	}

	private String joinLongs(List<Long> values) {
		List<String> items = new ArrayList<>();
		for (Long value : values) {
			if (value != null) {
				items.add(String.valueOf(value));
			}
		}
		return String.join(",", items);
	}

	private String joinRoomNames(List<Room> rooms) {
		List<String> items = new ArrayList<>();
		for (Room room : rooms) {
			if (room != null && !StringUtil.isBlank(room.getName())) {
				items.add(room.getName());
			}
		}
		return String.join("、", items);
	}

	private String joinBuildingNames(List<Room> rooms) {
		LinkedHashSet<String> items = new LinkedHashSet<>();
		for (Room room : rooms) {
			if (room != null && !StringUtil.isBlank(room.getBuildingName())) {
				items.add(room.getBuildingName());
			}
		}
		return String.join("、", items);
	}

	private String normalizeDirection(String direction) {
		String value = Func.toStr(direction, DIRECTION_RECEIVABLE).trim();
		if (!DIRECTION_RECEIVABLE.equals(value) && !DIRECTION_PAYABLE.equals(value)) {
			throw new ServiceException("账单方向不正确");
		}
		return value;
	}

	private void validateCreatePayment(ContractPayment payment) {
		if (StringUtil.isBlank(payment.getFeeType()) || StringUtil.isBlank(payment.getFeeName())) {
			throw new ServiceException("请选择费用类型");
		}
		if (payment.getPeriodStart() == null || payment.getPeriodEnd() == null) {
			throw new ServiceException("请选择计费周期");
		}
		if (payment.getPeriodStart().after(payment.getPeriodEnd())) {
			throw new ServiceException("计费周期开始日期不能晚于结束日期");
		}
		if (payment.getAmountDue() == null || payment.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("账单金额必须大于0");
		}
		if (payment.getPayDeadline() == null) {
			throw new ServiceException("请选择应收/应付日期");
		}
		assertNonNegative(payment.getTaxRate(), "税率");
		assertNonNegative(payment.getLateFeeRatio(), "滞纳金比例");
		assertNonNegative(payment.getLateFeeCap(), "滞纳金上限");
		if (payment.getLateFeeStartDays() != null && payment.getLateFeeStartDays() < 0) {
			throw new ServiceException("滞纳金起算天数不能小于0");
		}
	}

	private void assertNonNegative(BigDecimal value, String fieldName) {
		if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
			throw new ServiceException(fieldName + "不能小于0");
		}
	}

	private String directionName(String direction) {
		return DIRECTION_PAYABLE.equals(direction) ? "付款" : "收款";
	}

	private ContractPayment normalizeQuery(ContractPayment payment) {
		ContractPayment query = payment == null ? new ContractPayment() : payment;
		if (query.getParkId() != null && query.getParkId() <= 0) {
			query.setParkId(null);
		}
		return query;
	}

	private PaymentNoticeVO normalizeNoticeQuery(PaymentNoticeVO query) {
		PaymentNoticeVO normalized = query == null ? new PaymentNoticeVO() : query;
		if (normalized.getParkId() != null && normalized.getParkId() <= 0) {
			normalized.setParkId(null);
		}
		return normalized;
	}

	private ContractPayment requirePayment(Long paymentId) {
		if (Func.isEmpty(paymentId)) {
			throw new ServiceException("账单ID不能为空");
		}
		ContractPayment payment = paymentMapper.selectPaymentById(paymentId);
		if (payment == null) {
			throw new ServiceException("账单不存在");
		}
		return payment;
	}

	private ContractPayment requirePaymentForUpdate(Long paymentId) {
		if (Func.isEmpty(paymentId)) {
			throw new ServiceException("账单ID不能为空");
		}
		if (contractPaymentMapper.selectByIdForUpdate(paymentId) == null) {
			throw new ServiceException("账单不存在");
		}
		return requirePayment(paymentId);
	}

	private void assertAccessible(ContractPayment payment) {
		// 账单可见范围不再与用户部门绑定，操作权限由菜单和按钮权限控制。
	}

	private PaymentNotice getOrCreateNotice(Long paymentId, String noticeType) {
		PaymentNotice notice = paymentNoticeMapper.selectByPaymentId(paymentId, noticeType);
		if (notice != null) {
			return notice;
		}
		Date now = DateUtil.now();
		PaymentNotice created = new PaymentNotice();
		created.setPaymentId(paymentId);
		created.setNoticeNo(generateNoticeNo(paymentId));
		created.setNoticeType(noticeType);
		created.setSmsStatus(NOTICE_STATUS_PENDING);
		created.setEmailStatus(NOTICE_STATUS_PENDING);
		created.setInboxStatus(NOTICE_STATUS_PENDING);
		created.setMiniappStatus(NOTICE_STATUS_PENDING);
		created.setSendCount(0);
		created.setDelFlag("0");
		created.setCreateBy(currentUserName());
		created.setCreateTime(now);
		paymentNoticeMapper.insert(created);
		return created;
	}

	private String normalizeNoticeType(String noticeType) {
		if (NOTICE_TYPE_REMINDER.equals(noticeType) || NOTICE_TYPE_OVERDUE.equals(noticeType)) {
			return noticeType;
		}
		return NOTICE_TYPE_RECEIPT;
	}

	private String noticeTypeName(String noticeType) {
		return switch (normalizeNoticeType(noticeType)) {
			case NOTICE_TYPE_REMINDER -> "催款通知";
			case NOTICE_TYPE_OVERDUE -> "逾期通知";
			default -> "收款通知";
		};
	}

	private String generateNoticeNo(Long paymentId) {
		return "SKTZ" + DateUtil.format(DateUtil.now(), "yyyyMMdd") + "-" + paymentId;
	}

	private String buildNoticeRemark(PaymentNoticeVO detail) {
		if (detail == null) {
			return "收款通知已生成，等待通道回执";
		}
		String sms = hasText(detail.getContactPhone()) ? "短信通道未接入，未发送" : "缺少手机号";
		String email = hasText(detail.getContactEmail()) ? "邮箱通道未接入，未发送" : "缺少邮箱";
		return sms + "；" + email + "；站内信已发送；小程序发送记录已生成";
	}

	private boolean hasText(String value) {
		return !StringUtil.isBlank(value);
	}

	private boolean isDocumentRecord(ContractLog log, Long paymentId) {
		if (log == null) {
			return false;
		}
		String action = Func.toStr(log.getAction());
		return ("notice_generate".equals(action) || "payment_notice_generate".equals(action)) && logBelongsToPayment(log, paymentId);
	}

	private boolean isMiniAppRecord(ContractLog log, Long paymentId) {
		if (log == null) {
			return false;
		}
		String action = Func.toStr(log.getAction());
		return ("notice_miniapp".equals(action) || "payment_notice_miniapp".equals(action)) && logBelongsToPayment(log, paymentId);
	}

	private boolean isLegalSendRecord(ContractLog log, Long paymentId) {
		return log != null && "legal_letter_send".equals(Func.toStr(log.getAction()))
			&& logBelongsToPayment(log, paymentId);
	}

	private boolean logBelongsToPayment(ContractLog log, Long paymentId) {
		String actionDesc = Func.toStr(log == null ? null : log.getActionDesc());
		if (!actionDesc.contains("账单ID：")) {
			return true;
		}
		return paymentId != null && actionDesc.contains("账单ID：" + paymentId);
	}

	private boolean isOverdueWorkflowRecord(ContractWorkflowRecord record, Long paymentId) {
		if (record == null) {
			return false;
		}
		String businessType = Func.toStr(record.getBusinessType());
		if ("contract_overdue_legal".equals(businessType)
			|| "contract_termination".equals(businessType)
			|| "contract_room_review".equals(businessType)) {
			// 新记录必须绑定当前账单；旧退租/验收记录没有 payment_id 时按合同兼容回显。
			return record.getPaymentId() == null
				? !"contract_overdue_legal".equals(businessType)
				: Objects.equals(record.getPaymentId(), paymentId);
		}
		return false;
	}

	private void validateOverdueHistoryReceivable(ContractPayment payment, Date now) {
		if (!DIRECTION_RECEIVABLE.equals(normalizeDirection(payment == null ? null : payment.getDirection()))) {
			throw new ServiceException("付款账单不属于逾期处置记录");
		}
		if (!isOverdue(payment, now)) {
			throw new ServiceException("当前账单没有逾期记录");
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
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private String currentOperatorName() {
		String nickName = AuthUtil.getNickName();
		return StringUtil.isBlank(nickName) ? currentUserName() : nickName;
	}

	private String normalizeRemindSource(String source) {
		return switch (Func.toStr(source)) {
			case "overdue_bill" -> "overdue_bill";
			case "overdue_reminder" -> "overdue_reminder";
			default -> "bill_management";
		};
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private static class PaymentRoomSelection {

		private final String roomIds;
		private final String roomName;
		private final String buildingName;

		private PaymentRoomSelection(String roomIds, String roomName, String buildingName) {
			this.roomIds = roomIds;
			this.roomName = roomName;
			this.buildingName = buildingName;
		}

	}

}
