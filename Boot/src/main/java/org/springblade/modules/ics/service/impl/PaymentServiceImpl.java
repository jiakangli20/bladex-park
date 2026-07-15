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
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractWorkflowRecordMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springblade.modules.ics.mapper.PaymentMapper;
import org.springblade.modules.ics.mapper.PaymentNoticeMapper;
import org.springblade.modules.ics.pojo.entity.PaymentNotice;
import org.springblade.modules.ics.pojo.vo.OverdueDisposalDetailVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.park.pojo.entity.Room;
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
	private static final String PAY_STATUS_PAID = "1";
	private static final String PAY_STATUS_PARTIAL = "3";
	private static final String PAY_STATUS_UNPAID = "0";
	private static final String REMIND_STATUS_NONE = "0";
	private static final String DIRECTION_RECEIVABLE = "receivable";
	private static final String DIRECTION_PAYABLE = "payable";
	private static final String SPECIAL_BILL_REGULAR = "regular";
	private static final String DEFAULT_COMPANY_NAME = "吴中金融招商服务有限公司";
	private static final String REMIND_STATUS_REMINDED = "1";
	private static final String NOTICE_TYPE_RECEIPT = IContractNoticeService.NOTICE_INVOICE;
	private static final String NOTICE_STATUS_PENDING = "pending";
	private static final String NOTICE_STATUS_SUCCESS = "success";
	private static final String NOTICE_STATUS_FAILED = "failed";

	private final PaymentMapper paymentMapper;
	private final PaymentNoticeMapper paymentNoticeMapper;
	private final ContractMapper contractMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;
	private final ContractWorkflowRecordMapper contractWorkflowRecordMapper;
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
	public ContractPayment create(ContractPayment payment) {
		if (payment == null || Func.isEmpty(payment.getContractId())) {
			throw new ServiceException("请选择关联合同");
		}
		Contract contract = contractMapper.selectContractById(payment.getContractId());
		if (contract == null) {
			throw new ServiceException("关联合同不存在");
		}
		if (!AuthUtil.isAdministrator() && !Objects.equals(currentParkId(), contract.getParkId())) {
			throw new ServiceException("无权为该园区合同创建账单");
		}
		String direction = normalizeDirection(payment.getDirection());
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
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		List<Contract> contracts = paymentMapper.selectContractOptions(StringUtil.isBlank(keyword) ? null : keyword.trim(), parkId);
		attachContractRooms(contracts);
		return contracts;
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
		List<ContractPayment> payments = paymentMapper.selectPaymentPage(null, normalizeQuery(query), false);
		if (payments.isEmpty()) {
			return List.of();
		}
		return contractLogMapper.selectByContractId(contractId);
	}

	@Override
	public OverdueDisposalDetailVO overdueDisposalDetail(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		OverdueDisposalDetailVO detail = new OverdueDisposalDetailVO();
		detail.setPaymentNotice(paymentNoticeMapper.selectNoticeByPaymentId(paymentId));
		List<ContractLog> logs = payment.getContractId() == null ? List.of() : contractLogMapper.selectByContractId(payment.getContractId());
		detail.setDocumentRecords(logs.stream().filter(log -> isDocumentRecord(log, paymentId)).toList());
		detail.setMiniAppRecords(logs.stream().filter(log -> isMiniAppRecord(log, paymentId)).toList());
		detail.setWorkflowRecords(payment.getContractId() == null
			? List.of()
			: contractWorkflowRecordMapper.selectByContractId(payment.getContractId()).stream()
				.filter(record -> isOverdueWorkflowRecord(record, paymentId))
				.toList());
		return detail;
	}

	@Override
	public PaymentNoticePlaceholderVO noticePlaceholder() {
		return new PaymentNoticePlaceholderVO(
			"收款通知",
			"收款通知已按账单生成列表，下载文件对应《开票申请单》，支持重发和小程序发送。",
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
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(NOTICE_TYPE_RECEIPT, paymentId, null);
		contractNoticeService.buildMiniAppPayload(NOTICE_TYPE_RECEIPT, paymentId, null);
		Date now = DateUtil.now();
		notice.setNoticeType(NOTICE_TYPE_RECEIPT);
		notice.setInboxStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappSendTime(now);
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
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(NOTICE_TYPE_RECEIPT, paymentId, null);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		notice.setNoticeType(NOTICE_TYPE_RECEIPT);
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
		contractNoticeService.buildMiniAppPayload(NOTICE_TYPE_RECEIPT, paymentId, null);
		Date now = DateUtil.now();
		notice.setMiniappStatus(NOTICE_STATUS_SUCCESS);
		notice.setMiniappSendTime(now);
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_miniapp", "发送收款通知到小程序");
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendSmsNotice(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		Date now = DateUtil.now();
		notice.setSmsStatus(NOTICE_STATUS_FAILED);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setRemark(hasText(detail == null ? null : detail.getContactPhone()) ? "短信通道未接入，发送失败" : "缺少手机号，短信发送失败");
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_sms", notice.getRemark());
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PaymentNoticeVO sendEmailNotice(Long paymentId) {
		ContractPayment payment = requirePayment(paymentId);
		assertAccessible(payment);
		PaymentNoticeVO detail = paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
		PaymentNotice notice = getOrCreateNotice(paymentId);
		Date now = DateUtil.now();
		notice.setEmailStatus(NOTICE_STATUS_FAILED);
		notice.setSendCount((notice.getSendCount() == null ? 0 : notice.getSendCount()) + 1);
		notice.setLastSendTime(now);
		notice.setRemark(hasText(detail == null ? null : detail.getContactEmail()) ? "邮箱通道未接入，发送失败" : "缺少邮箱，邮件发送失败");
		notice.setUpdateBy(currentUserName());
		notice.setUpdateTime(now);
		paymentNoticeMapper.updateById(notice);
		addLog(payment.getContractId(), "payment_notice_email", notice.getRemark());
		return paymentNoticeMapper.selectNoticeByPaymentId(paymentId);
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
		created.setNoticeType(NOTICE_TYPE_RECEIPT);
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
		if ("contract_overdue_legal".equals(businessType)) {
			return Objects.equals(record.getPaymentId(), paymentId);
		}
		return "contract_termination".equals(businessType);
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
