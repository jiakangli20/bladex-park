/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.handler.IPermissionHandler;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.BusinessOpportunityMapper;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.business.service.ITenantEntryWorkflowService;
import org.springblade.modules.approval.service.impl.WorkflowApprovalTraceService;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.core.user.WfUserService;
import org.springblade.plugin.workflow.process.dto.WfCopyDTO;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;
import org.springblade.plugin.workflow.process.entity.WfNotice;
import org.springblade.plugin.workflow.process.service.IWfCopyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springblade.plugin.workflow.process.entity.WfNotice.Type.*;

/**
 * 入驻审批工作流业务服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class TenantEntryWorkflowServiceImpl implements ITenantEntryWorkflowService {

	private static final String PROCESS_KEY = "entry";
	private static final String PROCESS_KEY_LEGACY = "tenant_entry";
	private static final String PROCESS_KEY_CUSTOM_LEGACY = "tenant_entry-1";
	private static final String BUSINESS_TYPE = "tenant_entry";
	private static final Long HR_ROLE_ID = 1123598816738675203L;
	private static final String AUDIT_FLAG_NO = "0";
	private static final String AUDIT_FLAG_YES = "1";
	private static final String OPPORTUNITY_STATUS_AUDIT = "AUDIT";
	private static final String OPPORTUNITY_STATUS_INITIAL = "INITIAL";
	private static final String OPPORTUNITY_STATUS_DEAL = "DEAL";

	private final BusinessOpportunityMapper businessOpportunityMapper;
	private final CustomerMapper customerMapper;
	private final ICustomerService customerService;
	private final WfUserService wfUserService;
	private final WorkflowApprovalTraceService workflowApprovalTraceService;
	private final ObjectProvider<IWfCopyService> wfCopyServiceProvider;
	private final IPermissionHandler permissionHandler;

	@Override
	public boolean supports(WfNoticeDTO notice) {
		if (notice == null || notice.getProcessInstance() == null) {
			return false;
		}
		ProcessInstance processInstance = notice.getProcessInstance();
		Map<String, Object> variables = notice.getVariables();
		return isTenantEntryProcess(processInstance.getProcessDefinitionKey(), variables)
			|| businessOpportunityMapper.selectBusinessOpportunityByProcessInsId(processInstance.getId()) != null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void businessWithNotice(WfNoticeDTO notice) {
		if (notice == null || notice.getProcessInstance() == null) {
			return;
		}
		ProcessInstance processInstance = notice.getProcessInstance();
		Map<String, Object> variables = notice.getVariables();
		if (!supports(notice)) {
			return;
		}
		BusinessOpportunity opportunity = resolveOpportunity(processInstance, variables);
		if (opportunity == null || opportunity.getOpportunityId() == null) {
			throw new ServiceException("入驻流程关联商机不存在");
		}
		WfNotice.Type type = notice.getType();
		Task task = notice.getTask();
		String processInsId = processInstance.getId();
		String currentNode = task == null ? "流程结束" : task.getName();

		if (START == type) {
			opportunity = lockOpportunity(opportunity.getOpportunityId());
			validateWorkflowStart(notice, opportunity, processInsId);
			updateOpportunity(opportunity, processInsId, "running", currentNode, null, null, AUDIT_FLAG_YES, OPPORTUNITY_STATUS_AUDIT);
		} else if (TASK_CREATE == type) {
			updateOpportunity(opportunity, processInsId, "running", currentNode, null, null, AUDIT_FLAG_YES, OPPORTUNITY_STATUS_AUDIT);
			copyHr(task, processInstance);
		} else if (TASK_COMPLETE == type) {
			updateOpportunity(opportunity, processInsId, "running", currentNode, null, null, AUDIT_FLAG_YES, OPPORTUNITY_STATUS_AUDIT);
		} else if (FINISH == type) {
			String approvalUrl = buildApprovalFileUrl(opportunity, processInstance);
			Customer customer = customerService.completeTenantEntryApproval(opportunity, processInsId, approvalUrl);
			updateOpportunity(opportunity, processInsId, "approved", "流程结束", approvalUrl, DateUtil.now(), AUDIT_FLAG_YES, OPPORTUNITY_STATUS_DEAL);
			if (customer != null && isValidCustomerId(customer.getCustomerId())) {
				customerMapper.updateTenantEntryFlowState(customer.getCustomerId(), processInsId, "approved", "流程结束", approvalUrl, DateUtil.now(), 3, currentUserName());
			}
		} else if (REJECT == type || WITHDRAW == type || TERMINATE == type || DELETE_PROCESS == type) {
			String status = REJECT == type ? "rejected" : "canceled";
			updateOpportunity(opportunity, processInsId, status, currentNode, null, null, AUDIT_FLAG_NO, OPPORTUNITY_STATUS_INITIAL);
			if (isValidCustomerId(opportunity.getCustomerId())) {
				customerMapper.updateTenantEntryFlowState(opportunity.getCustomerId(), processInsId, status, currentNode, null, null, 1, currentUserName());
			}
		}
	}

	private boolean isValidCustomerId(Long customerId) {
		return customerId != null && customerId > 0;
	}

	private boolean isTenantEntryProcess(String processDefinitionKey, Map<String, Object> variables) {
		String businessType = getString(variables, "businessType", null);
		if (StringUtil.isNotBlank(businessType) && !BUSINESS_TYPE.equalsIgnoreCase(businessType)) {
			return false;
		}
		return StringUtil.isNotBlank(processDefinitionKey)
			&& (processDefinitionKey.equalsIgnoreCase(PROCESS_KEY)
			|| processDefinitionKey.equalsIgnoreCase(PROCESS_KEY_LEGACY)
			|| processDefinitionKey.equalsIgnoreCase(PROCESS_KEY_CUSTOM_LEGACY)
			|| processDefinitionKey.toLowerCase().startsWith(PROCESS_KEY_LEGACY + "-"));
	}

	private BusinessOpportunity resolveOpportunity(ProcessInstance processInstance, Map<String, Object> variables) {
		BusinessOpportunity persisted = businessOpportunityMapper.selectBusinessOpportunityByProcessInsId(processInstance.getId());
		Long persistedId = persisted == null ? null : persisted.getOpportunityId();
		Long variableId = getLong(variables, "opportunityId");
		Long businessKeyId = parseBusinessId(processInstance.getBusinessKey(), persistedId != null || variableId != null);
		assertSameOpportunityId(persistedId, businessKeyId);
		assertSameOpportunityId(persistedId, variableId);
		assertSameOpportunityId(businessKeyId, variableId);
		Long opportunityId = firstNotNull(persistedId, businessKeyId, variableId);
		if (opportunityId == null) {
			throw new ServiceException("入驻流程缺少商机ID");
		}
		BusinessOpportunity opportunity = persisted == null
			? businessOpportunityMapper.selectBusinessOpportunityById(opportunityId) : persisted;
		if (opportunity == null) {
			throw new ServiceException("入驻流程关联商机不存在");
		}
		return opportunity;
	}

	private BusinessOpportunity lockOpportunity(Long opportunityId) {
		BusinessOpportunity opportunity = businessOpportunityMapper.selectBusinessOpportunityByIdForUpdate(opportunityId);
		if (opportunity == null) {
			throw new ServiceException("入驻流程关联商机不存在");
		}
		return opportunity;
	}

	private void validateWorkflowStart(WfNoticeDTO notice, BusinessOpportunity opportunity, String processInsId) {
		if (!permissionHandler.hasMenu("business_opportunity_audit")
			&& !permissionHandler.hasMenu("settlement_project_approval_add")) {
			throw new ServiceException("当前账号无权发起入驻审批");
		}
		if ("approved".equalsIgnoreCase(Func.toStr(opportunity.getTenantEntryStatus()))
			|| OPPORTUNITY_STATUS_DEAL.equalsIgnoreCase(Func.toStr(opportunity.getOpportunityStatus()))) {
			throw new ServiceException("该商机已完成入驻审批，不能重复发起");
		}
		String existingProcessInsId = opportunity.getTenantEntryProcessInsId();
		boolean anotherProcess = StringUtil.isNotBlank(existingProcessInsId) && !existingProcessInsId.equals(processInsId);
		if (anotherProcess && ("running".equalsIgnoreCase(Func.toStr(opportunity.getTenantEntryStatus()))
			|| AUDIT_FLAG_YES.equals(opportunity.getSubmittedAuditFlag()))) {
			throw new ServiceException("该商机已有进行中的入驻审批");
		}
		WfUser startUser = notice.getStartUser();
		if (startUser == null) {
			throw new ServiceException("无法识别入驻流程发起人");
		}
	}

	private void assertSameOpportunityId(Long first, Long second) {
		if (first != null && second != null && !first.equals(second)) {
			throw new ServiceException("入驻流程商机ID与业务主键不一致");
		}
	}

	private Long parseBusinessId(String value, boolean hasOpportunityFallback) {
		if (StringUtil.isBlank(value)) {
			return null;
		}
		try {
			return Long.valueOf(value.trim());
		} catch (NumberFormatException exception) {
			if (hasOpportunityFallback) {
				return null;
			}
			throw new ServiceException("入驻流程业务主键格式不正确");
		}
	}

	private void updateOpportunity(BusinessOpportunity opportunity, String processInsId, String status, String currentNode,
								   String approvalPdfUrl, Date approvalTime, String submittedAuditFlag, String opportunityStatus) {
		int rows = businessOpportunityMapper.updateTenantEntryFlowState(
			opportunity.getOpportunityId(),
			processInsId,
			status,
			currentNode,
			approvalPdfUrl,
			approvalTime,
			submittedAuditFlag,
			opportunityStatus,
			currentUserName()
		);
		if (rows <= 0) {
			throw new ServiceException("入驻流程状态回写失败");
		}
	}

	private void copyHr(Task task, ProcessInstance processInstance) {
		if (task == null || !List.of("managerTask", "bossTask").contains(task.getTaskDefinitionKey())) {
			return;
		}
		String copyUser = wfUserService.listByRole(List.of(HR_ROLE_ID)).stream()
			.map(WfUser::getId)
			.filter(Func::isNotEmpty)
			.map(String::valueOf)
			.distinct()
			.collect(Collectors.joining(","));
		if (StringUtil.isBlank(copyUser)) {
			return;
		}
		WfCopyDTO dto = new WfCopyDTO();
		dto.setCopyUser(copyUser);
		dto.setTask(task);
		dto.setProcessInstance(processInstance);
		wfCopyServiceProvider.getObject().resolveCopyUser(dto);
	}

	private String buildApprovalFileUrl(BusinessOpportunity opportunity, ProcessInstance processInstance) {
		// 审批结束后回流到原始模板文件，入口保留在商机/入驻审批表下载接口。
		return "/blade-park/opportunity/tenant-entry/approval-form/" + opportunity.getOpportunityId()
			+ "?processInsId=" + processInstance.getId();
	}

	public String buildApprovalHtml(BusinessOpportunity opportunity, Map<String, Object> variables, String processInsId) {
		StringBuilder html = new StringBuilder();
		Map<String, String> approvalFields = workflowApprovalTraceService.approvalFields(processInsId);
		String applicant = firstNotBlank(getString(variables, "applicant", null), getString(variables, WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, opportunity.getCreateBy()));
		String dept = getString(variables, "applicantDept", "");
		String applyTime = normalizeDisplayDate(firstNotBlank(getString(variables, "applyTime", null), DateUtil.format(new Date(), DateUtil.PATTERN_DATE)));
		String principalName = firstNotBlank(getString(variables, "principalName", null), opportunity.getContactName());
		String principalPhone = firstNotBlank(getString(variables, "principalPhone", null), opportunity.getContactPhone());
		String leaseFloorArea = firstNotBlank(getString(variables, "leaseFloorArea", null), formatArea(opportunity));
		String legalContact = joinNonBlank("，", principalName, principalPhone);
		String financeContact = joinNonBlank("，",
			getString(variables, "financeContactName", null),
			getString(variables, "financeContactPhone", null)
		);
		String approvalTimeline = approvalValue(approvalFields, "审批流转信息", "流转信息", "审批记录");
		html.append("<style>")
			.append("@page{size:A4 portrait;margin:10mm 12mm;}")
			.append("@media print{body{margin:0;}}")
			.append(".tenant-entry-approval{font-family:SimSun,'Microsoft YaHei',Arial,sans-serif;line-height:1.35;padding:0;color:#111;}")
			.append(".tenant-entry-approval h2{text-align:center;margin:0 0 8px;font-size:20px;font-weight:600;}")
			.append(".tenant-entry-approval table{width:100%;border-collapse:collapse;table-layout:fixed;font-size:13px;page-break-inside:auto;}")
			.append(".tenant-entry-approval tr{page-break-inside:avoid;page-break-after:auto;}")
			.append("@media screen{.tenant-entry-approval{line-height:1.7;padding:24px;}.tenant-entry-approval h2{margin:0 0 14px;font-size:22px;}.tenant-entry-approval table{font-size:14px;}}")
			.append("</style>");
		html.append("<div class=\"tenant-entry-approval\">");
		html.append("<h2>企业入驻审核表</h2>");
		html.append("<table class=\"approval-table\">");
		appendTripleCells(html,
			"申请人", firstNotBlank(getString(variables, "handlerName", null), applicant),
			"部门", firstNotBlank(getString(variables, "handlerDept", null), dept),
			"申请日期", applyTime);
		appendFullRow(html, "企业名称", firstNotBlank(getString(variables, "enterpriseName", null), opportunity.getEnterpriseName()), 42);
		appendFullRow(html, "股东信息", firstNotBlank(getString(variables, "shareholderInfo", null), firstNotBlank(opportunity.getEquityStructure(), opportunity.getEnterpriseType())), 58);
		appendFullRow(html, "经营范围", firstNotBlank(getString(variables, "businessScope", null), opportunity.getBusinessScope()), 90);
		appendFullRow(html, "税收", getString(variables, "taxRevenue", ""), 42);
		appendCells(html, "法人、联系方式", legalContact, "财务、联系方式", financeContact);
		appendFullRow(html, "情况说明", getString(variables, "approvalMatter", ""), 58);
		appendTripleCells(html,
			"意向楼层", leaseFloorArea,
			"租金", firstNotBlank(getString(variables, "unitPrice", null), formatNumber(opportunity.getUnitPrice())),
			"免租期", firstNotBlank(getString(variables, "rentFreePeriod", null), opportunity.getRentFreePeriod()));
		appendSignRow(html, "部门审批：", approvalValue(approvalFields, "部门审批", "部门经理", "经理审批"));
		appendSignRow(html, "分管领导审批：", approvalValue(approvalFields, "分管领导审批", "分管领导"));
		appendSignRow(html, "总经理审批：", approvalValue(approvalFields, "总经理审批", "总经理"));
		if (StringUtil.isNotBlank(approvalTimeline)) {
			appendFullRow(html, "审批流转信息", approvalTimeline, 78);
		}
		html.append("</table>");
		html.append("</div>");
		return html.toString();
	}

	private void appendCells(StringBuilder html, String key1, String val1, String key2, String val2) {
		html.append("<tr style=\"height:40px;\">")
			.append(th(key1))
			.append(td(val1, 1))
			.append(th(key2))
			.append(td(val2, 1))
			.append("</tr>");
	}

	private void appendTripleCells(StringBuilder html, String key1, String val1, String key2, String val2,
								 String key3, String val3) {
		html.append("<tr style=\"height:40px;\">")
			.append(th(key1)).append(td(val1, 1))
			.append(th(key2)).append(td(val2, 1))
			.append(th(key3)).append(td(val3, 1))
			.append("</tr>");
	}

	private void appendFullRow(StringBuilder html, String key, String val, int height) {
		html.append("<tr style=\"height:").append(height).append("px;\">")
			.append(th(key))
			.append(td(val, 3))
			.append("</tr>");
	}

	private void appendSignRow(StringBuilder html, String key, String value) {
		html.append("<tr style=\"height:62px;\">")
			.append(th(key))
			.append("<td colspan=\"3\" style=\"border:1px solid #111;padding:6px;vertical-align:middle;text-align:left;white-space:pre-wrap;\">")
			.append(escapeHtml(firstNotBlank(value, "签字：")))
			.append("</td>")
			.append("</tr>");
	}

	private String th(String value) {
		return "<th style=\"width:18%;border:1px solid #111;padding:6px;text-align:center;font-weight:400;vertical-align:middle;\">"
			+ escapeHtml(value) + "</th>";
	}

	private String td(String value, int colspan) {
		return "<td colspan=\"" + colspan + "\" style=\"border:1px solid #111;padding:6px;vertical-align:middle;white-space:pre-wrap;\">"
			+ escapeHtml(value(value)) + "</td>";
	}

	private String joinNonBlank(String delimiter, String... values) {
		if (values == null) {
			return null;
		}
		List<String> parts = java.util.Arrays.stream(values)
			.filter(StringUtil::isNotBlank)
			.map(String::trim)
			.filter(item -> !"-".equals(item))
			.toList();
		return parts.isEmpty() ? null : String.join(delimiter, parts);
	}

	@SuppressWarnings("unused")
	private void appendRow(StringBuilder html, String key, String val) {
		html.append("<tr><th style=\"width:180px;text-align:left;border:1px solid #dcdfe6;background:#f5f7fa;padding:8px;\">")
			.append(escapeHtml(key))
			.append("</th><td style=\"border:1px solid #dcdfe6;padding:8px;\">")
			.append(escapeHtml(value(val)))
			.append("</td></tr>");
	}

	private Long getLong(Map<String, Object> variables, String key) {
		if (variables == null || variables.get(key) == null) {
			return null;
		}
		Object value = variables.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.valueOf(String.valueOf(value).trim());
		} catch (NumberFormatException exception) {
			throw new ServiceException("入驻流程商机ID格式不正确");
		}
	}

	@SafeVarargs
	private final <T> T firstNotNull(T... values) {
		if (values == null) {
			return null;
		}
		for (T value : values) {
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	private String getString(Map<String, Object> variables, String key, String defaultValue) {
		if (variables == null || variables.get(key) == null) {
			return defaultValue;
		}
		return Func.toStr(variables.get(key), defaultValue);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private String approvalValue(Map<String, String> fields, String... keys) {
		if (fields == null || fields.isEmpty() || keys == null) {
			return "";
		}
		for (String key : keys) {
			String value = fields.get(key);
			if (StringUtil.isNotBlank(value)) {
				return value;
			}
		}
		return "";
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

	private String value(String value) {
		return value == null ? "" : value;
	}

	private String formatNumber(BigDecimal value) {
		return value == null ? "" : value.stripTrailingZeros().toPlainString();
	}

	private String formatArea(BusinessOpportunity opportunity) {
		String area = formatNumber(opportunity.getIntentArea());
		if (StringUtil.isBlank(area)) {
			return value(opportunity.getLeaseFloor());
		}
		return value(opportunity.getLeaseFloor()) + (StringUtil.isBlank(opportunity.getLeaseFloor()) ? "" : "，") + area + "㎡";
	}

	private String normalizeDisplayDate(String value) {
		if (StringUtil.isBlank(value)) {
			return "";
		}
		return value.replace("-", ".");
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

}
