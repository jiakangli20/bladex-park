/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.MerchantServiceOrderMapper;
import org.springblade.modules.business.pojo.entity.Merchant;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrder;
import org.springblade.modules.business.pojo.entity.MerchantServiceOrderLog;
import org.springblade.modules.business.service.IMerchantService;
import org.springblade.modules.business.service.IMerchantServiceOrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 商户增值服务处理单服务实现.
 *
 * @author BladeX
 */
@Service
public class MerchantServiceOrderServiceImpl extends ServiceImpl<MerchantServiceOrderMapper, MerchantServiceOrder> implements IMerchantServiceOrderService {

	private static final String DEL_FLAG_NORMAL = "0";
	private static final String STATUS_PENDING = "0";
	private static final String STATUS_PROCESSING = "1";
	private static final String STATUS_DEALT = "2";
	private static final String STATUS_CLOSED = "3";
	private static final String PRIORITY_NORMAL = "1";

	private final IMerchantService merchantService;

	public MerchantServiceOrderServiceImpl(IMerchantService merchantService) {
		this.merchantService = merchantService;
	}

	@Override
	public MerchantServiceOrder selectOrderById(Long orderId) {
		MerchantServiceOrder order = baseMapper.selectOrderById(orderId);
		if (Func.isNotEmpty(order) && !hasAccessToPark(order.getParkId())) {
			throw new ServiceException("服务单不存在");
		}
		return order;
	}

	@Override
	public List<MerchantServiceOrder> selectOrderList(MerchantServiceOrder order) {
		return baseMapper.selectOrderList(normalizeQuery(order));
	}

	@Override
	public IPage<MerchantServiceOrder> selectOrderPage(IPage<MerchantServiceOrder> page, MerchantServiceOrder order) {
		return baseMapper.selectOrderPage(page, normalizeQuery(order));
	}

