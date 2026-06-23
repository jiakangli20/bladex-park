/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
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

	private static final String PROCESS_KEY = "tenant_entry";
	private static final String PROCESS_KEY_CUSTOM = "tenant_entry-1";
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
	private final ObjectProvider<IWfCopyService> wfCopyServiceProvider;

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
			return;
		}
		WfNotice.Type type = notice.getType();
		Task task = notice.getTask();
		String processInsId = processInstance.getId();
		String currentNode = task == null ? "流程结束" : task.getName();

		if (START == type) {
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
			if (customer != null && customer.getCustomerId() != null) {
				customerMapper.updateTenantEntryFlowState(customer.getCustomerId(), processInsId, "approved", "流程结束", approvalUrl, DateUtil.now(), 3, currentUserName());
			}
		} else if (REJECT == type || WITHDRAW == type || TERMINATE == type || DELETE_PROCESS == type) {
			String status = REJECT == type ? "rejected" : "canceled";
			updateOpportunity(opportunity, processInsId, status, currentNode, null, null, AUDIT_FLAG_NO, OPPORTUNITY_STATUS_INITIAL);
			if (opportunity.getCustomerId() != null) {
				customerMapper.updateTenantEntryFlowState(opportunity.getCustomerId(), processInsId, status, currentNode, null, null, 1, currentUserName());
			}
		}
	}

	private boolean isTenantEntryProcess(String processDefinitionKey, Map<String, Object> variables) {
		String businessType = getString(variables, "businessType", null);
		if (BUSINESS_TYPE.equalsIgnoreCase(businessType) || getLong(variables, "opportunityId") != null) {
			return true;
		}
		return StringUtil.isNotBlank(processDefinitionKey)
			&& (processDefinitionKey.equalsIgnoreCase(PROCESS_KEY)
			|| processDefinitionKey.equalsIgnoreCase(PROCESS_KEY_CUSTOM)
			|| processDefinitionKey.toLowerCase().startsWith(PROCESS_KEY + "-"));
	}

	private BusinessOpportunity resolveOpportunity(ProcessInstance processInstance, Map<String, Object> variables) {
		String businessKey = processInstance.getBusinessKey();
		if (StringUtil.isNotBlank(businessKey)) {
			BusinessOpportunity opportunity = businessOpportunityMapper.selectBusinessOpportunityById(Func.toLong(businessKey));
			if (opportunity != null) {
				return opportunity;
			}
		}
		Long opportunityId = getLong(variables, "opportunityId");
		if (opportunityId != null) {
			BusinessOpportunity opportunity = businessOpportunityMapper.selectBusinessOpportunityById(opportunityId);
			if (opportunity != null) {
				return opportunity;
			}
		}
		return businessOpportunityMapper.selectBusinessOpportunityByProcessInsId(processInstance.getId());
	}

	private void updateOpportunity(BusinessOpportunity opportunity, String processInsId, String status, String currentNode,
								   String approvalPdfUrl, Date approvalTime, String submittedAuditFlag, String opportunityStatus) {
		businessOpportunityMapper.updateTenantEntryFlowState(
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
		// 迁移阶段先生成可打印 HTML 审批表，后续可替换为正式 PDF 模板引擎。
		return "/blade-park/opportunity/tenant-entry/approval-form/" + opportunity.getOpportunityId()
			+ "?processInsId=" + processInstance.getId();
	}

	public String buildApprovalHtml(BusinessOpportunity opportunity, Map<String, Object> variables) {
		StringBuilder html = new StringBuilder();
		String applicant = firstNotBlank(getString(variables, "applicant", null), getString(variables, WfProcessConstant.TASK_VARIABLE_APPLY_USER_NAME, opportunity.getCreateBy()));
		String dept = getString(variables, "applicantDept", "");
		String applyTime = normalizeDisplayDate(firstNotBlank(getString(variables, "applyTime", null), DateUtil.format(new Date(), DateUtil.PATTERN_DATE)));
		String principalName = firstNotBlank(getString(variables, "principalName", null), opportunity.getContactName());
		String principalPhone = firstNotBlank(getString(variables, "principalPhone", null), opportunity.getContactPhone());
		String leaseFloorArea = firstNotBlank(getString(variables, "leaseFloorArea", null), formatArea(opportunity));
		html.append("<div style=\"font-family:SimSun,'Microsoft YaHei',Arial,sans-serif;line-height:1.7;padding:24px;color:#111;\">");
		html.append("<style>@media print{body{margin:0}.approval-table{page-break-inside:avoid}}</style>");
		html.append("<h2 style=\"text-align:center;margin:0 0 14px;font-size:22px;font-weight:600;\">企业入驻审批表</h2>");
		html.append("<table class=\"approval-table\" style=\"width:100%;border-collapse:collapse;table-layout:fixed;font-size:14px;\">");
		appendCells(html, "企业名称", firstNotBlank(getString(variables, "enterpriseName", null), opportunity.getEnterpriseName()), "申请时间", applyTime);
		appendFullRow(html, "股东信息", firstNotBlank(getString(variables, "shareholderInfo", null), firstNotBlank(opportunity.getEquityStructure(), opportunity.getEnterpriseType())), 80);
		appendFullRow(html, "经营范围", firstNotBlank(getString(variables, "businessScope", null), opportunity.getBusinessScope()), 136);
		appendCells(html, "负责人", principalName, "联系方式", principalPhone);
		appendCells(html, "租赁楼层、面积", leaseFloorArea, "免租期", getString(variables, "rentFreePeriod", ""));
		appendCells(html, "单价（元）", getString(variables, "unitPrice", ""), "保证金（元）", getString(variables, "deposit", ""));
		appendFullRow(html, "合同有效期", firstNotBlank(getString(variables, "contractPeriod", null), opportunity.getLeaseTermLabel()), 54);
		appendCells(html, "经办人", firstNotBlank(getString(variables, "handlerName", null), applicant), "部门", firstNotBlank(getString(variables, "handlerDept", null), dept));
		appendFullRow(html, "审批事项", getString(variables, "approvalMatter", ""), 82);
		appendSignRow(html, "部门审批：");
		appendSignRow(html, "分管领导审批：");
		appendSignRow(html, "总经理审批：");
		html.append("</table>");
		html.append("</div>");
		return html.toString();
	}

	private void appendCells(StringBuilder html, String key1, String val1, String key2, String val2) {
		html.append("<tr style=\"height:48px;\">")
			.append(th(key1))
			.append(td(val1, 1))
			.append(th(key2))
			.append(td(val2, 1))
			.append("</tr>");
	}

	private void appendFullRow(StringBuilder html, String key, String val, int height) {
		html.append("<tr style=\"height:").append(height).append("px;\">")
			.append(th(key))
			.append(td(val, 3))
			.append("</tr>");
	}

	private void appendSignRow(StringBuilder html, String key) {
		html.append("<tr style=\"height:86px;\">")
			.append(th(key))
			.append("<td colspan=\"3\" style=\"border:1px solid #111;padding:8px;vertical-align:bottom;text-align:center;\">签字：</td>")
			.append("</tr>");
	}

	private String th(String value) {
		return "<th style=\"width:18%;border:1px solid #111;padding:8px;text-align:center;font-weight:400;vertical-align:middle;\">"
			+ escapeHtml(value) + "</th>";
	}

	private String td(String value, int colspan) {
		return "<td colspan=\"" + colspan + "\" style=\"border:1px solid #111;padding:8px;vertical-align:middle;white-space:pre-wrap;\">"
			+ escapeHtml(value(value)) + "</td>";
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
		return Func.toLong(String.valueOf(variables.get(key)));
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

	private String firstNotBlank(String first, String second) {
		return StringUtil.isNotBlank(first) ? first : second;
	}

	private String value(String value) {
		return value == null ? "" : value;
	}

	private String formatNumber(BigDecimal value) {
		return value == null ? "" : value.stripTrailingZeros().toPlainString();
	}

	private String formatArea(BusinessOpportunity opportunity) {
		String area = formatNumber(opportunity.getIntentArea());
		return StringUtil.isBlank(area) ? "" : area + "平";
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
