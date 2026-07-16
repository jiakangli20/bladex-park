/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.task.Comment;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricVariableUpdate;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springblade.common.cache.SysCache;
import org.springblade.common.cache.UserCache;
import org.springblade.core.launch.constant.FlowConstant;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 工作流审批轨迹回流服务.
 *
 * @author BladeX
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApprovalTraceService {

	private static final String EMPTY = "-";
	private static final String LATEST_TASK_ASSIGNEE = "wf_latest_task_assignee";
	private static final long ASSIGNEE_TIME_TOLERANCE_MILLIS = 3000L;

	private static final List<ApprovalSlot> APPROVAL_SLOTS = List.of(
		new ApprovalSlot("部门经理", List.of("部门经理", "部门审批", "经理审批", "部门负责人"), List.of("部门经理", "经理", "部门负责人"), List.of("部门经理", "经理")),
		new ApprovalSlot("部门审批", List.of("部门审批", "部门经理", "经理审批"), List.of("部门经理", "经理", "部门负责人"), List.of("部门经理", "经理")),
		new ApprovalSlot("经理审批", List.of("经理审批", "部门经理", "部门审批"), List.of("部门经理", "经理", "部门负责人"), List.of("部门经理", "经理")),
		new ApprovalSlot("风控审核", List.of("风控", "风险", "风控审核"), List.of("风控", "风险"), List.of("风控", "风险")),
		new ApprovalSlot("律师意见", List.of("律师", "法务", "法律"), List.of("律师", "法务", "法律"), List.of("律师", "法务")),
		new ApprovalSlot("综合管理部", List.of("综合管理", "综合管理部", "行政"), List.of("综合管理", "综合管理部", "行政"), List.of("综合管理", "行政")),
		new ApprovalSlot("分管领导", List.of("分管领导", "主管领导", "领导审批"), List.of("分管领导", "主管领导"), List.of("分管领导", "主管领导")),
		new ApprovalSlot("分管领导审批", List.of("分管领导", "主管领导", "领导审批"), List.of("分管领导", "主管领导"), List.of("分管领导", "主管领导")),
		new ApprovalSlot("总经理审批", List.of("总经理", "老板", "总经理审批"), List.of("总经理", "老板"), List.of("总经理", "老板")),
		new ApprovalSlot("总经理", List.of("总经理", "老板", "总经理审批"), List.of("总经理", "老板"), List.of("总经理", "老板"))
	);

	private final HistoryService historyService;
	private final TaskService taskService;
	private final IUserService userService;
	private final IRoleService roleService;

	public Map<String, String> approvalFields(String processInsId) {
		return approvalFields(processInsId, "审批完成");
	}

	public Map<String, String> approvalFields(String processInsId, String unreadableCommentFallback) {
		if (StringUtil.isBlank(processInsId)) {
			return Collections.emptyMap();
		}
		List<ApprovalLine> lines = approvalLines(processInsId);
		if (lines.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> fields = new LinkedHashMap<>();
		for (ApprovalSlot slot : APPROVAL_SLOTS) {
			ApprovalLine line = matchLine(slot, lines);
			if (line != null) {
				fields.put(slot.fieldName(), formatApprovalCell(line, unreadableCommentFallback));
			}
		}
		String timeline = formatTimeline(lines, unreadableCommentFallback);
		ApprovalLine firstLine = lines.get(0);
		ApprovalLine lastLine = lines.get(lines.size() - 1);
		fields.put("审批流转信息", timeline);
		fields.put("流转信息", timeline);
		fields.put("审批记录", timeline);
		fields.put("审批意见汇总", timeline);
		fields.put("通用审批意见", timeline);
		fields.put("流程发起人", formatApprovalCell(firstLine, unreadableCommentFallback));
		fields.put("首个审批记录", formatApprovalCell(firstLine, unreadableCommentFallback));
		fields.put("最后审批记录", formatApprovalCell(lastLine, unreadableCommentFallback));
		return fields;
	}

	private List<ApprovalLine> approvalLines(String processInsId) {
		try {
			List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
				.processInstanceId(processInsId)
				.finished()
				.orderByHistoricTaskInstanceEndTime()
				.asc()
				.list();
			if (tasks == null || tasks.isEmpty()) {
				return Collections.emptyList();
			}
			List<AssigneeUpdate> assigneeUpdates = latestAssigneeUpdates(processInsId);
			String starter = processStarter(processInsId);
			List<ApprovalLine> lines = new ArrayList<>();
			for (HistoricTaskInstance task : tasks) {
				ApprovalLine line = toApprovalLine(task, assigneeUpdates, starter);
				if (StringUtil.isNotBlank(line.approverName()) || StringUtil.isNotBlank(line.comment())) {
					lines.add(line);
				}
			}
			return lines;
		} catch (Exception exception) {
			log.warn("读取流程审批轨迹失败，processInsId={}", processInsId, exception);
			return Collections.emptyList();
		}
	}

	private List<AssigneeUpdate> latestAssigneeUpdates(String processInsId) {
		try {
			List<HistoricDetail> details = historyService.createHistoricDetailQuery()
				.processInstanceId(processInsId)
				.variableUpdates()
				.orderByTime()
				.asc()
				.list();
			if (details == null || details.isEmpty()) {
				return Collections.emptyList();
			}
			List<AssigneeUpdate> updates = new ArrayList<>();
			for (HistoricDetail detail : details) {
				if (detail instanceof HistoricVariableUpdate variableUpdate
					&& LATEST_TASK_ASSIGNEE.equals(variableUpdate.getVariableName())
					&& variableUpdate.getValue() != null) {
					String assignee = Func.toStr(variableUpdate.getValue(), "");
					if (StringUtil.isNotBlank(assignee)) {
						updates.add(new AssigneeUpdate(detail.getTaskId(), detail.getTime(), assignee));
					}
				}
			}
			return updates;
		} catch (Exception exception) {
			log.debug("读取流程最近审批人历史变量失败，processInsId={}", processInsId, exception);
			return Collections.emptyList();
		}
	}

	private String processStarter(String processInsId) {
		try {
			HistoricProcessInstance process = historyService.createHistoricProcessInstanceQuery()
				.processInstanceId(processInsId)
				.singleResult();
			return process == null ? null : process.getStartUserId();
		} catch (Exception ignored) {
			return null;
		}
	}

	private ApprovalLine toApprovalLine(HistoricTaskInstance task, List<AssigneeUpdate> assigneeUpdates, String starter) {
		CommentInfo comment = latestComment(task.getId());
		String assignee = firstNotBlank(
			resolveLatestAssignee(task, assigneeUpdates),
			task.getCompletedBy(),
			comment.userId(),
			task.getAssignee(),
			starter
		);
		User user = resolveUser(assignee, task.getTenantId());
		List<String> roleNames = roleNames(user);
		List<String> roleAliases = roleAliases(user);
		List<String> deptNames = deptNames(user);
		String approverName = firstNotBlank(
			user == null ? null : user.getRealName(),
			user == null ? null : user.getName(),
			assignee
		);
		return new ApprovalLine(
			Func.toStr(task.getTaskDefinitionKey(), ""),
			Func.toStr(task.getName(), ""),
			assignee,
			approverName,
			roleNames,
			roleAliases,
			deptNames,
			task.getEndTime(),
			comment.message()
		);
	}

	private String resolveLatestAssignee(HistoricTaskInstance task, List<AssigneeUpdate> updates) {
		if (task == null || updates == null || updates.isEmpty()) {
			return null;
		}
		AssigneeUpdate matched = latestMatchedUpdate(updates, update -> Objects.equals(task.getId(), update.taskId()));
		if (matched != null) {
			return matched.assignee();
		}
		matched = latestMatchedUpdate(updates, update -> withinTaskTime(task, update.time()));
		return matched == null ? null : matched.assignee();
	}

	private AssigneeUpdate latestMatchedUpdate(List<AssigneeUpdate> updates, AssigneeUpdateMatcher matcher) {
		AssigneeUpdate matched = null;
		for (AssigneeUpdate update : updates) {
			if (matcher.matches(update) && (matched == null || compareTime(update.time(), matched.time()) >= 0)) {
				matched = update;
			}
		}
		return matched;
	}

	private boolean withinTaskTime(HistoricTaskInstance task, Date updateTime) {
		if (updateTime == null || task.getEndTime() == null) {
			return false;
		}
		long update = updateTime.getTime();
		long start = task.getStartTime() == null ? Long.MIN_VALUE : task.getStartTime().getTime() - ASSIGNEE_TIME_TOLERANCE_MILLIS;
		long end = task.getEndTime().getTime() + ASSIGNEE_TIME_TOLERANCE_MILLIS;
		return update >= start && update <= end;
	}

	private int compareTime(Date first, Date second) {
		if (first == null && second == null) {
			return 0;
		}
		if (first == null) {
			return -1;
		}
		if (second == null) {
			return 1;
		}
		return first.compareTo(second);
	}

	private User resolveUser(String assignee, String tenantId) {
		if (StringUtil.isBlank(assignee)) {
			return null;
		}
		Long userId = parseUserId(assignee);
		if (userId != null) {
			User user = UserCache.getUser(userId);
			return user == null ? userService.getById(userId) : user;
		}
		if (StringUtil.isNotBlank(tenantId)) {
			return userService.userByAccount(tenantId, assignee);
		}
		return null;
	}

	private Long parseUserId(String assignee) {
		String normalized = Func.toStr(assignee, "").trim();
		if (StringUtil.isBlank(normalized)) {
			return null;
		}
		normalized = StringUtil.removePrefix(normalized, FlowConstant.TASK_USR_PREFIX);
		normalized = normalized.replace("taskUser:", "").replace("taskUser_", "");
		Long userId = Func.toLong(normalized);
		return userId == null || userId <= 0 ? null : userId;
	}

	private List<String> roleNames(User user) {
		if (user == null || StringUtil.isBlank(user.getRoleId())) {
			return Collections.emptyList();
		}
		try {
			return roleService.getRoleNames(user.getRoleId());
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	private List<String> roleAliases(User user) {
		if (user == null || StringUtil.isBlank(user.getRoleId())) {
			return Collections.emptyList();
		}
		try {
			return roleService.getRoleAliases(user.getRoleId());
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	private List<String> deptNames(User user) {
		if (user == null || StringUtil.isBlank(user.getDeptId())) {
			return Collections.emptyList();
		}
		try {
			return SysCache.getDeptNames(user.getDeptId());
		} catch (Exception ignored) {
			return Collections.emptyList();
		}
	}

	private CommentInfo latestComment(String taskId) {
		if (StringUtil.isBlank(taskId)) {
			return CommentInfo.EMPTY;
		}
		try {
			List<Comment> comments = taskService.getTaskComments(taskId);
			if (comments == null || comments.isEmpty()) {
				return CommentInfo.EMPTY;
			}
			return comments.stream()
				.max(Comparator.comparing(Comment::getTime, Comparator.nullsLast(Date::compareTo)))
				.map(comment -> new CommentInfo(
					Func.toStr(comment.getUserId(), ""),
					Func.toStr(comment.getFullMessage(), "")
				))
				.orElse(CommentInfo.EMPTY);
		} catch (Exception ignored) {
			return CommentInfo.EMPTY;
		}
	}

	private ApprovalLine matchLine(ApprovalSlot slot, List<ApprovalLine> lines) {
		ApprovalLine byNode = findByKeywords(lines, slot.nodeKeywords(), line -> List.of(line.nodeName(), line.nodeKey()));
		if (byNode != null) {
			return byNode;
		}
		ApprovalLine byRole = findByKeywords(lines, slot.roleKeywords(), line -> merge(line.roleNames(), line.roleAliases(), line.deptNames()));
		if (byRole != null) {
			return byRole;
		}
		return findByKeywords(lines, slot.userKeywords(), line -> List.of(line.approverName(), line.assignee()));
	}

	private ApprovalLine findByKeywords(List<ApprovalLine> lines, List<String> keywords, KeywordSource source) {
		if (lines == null || lines.isEmpty() || keywords == null || keywords.isEmpty()) {
			return null;
		}
		for (ApprovalLine line : lines) {
			Collection<String> values = source.values(line);
			if (containsAny(values, keywords)) {
				return line;
			}
		}
		return null;
	}

	private boolean containsAny(Collection<String> values, List<String> keywords) {
		if (values == null || values.isEmpty()) {
			return false;
		}
		for (String value : values) {
			String normalizedValue = normalize(value);
			if (StringUtil.isBlank(normalizedValue)) {
				continue;
			}
			for (String keyword : keywords) {
				String normalizedKeyword = normalize(keyword);
				if (StringUtil.isNotBlank(normalizedKeyword)
					&& normalizedValue.contains(normalizedKeyword)
					&& !isGenericManagerKeywordMatchedTotalManager(normalizedValue, normalizedKeyword)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isGenericManagerKeywordMatchedTotalManager(String normalizedValue, String normalizedKeyword) {
		return normalizedValue.contains("总经理")
			&& ("经理".equals(normalizedKeyword) || "经理审批".equals(normalizedKeyword) || "部门经理".equals(normalizedKeyword));
	}

	@SafeVarargs
	private final List<String> merge(List<String>... groups) {
		List<String> values = new ArrayList<>();
		if (groups == null) {
			return values;
		}
		for (List<String> group : groups) {
			if (group != null) {
				values.addAll(group);
			}
		}
		return values;
	}

	private String formatApprovalCell(ApprovalLine line, String unreadableCommentFallback) {
		String date = line.endTime() == null ? EMPTY : DateUtil.format(line.endTime(), DateUtil.PATTERN_DATE);
		String comment = approvalComment(line.comment(), unreadableCommentFallback);
		return firstNotBlank(line.approverName(), EMPTY) + "（" + comment + "，" + date + "）";
	}

	private String formatTimeline(List<ApprovalLine> lines, String unreadableCommentFallback) {
		List<String> rows = new ArrayList<>();
		for (ApprovalLine line : lines) {
			String time = line.endTime() == null ? EMPTY : DateUtil.format(line.endTime(), DateUtil.PATTERN_DATETIME);
			String node = firstNotBlank(line.nodeName(), line.nodeKey(), "审批");
			String approver = firstNotBlank(line.approverName(), EMPTY);
			String comment = approvalComment(line.comment(), unreadableCommentFallback);
			rows.add(time + " " + node + " " + approver + "：" + comment);
		}
		return String.join("\n", rows);
	}

	String approvalComment(String comment, String unreadableCommentFallback) {
		String normalized = Func.toStr(comment, "").trim();
		if (StringUtil.isBlank(normalized) || normalized.matches("[?？�\\s]+")) {
			return firstNotBlank(unreadableCommentFallback, "审批完成");
		}
		return normalized;
	}

	private String firstNotBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return null;
	}

	private String normalize(String value) {
		return Func.toStr(value, "")
			.replace("：", "")
			.replace(":", "")
			.replace(" ", "")
			.trim()
			.toLowerCase();
	}

	private record ApprovalSlot(String fieldName, List<String> nodeKeywords, List<String> roleKeywords, List<String> userKeywords) {
	}

	private record ApprovalLine(String nodeKey, String nodeName, String assignee, String approverName, List<String> roleNames,
								List<String> roleAliases, List<String> deptNames, Date endTime, String comment) {
	}

	private record AssigneeUpdate(String taskId, Date time, String assignee) {
	}

	private record CommentInfo(String userId, String message) {
		private static final CommentInfo EMPTY = new CommentInfo("", "");
	}

	@FunctionalInterface
	private interface KeywordSource {
		Collection<String> values(ApprovalLine line);
	}

	@FunctionalInterface
	private interface AssigneeUpdateMatcher {
		boolean matches(AssigneeUpdate update);
	}

}