	@Override
	public Kv selectOrderStatistics(MerchantServiceOrder order) {
		Map<String, Object> statistics = baseMapper.selectOrderStatistics(normalizeQuery(order));
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("pendingCount", toLong(statistics, "pendingCount"))
			.set("processingCount", toLong(statistics, "processingCount"))
			.set("dealCount", toLong(statistics, "dealCount"))
			.set("closedCount", toLong(statistics, "closedCount"))
			.set("dealAmount", statistics == null ? 0 : statistics.getOrDefault("dealAmount", 0));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertOrder(MerchantServiceOrder order) {
		validateCreate(order);
		Date now = DateUtil.now();
		fillMerchantSnapshot(order);
		order.setParkId(resolveWriteParkId(order.getParkId()));
		order.setOrderNo(generateOrderNo());
		order.setOrderStatus(StringUtil.isBlank(order.getOrderStatus()) ? STATUS_PENDING : order.getOrderStatus());
		order.setPriority(StringUtil.isBlank(order.getPriority()) ? PRIORITY_NORMAL : order.getPriority());
		order.setDelFlag(DEL_FLAG_NORMAL);
		order.setCreateBy(currentUserName());
		order.setCreateTime(now);
		order.setUpdateBy(currentUserName());
		order.setUpdateTime(now);
		normalizeProcessFields(order, null, now);
		int rows = baseMapper.insertOrder(order);
		if (rows > 0) {
			addLog(order.getOrderId(), "create", "服务申请已提交");
			addProcessLogIfNeed(order);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateOrder(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getOrderId())) {
			throw new ServiceException("服务单不存在");
		}
		MerchantServiceOrder old = requireWritableOrder(order.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("已关闭服务单不可继续编辑");
		}
		fillMerchantSnapshot(order);
		order.setParkId(old.getParkId());
		order.setUpdateBy(currentUserName());
		order.setUpdateTime(DateUtil.now());
		int rows = baseMapper.updateOrder(order);
		if (rows > 0) {
			addProcessLogIfNeed(order);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitOrder(MerchantServiceOrder order) {
		return Func.isEmpty(order.getOrderId()) ? insertOrder(order) : updateOrder(order);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteOrderByIds(String ids) {
		List<Long> orderIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (orderIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的服务单");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteOrderByIds(orderIds, parkId, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean assignOrder(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getOrderId())) {
			throw new ServiceException("服务单不存在");
		}
		if (StringUtil.isBlank(order.getAssignTo())) {
			throw new ServiceException("请选择处理人");
		}
		MerchantServiceOrder old = requireWritableOrder(order.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus()) || STATUS_DEALT.equals(old.getOrderStatus())) {
			throw new ServiceException("当前服务单不可指派");
		}
		MerchantServiceOrder patch = new MerchantServiceOrder();
		patch.setOrderId(order.getOrderId());
		patch.setOrderStatus(STATUS_PROCESSING);
		patch.setAssignTo(order.getAssignTo());
		patch.setAssignTime(DateUtil.now());
		patch.setProcessContent(order.getProcessContent());
		patch.setNextFollowTime(order.getNextFollowTime());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateOrder(patch);
		if (rows > 0) {
			addLog(order.getOrderId(), "assign", "指派给：" + order.getAssignTo());
			addProcessLogIfNeed(order);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean followOrder(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getOrderId())) {
			throw new ServiceException("服务单不存在");
		}
		if (StringUtil.isBlank(order.getProcessContent())) {
			throw new ServiceException("请填写处理进展");
		}
		MerchantServiceOrder old = requireWritableOrder(order.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus()) || STATUS_DEALT.equals(old.getOrderStatus())) {
			throw new ServiceException("当前服务单不可继续跟进");
		}
		MerchantServiceOrder patch = new MerchantServiceOrder();
		patch.setOrderId(order.getOrderId());
		patch.setOrderStatus(STATUS_PROCESSING);
		patch.setAssignTo(StringUtil.isBlank(order.getAssignTo()) ? defaultAssignee(old) : order.getAssignTo());
		patch.setAssignTime(Func.isEmpty(old.getAssignTime()) ? DateUtil.now() : null);
		patch.setProcessContent(order.getProcessContent());
		patch.setNextFollowTime(order.getNextFollowTime());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateOrder(patch);
		if (rows > 0) {
			if (StringUtil.isNotBlank(order.getAssignTo()) && !order.getAssignTo().equals(old.getAssignTo())) {
				addLog(order.getOrderId(), "assign", "指派给：" + order.getAssignTo());
			}
			addLog(order.getOrderId(), "follow", order.getProcessContent());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean dealOrder(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getOrderId())) {
			throw new ServiceException("服务单不存在");
		}
		MerchantServiceOrder old = requireWritableOrder(order.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("已关闭服务单不可成交");
		}
		MerchantServiceOrder patch = new MerchantServiceOrder();
		patch.setOrderId(order.getOrderId());
		patch.setOrderStatus(STATUS_DEALT);
		patch.setAssignTo(StringUtil.isBlank(order.getAssignTo()) ? defaultAssignee(old) : order.getAssignTo());
		patch.setProcessContent(StringUtil.isBlank(order.getProcessContent()) ? "服务已成交" : order.getProcessContent());
		patch.setDealAmount(order.getDealAmount());
		patch.setDealTime(DateUtil.now());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateOrder(patch);
		if (rows > 0) {
			addLog(order.getOrderId(), "deal", patch.getProcessContent());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean closeOrder(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getOrderId())) {
			throw new ServiceException("服务单不存在");
		}
		MerchantServiceOrder old = requireWritableOrder(order.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("服务单已关闭");
		}
		MerchantServiceOrder patch = new MerchantServiceOrder();
		patch.setOrderId(order.getOrderId());
		patch.setOrderStatus(STATUS_CLOSED);
		patch.setCloseReason(StringUtil.isBlank(order.getCloseReason()) ? "服务单关闭" : order.getCloseReason());
		patch.setProcessContent(order.getProcessContent());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateOrder(patch);
		if (rows > 0) {
			addLog(order.getOrderId(), "close", patch.getCloseReason());
			addProcessLogIfNeed(order);
		}
		return rows > 0;
	}

	@Override
	public List<MerchantServiceOrderLog> selectLogByOrderId(Long orderId) {
		MerchantServiceOrder order = selectOrderById(orderId);
		if (Func.isEmpty(order)) {
			throw new ServiceException("服务单不存在");
		}
		return baseMapper.selectLogByOrderId(orderId);
	}

	private MerchantServiceOrder requireWritableOrder(Long orderId) {
		MerchantServiceOrder order = baseMapper.selectOrderById(orderId);
		if (Func.isEmpty(order) || !hasAccessToPark(order.getParkId())) {
			throw new ServiceException("服务单不存在");
		}
		return order;
	}

	private void validateCreate(MerchantServiceOrder order) {
		if (Func.isEmpty(order)) {
			throw new ServiceException("服务单不能为空");
		}
		if (Func.isEmpty(order.getMerchantId()) && StringUtil.isBlank(order.getMerchantName())) {
			throw new ServiceException("请选择服务商");
		}
		if (StringUtil.isBlank(order.getCustomerName())) {
			throw new ServiceException("客户名称不能为空");
		}
		if (StringUtil.isBlank(order.getContactName())) {
			throw new ServiceException("联系人不能为空");
		}
		if (StringUtil.isBlank(order.getContactPhone())) {
			throw new ServiceException("联系电话不能为空");
		}
		if (StringUtil.isBlank(order.getDemandDesc())) {
			throw new ServiceException("需求描述不能为空");
		}
	}

	private void fillMerchantSnapshot(MerchantServiceOrder order) {
		if (Func.isEmpty(order) || Func.isEmpty(order.getMerchantId())) {
			return;
		}
		Merchant merchant = merchantService.selectMerchantById(order.getMerchantId());
		if (Func.isEmpty(merchant)) {
			throw new ServiceException("服务商不存在");
		}
		order.setMerchantName(StringUtil.isBlank(order.getMerchantName()) ? merchant.getMerchantName() : order.getMerchantName());
		order.setServiceType(StringUtil.isBlank(order.getServiceType()) ? merchant.getBusinessType() : order.getServiceType());
		order.setServiceScope(StringUtil.isBlank(order.getServiceScope()) ? merchant.getServiceScope() : order.getServiceScope());
		if (Func.isNotEmpty(merchant.getParkId())) {
			order.setParkId(merchant.getParkId());
		}
	}

	private void normalizeProcessFields(MerchantServiceOrder order, MerchantServiceOrder old, Date now) {
		String targetStatus = StringUtil.isBlank(order.getOrderStatus()) ? (old == null ? STATUS_PENDING : old.getOrderStatus()) : order.getOrderStatus();
		if (STATUS_PROCESSING.equals(targetStatus) && StringUtil.isBlank(order.getAssignTo()) && (old == null || StringUtil.isBlank(old.getAssignTo()))) {
			throw new ServiceException("请选择处理人");
		}
		if (STATUS_PROCESSING.equals(targetStatus) && StringUtil.isBlank(order.getProcessContent())) {
			throw new ServiceException("请填写处理进展");
		}
		if (STATUS_PROCESSING.equals(targetStatus) && order.getAssignTime() == null && (old == null || old.getAssignTime() == null)) {
			order.setAssignTime(now);
		}
		if (STATUS_DEALT.equals(targetStatus) && order.getDealTime() == null && (old == null || old.getDealTime() == null)) {
			order.setDealTime(now);
		}
		order.setOrderStatus(targetStatus);
	}

	private void addProcessLogIfNeed(MerchantServiceOrder order) {
		if (Func.isNotEmpty(order) && StringUtil.isNotBlank(order.getProcessContent())) {
			addLog(order.getOrderId(), "follow", order.getProcessContent());
		}
	}

	private MerchantServiceOrder normalizeQuery(MerchantServiceOrder order) {
		MerchantServiceOrder query = Func.isEmpty(order) ? new MerchantServiceOrder() : order;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		if (Boolean.TRUE.equals(query.getMine())) {
			query.setCurrentUser(currentUserName());
		}
		return query;
	}

	private Long resolveWriteParkId(Long parkId) {
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(parkId) && parkId > 0) {
			return parkId;
		}
		return currentParkId();
	}

	private boolean hasAccessToPark(Long parkId) {
		return AuthUtil.isAdministrator() || Func.isEmpty(parkId) || currentParkId().equals(parkId);
	}

	private String defaultAssignee(MerchantServiceOrder old) {
		if (Func.isNotEmpty(old) && StringUtil.isNotBlank(old.getAssignTo())) {
			return old.getAssignTo();
		}
		return currentUserName();
	}

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

	private String generateOrderNo() {
		String datePrefix = DateUtil.format(new Date(), "yyyyMMddHHmmss");
		return "MS" + datePrefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
	}

	private void addLog(Long orderId, String action, String desc) {
		MerchantServiceOrderLog log = new MerchantServiceOrderLog();
		log.setOrderId(orderId);
		log.setAction(action);
		log.setActionDesc(desc);
		log.setOperator(currentUserName());
		log.setOperateTime(DateUtil.now());
		baseMapper.insertLog(log);
	}

	private long toLong(Map<String, Object> map, String key) {
		if (Func.isEmpty(map) || !map.containsKey(key) || map.get(key) == null) {
			return 0L;
		}
		Object value = map.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Func.toLong(value, 0L);
	}

}
