/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.PropertyWorkorderMapper;
import org.springblade.modules.business.pojo.entity.PropertyService;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.business.pojo.entity.WorkorderLog;
import org.springblade.modules.business.service.IPropertyServiceService;
import org.springblade.modules.business.service.IPropertyWorkorderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 物业服务工单服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class PropertyWorkorderServiceImpl extends ServiceImpl<PropertyWorkorderMapper, ServiceWorkorder> implements IPropertyWorkorderService {

	private static final String DEL_FLAG_NORMAL = "0";
	private static final String STATUS_PENDING = "0";
	private static final String STATUS_PROCESSING = "1";
	private static final String STATUS_FINISHED = "2";
	private static final String STATUS_RATED = "3";
	private static final String STATUS_CLOSED = "4";
	private static final String PRIORITY_NORMAL = "1";
	private static final String SERVICE_STATUS_NORMAL = "0";

	private final IPropertyServiceService propertyServiceService;

	@Override
	public ServiceWorkorder selectWorkorderById(Long orderId) {
		ServiceWorkorder workorder = baseMapper.selectWorkorderById(orderId);
		if (Func.isNotEmpty(workorder) && !hasAccessToPark(workorder.getParkId())) {
			throw new ServiceException("工单不存在");
		}
		return workorder;
	}

	@Override
	public List<ServiceWorkorder> selectWorkorderList(ServiceWorkorder workorder) {
		return baseMapper.selectWorkorderList(normalizeQuery(workorder));
	}

	@Override
	public IPage<ServiceWorkorder> selectWorkorderPage(IPage<ServiceWorkorder> page, ServiceWorkorder workorder) {
		return baseMapper.selectWorkorderPage(page, normalizeQuery(workorder));
	}

	@Override
	public Map<String, Object> selectWorkorderStatistics(ServiceWorkorder workorder) {
		Map<String, Object> statistics = baseMapper.selectWorkorderStatistics(normalizeQuery(workorder));
		return Func.isEmpty(statistics) ? Collections.emptyMap() : statistics;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertWorkorder(ServiceWorkorder workorder) {
		validateCreate(workorder);
		PropertyService service = requireUsableService(workorder.getServiceId());
		Long targetParkId = resolveWriteParkId(workorder.getParkId());
		if (Func.isNotEmpty(service.getParkId())) {
			targetParkId = service.getParkId();
		}
		validateServicePark(targetParkId, service);
		Date now = DateUtil.now();
		workorder.setParkId(targetParkId);
		workorder.setOrderNo(generateOrderNo());
		workorder.setOrderStatus(StringUtil.isBlank(workorder.getOrderStatus()) ? STATUS_PENDING : workorder.getOrderStatus());
		workorder.setPriority(StringUtil.isBlank(workorder.getPriority()) ? PRIORITY_NORMAL : workorder.getPriority());
		workorder.setDelFlag(DEL_FLAG_NORMAL);
		workorder.setCreateBy(currentUserName());
		workorder.setCreateTime(now);
		normalizeProcessFields(workorder, null, now);
		int rows = baseMapper.insertWorkorder(workorder);
		if (rows > 0) {
			addLog(workorder.getOrderId(), "create", "工单创建");
			addProcessLogs(workorder, null);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateWorkorder(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder.getOrderId())) {
			throw new ServiceException("工单不存在");
		}
		ServiceWorkorder old = requireWritableWorkorder(workorder.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("已关闭工单不可继续处置");
		}
		if (Func.isNotEmpty(workorder.getServiceId())) {
			PropertyService service = requireUsableService(workorder.getServiceId());
			validateServicePark(old.getParkId(), service);
		}
		Date now = DateUtil.now();
		workorder.setParkId(old.getParkId());
		workorder.setUpdateBy(currentUserName());
		workorder.setUpdateTime(now);
		normalizeProcessFields(workorder, old, now);
		int rows = baseMapper.updateWorkorder(workorder);
		if (rows > 0) {
			addProcessLogs(workorder, old);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitWorkorder(ServiceWorkorder workorder) {
		return Func.isEmpty(workorder.getOrderId()) ? insertWorkorder(workorder) : updateWorkorder(workorder);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteWorkorderByIds(String ids) {
		List<Long> orderIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (orderIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的工单");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteWorkorderByIds(orderIds, parkId) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean assignWorkorder(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder) || Func.isEmpty(workorder.getOrderId())) {
			throw new ServiceException("工单不存在");
		}
		if (StringUtil.isBlank(workorder.getAssignTo())) {
			throw new ServiceException("请选择指派人");
		}
		ServiceWorkorder old = requireWritableWorkorder(workorder.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("已关闭工单不可继续处置");
		}
		ServiceWorkorder patch = new ServiceWorkorder();
		patch.setOrderId(workorder.getOrderId());
		patch.setOrderStatus(STATUS_PROCESSING);
		patch.setAssignTo(workorder.getAssignTo());
		patch.setProcessor(workorder.getProcessor());
		patch.setProcessRemark(workorder.getProcessRemark());
		patch.setAssignTime(DateUtil.now());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateWorkorder(patch);
		if (rows > 0) {
			addLog(workorder.getOrderId(), "assign", "指派给：" + workorder.getAssignTo());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean finishWorkorder(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder) || Func.isEmpty(workorder.getOrderId())) {
			throw new ServiceException("工单不存在");
		}
		ServiceWorkorder old = requireWritableWorkorder(workorder.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("已关闭工单不可继续处置");
		}
		if (StringUtil.isBlank(workorder.getAssignTo()) && StringUtil.isBlank(old.getAssignTo())) {
			throw new ServiceException("请选择指派人");
		}
		if (StringUtil.isBlank(workorder.getDisposalContent())) {
			throw new ServiceException("请填写处置内容");
		}
		ServiceWorkorder patch = new ServiceWorkorder();
		patch.setOrderId(workorder.getOrderId());
		patch.setOrderStatus(STATUS_FINISHED);
		patch.setAssignTo(StringUtil.isBlank(workorder.getAssignTo()) ? old.getAssignTo() : workorder.getAssignTo());
		patch.setDisposalContent(workorder.getDisposalContent());
		patch.setProcessor(workorder.getProcessor());
		patch.setProcessRemark(workorder.getProcessRemark());
		patch.setFinishTime(DateUtil.now());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateWorkorder(patch);
		if (rows > 0) {
			addLog(workorder.getOrderId(), "finish", "工单已完成");
			addLog(workorder.getOrderId(), "process", workorder.getDisposalContent());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean closeWorkorder(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder) || Func.isEmpty(workorder.getOrderId())) {
			throw new ServiceException("工单不存在");
		}
		ServiceWorkorder old = requireWritableWorkorder(workorder.getOrderId());
		if (STATUS_CLOSED.equals(old.getOrderStatus())) {
			throw new ServiceException("工单已关闭");
		}
		ServiceWorkorder patch = new ServiceWorkorder();
		patch.setOrderId(workorder.getOrderId());
		patch.setOrderStatus(STATUS_CLOSED);
		patch.setProcessRemark(workorder.getProcessRemark());
		patch.setRemark(workorder.getRemark());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateWorkorder(patch);
		if (rows > 0) {
			addLog(workorder.getOrderId(), "close", StringUtil.isBlank(workorder.getProcessRemark()) ? "工单已关闭" : workorder.getProcessRemark());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean rateWorkorder(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder) || Func.isEmpty(workorder.getOrderId())) {
			throw new ServiceException("工单不存在");
		}
		ServiceWorkorder old = requireWritableWorkorder(workorder.getOrderId());
		if (!STATUS_FINISHED.equals(old.getOrderStatus()) && !STATUS_RATED.equals(old.getOrderStatus())) {
			throw new ServiceException("已完成工单才可评价");
		}
		if (Func.isEmpty(workorder.getRating()) || workorder.getRating() < 1 || workorder.getRating() > 5) {
			throw new ServiceException("评分范围为1-5");
		}
		ServiceWorkorder patch = new ServiceWorkorder();
		patch.setOrderId(workorder.getOrderId());
		patch.setOrderStatus(STATUS_RATED);
		patch.setRating(workorder.getRating());
		patch.setRatingContent(workorder.getRatingContent());
		patch.setRatingTime(DateUtil.now());
		patch.setUpdateBy(currentUserName());
		int rows = baseMapper.updateWorkorder(patch);
		if (rows > 0) {
			addLog(workorder.getOrderId(), "rate", "评价：" + workorder.getRating() + "星");
		}
		return rows > 0;
	}

	@Override
	public List<WorkorderLog> selectLogByOrderId(Long orderId) {
		ServiceWorkorder workorder = selectWorkorderById(orderId);
		if (Func.isEmpty(workorder)) {
			throw new ServiceException("工单不存在");
		}
		return baseMapper.selectLogByOrderId(orderId);
	}

	private ServiceWorkorder requireWritableWorkorder(Long orderId) {
		ServiceWorkorder workorder = baseMapper.selectWorkorderById(orderId);
		if (Func.isEmpty(workorder) || !hasAccessToPark(workorder.getParkId())) {
			throw new ServiceException("工单不存在");
		}
		return workorder;
	}

	private PropertyService requireUsableService(Long serviceId) {
		if (Func.isEmpty(serviceId)) {
			throw new ServiceException("请选择服务项");
		}
		PropertyService service = propertyServiceService.selectPropertyServiceById(serviceId);
		if (Func.isEmpty(service)) {
			throw new ServiceException("服务项不存在");
		}
		if (!SERVICE_STATUS_NORMAL.equals(service.getStatus())) {
			throw new ServiceException("服务项已停用，不能发起工单");
		}
		return service;
	}

	private void validateCreate(ServiceWorkorder workorder) {
		if (Func.isEmpty(workorder)) {
			throw new ServiceException("工单不能为空");
		}
		if (StringUtil.isBlank(workorder.getCustomerName())) {
			throw new ServiceException("客户名称不能为空");
		}
		if (StringUtil.isBlank(workorder.getContactName())) {
			throw new ServiceException("联系人不能为空");
		}
		if (StringUtil.isBlank(workorder.getContactPhone())) {
			throw new ServiceException("联系电话不能为空");
		}
		if (StringUtil.isBlank(workorder.getRoomInfo()) && StringUtil.isBlank(workorder.getRoomIds())) {
			throw new ServiceException("房源不能为空");
		}
		if (StringUtil.isBlank(workorder.getDemandDesc())) {
			throw new ServiceException("需求描述不能为空");
		}
	}

	private void normalizeProcessFields(ServiceWorkorder workorder, ServiceWorkorder old, Date now) {
		String targetStatus = StringUtil.isBlank(workorder.getOrderStatus()) ? (old == null ? STATUS_PENDING : old.getOrderStatus()) : workorder.getOrderStatus();
		if ((STATUS_PROCESSING.equals(targetStatus) || STATUS_FINISHED.equals(targetStatus))
			&& StringUtil.isBlank(workorder.getAssignTo())
			&& (old == null || StringUtil.isBlank(old.getAssignTo()))) {
			throw new ServiceException("请选择指派人");
		}
		if ((STATUS_PROCESSING.equals(targetStatus) || STATUS_FINISHED.equals(targetStatus))
			&& StringUtil.isBlank(workorder.getDisposalContent())) {
			throw new ServiceException("请填写处置内容");
		}
		if (STATUS_PROCESSING.equals(targetStatus) && workorder.getAssignTime() == null && (old == null || old.getAssignTime() == null)) {
			workorder.setAssignTime(now);
		}
		if (STATUS_FINISHED.equals(targetStatus) && workorder.getFinishTime() == null && (old == null || old.getFinishTime() == null)) {
			workorder.setFinishTime(now);
		}
		workorder.setOrderStatus(targetStatus);
	}

	private void addProcessLogs(ServiceWorkorder workorder, ServiceWorkorder old) {
		if (StringUtil.isNotBlank(workorder.getAssignTo()) && (old == null || !workorder.getAssignTo().equals(old.getAssignTo()))) {
			addLog(workorder.getOrderId(), "assign", "指派给：" + workorder.getAssignTo());
		}
		if (STATUS_FINISHED.equals(workorder.getOrderStatus()) && (old == null || !STATUS_FINISHED.equals(old.getOrderStatus()))) {
			addLog(workorder.getOrderId(), "finish", "工单已完成");
		}
		if (STATUS_CLOSED.equals(workorder.getOrderStatus()) && (old == null || !STATUS_CLOSED.equals(old.getOrderStatus()))) {
			addLog(workorder.getOrderId(), "close", "工单已关闭");
		}
		if (StringUtil.isNotBlank(workorder.getDisposalContent())) {
			addLog(workorder.getOrderId(), "process", workorder.getDisposalContent());
		}
	}

	private void validateServicePark(Long targetParkId, PropertyService service) {
		if (Func.isNotEmpty(service.getParkId()) && Func.isNotEmpty(targetParkId) && !service.getParkId().equals(targetParkId)) {
			throw new ServiceException("服务项与工单园区不一致");
		}
	}

	private ServiceWorkorder normalizeQuery(ServiceWorkorder workorder) {
		ServiceWorkorder query = Func.isEmpty(workorder) ? new ServiceWorkorder() : workorder;
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
		return AuthUtil.isAdministrator() || currentParkId().equals(parkId);
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
		return "WO" + datePrefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
	}

	private void addLog(Long orderId, String action, String desc) {
		WorkorderLog log = new WorkorderLog();
		log.setOrderId(orderId);
		log.setAction(action);
		log.setActionDesc(desc);
		log.setOperator(currentUserName());
		log.setOperateTime(DateUtil.now());
		baseMapper.insertLog(log);
	}

}
