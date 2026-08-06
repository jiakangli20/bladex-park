/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.miniapp.mapper.MiniPaymentSubmissionMapper;
import org.springblade.modules.miniapp.mapper.UtilityBillDetailMapper;
import org.springblade.modules.miniapp.pojo.dto.UtilityBillingDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniPaymentSubmission;
import org.springblade.modules.miniapp.pojo.entity.UtilityBillDetail;
import org.springblade.modules.miniapp.pojo.vo.UtilityBillingPreviewVO;
import org.springblade.modules.miniapp.service.IUtilityBillingService;
import org.springblade.modules.miniapp.service.AdminParkScopeService;
import org.springblade.modules.park.mapper.RoomMapper;
import org.springblade.modules.park.mapper.RoomUtilityRecordMapper;
import org.springblade.modules.park.mapper.SmartDeviceMapper;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.entity.RoomUtilityRecord;
import org.springblade.modules.park.pojo.entity.SmartDevice;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 园区水电计费与付款凭证审核服务实现。
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class UtilityBillingServiceImpl implements IUtilityBillingService {

	private static final String DEL_FLAG_NORMAL = "0";
	private static final String CONTRACT_ACTIVE = "1";
	private static final String PUBLISH_DRAFT = "DRAFT";
	private static final String PUBLISH_PUBLISHED = "PUBLISHED";
	private static final String SUBMISSION_PENDING = "PENDING";
	private static final String SUBMISSION_CONFIRMED = "CONFIRMED";
	private static final String SUBMISSION_REJECTED = "REJECTED";

	private final RoomUtilityRecordMapper utilityRecordMapper;
	private final SmartDeviceMapper smartDeviceMapper;
	private final RoomMapper roomMapper;
	private final ContractMapper contractMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final UtilityBillDetailMapper utilityBillDetailMapper;
	private final MiniPaymentSubmissionMapper paymentSubmissionMapper;
	private final IPaymentService paymentService;
	private final AdminParkScopeService adminParkScopeService;

	@Override
	public UtilityBillingPreviewVO preview(UtilityBillingDTO.BillingRequest request) {
		UtilityBillingPreviewVO result = new UtilityBillingPreviewVO();
		try {
			result.setDetail(buildDetail(request));
			result.setValid(true);
			result.setMessage("计费校验通过，可以生成账单");
		} catch (ServiceException exception) {
			result.setValid(false);
			result.setMessage(exception.getMessage());
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public UtilityBillDetail generate(UtilityBillingDTO.BillingRequest request) {
		UtilityBillDetail detail = buildDetail(request);
		ContractPayment payment = new ContractPayment();
		payment.setContractId(detail.getContractId());
		payment.setDirection("receivable");
		payment.setFeeType(detail.getRecordType());
		payment.setFeeName("water".equals(detail.getRecordType()) ? "水费" : "电费");
		payment.setPeriodStart(detail.getPeriodStart());
		payment.setPeriodEnd(detail.getPeriodEnd());
		payment.setAmountDue(detail.getAmount());
		payment.setPayDeadline(detail.getPayDeadline());
		payment.setSelectedRoomIds(String.valueOf(detail.getRoomId()));
		payment.setRemark(StringUtil.isBlank(request.getRemark())
			? payment.getFeeName() + "（小程序账单，发布后企业可见）"
			: request.getRemark().trim());

		ContractPayment createdPayment = paymentService.create(payment);
		detail.setPaymentId(createdPayment.getPaymentId());
		try {
			if (utilityBillDetailMapper.insert(detail) <= 0) {
				throw new ServiceException("水电计费明细保存失败");
			}
		} catch (DuplicateKeyException exception) {
			throw new ServiceException("该抄表区间已生成账单，请勿重复生成");
		}
		return requireDetail(detail.getId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean publish(Long detailId) {
		UtilityBillDetail detail = lockDetail(detailId);
		adminParkScopeService.assertAccess(detail.getParkId());
		if (PUBLISH_PUBLISHED.equals(detail.getPublishStatus())) {
			throw new ServiceException("水电账单已发布，请勿重复操作");
		}
		ContractPayment payment = paymentService.selectPaymentById(detail.getPaymentId());
		if (payment == null || !Objects.equals(payment.getParkId(), detail.getParkId())
			|| !Objects.equals(payment.getContractId(), detail.getContractId())) {
			throw new ServiceException("水电账单关联数据异常，无法发布");
		}
		Date now = DateUtil.now();
		return utilityBillDetailMapper.update(null,
			Wrappers.<UtilityBillDetail>lambdaUpdate()
				.eq(UtilityBillDetail::getId, detailId)
				.eq(UtilityBillDetail::getPublishStatus, PUBLISH_DRAFT)
				.set(UtilityBillDetail::getPublishStatus, PUBLISH_PUBLISHED)
				.set(UtilityBillDetail::getPublishedBy, AuthUtil.getUserId())
				.set(UtilityBillDetail::getPublishedTime, now)
				.set(UtilityBillDetail::getUpdateUser, AuthUtil.getUserId())
				.set(UtilityBillDetail::getUpdateTime, now)) > 0;
	}

	@Override
	public IPage<MiniPaymentSubmission> paymentSubmissionPage(IPage<MiniPaymentSubmission> page,
		UtilityBillingDTO.SubmissionQuery query) {
		UtilityBillingDTO.SubmissionQuery normalized = query == null ? new UtilityBillingDTO.SubmissionQuery() : query;
		page = paymentSubmissionMapper.selectPage(page,
			Wrappers.<MiniPaymentSubmission>lambdaQuery()
				.eq(normalized.getParkId() != null, MiniPaymentSubmission::getParkId, normalized.getParkId())
				.eq(normalized.getCustomerId() != null, MiniPaymentSubmission::getCustomerId, normalized.getCustomerId())
				.eq(normalized.getPaymentId() != null, MiniPaymentSubmission::getPaymentId, normalized.getPaymentId())
				.eq(StringUtil.isNotBlank(normalized.getSubmitStatus()), MiniPaymentSubmission::getSubmitStatus,
					normalizeSubmissionStatus(normalized.getSubmitStatus()))
				.orderByAsc(MiniPaymentSubmission::getSubmitStatus)
				.orderByDesc(MiniPaymentSubmission::getCreateTime));
		page.getRecords().forEach(this::attachPaymentInfo);
		return page;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean confirmSubmission(Long submissionId, UtilityBillingDTO.AuditRequest request) {
		MiniPaymentSubmission submission = lockPendingSubmission(submissionId);
		adminParkScopeService.assertAccess(submission.getParkId());
		ContractPayment lockedPayment = contractPaymentMapper.selectByIdForUpdate(submission.getPaymentId());
		if (lockedPayment == null) {
			throw new ServiceException("关联账单不存在");
		}
		ContractPayment payment = paymentService.selectPaymentById(submission.getPaymentId());
		assertSubmissionScope(submission, payment);
		BigDecimal paid = nullToZero(payment.getAmountPaid());
		BigDecimal cumulative = paid.add(submission.getSubmitAmount());
		if (cumulative.compareTo(nullToZero(payment.getAmountDue())) > 0) {
			throw new ServiceException("凭证金额超过账单剩余应收金额");
		}
		ContractPayment confirmation = new ContractPayment();
		confirmation.setAmountPaid(cumulative);
		confirmation.setPayTime(request == null ? null : request.getPayTime());
		confirmation.setPaymentVoucherName(submission.getVoucherName());
		confirmation.setPaymentVoucherUrl(submission.getVoucherUrl());
		confirmation.setRemark(buildAuditRemark("小程序付款凭证确认", request));
		if (!paymentService.confirm(submission.getPaymentId(), confirmation)) {
			throw new ServiceException("确认到账失败，请刷新后重试");
		}
		return updateSubmissionAudit(submissionId, SUBMISSION_CONFIRMED, request == null ? null : request.getOpinion());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean rejectSubmission(Long submissionId, UtilityBillingDTO.AuditRequest request) {
		String opinion = request == null ? null : request.getOpinion();
		if (StringUtil.isBlank(opinion)) {
			throw new ServiceException("请填写驳回原因");
		}
		MiniPaymentSubmission submission = lockPendingSubmission(submissionId);
		adminParkScopeService.assertAccess(submission.getParkId());
		ContractPayment payment = paymentService.selectPaymentById(submission.getPaymentId());
		assertSubmissionScope(submission, payment);
		return updateSubmissionAudit(submissionId, SUBMISSION_REJECTED, opinion.trim());
	}

	private UtilityBillDetail buildDetail(UtilityBillingDTO.BillingRequest request) {
		if (request == null || request.getStartRecordId() == null || request.getEndRecordId() == null) {
			throw new ServiceException("请选择起止抄表记录");
		}
		if (Objects.equals(request.getStartRecordId(), request.getEndRecordId())) {
			throw new ServiceException("起始和截止抄表记录不能相同");
		}
		if (request.getUnitPrice() == null || request.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("计费单价必须大于0");
		}
		if (request.getPayDeadline() == null) {
			throw new ServiceException("缴费截止日期不能为空");
		}

		RoomUtilityRecord start = requireRecord(request.getStartRecordId());
		RoomUtilityRecord end = requireRecord(request.getEndRecordId());
		if (!Objects.equals(start.getRoomId(), end.getRoomId())
			|| !Objects.equals(start.getDeviceId(), end.getDeviceId())
			|| !Objects.equals(start.getRecordType(), end.getRecordType())) {
			throw new ServiceException("起止记录必须属于同一房间、同一表计和同一类型");
		}
		if (!List.of("water", "electric").contains(start.getRecordType())) {
			throw new ServiceException("仅支持水表或电表记录生成账单");
		}
		if (start.getReadingTime() == null || end.getReadingTime() == null
			|| !end.getReadingTime().after(start.getReadingTime())) {
			throw new ServiceException("截止抄表时间必须晚于起始抄表时间");
		}
		if (start.getCurrentReading() == null || end.getCurrentReading() == null
			|| end.getCurrentReading().compareTo(start.getCurrentReading()) < 0) {
			throw new ServiceException("截止读数不能小于起始读数");
		}
		if (utilityBillDetailMapper.selectCount(Wrappers.<UtilityBillDetail>lambdaQuery()
			.eq(UtilityBillDetail::getRoomId, start.getRoomId())
			.eq(UtilityBillDetail::getDeviceId, start.getDeviceId())
			.eq(UtilityBillDetail::getStartRecordId, start.getRecordId())
			.eq(UtilityBillDetail::getEndRecordId, end.getRecordId())) > 0) {
			throw new ServiceException("该抄表区间已生成账单，请勿重复生成");
		}

		SmartDevice device = smartDeviceMapper.selectDeviceById(start.getDeviceId());
		Room room = roomMapper.selectById(start.getRoomId());
		if (device == null || room == null || !Objects.equals(device.getRoomId(), room.getId())
			|| !Objects.equals(device.getParkId(), room.getParkId())
			|| !Objects.equals(device.getDeviceType(), start.getRecordType())) {
			throw new ServiceException("抄表记录关联的房间或表计信息异常");
		}
		adminParkScopeService.assertAccess(room.getParkId());
		Contract contract = requireActiveContract(room, start.getReadingTime(), end.getReadingTime());
		BigDecimal multiplier = device.getMultiplier() == null ? BigDecimal.ONE : device.getMultiplier();
		BigDecimal usage = end.getCurrentReading().subtract(start.getCurrentReading()).multiply(multiplier);
		BigDecimal amount = usage.multiply(request.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("本账期用量或金额为0，无需生成账单");
		}
		Date periodStart = dateOnly(start.getReadingTime());
		Date periodEnd = dateOnly(end.getReadingTime());
		if (request.getPayDeadline().before(periodEnd)) {
			throw new ServiceException("缴费截止日期不能早于账期结束日期");
		}

		UtilityBillDetail detail = new UtilityBillDetail();
		detail.setContractId(contract.getContractId());
		detail.setCustomerId(contract.getCustomerId());
		detail.setParkId(room.getParkId());
		detail.setRoomId(room.getId());
		detail.setDeviceId(device.getDeviceId());
		detail.setRecordType(start.getRecordType());
		detail.setStartRecordId(start.getRecordId());
		detail.setEndRecordId(end.getRecordId());
		detail.setPreviousReading(start.getCurrentReading());
		detail.setCurrentReading(end.getCurrentReading());
		detail.setUsageAmount(usage);
		detail.setUnitPrice(request.getUnitPrice());
		detail.setAmount(amount);
		detail.setPeriodStart(periodStart);
		detail.setPeriodEnd(periodEnd);
		detail.setPayDeadline(dateOnly(request.getPayDeadline()));
		detail.setPublishStatus(PUBLISH_DRAFT);
		detail.setStatus(1);
		detail.setContractNo(contract.getContractNo());
		detail.setCustomerName(contract.getCustomerName());
		detail.setRoomName(room.getName());
		detail.setDeviceName(device.getDeviceName());
		return detail;
	}

	private Contract requireActiveContract(Room room, Date periodStart, Date periodEnd) {
		List<Contract> contracts = contractMapper.selectList(Wrappers.<Contract>query()
			.eq("del_flag", DEL_FLAG_NORMAL)
			.eq("contract_status", CONTRACT_ACTIVE)
			.eq("park_id", room.getParkId())
			.le("start_date", periodEnd)
			.ge("end_date", periodStart)
			.and(wrapper -> wrapper.eq("room_id", room.getId())
				.or().apply("FIND_IN_SET({0}, REPLACE(COALESCE(room_ids, ''), 'room_', '')) > 0", room.getId()))
			.last("LIMIT 2"));
		if (contracts.isEmpty()) {
			throw new ServiceException("该房间在所选账期内没有有效合同");
		}
		if (contracts.size() > 1) {
			throw new ServiceException("该房间存在多个有效合同，请先处理合同冲突");
		}
		return contracts.get(0);
	}

	private RoomUtilityRecord requireRecord(Long recordId) {
		RoomUtilityRecord record = utilityRecordMapper.selectOne(Wrappers.<RoomUtilityRecord>lambdaQuery()
			.eq(RoomUtilityRecord::getRecordId, recordId)
			.eq(RoomUtilityRecord::getDelFlag, DEL_FLAG_NORMAL));
		if (record == null) {
			throw new ServiceException("抄表记录不存在或已删除");
		}
		return record;
	}

	private UtilityBillDetail requireDetail(Long detailId) {
		UtilityBillDetail detail = detailId == null ? null : utilityBillDetailMapper.selectById(detailId);
		if (detail == null) {
			throw new ServiceException("水电计费明细不存在");
		}
		Contract contract = contractMapper.selectById(detail.getContractId());
		Room room = roomMapper.selectById(detail.getRoomId());
		SmartDevice device = smartDeviceMapper.selectDeviceById(detail.getDeviceId());
		detail.setContractNo(contract == null ? null : contract.getContractNo());
		detail.setCustomerName(contract == null ? null : contract.getCustomerName());
		detail.setRoomName(room == null ? null : room.getName());
		detail.setDeviceName(device == null ? null : device.getDeviceName());
		return detail;
	}

	private UtilityBillDetail lockDetail(Long detailId) {
		if (detailId == null) {
			throw new ServiceException("水电计费明细不能为空");
		}
		UtilityBillDetail detail = utilityBillDetailMapper.selectOne(Wrappers.<UtilityBillDetail>lambdaQuery()
			.eq(UtilityBillDetail::getId, detailId)
			.last("FOR UPDATE"));
		if (detail == null) {
			throw new ServiceException("水电计费明细不存在");
		}
		return detail;
	}

	private MiniPaymentSubmission lockPendingSubmission(Long submissionId) {
		if (submissionId == null) {
			throw new ServiceException("付款凭证不能为空");
		}
		MiniPaymentSubmission submission = paymentSubmissionMapper.selectOne(
			Wrappers.<MiniPaymentSubmission>lambdaQuery()
				.eq(MiniPaymentSubmission::getId, submissionId)
				.last("FOR UPDATE"));
		if (submission == null) {
			throw new ServiceException("付款凭证不存在");
		}
		if (!SUBMISSION_PENDING.equals(submission.getSubmitStatus())) {
			throw new ServiceException("付款凭证已处理，请勿重复操作");
		}
		if (submission.getSubmitAmount() == null || submission.getSubmitAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ServiceException("付款凭证金额异常");
		}
		return submission;
	}

	private void assertSubmissionScope(MiniPaymentSubmission submission, ContractPayment payment) {
		UtilityBillDetail detail = utilityBillDetailMapper.selectOne(Wrappers.<UtilityBillDetail>lambdaQuery()
			.eq(UtilityBillDetail::getPaymentId, submission.getPaymentId()));
		if (detail == null || payment == null || !PUBLISH_PUBLISHED.equals(detail.getPublishStatus())
			|| !Objects.equals(detail.getCustomerId(), submission.getCustomerId())
			|| !Objects.equals(detail.getParkId(), submission.getParkId())
			|| !Objects.equals(payment.getParkId(), submission.getParkId())
			|| !Objects.equals(payment.getContractId(), detail.getContractId())) {
			throw new ServiceException("付款凭证与水电账单归属不一致");
		}
	}

	private boolean updateSubmissionAudit(Long submissionId, String status, String opinion) {
		Date now = DateUtil.now();
		return paymentSubmissionMapper.update(null,
			Wrappers.<MiniPaymentSubmission>lambdaUpdate()
				.eq(MiniPaymentSubmission::getId, submissionId)
				.eq(MiniPaymentSubmission::getSubmitStatus, SUBMISSION_PENDING)
				.set(MiniPaymentSubmission::getSubmitStatus, status)
				.set(MiniPaymentSubmission::getAuditUserId, AuthUtil.getUserId())
				.set(MiniPaymentSubmission::getAuditUserName, currentUserName())
				.set(MiniPaymentSubmission::getAuditTime, now)
				.set(MiniPaymentSubmission::getAuditOpinion, StringUtil.isBlank(opinion) ? null : opinion.trim())
				.set(MiniPaymentSubmission::getUpdateUser, AuthUtil.getUserId())
				.set(MiniPaymentSubmission::getUpdateTime, now)) > 0;
	}

	private void attachPaymentInfo(MiniPaymentSubmission submission) {
		try {
			ContractPayment payment = paymentService.selectPaymentById(submission.getPaymentId());
			submission.setCustomerName(payment.getCustomerName());
			submission.setFeeName(payment.getFeeName());
			submission.setAmountDue(payment.getAmountDue());
			submission.setAmountPaid(payment.getAmountPaid());
			submission.setPayStatus(payment.getPayStatus());
			submission.setRoomName(StringUtil.isBlank(payment.getSelectedRoomName())
				? payment.getRoomName() : payment.getSelectedRoomName());
		} catch (ServiceException ignored) {
			// 保留孤立凭证供管理员排查，不因历史关联异常导致整页不可用。
		}
	}

	private String normalizeSubmissionStatus(String status) {
		String normalized = status.trim().toUpperCase();
		if (!List.of(SUBMISSION_PENDING, SUBMISSION_CONFIRMED, SUBMISSION_REJECTED).contains(normalized)) {
			throw new ServiceException("付款凭证状态不正确");
		}
		return normalized;
	}

	private String buildAuditRemark(String prefix, UtilityBillingDTO.AuditRequest request) {
		return request == null || StringUtil.isBlank(request.getOpinion())
			? prefix : prefix + "：" + request.getOpinion().trim();
	}

	private BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private Date dateOnly(Date date) {
		return DateUtil.parse(DateUtil.format(date, DateUtil.PATTERN_DATE), DateUtil.PATTERN_DATE);
	}

	private String currentUserName() {
		return StringUtil.isBlank(AuthUtil.getUserName()) ? "system" : AuthUtil.getUserName();
	}
}
