/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.pojo.entity.ServiceWorkorder;
import org.springblade.modules.business.pojo.entity.PolicyService;
import org.springblade.modules.business.service.IPolicyServiceService;
import org.springblade.modules.home.mapper.HomeMapper;
import org.springblade.modules.home.pojo.vo.HomeMissingApiVO;
import org.springblade.modules.home.pojo.vo.HomeOverviewVO;
import org.springblade.modules.home.pojo.vo.HomePolicyNoticeVO;
import org.springblade.modules.home.pojo.vo.HomeTodoItemVO;
import org.springblade.modules.home.pojo.vo.HomeTodoVO;
import org.springblade.modules.home.pojo.vo.HomeWorkbenchVO;
import org.springblade.modules.home.service.IHomeService;
import org.springblade.modules.ics.service.IPaymentService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.process.service.IWfProcessService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 首页聚合服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class HomeServiceImpl implements IHomeService {
	private static final int WORKFLOW_PAGE_SIZE = 500;

	private final HomeMapper homeMapper;
	private final IPaymentService paymentService;
	private final IPolicyServiceService policyServiceService;
	private final IWfProcessService wfProcessService;
	private final IParkPermissionService parkPermissionService;

	@Override
	public HomeWorkbenchVO workbench() {
		Long parkId = null;
		String currentUser = currentUserName();
		Boolean admin = AuthUtil.isAdministrator();
		List<Long> authorizedParkIds = parkPermissionService.authorizedParkIds();

		Long roomCount = zeroIfNull(homeMapper.countRooms(parkId, authorizedParkIds));
		Long customerCount = zeroIfNull(homeMapper.countCustomers(parkId, authorizedParkIds));
		Long expiringContractCount = zeroIfNull(homeMapper.countExpiringContracts(parkId, authorizedParkIds));
		Long approvalTodoCount = countWorkflowTodos(authorizedParkIds);
		Long workorderTodoCount = zeroIfNull(homeMapper.countWorkorderTodos(parkId, currentUser, admin, authorizedParkIds));
		Long overdueNoticeCount = zeroIfNull(paymentService.unreadOverdueNoticeCount());
		List<HomeTodoItemVO> workorderItems = homeMapper.selectWorkorderTodos(parkId, currentUser, admin, authorizedParkIds).stream()
			.map(this::toTodoItem)
			.toList();

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
		todos.setItems(workorderItems);

		HomeWorkbenchVO workbench = new HomeWorkbenchVO();
		workbench.setOverview(overview);
		workbench.setTodos(todos);
		workbench.setPolicyNotices(policyNotices());
		workbench.setEnterprises(homeMapper.selectEnterprises(parkId, authorizedParkIds));
		workbench.setMissingApis(missingApis());
		return workbench;
	}

	@Override
	public List<HomeMissingApiVO> missingApis() {
		List<HomeMissingApiVO> list = new ArrayList<>();
		list.add(new HomeMissingApiVO("首页Banner", "home_banner / /blade-home/home/banner", "目标库暂未发现 Banner 表和管理接口，当前返回默认 Banner", "第三步"));
		list.add(new HomeMissingApiVO("日程安排", "/blade-home/home/schedule", "迁移清单未指定可用日程表，本期仅保留页面占位", "第三步"));
		return list;
	}

	private List<HomePolicyNoticeVO> policyNotices() {
		PolicyService query = new PolicyService();
		query.setServiceStatus("0");
		query.setOnlineFlag("0");
		Date now = new Date();
		return policyServiceService.selectPolicyList(query).stream()
			.filter(policy -> !"1".equals(policy.getPermanentFlag())
				|| (policy.getValidTime() != null && !policy.getValidTime().before(now)))
			.limit(4)
			.map(this::toPolicyNotice)
			.toList();
	}

	private HomePolicyNoticeVO toPolicyNotice(PolicyService policy) {
		HomePolicyNoticeVO notice = new HomePolicyNoticeVO();
		notice.setTitle(policy.getServiceTitle());
		notice.setPublishTime(policy.getCreateTime());
		notice.setLinkUrl("/enterprise/policy-service");
		return notice;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private Long countWorkflowTodos(List<Long> authorizedParkIds) {
		WfProcess process = new WfProcess();
		process.setStatus(WfProcessConstant.STATUS_TODO);
		int current = 1;
		long count = 0L;
		do {
			Query query = new Query().setCurrent(current).setSize(WORKFLOW_PAGE_SIZE);
			var page = wfProcessService.selectTaskPage(process, query);
			if (authorizedParkIds == null) return page.getTotal();
			count += page.getRecords().stream().filter(item -> {
				Long parkId = workflowParkId(item.getVariables());
				return parkId != null && authorizedParkIds.contains(parkId);
			}).count();
			if (page.getRecords().isEmpty() || current >= page.getPages()) break;
			current++;
		} while (true);
		return count;
	}

	private Long workflowParkId(Map<String, Object> variables) {
		if (variables == null) return null;
		for (String key : List.of("parkId", "park_id")) {
			Object value = variables.get(key);
			if (value == null) continue;
			try {
				return Long.valueOf(String.valueOf(value));
			} catch (NumberFormatException ignored) {
				// Try the next conventional variable name.
			}
		}
		return null;
	}

	private Long zeroIfNull(Long value) {
		return value == null ? 0L : value;
	}

	private HomeTodoItemVO toTodoItem(ServiceWorkorder workorder) {
		HomeTodoItemVO item = new HomeTodoItemVO();
		String title = StringUtil.isNotBlank(workorder.getCustomerName()) ? workorder.getCustomerName() : workorder.getOrderNo();
		item.setTitle(StringUtil.isNotBlank(title) ? title : "待处理工单");
		item.setDesc(buildWorkorderDesc(workorder));
		item.setPath("/enterprise/property-workorder?orderNo=" + safeQueryValue(workorder.getOrderNo()));
		item.setIcon("Tools");
		item.setTone(priorityTone(workorder.getPriority()));
		return item;
	}

	private String buildWorkorderDesc(ServiceWorkorder workorder) {
		List<String> parts = new ArrayList<>();
		if (StringUtil.isNotBlank(workorder.getOrderNo())) {
			parts.add("工单号 " + workorder.getOrderNo());
		}
		if (StringUtil.isNotBlank(workorder.getServiceName())) {
			parts.add(workorder.getServiceName());
		}
		if (StringUtil.isNotBlank(workorder.getRoomInfo())) {
			parts.add(workorder.getRoomInfo());
		}
		if (StringUtil.isNotBlank(workorder.getDemandDesc())) {
			parts.add(truncate(workorder.getDemandDesc(), 24));
		}
		return parts.isEmpty() ? "待处理物业工单" : String.join(" · ", parts);
	}

	private String priorityTone(String priority) {
		if ("0".equals(priority)) {
			return "red";
		}
		if ("2".equals(priority)) {
			return "blue";
		}
		return "orange";
	}

	private String safeQueryValue(String value) {
		return StringUtil.isBlank(value) ? "" : value.trim();
	}

	private String truncate(String value, int maxLength) {
		if (StringUtil.isBlank(value) || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, Math.max(0, maxLength - 1)) + "…";
	}

}
