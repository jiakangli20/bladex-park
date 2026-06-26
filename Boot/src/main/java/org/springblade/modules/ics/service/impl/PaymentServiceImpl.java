/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springblade.modules.ics.mapper.PaymentMapper;
import org.springblade.modules.ics.mapper.PaymentNoticeMapper;
import org.springblade.modules.ics.pojo.entity.PaymentNotice;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;
import org.springblade.modules.ics.service.IPaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 财务缴费服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

	private static final String SCOPE_OVERDUE = "overdue";
	private static final String PAY_STATUS_PAID = "1";
	private static final String PAY_STATUS_PARTIAL = "3";
	private static final String REMIND_STATUS_REMINDED = "1";
	private static final String NOTICE_TYPE_PAYMENT = IContractNoticeService.NOTICE_PAYMENT;
	private static final String NOTICE_STATUS_PENDING = "pending";
	private static final String NOTICE_STATUS_SUCCESS = "success";
	private static final String NOTICE_STATUS_FAILED = "failed";

	private final PaymentMapper paymentMapper;
	private final PaymentNoticeMapper paymentNoticeMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;
	private final IContractNoticeService contractNoticeService;

	@Override
	public IPage<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, ContractPayment payment, String scope) {
		ContractPayment query = normalizeQuery(payment);
		page.setRecords(paymentMapper.selectPaymentPage(page, query, SCOPE_OVERDUE.equals(scope)));
		return page;
	}

	@Override
	public ContractPayment selectPaymentById(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
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
		return paymentMapper.selectPaymentPage(null, query, false);
	}

	@Override
	public PaymentSummaryVO summary(ContractPayment payment) {
		PaymentSummaryVO summary = paymentMapper.selectSummary(normalizeQuery(payment));
		return summary == null ? new PaymentSummaryVO() : summary;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean confirm(Long paymentId, ContractPayment payment) {
		ContractPayment existing = requirePayment(paymentId);
		assertAccessible(existing);
		BigDecimal amountPaid = payment == null ? existing.getAmountDue() : payment.getAmountPaid();
		if (amountPaid == null) {
			amountPaid = existing.getAmountDue();
		}
		if (amountPaid == null || amountPaid.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("实收金额必须大于0");
		}
		String payStatus = amountPaid.compareTo(nullToZero(existing.getAmountDue())) < 0 ? PAY_STATUS_PARTIAL : PAY_STATUS_PAID;
		Date now = DateUtil.now();
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setAmountPaid(amountPaid);
		update.setPayStatus(payStatus);
		update.setPayTime(now);
		update.setRemark(payment == null ? null : payment.getRemark());
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			String feeName = StringUtil.isBlank(existing.getFeeName()) ? "-" : existing.getFeeName();
			addLog(existing.getContractId(), "payment", "确认缴费，费用：" + feeName);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean remind(Long paymentId) {
		ContractPayment existing = requirePayment(paymentId);
		assertAccessible(existing);
		if (PAY_STATUS_PAID.equals(existing.getPayStatus())) {
			throw new ServiceException("已缴账单无需催缴");
		}
		Date now = DateUtil.now();
		ContractPayment update = new ContractPayment();
		update.setPaymentId(paymentId);
		update.setRemindStatus(REMIND_STATUS_REMINDED);
		update.setRemindTime(now);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(now);
		boolean result = contractPaymentMapper.updateById(update) > 0;
		if (result) {
			addLog(existing.getContractId(), "remind", "发起催缴提醒");
		}
		return result;
	}

	@Override
	public List<ContractLog> logList(Long contractId) {
		if (Func.isEmpty(contractId)) {
			throw new ServiceException("合同ID不能为空");
		}
		ContractPayment query = new ContractPayment();
		query.setContractId(contractId);
		List<ContractPayment> payments = paymentMapper.selectPaymentPage(null, normalizeQuery(query), false);
		if (payments.isEmpty()) {
			return List.of();
		}
		return contractLogMapper.selectByContractId(contractId);
	}

	@Override
	public PaymentNoticePlaceholderVO noticePlaceholder() {
		return new PaymentNoticePlaceholderVO(
			"收款通知",
			"收款通知已按账单生成列表，支持重发、下载和小程序发送预留接口。",
			"短信、邮箱、站内信通道状态记录在收款通知表，后续接入真实通道后替换发送实现。"
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
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(NOTICE_TYPE_PAYMENT, paymentId, null);
		Date now = DateUtil.now();
		notice.setNoticeType(NOTICE_TYPE_PAYMENT);
		notice.setSmsStatus(hasText(detail == null ? null : detail.getContactPhone()) ? NOTICE_STATUS_SUCCESS : NOTICE_STATUS_FAILED);
		notice.setEmailStatus(hasText(detail == null ? null : detail.getContactEmail()) ? NOTICE_STATUS_SUCCESS : NOTICE_STATUS_FAILED);
		notice.setInboxStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappStatus(NOTICE_STATUS_PENDING);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setFileName(file.getFileName());
		notice.setFileUrl(file.getFileUrl());
		notice.setRemark(buildNoticeRemark(detail));
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice", "重新发送收款通知");
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ContractNoticeFileVO generatePaymentNoticeFile(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(NOTICE_TYPE_PAYMENT, paymentId, null);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		notice.setNoticeType(NOTICE_TYPE_PAYMENT);
		notice.setFileName(file.getFileName());
		notice.setFileUrl(file.getFileUrl());
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(DateUtil.now());
		paymentNoticeMapper.updateById(notice);
		return file;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendMiniAppNotice(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		contractNoticeService.buildMiniAppPayload(NOTICE_TYPE_PAYMENT, paymentId, null);
		Date now = DateUtil.now();
		notice.setMiniappStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappSendTime(now);
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_miniapp", "发送收款通知到小程序");
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
	}

	private ContractPayment normalizeQuery(ContractPayment payment) {
		ContractPayment query = payment == null ? new ContractPayment() : payment;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		} else if (query.getParkId() != null && query.getParkId() <= 0) {
			query.setParkId(null);
		}
		return query;
	}

	private PaymentNoticeVO normalizeNoticeQuery(PaymentNoticeVO query) {
		PaymentNoticeVO normalized = query == null ? new PaymentNoticeVO() : query;
		if (!AuthUtil.isAdministrator()) {
			normalized.setParkId(currentParkId());
		} else if (normalized.getParkId() != null && normalized.getParkId() <= 0) {
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

	private void assertAccessible(ContractPayment payment) {
		if (AuthUtil.isAdministrator()) {
			return;
		}
		if (!Objects.equals(currentParkId(), payment.getParkId())) {
			throw new ServiceException("无权操作该园区账单");
		}
	}

	private PaymentNotice getOrCreateNotice(Long paymentId) {
		PaymentNotice notice = paymentNoticeMapper.selectByPaymentId(paymentId);
		if (notice != null) {
			return notice;
		}
		Date now = DateUtil.now();
		PaymentNotice created = new PaymentNotice();
		created.setPaymentId(paymentId);
		created.setNoticeNo(generateNoticeNo(paymentId));
		created.setNoticeType(NOTICE_TYPE_PAYMENT);
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

	private String generateNoticeNo(Long paymentId) {
		return "SKTZ" + DateUtil.format(DateUtil.now(), "yyyyMMdd") + "-" + paymentId;
	}

	private String buildNoticeRemark(PaymentNoticeVO detail) {
		if (detail == null) {
			return "收款通知已生成，等待通道回执";
		}
		String sms = hasText(detail.getContactPhone()) ? "短信已发送" : "缺少手机号";
		String email = hasText(detail.getContactEmail()) ? "邮件已发送" : "缺少邮箱";
		return sms + "；" + email + "；站内信已发送";
	}

	private boolean hasText(String value) {
		return !StringUtil.isBlank(value);
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

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

}
