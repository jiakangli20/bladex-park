/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.home.mapper.HomeMapper;
import org.springblade.modules.home.pojo.vo.HomeMissingApiVO;
import org.springblade.modules.home.pojo.vo.HomeOverviewVO;
import org.springblade.modules.home.pojo.vo.HomeTodoVO;
import org.springblade.modules.home.pojo.vo.HomeWorkbenchVO;
import org.springblade.modules.home.service.IHomeService;
import org.springblade.modules.ics.service.IPaymentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页聚合服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements IHomeService {

	private final HomeMapper homeMapper;
	private final IPaymentService paymentService;

	@Override
	public HomeWorkbenchVO workbench() {
		Long parkId = null;
		String currentUser = currentUserName();
		Boolean admin = AuthUtil.isAdministrator();

		Long roomCount = zeroIfNull(homeMapper.countRooms(parkId));
		Long customerCount = zeroIfNull(homeMapper.countCustomers(parkId));
		Long expiringContractCount = zeroIfNull(homeMapper.countExpiringContracts(parkId));
		Long approvalTodoCount = zeroIfNull(homeMapper.countApprovalTodos(parkId, currentUser, admin));
		Long workorderTodoCount = zeroIfNull(homeMapper.countWorkorderTodos(parkId, currentUser, admin));
		Long overdueNoticeCount = zeroIfNull(paymentService.unreadOverdueNoticeCount());

		HomeOverviewVO overview = new HomeOverviewVO();
		overview.setRoomCount(roomCount);
		overview.setCustomerCount(customerCount);
		overview.setExpiringContractCount(expiringContractCount);
		overview.setApprovalTodoCount(approvalTodoCount);
		overview.setWorkorderTodoCount(workorderTodoCount);

		HomeTodoVO todos = new HomeTodoVO();
		todos.setApprovalTodoCount(approvalTodoCount);
		todos.setExpiringContractCount(expiringContractCount);
		todos.setWorkorderTodoCount(workorderTodoCount);
		todos.setOverdueNoticeCount(overdueNoticeCount);

		HomeWorkbenchVO workbench = new HomeWorkbenchVO();
		workbench.setOverview(overview);
		workbench.setTodos(todos);
		workbench.setEnterprises(homeMapper.selectEnterprises(parkId));
		workbench.setMissingApis(missingApis());
		return workbench;
	}

	@Override
	public List<HomeMissingApiVO> missingApis() {
		List<HomeMissingApiVO> list = new ArrayList<>();
		list.add(new HomeMissingApiVO("首页Banner", "home_banner / /blade-home/home/banner", "目标库暂未发现 Banner 表和管理接口，当前返回默认 Banner", "第三步"));
		list.add(new HomeMissingApiVO("园区政策通知", "home_policy / /blade-home/home/policy-notice", "目标库暂未发现政策通知表和发布接口，当前返回空列表", "第三步"));
		list.add(new HomeMissingApiVO("日程安排", "/blade-home/home/schedule", "迁移清单未指定可用日程表，本期仅保留页面占位", "第三步"));
		return list;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private Long zeroIfNull(Long value) {
		return value == null ? 0L : value;
	}

}
