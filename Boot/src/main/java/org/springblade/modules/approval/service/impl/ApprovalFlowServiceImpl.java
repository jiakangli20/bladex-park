/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.approval.mapper.ApprovalFlowMapper;
import org.springblade.modules.approval.mapper.ApprovalNodeMapper;
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;
import org.springblade.modules.approval.pojo.entity.ApprovalNode;
import org.springblade.modules.approval.service.IApprovalFlowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批流程配置服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl extends ServiceImpl<ApprovalFlowMapper, ApprovalFlow> implements IApprovalFlowService {

	private static final String STATUS_DRAFT = "0";
	private static final String STATUS_PUBLISHED = "1";
	private static final String STATUS_DISABLED = "9";
	private static final String NODE_TYPE_APPROVE = "approve";
	private static final String NODE_TYPE_CC = "cc";
	private static final String NODE_TYPE_SUBMIT = "submit";
	private static final String DEFAULT_COMPLETE_CONDITION = "all";

	private final ApprovalNodeMapper approvalNodeMapper;

	@Override
	public ApprovalFlow selectApprovalFlowById(Long flowId) {
		ApprovalFlow flow = baseMapper.selectApprovalFlowById(flowId);
		if (Func.isNotEmpty(flow)) {
			flow.setNodes(loadNodesWithFallback(flow));
		}
		return flow;
	}

	@Override
	public List<ApprovalFlow> selectApprovalFlowList(ApprovalFlow flow) {
		return fillNodes(baseMapper.selectApprovalFlowList(normalizeQuery(flow)));
	}

	@Override
	public IPage<ApprovalFlow> selectApprovalFlowPage(IPage<ApprovalFlow> page, ApprovalFlow flow) {
		IPage<ApprovalFlow> result = baseMapper.selectApprovalFlowPage(page, normalizeQuery(flow));
		result.setRecords(fillNodes(result.getRecords()));
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ApprovalFlow insertApprovalFlow(ApprovalFlow flow) {
		validateFlow(flow);
		flow.setParkId(resolveWriteParkId(flow.getParkId()));
		flow.setCreateBy(currentUserName());
		flow.setCreateTime(DateUtil.now());
		if (Func.isEmpty(flow.getFlowVersion())) {
			flow.setFlowVersion(1);
		}
		flow.setStatus(STATUS_DRAFT);
		normalizeFlowNodes(flow);
		baseMapper.insertApprovalFlow(flow);
		if (Func.isNotEmpty(flow.getNodes())) {
			saveFlowNodes(flow.getFlowId(), flow.getNodes());
		}
		return selectApprovalFlowById(flow.getFlowId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateApprovalFlow(ApprovalFlow flow) {
		if (Func.isEmpty(flow.getFlowId())) {
			throw new ServiceException("审批流程不存在");
		}
		ApprovalFlow old = requireWritableFlow(flow.getFlowId());
		validateFlow(flow);
		flow.setParkId(old.getParkId());
		flow.setUpdateBy(currentUserName());
		flow.setUpdateTime(DateUtil.now());
		normalizeFlowNodes(flow);
		int rows = baseMapper.updateApprovalFlow(flow);
		if (flow.getNodes() != null) {
			saveFlowNodes(flow.getFlowId(), flow.getNodes());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitApprovalFlow(ApprovalFlow flow) {
		return Func.isEmpty(flow.getFlowId()) ? Func.isNotEmpty(insertApprovalFlow(flow)) : updateApprovalFlow(flow);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteApprovalFlowByIds(String ids) {
		List<Long> idList = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的审批流程");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteApprovalFlowByIds(idList, parkId, currentUserName()) > 0;
	}

	@Override
	public List<ApprovalNode> getStructuredNodes(Long flowId) {
		ApprovalFlow flow = requireReadableFlow(flowId);
		return loadNodesWithFallback(flow);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveFlowNodes(Long flowId, List<ApprovalNode> nodes) {
		ApprovalFlow flow = requireWritableFlow(flowId);
		List<ApprovalNode> normalized = normalizeNodes(flowId, flow.getParkId(), nodes);
		approvalNodeMapper.deleteNodesByFlowId(flowId);
		for (ApprovalNode node : normalized) {
			approvalNodeMapper.insertNode(node);
		}
		ApprovalFlow patch = new ApprovalFlow();
		patch.setFlowId(flowId);
		patch.setNodeConfig(toNodeConfig(normalized));
		patch.setUpdateBy(currentUserName());
		patch.setUpdateTime(DateUtil.now());
		return baseMapper.updateApprovalFlow(patch) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean publishFlow(Long flowId) {
		ApprovalFlow flow = requireWritableFlow(flowId);
		List<ApprovalNode> nodes = loadNodesWithFallback(flow);
		validatePublishableNodes(nodes);
		ApprovalFlow patch = new ApprovalFlow();
		patch.setFlowId(flowId);
		patch.setStatus(STATUS_PUBLISHED);
		patch.setFlowVersion((Func.isEmpty(flow.getFlowVersion()) ? 0 : flow.getFlowVersion()) + 1);
		patch.setNodeConfig(toNodeConfig(normalizeNodes(flowId, flow.getParkId(), nodes)));
		patch.setUpdateBy(currentUserName());
		patch.setUpdateTime(DateUtil.now());
		return baseMapper.updateApprovalFlow(patch) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public ApprovalFlow copyFlow(Long flowId) {
		ApprovalFlow source = requireReadableFlow(flowId);
		Long targetParkId = AuthUtil.isAdministrator() ? Func.toLong(source.getParkId(), currentParkId()) : currentParkId();
		ApprovalFlow copy = new ApprovalFlow();
		copy.setParkId(targetParkId);
		copy.setFlowName(source.getFlowName() + "（副本）");
		copy.setBusinessType(source.getBusinessType());
		copy.setFlowVersion(1);
		copy.setStatus(STATUS_DRAFT);
		copy.setRemark(source.getRemark());
		copy.setCreateBy(currentUserName());
		copy.setCreateTime(DateUtil.now());
		baseMapper.insertApprovalFlow(copy);
		saveFlowNodes(copy.getFlowId(), loadNodesWithFallback(source));
		return selectApprovalFlowById(copy.getFlowId());
	}

	private ApprovalFlow normalizeQuery(ApprovalFlow flow) {
		ApprovalFlow query = Func.isEmpty(flow) ? new ApprovalFlow() : flow;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		if (Boolean.TRUE.equals(query.getIncludeGlobal()) && Func.isEmpty(query.getParkId())) {
			query.setParkId(currentParkId());
		}
		return query;
	}

	private List<ApprovalFlow> fillNodes(List<ApprovalFlow> flows) {
		if (Func.isEmpty(flows)) {
			return flows;
		}
		for (ApprovalFlow flow : flows) {
			flow.setNodes(loadNodesWithFallback(flow));
		}
		return flows;
	}

	private ApprovalFlow requireReadableFlow(Long flowId) {
		ApprovalFlow flow = baseMapper.selectApprovalFlowById(flowId);
		if (Func.isEmpty(flow) || STATUS_DISABLED.equals(flow.getStatus())) {
			throw new ServiceException("审批流程不存在");
		}
		if (!AuthUtil.isAdministrator() && !currentParkId().equals(flow.getParkId())) {
			throw new ServiceException("无权访问该审批流程");
		}
		return flow;
	}

	private ApprovalFlow requireWritableFlow(Long flowId) {
		ApprovalFlow flow = requireReadableFlow(flowId);
		if (!AuthUtil.isAdministrator() && !currentParkId().equals(flow.getParkId())) {
			throw new ServiceException("无权操作该审批流程");
		}
		return flow;
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

	private void validateFlow(ApprovalFlow flow) {
		if (Func.isEmpty(flow) || StringUtil.isBlank(flow.getFlowName())) {
			throw new ServiceException("流程名称不能为空");
		}
		if (StringUtil.isBlank(flow.getBusinessType())) {
			throw new ServiceException("业务类型不能为空");
		}
	}

	private void normalizeFlowNodes(ApprovalFlow flow) {
		if (flow.getNodes() != null) {
			flow.setNodeConfig(toNodeConfig(normalizeNodes(flow.getFlowId(), flow.getParkId(), flow.getNodes())));
		}
	}

	private List<ApprovalNode> loadNodesWithFallback(ApprovalFlow flow) {
		if (Func.isEmpty(flow) || Func.isEmpty(flow.getFlowId())) {
			return new ArrayList<>();
		}
		List<ApprovalNode> nodes = approvalNodeMapper.selectNodesByFlowId(flow.getFlowId());
		if (Func.isNotEmpty(nodes)) {
			return nodes;
		}
		return normalizeNodes(flow.getFlowId(), flow.getParkId(), parseNodeConfig(flow.getNodeConfig()));
	}

	private List<ApprovalNode> normalizeNodes(Long flowId, Long parkId, List<ApprovalNode> nodes) {
		List<ApprovalNode> normalized = new ArrayList<>();
		if (Func.isEmpty(nodes)) {
			return normalized;
		}
		for (ApprovalNode source : nodes) {
			if (source == null || StringUtil.isBlank(source.getNodeName())) {
				continue;
			}
			ApprovalNode node = new ApprovalNode();
			node.setFlowId(flowId);
			node.setParkId(parkId);
			node.setNodeName(source.getNodeName().trim());
			node.setNodeOrder(normalized.size() + 1);
			node.setNodeType(StringUtil.isBlank(source.getNodeType()) ? NODE_TYPE_APPROVE : source.getNodeType());
			node.setApproverLogin(trimToNull(source.getApproverLogin()));
			node.setApproverName(trimToNull(source.getApproverName()));
			node.setCompleteCondition(StringUtil.isBlank(source.getCompleteCondition()) ? DEFAULT_COMPLETE_CONDITION : source.getCompleteCondition());
			node.setTimeLimit(source.getTimeLimit());
			node.setCcUsers(trimToNull(source.getCcUsers()));
			normalized.add(node);
		}
		return normalized;
	}

	private void validatePublishableNodes(List<ApprovalNode> nodes) {
		if (Func.isEmpty(nodes)) {
			throw new ServiceException("请先配置审批节点后再发布流程");
		}
		boolean hasApproverNode = false;
		for (ApprovalNode node : nodes) {
			if (StringUtil.isBlank(node.getNodeName())) {
				throw new ServiceException("审批节点名称不能为空");
			}
			String nodeType = StringUtil.isBlank(node.getNodeType()) ? NODE_TYPE_APPROVE : node.getNodeType();
			if (NODE_TYPE_CC.equals(nodeType) || NODE_TYPE_SUBMIT.equals(nodeType)) {
				continue;
			}
			if (StringUtil.isBlank(node.getApproverLogin())) {
				throw new ServiceException("审批节点「" + node.getNodeName() + "」未配置审批人");
			}
			hasApproverNode = true;
		}
		if (!hasApproverNode) {
			throw new ServiceException("请至少配置一个审批节点后再发布流程");
		}
	}

	private String toNodeConfig(List<ApprovalNode> nodes) {
		List<Map<String, Object>> jsonNodes = new ArrayList<>();
		for (ApprovalNode node : nodes) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("nodeName", node.getNodeName());
			item.put("approverLogin", Func.toStr(node.getApproverLogin()));
			item.put("approverLoginName", Func.toStr(node.getApproverLogin()));
			item.put("approverName", Func.toStr(node.getApproverName()));
			item.put("nodeType", StringUtil.isBlank(node.getNodeType()) ? NODE_TYPE_APPROVE : node.getNodeType());
			item.put("completeCondition", StringUtil.isBlank(node.getCompleteCondition()) ? DEFAULT_COMPLETE_CONDITION : node.getCompleteCondition());
			item.put("timeLimit", node.getTimeLimit());
			item.put("ccUsers", Func.toStr(node.getCcUsers()));
			jsonNodes.add(item);
		}
		return JsonUtil.toJson(jsonNodes);
	}

	@SneakyThrows
	private List<ApprovalNode> parseNodeConfig(String nodeConfig) {
		List<ApprovalNode> nodes = new ArrayList<>();
		if (StringUtil.isBlank(nodeConfig)) {
			return nodes;
		}
		String content = nodeConfig.trim();
		if (content.startsWith("[")) {
			List<Map<String, Object>> list = JsonUtil.getInstance().readValue(content, new TypeReference<List<Map<String, Object>>>() {
			});
			for (Map<String, Object> item : list) {
				ApprovalNode node = new ApprovalNode();
				node.setNodeName(Func.toStr(item.get("nodeName")));
				node.setNodeType(Func.toStr(item.get("nodeType"), NODE_TYPE_APPROVE));
				node.setApproverLogin(firstNotBlank(Func.toStr(item.get("approverLogin")), Func.toStr(item.get("approverLoginName"))));
				node.setApproverName(Func.toStr(item.get("approverName")));
				node.setCompleteCondition(Func.toStr(item.get("completeCondition"), DEFAULT_COMPLETE_CONDITION));
				node.setTimeLimit(parseInteger(item.get("timeLimit")));
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
			node.setNodeName(name.trim());
			node.setNodeType(NODE_TYPE_APPROVE);
			node.setCompleteCondition(DEFAULT_COMPLETE_CONDITION);
			nodes.add(node);
		}
		return nodes;
	}

	private String firstNotBlank(String first, String second) {
		return StringUtil.isNotBlank(first) ? first : second;
	}

	private String trimToNull(String value) {
		return value == null ? null : value.trim();
	}

	private Integer parseInteger(Object value) {
		if (value == null || StringUtil.isBlank(Func.toStr(value))) {
			return null;
		}
		return Func.toInt(value);
	}

}
