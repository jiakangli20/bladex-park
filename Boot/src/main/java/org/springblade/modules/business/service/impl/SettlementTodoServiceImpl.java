/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.SettlementTodoMapper;
import org.springblade.modules.business.pojo.dto.SettlementTodoActionDTO;
import org.springblade.modules.business.pojo.entity.SettlementTodo;
import org.springblade.modules.business.service.ISettlementTodoService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 招商待办服务实现。
 */
@Service
@RequiredArgsConstructor
public class SettlementTodoServiceImpl extends ServiceImpl<SettlementTodoMapper, SettlementTodo> implements ISettlementTodoService {

	private static final Set<String> FINISHED_STATUSES = Set.of("3", "4");
	private final IParkPermissionService parkPermissionService;

	@Override
	public IPage<SettlementTodo> selectPage(IPage<SettlementTodo> page, SettlementTodo query) {
		return page(page, queryWrapper(query)
			.orderByAsc(SettlementTodo::getTodoStatus).orderByDesc(SettlementTodo::getCreateTime));
	}

	@Override
	public Map<String, Object> statistics(SettlementTodo query) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("totalCount", count(queryWrapper(query)));
		result.put("pendingCount", count(queryWrapper(query).eq(SettlementTodo::getTodoStatus, "0")));
		result.put("processingCount", count(queryWrapper(query).in(SettlementTodo::getTodoStatus, "1", "2")));
		result.put("finishedCount", count(queryWrapper(query).eq(SettlementTodo::getTodoStatus, "3")));
		result.put("rejectedCount", count(queryWrapper(query).eq(SettlementTodo::getTodoStatus, "4")));
		return result;
	}

	private LambdaQueryWrapper<SettlementTodo> queryWrapper(SettlementTodo query) {
		SettlementTodo normalizedQuery = query == null ? new SettlementTodo() : query;
		if (normalizedQuery.getParkId() != null) parkPermissionService.requirePark(normalizedQuery.getParkId());
		List<Long> authorizedParkIds = parkPermissionService.authorizedParkIds();
		LambdaQueryWrapper<SettlementTodo> wrapper = Wrappers.<SettlementTodo>lambdaQuery()
			.eq(SettlementTodo::getTenantId, tenantId())
			.eq(normalizedQuery.getParkId() != null, SettlementTodo::getParkId, normalizedQuery.getParkId())
			.eq(StringUtil.isNotBlank(normalizedQuery.getTodoStatus()), SettlementTodo::getTodoStatus, normalizedQuery.getTodoStatus())
			.and(StringUtil.isNotBlank(normalizedQuery.getKeyword()), nested -> nested
				.like(SettlementTodo::getEnterpriseName, normalizedQuery.getKeyword())
				.or().like(SettlementTodo::getContactName, normalizedQuery.getKeyword())
				.or().like(SettlementTodo::getContactPhone, normalizedQuery.getKeyword()))
			.eq(SettlementTodo::getDelFlag, "0");
		if (authorizedParkIds != null) {
			if (authorizedParkIds.isEmpty()) wrapper.apply("1 = 0");
			else wrapper.in(SettlementTodo::getParkId, authorizedParkIds);
		}
		return wrapper;
	}

	@Override
	public SettlementTodo requireScoped(Long id, Long parkId, Long customerId) {
		SettlementTodo todo = getOne(Wrappers.<SettlementTodo>lambdaQuery()
			.eq(SettlementTodo::getTodoId, id).eq(SettlementTodo::getTenantId, tenantId())
			.eq(parkId != null, SettlementTodo::getParkId, parkId)
			.eq(customerId != null, SettlementTodo::getCustomerId, customerId)
			.eq(SettlementTodo::getDelFlag, "0"));
		if (todo == null) throw new ServiceException("招商待办不存在或无权访问");
		parkPermissionService.requirePark(todo.getParkId());
		return todo;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void process(Long id, SettlementTodoActionDTO request) {
		SettlementTodo todo = requireScoped(id, null, null);
		if (FINISHED_STATUSES.contains(todo.getTodoStatus())) throw new ServiceException("当前招商待办已结束，不能重复处理");
		String action = request.getAction().toUpperCase(Locale.ROOT);
		String nextStatus = nextStatus(todo.getTodoStatus(), action);
		todo.setTodoStatus(nextStatus);
		todo.setProcessRemark(request.getContent());
		todo.setRejectReason(request.getReason());
		todo.setAssigneeUserId(AuthUtil.getUserId());
		todo.setAssigneeName(StringUtil.isBlank(AuthUtil.getNickName()) ? AuthUtil.getUserName() : AuthUtil.getNickName());
		todo.setProcessedTime(new Date());
		todo.setUpdateBy(AuthUtil.getUserName());
		todo.setUpdateTime(new Date());
		updateById(todo);
	}

	private String nextStatus(String currentStatus, String action) {
		return switch (currentStatus) {
			case "0" -> switch (action) {
				case "ACCEPT", "ASSIGN" -> "1";
				case "REJECT" -> "4";
				default -> throw new ServiceException("请先受理招商待办");
			};
			case "1" -> switch (action) {
				case "FOLLOW" -> "2";
				case "COMPLETE", "DEAL" -> "3";
				case "REJECT" -> "4";
				default -> throw new ServiceException("当前招商待办已受理，不能重复受理");
			};
			case "2" -> switch (action) {
				case "FOLLOW" -> "2";
				case "COMPLETE", "DEAL" -> "3";
				case "REJECT" -> "4";
				default -> throw new ServiceException("跟进中的招商待办不能退回受理状态");
			};
			default -> throw new ServiceException("招商待办状态异常");
		};
	}

	private String tenantId() {
		return Objects.toString(AuthUtil.getTenantId(), "000000");
	}
}
