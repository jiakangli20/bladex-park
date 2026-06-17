/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.approval.mapper.ApprovalFlowMapper;
import org.springblade.modules.approval.mapper.ApprovalLogMapper;
import org.springblade.modules.approval.mapper.ApprovalMaterialMapper;
import org.springblade.modules.approval.mapper.ApprovalNodeMapper;
import org.springblade.modules.approval.mapper.ApprovalProjectMapper;
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;
import org.springblade.modules.approval.pojo.entity.ApprovalLog;
import org.springblade.modules.approval.pojo.entity.ApprovalMaterial;
import org.springblade.modules.approval.pojo.entity.ApprovalNode;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;
import org.springblade.modules.approval.service.IApprovalProjectService;
import org.springblade.modules.business.service.ICustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 审批项目服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class ApprovalProjectServiceImpl extends ServiceImpl<ApprovalProjectMapper, ApprovalProject> implements IApprovalProjectService {

	private static final String STATUS_DRAFT = "0";
	private static final String STATUS_PROCESSING = "1";
	private static final String STATUS_APPROVED = "2";
	private static final String STATUS_REJECTED = "3";
	private static final String STATUS_WITHDRAWN = "4";
	private static final String STATUS_ARCHIVED = "5";
	private static final String STATUS_DELETED = "9";
	private static final String NODE_TYPE_SUBMIT = "submit";
	private static final String NODE_TYPE_CC = "cc";
	private static final String NODE_TYPE_APPROVE = "approve";

	private final ApprovalFlowMapper approvalFlowMapper;
	private final ApprovalNodeMapper approvalNodeMapper;
	private final ApprovalMaterialMapper approvalMaterialMapper;
	private final ApprovalLogMapper approvalLogMapper;
	private final ICustomerService customerService;

	@Override
	public ApprovalProject selectApprovalProjectById(Long projectId) {
		return baseMapper.selectApprovalProjectById(projectId);
	}

	@Override
	public List<ApprovalProject> selectApprovalProjectList(ApprovalProject project) {
		return baseMapper.selectApprovalProjectList(normalizeQuery(project));
	}

	@Override
	public IPage<ApprovalProject> selectApprovalProjectPage(IPage<ApprovalProject> page, ApprovalProject project) {
		return baseMapper.selectApprovalProjectPage(page, normalizeQuery(project));
	}

	@Override
	public Kv selectApprovalProjectStatistics(ApprovalProject project) {
		ApprovalProject query = normalizeQuery(project);
		String originalStatus = query.getProcessStatus();
		query.setProcessStatus(null);
		Kv result = Kv.create()
			.set("pending", 0L)
			.set("approved", 0L)
			.set("rejected", 0L)
			.set("archived", 0L)
			.set("total", 0L);
		for (Map<String, Object> row : baseMapper.selectApprovalProjectStatistics(query)) {
			String status = Func.toStr(firstValue(row, "processStatus", "process_status"));
			long total = toLong(firstValue(row, "total", "TOTAL"));
			result.set(statKey(status), total);
			result.set("total", toLong(result.get("total")) + total);
		}
		query.setProcessStatus(originalStatus);
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ApprovalProject insertApprovalProject(ApprovalProject project) {
		if (Func.isEmpty(project)) {
			throw new ServiceException("审批项目不能为空");
		}
		project.setParkId(resolveWriteParkId(project.getParkId()));
		project.setProcessStatus(StringUtil.isBlank(project.getProcessStatus()) ? STATUS_DRAFT : project.getProcessStatus());
		project.setDelFlag(StringUtil.isBlank(project.getDelFlag()) ? "0" : project.getDelFlag());
		project.setCreateBy(currentUserName());
		project.setCreateTime(DateUtil.now());
		if (StringUtil.isBlank(project.getApplicant())) {
			project.setApplicant(currentUserName());
		}
		if (Func.isEmpty(project.getApplicantTime())) {
			project.setApplicantTime(DateUtil.now());
		}
		if (StringUtil.isBlank(project.getProjectName()) && StringUtil.isNotBlank(project.getEnterpriseName())) {
			project.setProjectName(project.getEnterpriseName() + " 审批");
		}
		baseMapper.insertApprovalProject(project);
		return selectApprovalProjectById(project.getProjectId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateApprovalProject(ApprovalProject project) {
		if (Func.isEmpty(project) || Func.isEmpty(project.getProjectId())) {
			throw new ServiceException("审批项目不存在");
		}
		ApprovalProject old = requireAccessibleProject(project.getProjectId());
		project.setParkId(old.getParkId());
		project.setUpdateBy(currentUserName());
		project.setUpdateTime(DateUtil.now());
		return baseMapper.updateApprovalProject(project) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteApprovalProjectByIds(String ids) {
		List<Long> idList = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的审批项目");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteApprovalProjectByIds(idList, parkId, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitProject(Long projectId) {
		ApprovalProject project = requireAccessibleProject(projectId);
		if (STATUS_PROCESSING.equals(project.getProcessStatus())) {
			throw new ServiceException("该审批项目已在审批中");
		}
		List<ApprovalNode> nodes = getFlowNodes(project);
		ApprovalNode firstNode = firstApproveNode(nodes);
		insertLog(project, nodeName(firstNode, "经办人提交"), nodeType(firstNode, NODE_TYPE_SUBMIT), "SUBMIT", "提交审批", currentUserName(), "已提交", null, ccUsers(firstNode));
		if (Func.isEmpty(firstNode)) {
			boolean updated = baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_APPROVED, null, null, "通过", currentUserName()) > 0;
			completeTenantEntryIfApproved(project, updated);
			return updated;
		}
		return baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_PROCESSING, firstNode.getApproverLogin(), firstNode.getNodeName(), null, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean withdrawProject(Long projectId) {
		ApprovalProject project = requireAccessibleProject(projectId);
		if (!AuthUtil.isAdministrator() && !Objects.equals(currentUserName(), project.getApplicant()) && !Objects.equals(currentUserName(), project.getCreateBy())) {
			throw new ServiceException("只有发起人可以撤回审批项目");
		}
		if (!STATUS_DRAFT.equals(project.getProcessStatus()) && !STATUS_PROCESSING.equals(project.getProcessStatus())) {
			throw new ServiceException("当前状态不允许撤回");
		}
		insertLog(project, firstNotBlank(project.getCurrentNodeName(), "撤回审批"), NODE_TYPE_SUBMIT, "WITHDRAW", "撤回审批", currentUserName(), "撤回", null, null);
		boolean updated = baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_WITHDRAWN, null, null, "撤回", currentUserName()) > 0;
		if (updated) {
			customerService.resetTenantEntryApproval(project);
		}
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean approveProject(Long projectId, ApprovalLog log) {
		ApprovalProject project = requireActionableProject(projectId);
		List<ApprovalNode> nodes = getFlowNodes(project);
		ApprovalNode currentNode = currentNode(project, nodes);
		ApprovalNode nextNode = nextApproveNode(nodes, currentNode);
		insertLog(project, nodeName(currentNode, firstNotBlank(project.getCurrentNodeName(), "审批")), nodeType(currentNode, NODE_TYPE_APPROVE), "APPROVE", opinion(log, "同意"), currentUserName(), "通过", null, ccUsers(currentNode));
		if (Func.isEmpty(nextNode)) {
			boolean updated = baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_APPROVED, null, null, "通过", currentUserName()) > 0;
			completeTenantEntryIfApproved(project, updated);
			return updated;
		}
		return baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_PROCESSING, nextNode.getApproverLogin(), nextNode.getNodeName(), null, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean rejectProject(Long projectId, ApprovalLog log) {
		ApprovalProject project = requireActionableProject(projectId);
		List<ApprovalNode> nodes = getFlowNodes(project);
		ApprovalNode currentNode = currentNode(project, nodes);
		insertLog(project, nodeName(currentNode, firstNotBlank(project.getCurrentNodeName(), "审批")), nodeType(currentNode, NODE_TYPE_APPROVE), "REJECT", opinion(log, "驳回"), currentUserName(), "驳回", null, ccUsers(currentNode));
		return baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_REJECTED, null, null, "驳回", currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean transferProject(Long projectId, ApprovalLog log) {
		ApprovalProject project = requireActionableProject(projectId);
		if (Func.isEmpty(log) || StringUtil.isBlank(log.getTransferTo())) {
			throw new ServiceException("请选择转审人");
		}
		List<ApprovalNode> nodes = getFlowNodes(project);
		ApprovalNode currentNode = currentNode(project, nodes);
		insertLog(project, nodeName(currentNode, firstNotBlank(project.getCurrentNodeName(), "审批")), nodeType(currentNode, NODE_TYPE_APPROVE), "TRANSFER", opinion(log, "转审"), currentUserName(), "转审", log.getTransferTo(), ccUsers(currentNode));
		return baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_PROCESSING, log.getTransferTo().trim(), nodeName(currentNode, project.getCurrentNodeName()), null, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean resubmitProject(Long projectId, ApprovalLog log) {
		ApprovalProject project = requireAccessibleProject(projectId);
		if (!STATUS_REJECTED.equals(project.getProcessStatus())) {
			throw new ServiceException("只有驳回状态的审批项目可以重新提交");
		}
		List<ApprovalNode> nodes = getFlowNodes(project);
		ApprovalNode firstNode = firstApproveNode(nodes);
		insertLog(project, "重新提交", NODE_TYPE_SUBMIT, "RESUBMIT", opinion(log, "重新提交审批"), currentUserName(), "重新提交", null, ccUsers(firstNode));
		if (Func.isEmpty(firstNode)) {
			boolean updated = baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_APPROVED, null, null, "通过", currentUserName()) > 0;
			completeTenantEntryIfApproved(project, updated);
			return updated;
		}
		return baseMapper.updateProcessStatus(projectId, project.getParkId(), STATUS_PROCESSING, firstNode.getApproverLogin(), firstNode.getNodeName(), null, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean archiveProject(Long projectId) {
		ApprovalProject project = requireAccessibleProject(projectId);
		if (!STATUS_APPROVED.equals(project.getProcessStatus()) && !STATUS_REJECTED.equals(project.getProcessStatus())) {
			throw new ServiceException("只有已完成审批的项目可以归档");
		}
		insertLog(project, "归档", NODE_TYPE_SUBMIT, "ARCHIVE", "归档审批项目", currentUserName(), "归档", null, null);
		return baseMapper.archiveProject(projectId, project.getParkId(), currentUserName()) > 0;
	}

	@Override
	public Kv generateApprovalForm(Long projectId) {
		ApprovalProject project = requireAccessibleProject(projectId);
		ApprovalMaterial material = new ApprovalMaterial();
		material.setProjectId(projectId);
		material.setParkId(project.getParkId());
		ApprovalLog log = new ApprovalLog();
		log.setProjectId(projectId);
		log.setParkId(project.getParkId());
		ApprovalFlow flow = Func.isEmpty(project.getFlowId()) ? null : approvalFlowMapper.selectApprovalFlowById(project.getFlowId());
		return Kv.create()
			.set("project", project)
			.set("flow", flow)
			.set("flowNodes", getFlowNodes(project))
			.set("materials", approvalMaterialMapper.selectApprovalMaterialList(material))
			.set("logs", approvalLogMapper.selectApprovalLogList(log))
			.set("templateName", templateName(project.getBusinessType()));
	}

	@Override
	public List<ApprovalMaterial> selectApprovalMaterialList(ApprovalMaterial material) {
		ApprovalMaterial query = Func.isEmpty(material) ? new ApprovalMaterial() : material;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		return approvalMaterialMapper.selectApprovalMaterialList(query);
	}

	@Override
	public List<ApprovalLog> selectApprovalLogList(ApprovalLog log) {
		ApprovalLog query = Func.isEmpty(log) ? new ApprovalLog() : log;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		return approvalLogMapper.selectApprovalLogList(query);
	}

	private ApprovalProject normalizeQuery(ApprovalProject project) {
		ApprovalProject query = Func.isEmpty(project) ? new ApprovalProject() : project;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		query.setCurrentUser(currentUserName());
		query.setAdmin(AuthUtil.isAdministrator());
		if (StringUtil.isBlank(query.getScope())) {
			query.setScope("mine");
		}
		return query;
	}

	private ApprovalProject requireAccessibleProject(Long projectId) {
		ApprovalProject project = baseMapper.selectApprovalProjectById(projectId);
		if (Func.isEmpty(project) || STATUS_DELETED.equals(project.getProcessStatus()) || "1".equals(project.getDelFlag())) {
			throw new ServiceException("审批项目不存在");
		}
		if (!AuthUtil.isAdministrator() && !Objects.equals(currentParkId(), project.getParkId())) {
			throw new ServiceException("无权访问该审批项目");
		}
		return project;
	}

	private ApprovalProject requireActionableProject(Long projectId) {
		ApprovalProject project = requireAccessibleProject(projectId);
		if (!STATUS_PROCESSING.equals(project.getProcessStatus())) {
			throw new ServiceException("当前审批项目不在审批中");
		}
		if (!AuthUtil.isAdministrator() && !hasLogin(project.getCurrentNode(), currentUserName()) && !hasLogin(project.getCurrentNode(), AuthUtil.getNickName())) {
			throw new ServiceException("当前用户不是该节点审批人");
		}
		return project;
	}

	private List<ApprovalNode> getFlowNodes(ApprovalProject project) {
		if (Func.isEmpty(project) || Func.isEmpty(project.getFlowId())) {
			return Collections.emptyList();
		}
		List<ApprovalNode> nodes = approvalNodeMapper.selectNodesByFlowId(project.getFlowId());
		if (Func.isEmpty(nodes)) {
			nodes = parseFlowNodes(project.getFlowId(), project.getParkId());
		}
		nodes.sort(Comparator.comparing(node -> Func.toInt(node.getNodeOrder(), 0)));
		return nodes;
	}

	private List<ApprovalNode> parseFlowNodes(Long flowId, Long parkId) {
		ApprovalFlow flow = approvalFlowMapper.selectApprovalFlowById(flowId);
		if (Func.isEmpty(flow) || StringUtil.isBlank(flow.getNodeConfig())) {
			return new ArrayList<>();
		}
		try {
			List<ApprovalNode> nodes = new ArrayList<>();
			String content = flow.getNodeConfig().trim();
			if (content.startsWith("[")) {
				List<Map<String, Object>> list = JsonUtil.getInstance().readValue(content, new TypeReference<List<Map<String, Object>>>() {
				});
				for (Map<String, Object> item : list) {
					ApprovalNode node = new ApprovalNode();
					node.setFlowId(flowId);
					node.setParkId(parkId);
					node.setNodeOrder(nodes.size() + 1);
					node.setNodeName(Func.toStr(item.get("nodeName")));
					node.setNodeType(Func.toStr(item.get("nodeType"), NODE_TYPE_APPROVE));
					node.setApproverLogin(firstNotBlank(Func.toStr(item.get("approverLogin")), Func.toStr(item.get("approverLoginName"))));
					node.setApproverName(Func.toStr(item.get("approverName")));
					node.setCompleteCondition(Func.toStr(item.get("completeCondition"), "all"));
					node.setTimeLimit(toInteger(item.get("timeLimit")));
					node.setCcUsers(Func.toStr(item.get("ccUsers")));
					nodes.add(node);
				}
				return nodes;
			}
			for (String name : content.split(",")) {
				if (StringUtil.isBlank(name)) {
					continue;
				}
				ApprovalNode node = new ApprovalNode();
				node.setFlowId(flowId);
				node.setParkId(parkId);
				node.setNodeOrder(nodes.size() + 1);
				node.setNodeName(name.trim());
				node.setNodeType(NODE_TYPE_APPROVE);
				nodes.add(node);
			}
			return nodes;
		} catch (Exception ignored) {
			return new ArrayList<>();
		}
	}

	private ApprovalNode firstApproveNode(List<ApprovalNode> nodes) {
		for (ApprovalNode node : nodes) {
			if (isApproveNode(node) && StringUtil.isNotBlank(node.getApproverLogin())) {
				return node;
			}
		}
		return null;
	}

	private ApprovalNode currentNode(ApprovalProject project, List<ApprovalNode> nodes) {
		for (ApprovalNode node : nodes) {
			if (Objects.equals(node.getNodeName(), project.getCurrentNodeName()) || hasLogin(node.getApproverLogin(), project.getCurrentNode())) {
				return node;
			}
		}
		return null;
	}

	private ApprovalNode nextApproveNode(List<ApprovalNode> nodes, ApprovalNode currentNode) {
		int start = currentNode == null ? 0 : nodes.indexOf(currentNode) + 1;
		for (int i = Math.max(0, start); i < nodes.size(); i++) {
			ApprovalNode node = nodes.get(i);
			if (isApproveNode(node) && StringUtil.isNotBlank(node.getApproverLogin())) {
				return node;
			}
		}
		return null;
	}

	private boolean isApproveNode(ApprovalNode node) {
		String type = nodeType(node, NODE_TYPE_APPROVE);
		return !NODE_TYPE_SUBMIT.equals(type) && !NODE_TYPE_CC.equals(type);
	}

	private void insertLog(ApprovalProject project, String nodeName, String nodeType, String actionType, String opinion,
						   String operatorName, String resultStatus, String transferTo, String ccUsers) {
		ApprovalLog log = new ApprovalLog();
		log.setParkId(project.getParkId());
		log.setProjectId(project.getProjectId());
		log.setFlowId(project.getFlowId());
		log.setNodeName(nodeName);
		log.setNodeType(nodeType);
		log.setActionType(actionType);
		log.setOperatorName(operatorName);
		log.setOpinion(opinion);
		log.setResultStatus(resultStatus);
		log.setTransferTo(transferTo);
		log.setCcUsers(ccUsers);
		log.setOperateTime(new Date());
		log.setCreateBy(operatorName);
		log.setCreateTime(DateUtil.now());
		approvalLogMapper.insertApprovalLog(log);
	}

	private void completeTenantEntryIfApproved(ApprovalProject project, boolean updated) {
		if (updated) {
			customerService.completeTenantEntryApproval(project);
		}
	}

	private Long resolveWriteParkId(Long parkId) {
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(parkId) && parkId > 0) {
			return parkId;
		}
		return currentParkId();
	}

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private boolean hasLogin(String value, String loginName) {
		if (StringUtil.isBlank(value) || StringUtil.isBlank(loginName)) {
			return false;
		}
		for (String item : value.split(",")) {
			if (loginName.equals(item.trim())) {
				return true;
			}
		}
		return false;
	}

	private String firstNotBlank(String first, String second) {
		return StringUtil.isNotBlank(first) ? first : second;
	}

	private String opinion(ApprovalLog log, String defaultOpinion) {
		return Func.isEmpty(log) || StringUtil.isBlank(log.getOpinion()) ? defaultOpinion : log.getOpinion();
	}

	private String nodeName(ApprovalNode node, String defaultName) {
		return Func.isEmpty(node) || StringUtil.isBlank(node.getNodeName()) ? defaultName : node.getNodeName();
	}

	private String nodeType(ApprovalNode node, String defaultType) {
		return Func.isEmpty(node) || StringUtil.isBlank(node.getNodeType()) ? defaultType : node.getNodeType();
	}

	private String ccUsers(ApprovalNode node) {
		return Func.isEmpty(node) ? null : node.getCcUsers();
	}

	private String templateName(String businessType) {
		return switch (Func.toStr(businessType)) {
			case "tenant_entry" -> "企业入驻审批表";
			case "contract_renewal" -> "合同续签审批表";
			case "termination" -> "退租审批表";
			default -> "项目审批表";
		};
	}

	private String statKey(String status) {
		return switch (Func.toStr(status)) {
			case STATUS_PROCESSING -> "pending";
			case STATUS_APPROVED -> "approved";
			case STATUS_REJECTED -> "rejected";
			case STATUS_ARCHIVED -> "archived";
			default -> "other";
		};
	}

	private Object firstValue(Map<String, Object> row, String first, String second) {
		Object value = row.get(first);
		return value == null ? row.get(second) : value;
	}

	private long toLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Func.toLong(value, 0L);
	}

	private Integer toInteger(Object value) {
		return value == null ? null : Func.toInt(value);
	}

}
