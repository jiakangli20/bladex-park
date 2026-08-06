/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.redis.cache.BladeRedis;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.pojo.entity.*;
import org.springblade.modules.business.service.*;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.service.IContractService;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.miniapp.config.MiniAppProperties;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springblade.modules.miniapp.mapper.HouseAppointmentMapper;
import org.springblade.modules.miniapp.mapper.MiniInviteMapper;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.mapper.MiniNotificationMapper;
import org.springblade.modules.miniapp.mapper.ParkActivityMapper;
import org.springblade.modules.miniapp.mapper.MiniPaymentSubmissionMapper;
import org.springblade.modules.miniapp.mapper.UtilityBillDetailMapper;
import org.springblade.modules.miniapp.pojo.dto.MiniBusinessDTO;
import org.springblade.modules.miniapp.pojo.entity.*;
import org.springblade.modules.miniapp.service.IMiniAuthService;
import org.springblade.modules.miniapp.service.IMiniBusinessService;
import org.springblade.modules.park.pojo.entity.Room;
import org.springblade.modules.park.pojo.vo.RoomVO;
import org.springblade.modules.park.service.IRoomService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 小程序聚合业务服务实现。
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class MiniBusinessServiceImpl implements IMiniBusinessService {

	private final MiniAppProperties properties;
	private final IMiniAuthService authService;
	private final IRoomService roomService;
	private final IMerchantAdService merchantAdService;
	private final IPolicyServiceService policyService;
	private final IPropertyServiceService propertyService;
	private final IMerchantService merchantService;
	private final ICustomerService customerService;
	private final IBusinessOpportunityService opportunityService;
	private final IPropertyWorkorderService propertyWorkorderService;
	private final IMerchantServiceOrderService merchantOrderService;
	private final IContractService contractService;
	private final IPaymentService paymentService;
	private final IEnterpriseDataService enterpriseDataService;
	private final HouseAppointmentMapper appointmentMapper;
	private final MiniMemberMapper memberMapper;
	private final MiniInviteMapper inviteMapper;
	private final MiniNotificationMapper notificationMapper;
	private final ParkActivityMapper activityMapper;
	private final UtilityBillDetailMapper utilityBillDetailMapper;
	private final MiniPaymentSubmissionMapper paymentSubmissionMapper;
	private final BladeRedis bladeRedis;

	@Override
	public Map<String, Object> home() {
		Long parkId = properties.getDefaultParkId();
		PolicyService policyQuery = new PolicyService();
		policyQuery.setParkId(parkId);
		policyQuery.setOnlineFlag("1");
		List<Map<String, Object>> banners = merchantAdService.selectPublicAdList(parkId, "miniapp_home")
			.stream().map(this::adMap).toList();
		List<Map<String, Object>> policies = policyService.selectPolicyList(policyQuery).stream().limit(6).map(this::policyMap).toList();
		List<Map<String, Object>> activities = activityMapper.selectList(Wrappers.<ParkActivity>lambdaQuery()
			.eq(ParkActivity::getTenantId, properties.getDefaultTenantId()).eq(ParkActivity::getParkId, parkId)
			.eq(ParkActivity::getPublishStatus, 1).eq(ParkActivity::getStatus, StatusType.ACTIVE.getType())
			.eq(ParkActivity::getIsDeleted, 0).orderByAsc(ParkActivity::getSortOrder).orderByDesc(ParkActivity::getStartTime))
			.stream().map(this::activityMap).toList();
		return Kv.create().set("banners", banners).set("policies", policies).set("activities", activities)
			.set("quickEntries", List.of("house", "property", "value", "orders", "settlement"));
	}

	@Override
	public List<Map<String, Object>> houses(String keyword) {
		Room query = new Room();
		query.setParkId(properties.getDefaultParkId());
		query.setStatus("0");
		query.setSyncStatus("1");
		return roomService.selectRoomList(query).stream()
			.filter(room -> StringUtil.isBlank(keyword) || contains(room.getName(), keyword) || contains(room.getBuildingName(), keyword))
			.map(this::houseMap).toList();
	}

	@Override
	public Map<String, Object> house(Long id) {
		RoomVO room = roomService.selectRoomById(id);
		if (room == null || !Objects.equals(room.getParkId(), properties.getDefaultParkId())
			|| !"0".equals(room.getStatus()) || !"1".equals(room.getSyncStatus())) {
			throw new ServiceException("公开房源不存在或已下架");
		}
		return houseMap(room);
	}

	@Override
	public List<Map<String, Object>> propertyServices() {
		PropertyService query = new PropertyService();
		query.setParkId(properties.getDefaultParkId());
		query.setStatus("0");
		return propertyService.selectPropertyServiceList(query).stream().map(this::propertyServiceMap).toList();
	}

	@Override
	public List<Map<String, Object>> valueServices(String keyword) {
		Merchant query = new Merchant();
		query.setParkId(properties.getDefaultParkId());
		query.setStatus("0");
		return merchantService.selectMerchantList(query).stream()
			.filter(item -> StringUtil.isBlank(keyword) || contains(item.getMerchantName(), keyword) || contains(item.getBusinessType(), keyword))
			.map(this::merchantMap).toList();
	}

	@Override
	public Map<String, Object> valueService(Long id) {
		Merchant merchant = merchantService.selectMerchantById(id);
		if (merchant == null || !Objects.equals(merchant.getParkId(), properties.getDefaultParkId()) || !"0".equals(merchant.getStatus())) {
			throw new ServiceException("增值服务不存在或已下架");
		}
		return merchantMap(merchant);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createAppointment(String requestId, MiniBusinessDTO.Appointment request) {
		MiniMember member = authService.requireCustomer();
		return idempotent(requestId, () -> {
			house(request.getRoomId());
			HouseAppointment appointment = new HouseAppointment();
			appointment.setTenantId(member.getTenantId());
			appointment.setAppointmentNo("YY" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + randomDigits());
			appointment.setMemberId(member.getId());
			appointment.setCustomerId(member.getCustomerId());
			appointment.setParkId(member.getParkId());
			appointment.setRoomId(request.getRoomId());
			appointment.setEnterpriseName(request.getEnterpriseName());
			appointment.setContactName(request.getContactName());
			appointment.setContactPhone(request.getContactPhone());
			appointment.setPreferredTime(request.getPreferredTime());
			appointment.setDemandDesc(request.getDemandDesc());
			appointment.setAppointmentStatus("PENDING");
			appointment.setStatus(StatusType.ACTIVE.getType());
			appointment.setIsDeleted(0);
			appointmentMapper.insert(appointment);
			notifyAdmins(member, "HOUSE_APPOINTMENT", "新的看房预约", request.getEnterpriseName(), "appointment", appointment.getId());
			return appointmentMap(appointment);
		});
	}

	@Override
	public List<Map<String, Object>> appointments() {
		MiniMember member = authService.requireCustomer();
		return appointmentMapper.selectList(Wrappers.<HouseAppointment>lambdaQuery()
			.eq(HouseAppointment::getTenantId, member.getTenantId()).eq(HouseAppointment::getParkId, member.getParkId())
			.eq(HouseAppointment::getCustomerId, member.getCustomerId()).eq(HouseAppointment::getIsDeleted, 0)
			.orderByDesc(HouseAppointment::getCreateTime)).stream().map(this::appointmentMap).toList();
	}

	@Override
	public void cancelAppointment(Long id, String reason) {
		MiniMember member = authService.requireCustomer();
		HouseAppointment appointment = scopedAppointment(id, member.getParkId(), member.getCustomerId());
		if (!"PENDING".equals(appointment.getAppointmentStatus())) {
			throw new ServiceException("当前预约状态不可取消");
		}
		appointment.setAppointmentStatus("CANCELLED");
		appointment.setCancelReason(reason);
		appointmentMapper.updateById(appointment);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createSettlement(String requestId, MiniBusinessDTO.Settlement request) {
		MiniMember member = authService.requireCustomer();
		return idempotent(requestId, () -> {
			BusinessOpportunity opportunity = new BusinessOpportunity();
			opportunity.setParkId(member.getParkId());
			opportunity.setCustomerId(member.getCustomerId());
			opportunity.setSourceRoomId(request.getRoomId());
			opportunity.setEnterpriseName(request.getEnterpriseName());
			opportunity.setCreditCode(request.getCreditCode());
			opportunity.setIndustryType(request.getIndustryType());
			opportunity.setEnterpriseScale(request.getEnterpriseScale());
			opportunity.setIntentArea(request.getIntentArea());
			opportunity.setExpectedEntryDate(request.getExpectedEntryDate());
			opportunity.setContactName(request.getContactName());
			opportunity.setContactPhone(request.getContactPhone());
			opportunity.setChannel("MINIAPP");
			opportunity.setRemark(request.getDemandDesc());
			opportunityService.insertBusinessOpportunity(opportunity);
			notifyAdmins(member, "SETTLEMENT", "新的入驻意向", request.getEnterpriseName(), "settlement", opportunity.getOpportunityId());
			return Kv.create().set("id", opportunity.getOpportunityId()).set("status", opportunity.getOpportunityStatus());
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createPropertyOrder(String requestId, MiniBusinessDTO.PropertyOrder request) {
		MiniMember member = authService.requireCustomer();
		return idempotent(requestId, () -> {
			Customer customer = requireCustomer(member.getCustomerId(), member.getParkId());
			ServiceWorkorder order = new ServiceWorkorder();
			order.setParkId(member.getParkId());
			order.setServiceId(request.getServiceId());
			order.setCustomerId(member.getCustomerId());
			order.setCustomerName(customer.getEnterpriseName());
			order.setContactName(request.getContactName());
			order.setContactPhone(request.getContactPhone());
			order.setRoomIds(request.getRoomIds());
			order.setRoomInfo(request.getRoomInfo());
			order.setDemandDesc(request.getDemandDesc());
			order.setDemandImages(request.getDemandImages());
			order.setPriority(request.getPriority());
			propertyWorkorderService.insertWorkorder(order);
			notifyAdmins(member, "PROPERTY_ORDER", "新的物业服务申请", customer.getEnterpriseName(), "property", order.getOrderId());
			return propertyOrderMap(order);
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createValueOrder(String requestId, MiniBusinessDTO.ValueOrder request) {
		MiniMember member = authService.requireCustomer();
		return idempotent(requestId, () -> {
			Customer customer = requireCustomer(member.getCustomerId(), member.getParkId());
			MerchantServiceOrder order = new MerchantServiceOrder();
			order.setParkId(member.getParkId());
			order.setMerchantId(request.getMerchantId());
			order.setServiceType(request.getServiceType());
			order.setCustomerId(member.getCustomerId());
			order.setCustomerName(customer.getEnterpriseName());
			order.setContactName(request.getContactName());
			order.setContactPhone(request.getContactPhone());
			order.setServiceScope(request.getServiceScope());
			order.setDemandDesc(request.getDemandDesc());
			order.setDemandImages(request.getDemandImages());
			merchantOrderService.insertOrder(order);
			notifyAdmins(member, "VALUE_ORDER", "新的增值服务申请", customer.getEnterpriseName(), "value", order.getOrderId());
			return valueOrderMap(order);
		});
	}

	@Override
	public Map<String, Object> company() {
		MiniMember member = authService.requireCustomer();
		Customer customer = requireCustomer(member.getCustomerId(), member.getParkId());
		Map<String, Object> result = customerMap(customer, true);
		result.put("contact", customer.getContactName());
		result.put("phone", customer.getContactPhone());
		return result;
	}

	@Override
	public void saveCompany(MiniBusinessDTO.Company request) {
		MiniMember member = authService.requireCustomerAdmin();
		Customer customer = requireCustomer(member.getCustomerId(), member.getParkId());
		customer.setEnterpriseName(request.getEnterpriseName());
		customer.setIndustry(request.getIndustry());
		customer.setScale(request.getScale());
		customer.setContactName(request.getContactName());
		customer.setContactPhone(request.getContactPhone());
		customer.setContactEmail(request.getContactEmail());
		customer.setAddress(request.getAddress());
		customer.setBusinessScope(request.getBusinessScope());
		customerService.updateCustomer(customer);
	}

	@Override
	public List<Map<String, Object>> contracts() {
		MiniMember member = authService.requireCustomer();
		return contractService.list(Wrappers.<Contract>lambdaQuery().eq(Contract::getCustomerId, member.getCustomerId())
			.eq(Contract::getParkId, member.getParkId()).eq(Contract::getDelFlag, "0").orderByDesc(Contract::getCreateTime))
			.stream().map(item -> contractMap(item, false)).toList();
	}

	@Override
	public Map<String, Object> contract(Long id) {
		MiniMember member = authService.requireCustomer();
		Contract contract = scopedContract(id, member.getParkId(), member.getCustomerId());
		return contractMap(contract, true);
	}

	@Override
	public List<Map<String, Object>> bills() {
		MiniMember member = authService.requireCustomer();
		ContractPayment query = new ContractPayment();
		query.setCustomerId(member.getCustomerId());
		query.setParkId(member.getParkId());
		IPage<ContractPayment> page = contractService.selectPaymentPage(new Page<>(1, 500), query);
		return page.getRecords().stream().map(this::billMap).toList();
	}

	@Override
	public Map<String, Object> bill(Long id) {
		return bills().stream().filter(item -> Objects.equals(String.valueOf(item.get("id")), String.valueOf(id)))
			.findFirst().orElseThrow(() -> new ServiceException("账单不存在或无权访问"));
	}

	@Override
	public List<Map<String, Object>> utilityBills() {
		MiniMember member = authService.requireCustomer();
		return utilityBillDetailMapper.selectList(Wrappers.<UtilityBillDetail>lambdaQuery()
			.eq(UtilityBillDetail::getTenantId, member.getTenantId())
			.eq(UtilityBillDetail::getParkId, member.getParkId())
			.eq(UtilityBillDetail::getCustomerId, member.getCustomerId())
			.eq(UtilityBillDetail::getPublishStatus, "PUBLISHED")
			.eq(UtilityBillDetail::getIsDeleted, 0)
			.orderByDesc(UtilityBillDetail::getPeriodEnd)
			.orderByDesc(UtilityBillDetail::getId)).stream()
			.map(item -> utilityBillMap(item, paymentService.selectPaymentById(item.getPaymentId()), false))
			.toList();
	}

	@Override
	public Map<String, Object> utilityBill(Long id) {
		MiniMember member = authService.requireCustomer();
		UtilityBillDetail detail = scopedUtilityBill(id, member);
		ContractPayment payment = paymentService.selectPaymentById(detail.getPaymentId());
		assertUtilityPaymentScope(detail, payment);
		Map<String, Object> result = utilityBillMap(detail, payment, true);
		result.put("submissions", utilityBillSubmissions(id));
		return result;
	}

	@Override
	public List<Map<String, Object>> utilityBillSubmissions(Long id) {
		MiniMember member = authService.requireCustomer();
		UtilityBillDetail detail = scopedUtilityBill(id, member);
		return paymentSubmissionMapper.selectList(Wrappers.<MiniPaymentSubmission>lambdaQuery()
			.eq(MiniPaymentSubmission::getTenantId, member.getTenantId())
			.eq(MiniPaymentSubmission::getParkId, member.getParkId())
			.eq(MiniPaymentSubmission::getCustomerId, member.getCustomerId())
			.eq(MiniPaymentSubmission::getPaymentId, detail.getPaymentId())
			.eq(MiniPaymentSubmission::getIsDeleted, 0)
			.orderByDesc(MiniPaymentSubmission::getCreateTime)
			.orderByDesc(MiniPaymentSubmission::getId)).stream().map(this::paymentSubmissionMap).toList();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> submitUtilityPayment(String requestId, Long id,
		MiniBusinessDTO.PaymentSubmission request) {
		MiniMember member = authService.requireCustomerAdmin();
		return idempotent(requestId, () -> {
			UtilityBillDetail detail = scopedUtilityBill(id, member);
			ContractPayment payment = paymentService.selectPaymentById(detail.getPaymentId());
			assertUtilityPaymentScope(detail, payment);
			BigDecimal remaining = nullToZero(payment.getAmountDue()).subtract(nullToZero(payment.getAmountPaid()));
			if (remaining.compareTo(BigDecimal.ZERO) <= 0 || "1".equals(payment.getPayStatus())) {
				throw new ServiceException("水电账单已结清，无需重复提交凭证");
			}
			if (request.getSubmitAmount().compareTo(remaining) > 0) {
				throw new ServiceException("付款金额不能超过账单剩余应收金额");
			}
			long pending = paymentSubmissionMapper.selectCount(Wrappers.<MiniPaymentSubmission>lambdaQuery()
				.eq(MiniPaymentSubmission::getTenantId, member.getTenantId())
				.eq(MiniPaymentSubmission::getPaymentId, detail.getPaymentId())
				.eq(MiniPaymentSubmission::getSubmitStatus, "PENDING")
				.eq(MiniPaymentSubmission::getIsDeleted, 0));
			if (pending > 0) {
				throw new ServiceException("已有待确认付款凭证，请等待园区管理员处理");
			}
			MiniPaymentSubmission submission = new MiniPaymentSubmission();
			submission.setTenantId(member.getTenantId());
			submission.setPaymentId(detail.getPaymentId());
			submission.setCustomerId(member.getCustomerId());
			submission.setParkId(member.getParkId());
			submission.setMemberId(member.getId());
			submission.setSubmitAmount(request.getSubmitAmount());
			submission.setVoucherName(request.getVoucherName().trim());
			submission.setVoucherUrl(request.getVoucherUrl().trim());
			submission.setSubmitStatus("PENDING");
			submission.setStatus(StatusType.ACTIVE.getType());
			submission.setIsDeleted(0);
			if (paymentSubmissionMapper.insert(submission) <= 0) {
				throw new ServiceException("付款凭证提交失败");
			}
			notifyAdmins(member, "UTILITY_PAYMENT", "新的水电付款凭证",
				payment.getFeeName() + " " + request.getSubmitAmount() + "元", "utility_payment", submission.getId());
			return paymentSubmissionMap(submission);
		});
	}

	@Override
	public List<Map<String, Object>> customerAds() {
		MiniMember member = authService.requireCustomer();
		return merchantAdService.selectCustomerAdList(member.getParkId(), member.getCustomerId())
			.stream().map(item -> customerAdMap(item, false)).toList();
	}

	@Override
	public Map<String, Object> customerAd(Long id) {
		MiniMember member = authService.requireCustomer();
		MerchantAd ad = merchantAdService.selectCustomerAdById(id, member.getParkId(), member.getCustomerId());
		Map<String, Object> result = customerAdMap(ad, true);
		result.put("logs", merchantAdService.auditLogs(ad.getAdId()).stream().map(this::adAuditLogMap).toList());
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> createCustomerAd(String requestId, MiniBusinessDTO.CustomerAd request) {
		MiniMember member = authService.requireCustomerAdmin();
		return idempotent(requestId, () -> customerAdMap(merchantAdService.createCustomerAd(
			toCustomerAd(request, null), member.getParkId(), member.getCustomerId(), member.getId()), true));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateCustomerAd(Long id, MiniBusinessDTO.CustomerAd request) {
		MiniMember member = authService.requireCustomerAdmin();
		return customerAdMap(merchantAdService.updateCustomerAd(
			toCustomerAd(request, id), member.getParkId(), member.getCustomerId()), true);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void submitCustomerAd(String requestId, Long id) {
		MiniMember member = authService.requireCustomerAdmin();
		idempotent(requestId, () -> {
			merchantAdService.submitCustomerAd(id, member.getParkId(), member.getCustomerId());
			notifyAdmins(member, "MERCHANT_AD", "新的企业广告待审核",
				merchantAdService.selectCustomerAdById(id, member.getParkId(), member.getCustomerId()).getAdTitle(),
				"merchant_ad", id);
			return Boolean.TRUE;
		});
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void withdrawCustomerAd(Long id) {
		MiniMember member = authService.requireCustomerAdmin();
		merchantAdService.withdrawCustomerAd(id, member.getParkId(), member.getCustomerId());
	}

	@Override
	public List<Map<String, Object>> customerWorkOrders() {
		MiniMember member = authService.requireCustomer();
		ServiceWorkorder propertyQuery = new ServiceWorkorder();
		propertyQuery.setParkId(member.getParkId());
		propertyQuery.setCustomerId(member.getCustomerId());
		MerchantServiceOrder valueQuery = new MerchantServiceOrder();
		valueQuery.setParkId(member.getParkId());
		valueQuery.setCustomerId(member.getCustomerId());
		return Stream.concat(propertyWorkorderService.selectWorkorderList(propertyQuery).stream().map(this::propertyOrderMap),
			merchantOrderService.selectOrderList(valueQuery).stream().map(this::valueOrderMap))
			.sorted(Comparator.comparing(item -> String.valueOf(item.get("createTime")), Comparator.reverseOrder())).toList();
	}

	@Override
	public Map<String, Object> customerWorkOrder(String type, Long id) {
		MiniMember member = authService.requireCustomer();
		if ("property".equals(type)) {
			ServiceWorkorder order = propertyWorkorderService.selectWorkorderById(id);
			assertScope(order == null ? null : order.getParkId(), order == null ? null : order.getCustomerId(), member);
			Map<String, Object> result = propertyOrderMap(order);
			result.put("steps", propertyWorkorderService.selectLogByOrderId(id));
			return result;
		}
		if ("value".equals(type)) {
			MerchantServiceOrder order = merchantOrderService.selectOrderById(id);
			assertScope(order == null ? null : order.getParkId(), order == null ? null : order.getCustomerId(), member);
			Map<String, Object> result = valueOrderMap(order);
			result.put("steps", merchantOrderService.selectLogByOrderId(id));
			return result;
		}
		throw new ServiceException("不支持的工单类型");
	}

	@Override
	public void customerAction(String type, Long id, MiniBusinessDTO.CustomerAction request) {
		customerWorkOrder(type, id);
		if ("property".equals(type)) {
			ServiceWorkorder order = new ServiceWorkorder();
			order.setOrderId(id);
			if ("cancel".equalsIgnoreCase(request.getAction())) {
				order.setProcessRemark(request.getReason());
				propertyWorkorderService.closeWorkorder(order);
			} else if ("rate".equalsIgnoreCase(request.getAction()) || "confirm".equalsIgnoreCase(request.getAction())) {
				order.setRating(request.getRating() == null ? 5 : request.getRating());
				order.setRatingContent(request.getContent());
				propertyWorkorderService.rateWorkorder(order);
			} else {
				throw new ServiceException("不支持的工单操作");
			}
		} else if ("cancel".equalsIgnoreCase(request.getAction())) {
			MerchantServiceOrder order = new MerchantServiceOrder();
			order.setOrderId(id);
			order.setCloseReason(request.getReason());
			merchantOrderService.closeOrder(order);
		} else {
			throw new ServiceException("增值服务单仅支持取消");
		}
	}

	@Override
	public List<Map<String, Object>> members() {
		MiniMember current = authService.requireCustomerAdmin();
		return memberMapper.selectList(Wrappers.<MiniMember>lambdaQuery().eq(MiniMember::getTenantId, current.getTenantId())
			.eq(MiniMember::getParkId, current.getParkId()).eq(MiniMember::getCustomerId, current.getCustomerId())
			.eq(MiniMember::getIsDeleted, 0).orderByDesc(MiniMember::getCreateTime)).stream().map(this::memberMap).toList();
	}

	@Override
	public List<Map<String, Object>> invites() {
		MiniMember current = authService.requireCustomerAdmin();
		return inviteMapper.selectList(Wrappers.<MiniInvite>lambdaQuery().eq(MiniInvite::getTenantId, current.getTenantId())
			.eq(MiniInvite::getParkId, current.getParkId()).eq(MiniInvite::getCustomerId, current.getCustomerId())
			.eq(MiniInvite::getIsDeleted, 0).orderByDesc(MiniInvite::getCreateTime)).stream().map(this::inviteMap).toList();
	}

	@Override
	public Map<String, Object> createInvite(MiniBusinessDTO.Invite request) {
		MiniMember current = authService.requireCustomerAdmin();
		String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
		MiniInvite invite = new MiniInvite();
		invite.setTenantId(current.getTenantId());
		invite.setCodeHash(DigestUtil.sha256Hex(code));
		invite.setCustomerId(current.getCustomerId());
		invite.setParkId(current.getParkId());
		invite.setRoleCode(MiniAppConstant.ROLE_CUSTOMER_ADMIN.equals(request.getRoleCode())
			? MiniAppConstant.ROLE_CUSTOMER_ADMIN : MiniAppConstant.ROLE_CUSTOMER_MEMBER);
		invite.setMobile(request.getMobile());
		invite.setExpireTime(new Date(System.currentTimeMillis() + request.getValidHours() * 3600000L));
		invite.setMaxUses(request.getMaxUses());
		invite.setUsedCount(0);
		invite.setStatus(StatusType.ACTIVE.getType());
		invite.setIsDeleted(0);
		inviteMapper.insert(invite);
		return Kv.create().set("id", invite.getId()).set("code", code).set("expireTime", invite.getExpireTime());
	}

	@Override
	public void disableMember(Long id) {
		MiniMember current = authService.requireCustomerAdmin();
		MiniMember target = memberMapper.selectById(id);
		if (target == null || !Objects.equals(target.getCustomerId(), current.getCustomerId()) || !Objects.equals(target.getParkId(), current.getParkId())) {
			throw new ServiceException("成员不存在或无权操作");
		}
		if (Objects.equals(target.getId(), current.getId())) {
			throw new ServiceException("不能停用当前账号");
		}
		target.setStatus(StatusType.DISABLED.getType());
		memberMapper.updateById(target);
	}

	@Override
	public List<Map<String, Object>> notifications() {
		MiniMember admin = authService.requireParkAdmin();
		return notificationMapper.selectList(Wrappers.<MiniNotification>lambdaQuery().eq(MiniNotification::getTenantId, admin.getTenantId())
			.eq(MiniNotification::getParkId, admin.getParkId()).eq(MiniNotification::getRecipientUserId, admin.getUserId())
			.eq(MiniNotification::getIsDeleted, 0).orderByDesc(MiniNotification::getCreateTime)).stream().map(this::notificationMap).toList();
	}

	@Override
	public void readNotification(Long id) {
		MiniMember admin = authService.requireParkAdmin();
		MiniNotification notice = notificationMapper.selectById(id);
		if (notice == null || !Objects.equals(notice.getParkId(), admin.getParkId()) || !Objects.equals(notice.getRecipientUserId(), admin.getUserId())) {
			throw new ServiceException("通知不存在或无权访问");
		}
		notice.setReadStatus(1);
		notice.setReadTime(new Date());
		notificationMapper.updateById(notice);
	}

	@Override
	public List<Map<String, Object>> adminWorkOrders(String type) {
		MiniMember admin = authService.requireParkAdmin();
		Stream<Map<String, Object>> property = Stream.empty();
		Stream<Map<String, Object>> value = Stream.empty();
		Stream<Map<String, Object>> appointment = Stream.empty();
		Stream<Map<String, Object>> settlement = Stream.empty();
		if (StringUtil.isBlank(type) || "property".equals(type)) {
			ServiceWorkorder query = new ServiceWorkorder(); query.setParkId(admin.getParkId());
			property = propertyWorkorderService.selectWorkorderList(query).stream().map(this::propertyOrderMap);
		}
		if (StringUtil.isBlank(type) || "value".equals(type)) {
			MerchantServiceOrder query = new MerchantServiceOrder(); query.setParkId(admin.getParkId());
			value = merchantOrderService.selectOrderList(query).stream().map(this::valueOrderMap);
		}
		if (StringUtil.isBlank(type) || "appointment".equals(type)) {
			appointment = appointmentMapper.selectList(Wrappers.<HouseAppointment>lambdaQuery().eq(HouseAppointment::getTenantId, admin.getTenantId())
				.eq(HouseAppointment::getParkId, admin.getParkId()).eq(HouseAppointment::getIsDeleted, 0)).stream().map(this::appointmentMap);
		}
		if (StringUtil.isBlank(type) || "settlement".equals(type)) {
			BusinessOpportunity query = new BusinessOpportunity(); query.setParkId(admin.getParkId());
			settlement = opportunityService.selectBusinessOpportunityList(query).stream().map(this::settlementMap);
		}
		return Stream.of(property, value, appointment, settlement).flatMap(stream -> stream)
			.sorted(Comparator.comparing(item -> String.valueOf(item.get("createTime")), Comparator.reverseOrder())).toList();
	}

	@Override
	public Map<String, Object> adminWorkOrder(String type, Long id) {
		MiniMember admin = authService.requireParkAdmin();
		return switch (type) {
			case "property" -> {
				ServiceWorkorder order = propertyWorkorderService.selectWorkorderById(id);
				assertAdminPark(order == null ? null : order.getParkId(), admin);
				Map<String, Object> result = propertyOrderMap(order);
				result.put("steps", propertyWorkorderService.selectLogByOrderId(id));
				yield result;
			}
			case "value" -> {
				MerchantServiceOrder order = merchantOrderService.selectOrderById(id);
				assertAdminPark(order == null ? null : order.getParkId(), admin);
				Map<String, Object> result = valueOrderMap(order);
				result.put("steps", merchantOrderService.selectLogByOrderId(id));
				yield result;
			}
			case "appointment" -> appointmentMap(scopedAppointment(id, admin.getParkId(), null));
			case "settlement" -> {
				BusinessOpportunity opportunity = opportunityService.selectBusinessOpportunityById(id);
				assertAdminPark(opportunity == null ? null : opportunity.getParkId(), admin);
				yield settlementMap(opportunity);
			}
			default -> throw new ServiceException("不支持的工单类型");
		};
	}

	@Override
	public void adminAction(String type, Long id, MiniBusinessDTO.AdminAction request) {
		MiniMember admin = authService.requireParkAdmin();
		adminWorkOrder(type, id);
		String action = request.getAction().toUpperCase(Locale.ROOT);
		if ("property".equals(type)) {
			ServiceWorkorder order = new ServiceWorkorder();
			order.setOrderId(id); order.setAssignTo(request.getAssignee()); order.setProcessor(admin.getNickname());
			order.setProcessRemark(request.getContent()); order.setDisposalContent(request.getContent()); order.setRemark(request.getReason());
			switch (action) {
				case "ACCEPT", "ASSIGN" -> { if (StringUtil.isBlank(order.getAssignTo())) order.setAssignTo(AuthUtil.getUserName()); propertyWorkorderService.assignWorkorder(order); }
				case "FOLLOW" -> { order.setOrderStatus("1"); if (StringUtil.isBlank(order.getAssignTo())) order.setAssignTo(AuthUtil.getUserName()); propertyWorkorderService.updateWorkorder(order); }
				case "REJECT" -> propertyWorkorderService.closeWorkorder(order);
				case "COMPLETE" -> { if (StringUtil.isBlank(order.getAssignTo())) order.setAssignTo(AuthUtil.getUserName()); propertyWorkorderService.finishWorkorder(order); }
				default -> throw new ServiceException("不支持的处理动作");
			}
		} else if ("value".equals(type)) {
			MerchantServiceOrder order = new MerchantServiceOrder();
			order.setOrderId(id); order.setAssignTo(request.getAssignee()); order.setProcessContent(request.getContent());
			order.setCloseReason(request.getReason()); order.setDealAmount(request.getDealAmount()); order.setNextFollowTime(request.getNextFollowTime());
			switch (action) {
				case "ACCEPT", "ASSIGN" -> { if (StringUtil.isBlank(order.getAssignTo())) order.setAssignTo(AuthUtil.getUserName()); merchantOrderService.assignOrder(order); }
				case "FOLLOW" -> merchantOrderService.followOrder(order);
				case "REJECT" -> merchantOrderService.closeOrder(order);
				case "COMPLETE", "DEAL" -> merchantOrderService.dealOrder(order);
				default -> throw new ServiceException("不支持的处理动作");
			}
		} else if ("appointment".equals(type)) {
			HouseAppointment appointment = scopedAppointment(id, admin.getParkId(), null);
			if (!"PENDING".equals(appointment.getAppointmentStatus()) && !"ACCEPTED".equals(appointment.getAppointmentStatus())) {
				throw new ServiceException("当前预约状态不可处理");
			}
			appointment.setAppointmentStatus(switch (action) {
				case "ACCEPT", "ASSIGN", "FOLLOW" -> "ACCEPTED";
				case "REJECT" -> "REJECTED";
				case "COMPLETE" -> "COMPLETED";
				default -> throw new ServiceException("不支持的处理动作");
			});
			appointment.setCancelReason(request.getReason());
			appointmentMapper.updateById(appointment);
		} else if ("settlement".equals(type)) {
			BusinessOpportunity opportunity = opportunityService.selectBusinessOpportunityById(id);
			assertAdminPark(opportunity == null ? null : opportunity.getParkId(), admin);
			opportunity.setOpportunityStatus(switch (action) {
				case "ACCEPT" -> "1"; case "FOLLOW", "ASSIGN" -> "2"; case "COMPLETE", "DEAL" -> "3"; case "REJECT" -> "4";
				default -> throw new ServiceException("不支持的处理动作");
			});
			opportunity.setRemark(request.getContent());
			opportunityService.updateBusinessOpportunity(opportunity);
		} else {
			throw new ServiceException("不支持的工单类型");
		}
	}

	@Override
	public Map<String, Object> overview() {
		MiniMember admin = authService.requireParkAdmin();
		Kv data = enterpriseDataService.overview(admin.getParkId());
		long unread = notificationMapper.selectCount(Wrappers.<MiniNotification>lambdaQuery().eq(MiniNotification::getTenantId, admin.getTenantId())
			.eq(MiniNotification::getParkId, admin.getParkId()).eq(MiniNotification::getRecipientUserId, admin.getUserId())
			.eq(MiniNotification::getReadStatus, 0).eq(MiniNotification::getIsDeleted, 0));
		return Kv.create(data).set("unreadNotifications", unread);
	}

	@Override
	public List<Map<String, Object>> tenants(String keyword) {
		MiniMember admin = authService.requireParkAdmin();
		Customer query = new Customer(); query.setParkId(admin.getParkId()); query.setKeyword(keyword);
		return customerService.selectCustomerList(query).stream().map(item -> customerMap(item, false)).toList();
	}

	@Override
	public Map<String, Object> tenant(Long id) {
		MiniMember admin = authService.requireParkAdmin();
		Customer customer = requireCustomer(id, admin.getParkId());
		Map<String, Object> result = customerMap(customer, true);
		List<Map<String, Object>> contractList = contractService.list(Wrappers.<Contract>lambdaQuery().eq(Contract::getCustomerId, id)
			.eq(Contract::getParkId, admin.getParkId()).eq(Contract::getDelFlag, "0")).stream().map(item -> contractMap(item, false)).toList();
		result.put("contracts", contractList);
		return result;
	}

	private <T> T idempotent(String requestId, Supplier<T> supplier) {
		if (StringUtil.isBlank(requestId)) throw new ServiceException("缺少 X-Request-Id 请求头");
		String key = "miniapp:idempotent:" + AuthUtil.getTenantId() + ":" + AuthUtil.getUserId() + ":" + requestId;
		Boolean first = bladeRedis.getStringRedisTemplate().opsForValue().setIfAbsent(key, "PROCESSING", Duration.ofHours(24));
		if (!Boolean.TRUE.equals(first)) throw new ServiceException("请勿重复提交");
		try {
			T result = supplier.get(); bladeRedis.getStringRedisTemplate().opsForValue().set(key, "SUCCESS", Duration.ofHours(24)); return result;
		} catch (RuntimeException exception) {
			bladeRedis.del(key); throw exception;
		}
	}

	private void notifyAdmins(MiniMember source, String noticeType, String title, String content, String targetType, Long targetId) {
		List<MiniMember> admins = memberMapper.selectList(Wrappers.<MiniMember>lambdaQuery().eq(MiniMember::getTenantId, source.getTenantId())
			.eq(MiniMember::getParkId, source.getParkId()).eq(MiniMember::getRoleCode, MiniAppConstant.ROLE_PARK_ADMIN)
			.eq(MiniMember::getStatus, StatusType.ACTIVE.getType()).eq(MiniMember::getIsDeleted, 0));
		for (MiniMember admin : admins) {
			MiniNotification notice = new MiniNotification();
			notice.setTenantId(source.getTenantId()); notice.setRecipientUserId(admin.getUserId()); notice.setParkId(source.getParkId());
			notice.setCustomerId(source.getCustomerId()); notice.setNoticeType(noticeType); notice.setTitle(title); notice.setContent(content);
			notice.setTargetType(targetType); notice.setTargetId(String.valueOf(targetId)); notice.setReadStatus(0);
			notice.setStatus(StatusType.ACTIVE.getType()); notice.setIsDeleted(0); notificationMapper.insert(notice);
		}
	}

	private HouseAppointment scopedAppointment(Long id, Long parkId, Long customerId) {
		HouseAppointment item = appointmentMapper.selectById(id);
		if (item == null || !Objects.equals(item.getParkId(), parkId) || (customerId != null && !Objects.equals(item.getCustomerId(), customerId)))
			throw new ServiceException("预约不存在或无权访问");
		return item;
	}

	private Contract scopedContract(Long id, Long parkId, Long customerId) {
		Contract item = contractService.getById(id);
		if (item == null || !Objects.equals(item.getParkId(), parkId) || !Objects.equals(item.getCustomerId(), customerId) || !"0".equals(item.getDelFlag()))
			throw new ServiceException("合同不存在或无权访问");
		return item;
	}

	private Customer requireCustomer(Long customerId, Long parkId) {
		Customer customer = customerService.selectCustomerById(customerId);
		if (customer == null || !Objects.equals(customer.getParkId(), parkId) || !"0".equals(customer.getDelFlag()))
			throw new ServiceException("企业不存在或无权访问");
		return customer;
	}

	private void assertScope(Long parkId, Long customerId, MiniMember member) {
		if (parkId == null || !Objects.equals(parkId, member.getParkId()) || !Objects.equals(customerId, member.getCustomerId()))
			throw new ServiceException("工单不存在或无权访问");
	}

	private void assertAdminPark(Long parkId, MiniMember admin) {
		if (parkId == null || !Objects.equals(parkId, admin.getParkId())) throw new ServiceException("业务不存在或无权访问");
	}

	private UtilityBillDetail scopedUtilityBill(Long id, MiniMember member) {
		UtilityBillDetail detail = id == null ? null : utilityBillDetailMapper.selectOne(
			Wrappers.<UtilityBillDetail>lambdaQuery()
				.eq(UtilityBillDetail::getId, id)
				.eq(UtilityBillDetail::getTenantId, member.getTenantId())
				.eq(UtilityBillDetail::getParkId, member.getParkId())
				.eq(UtilityBillDetail::getCustomerId, member.getCustomerId())
				.eq(UtilityBillDetail::getPublishStatus, "PUBLISHED")
				.eq(UtilityBillDetail::getIsDeleted, 0));
		if (detail == null) {
			throw new ServiceException("水电账单不存在或无权访问");
		}
		return detail;
	}

	private void assertUtilityPaymentScope(UtilityBillDetail detail, ContractPayment payment) {
		if (payment == null || !Objects.equals(payment.getPaymentId(), detail.getPaymentId())
			|| !Objects.equals(payment.getContractId(), detail.getContractId())
			|| !Objects.equals(payment.getParkId(), detail.getParkId())
			|| !Objects.equals(payment.getCustomerId(), detail.getCustomerId())) {
			throw new ServiceException("水电账单关联信息异常或无权访问");
		}
	}

	private MerchantAd toCustomerAd(MiniBusinessDTO.CustomerAd request, Long adId) {
		MerchantAd ad = new MerchantAd();
		ad.setAdId(adId);
		ad.setAdTitle(request.getAdTitle());
		ad.setAdPosition("miniapp_home");
		ad.setCoverUrl(request.getCoverUrl());
		ad.setLinkType(StringUtil.isBlank(request.getLinkType()) ? "none" : request.getLinkType());
		ad.setLinkUrl(request.getLinkUrl());
		ad.setMerchantId(request.getMerchantId());
		ad.setStartTime(request.getStartTime());
		ad.setEndTime(request.getEndTime());
		ad.setRemark(request.getRemark());
		return ad;
	}

	private Map<String, Object> houseMap(Room room) {
		return Kv.create().set("id", room.getId()).set("title", room.getBuildingName() + " " + room.getName())
			.set("image", firstImage(room.getSceneImages())).set("building", room.getBuildingName()).set("room", room.getName())
			.set("area", room.getArea()).set("floor", room.getFloor()).set("layout", room.getHouseType())
			.set("orientation", room.getOrientation()).set("price", room.getRentPrice()).set("propertyFee", room.getPropertyFee())
			.set("status", "可租").set("availableDate", room.getVacantSince()).set("intro", room.getHighlights())
			.set("facilities", split(room.getFacilities())).set("images", split(room.getSceneImages()));
	}

	private Map<String, Object> adMap(MerchantAd item) { return Kv.create().set("id", item.getAdId()).set("title", item.getAdTitle()).set("image", item.getCoverUrl()).set("linkType", item.getLinkType()).set("linkUrl", item.getLinkUrl()); }
	private Map<String, Object> policyMap(PolicyService item) { return Kv.create().set("id", item.getPolicyId()).set("title", item.getServiceTitle()).set("summary", item.getProjectScope()).set("image", item.getCoverUrl()).set("time", item.getCreateTime()); }
	private Map<String, Object> activityMap(ParkActivity item) { return Kv.create().set("id", item.getId()).set("title", item.getTitle()).set("image", item.getCoverUrl()).set("summary", item.getSummary()).set("startTime", item.getStartTime()).set("endTime", item.getEndTime()).set("address", item.getAddress()).set("price", item.getPriceText()); }
	private Map<String, Object> propertyServiceMap(PropertyService item) { return Kv.create().set("id", item.getServiceId()).set("title", item.getServiceName()).set("type", item.getServiceType()).set("desc", item.getServiceDesc()).set("materials", item.getRequiredMaterials()).set("flow", item.getServiceFlow()).set("charge", item.getChargeStandard()); }
	private Map<String, Object> merchantMap(Merchant item) { return Kv.create().set("id", item.getMerchantId()).set("title", item.getMerchantName()).set("category", item.getBusinessType()).set("desc", item.getServiceScope()).set("serviceArea", item.getServiceArea()).set("contactName", maskName(item.getContactName())).set("contactPhone", maskPhone(item.getContactPhone())).set("address", item.getAddress()); }
	private Map<String, Object> appointmentMap(HouseAppointment item) { return Kv.create().set("id", item.getId()).set("kind", "appointment").set("no", item.getAppointmentNo()).set("roomId", item.getRoomId()).set("companyName", item.getEnterpriseName()).set("contact", item.getContactName()).set("phone", maskPhone(item.getContactPhone())).set("preferredTime", item.getPreferredTime()).set("description", item.getDemandDesc()).set("status", item.getAppointmentStatus()).set("createTime", item.getCreateTime()); }
	private Map<String, Object> settlementMap(BusinessOpportunity item) { return Kv.create().set("id", item.getOpportunityId()).set("kind", "settlement").set("title", item.getEnterpriseName()).set("companyName", item.getEnterpriseName()).set("contact", item.getContactName()).set("phone", maskPhone(item.getContactPhone())).set("status", item.getOpportunityStatus()).set("description", item.getRemark()).set("createTime", item.getCreateTime()); }
	private Map<String, Object> propertyOrderMap(ServiceWorkorder item) { return Kv.create().set("id", item.getOrderId()).set("kind", "property").set("no", item.getOrderNo()).set("title", item.getServiceName()).set("companyName", item.getCustomerName()).set("room", item.getRoomInfo()).set("contact", item.getContactName()).set("phone", maskPhone(item.getContactPhone())).set("status", item.getOrderStatus()).set("urgency", item.getPriority()).set("handler", item.getAssignTo()).set("description", item.getDemandDesc()).set("createTime", item.getCreateTime()); }
	private Map<String, Object> valueOrderMap(MerchantServiceOrder item) { return Kv.create().set("id", item.getOrderId()).set("kind", "value").set("no", item.getOrderNo()).set("title", item.getServiceType()).set("companyName", item.getCustomerName()).set("contact", item.getContactName()).set("phone", maskPhone(item.getContactPhone())).set("status", item.getOrderStatus()).set("urgency", item.getPriority()).set("handler", item.getAssignTo()).set("description", item.getDemandDesc()).set("createTime", item.getCreateTime()); }
	private Map<String, Object> customerMap(Customer item, boolean detail) { Kv map = Kv.create().set("id", item.getCustomerId()).set("companyName", item.getEnterpriseName()).set("industry", item.getIndustry()).set("scale", item.getScale()).set("status", item.getStatus()).set("contact", maskName(item.getContactName())).set("phone", maskPhone(item.getContactPhone())).set("settlementStatus", item.getSettlementStatus()); if (detail) map.set("creditCode", item.getCreditCode()).set("email", item.getContactEmail()).set("address", item.getAddress()).set("businessScope", item.getBusinessScope()); return map; }
	private Map<String, Object> contractMap(Contract item, boolean detail) { Kv map = Kv.create().set("id", item.getContractId()).set("title", item.getContractName()).set("contractNo", item.getContractNo()).set("room", item.getRoomName()).set("periodStart", item.getStartDate()).set("periodEnd", item.getEndDate()).set("amount", item.getMonthlyRent()).set("status", item.getContractStatus()).set("signDate", item.getSignDate()); if (detail) map.set("rentArea", item.getRentArea()).set("propertyFee", item.getPropertyFee()).set("attachment", item.getContractFileUrl()); return map; }
	private Map<String, Object> billMap(ContractPayment item) { return Kv.create().set("id", item.getPaymentId()).set("contractId", item.getContractId()).set("title", item.getFeeName()).set("periodStart", item.getPeriodStart()).set("periodEnd", item.getPeriodEnd()).set("amount", item.getAmountDue()).set("paidAmount", item.getAmountPaid()).set("dueDate", item.getPayDeadline()).set("payTime", item.getPayTime()).set("status", item.getPayStatus()); }
	private Map<String, Object> utilityBillMap(UtilityBillDetail detail, ContractPayment payment, boolean includeDetail) {
		assertUtilityPaymentScope(detail, payment);
		BigDecimal amountDue = nullToZero(payment.getAmountDue());
		BigDecimal amountPaid = nullToZero(payment.getAmountPaid());
		Kv map = Kv.create().set("id", detail.getId()).set("paymentId", detail.getPaymentId())
			.set("contractId", detail.getContractId()).set("type", detail.getRecordType())
			.set("title", "water".equals(detail.getRecordType()) ? "水费" : "电费")
			.set("room", payment.getSelectedRoomName()).set("periodStart", detail.getPeriodStart())
			.set("periodEnd", detail.getPeriodEnd()).set("amount", amountDue).set("paidAmount", amountPaid)
			.set("remainingAmount", amountDue.subtract(amountPaid).max(BigDecimal.ZERO))
			.set("dueDate", payment.getPayDeadline()).set("payTime", payment.getPayTime())
			.set("status", payment.getPayStatus()).set("publishTime", detail.getPublishedTime());
		if (includeDetail) {
			map.set("deviceId", detail.getDeviceId()).set("startRecordId", detail.getStartRecordId())
				.set("endRecordId", detail.getEndRecordId()).set("previousReading", detail.getPreviousReading())
				.set("currentReading", detail.getCurrentReading()).set("usage", detail.getUsageAmount())
				.set("unitPrice", detail.getUnitPrice()).set("paymentVoucherName", payment.getPaymentVoucherName())
				.set("paymentVoucherUrl", payment.getPaymentVoucherUrl());
		}
		return map;
	}
	private Map<String, Object> paymentSubmissionMap(MiniPaymentSubmission item) {
		return Kv.create().set("id", item.getId()).set("paymentId", item.getPaymentId())
			.set("amount", item.getSubmitAmount()).set("voucherName", item.getVoucherName())
			.set("voucherUrl", item.getVoucherUrl()).set("status", item.getSubmitStatus())
			.set("auditUserName", item.getAuditUserName()).set("auditTime", item.getAuditTime())
			.set("auditOpinion", item.getAuditOpinion()).set("createTime", item.getCreateTime());
	}
	private Map<String, Object> customerAdMap(MerchantAd item, boolean detail) {
		Kv map = Kv.create().set("id", item.getAdId()).set("title", item.getAdTitle())
			.set("image", item.getCoverUrl()).set("auditStatus", item.getAuditStatus())
			.set("onlineStatus", item.getStatus()).set("startTime", item.getStartTime())
			.set("endTime", item.getEndTime()).set("auditOpinion", item.getAuditOpinion())
			.set("auditTime", item.getAuditTime()).set("createTime", item.getCreateTime());
		if (detail) {
			map.set("linkType", item.getLinkType()).set("linkUrl", item.getLinkUrl())
				.set("merchantId", item.getMerchantId()).set("merchantName", item.getMerchantName())
				.set("remark", item.getRemark()).set("auditUserName", item.getAuditUserName());
		}
		return map;
	}
	private Map<String, Object> adAuditLogMap(MerchantAdAuditLog item) {
		return Kv.create().set("id", item.getId()).set("action", item.getActionType())
			.set("beforeAuditStatus", item.getBeforeAuditStatus()).set("afterAuditStatus", item.getAfterAuditStatus())
			.set("beforeOnlineStatus", item.getBeforeOnlineStatus()).set("afterOnlineStatus", item.getAfterOnlineStatus())
			.set("operatorName", item.getOperatorName()).set("opinion", item.getOpinion())
			.set("operateTime", item.getOperateTime());
	}
	private Map<String, Object> memberMap(MiniMember item) { return Kv.create().set("id", item.getId()).set("nickname", item.getNickname()).set("mobile", maskPhone(item.getMobile())).set("roleCode", item.getRoleCode()).set("status", item.getStatus()).set("lastLoginTime", item.getLastLoginTime()); }
	private Map<String, Object> inviteMap(MiniInvite item) { return Kv.create().set("id", item.getId()).set("mobile", maskPhone(item.getMobile())).set("roleCode", item.getRoleCode()).set("expireTime", item.getExpireTime()).set("maxUses", item.getMaxUses()).set("usedCount", item.getUsedCount()).set("status", item.getStatus()); }
	private Map<String, Object> notificationMap(MiniNotification item) { return Kv.create().set("id", item.getId()).set("type", item.getNoticeType()).set("title", item.getTitle()).set("content", item.getContent()).set("target", item.getTargetType()).set("targetId", item.getTargetId()).set("status", item.getReadStatus() == 1 ? "read" : "unread").set("time", item.getCreateTime()); }

	private String firstImage(String value) { List<String> images = split(value); return images.isEmpty() ? null : images.get(0); }
	private List<String> split(String value) { return StringUtil.isBlank(value) ? Collections.emptyList() : Arrays.stream(value.split(",")).map(String::trim).filter(StringUtil::isNotBlank).toList(); }
	private boolean contains(String value, String keyword) { return value != null && value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT)); }
	private String maskPhone(String phone) { return StringUtil.isBlank(phone) || phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4); }
	private String maskName(String name) { return StringUtil.isBlank(name) || name.length() < 2 ? name : name.substring(0, 1) + "**"; }
	private BigDecimal nullToZero(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
	private String randomDigits() { return String.valueOf(1000 + new Random().nextInt(9000)); }
}
