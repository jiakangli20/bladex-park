/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.flowable.engine.HistoryService;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.jackson.JsonUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.FileUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.BusinessOpportunityMapper;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springblade.modules.business.service.ITagService;
import org.springblade.modules.approval.service.impl.WorkflowApprovalTraceService;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springblade.modules.contract.service.impl.ContractDocumentPreviewService;
import org.springblade.modules.resource.builder.OssBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商机服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class BusinessOpportunityServiceImpl extends ServiceImpl<BusinessOpportunityMapper, BusinessOpportunity> implements IBusinessOpportunityService {

	private static final String DEL_FLAG_NORMAL = "0";
	private static final String DEL_FLAG_DELETE = "1";
	private static final String AUDIT_FLAG_NO = "0";
	private static final String AUDIT_FLAG_YES = "1";
	private static final String STATUS_INITIAL = "INITIAL";
	private static final String STATUS_DEAL = "DEAL";
	private static final String BUSINESS_TYPE_TENANT_ENTRY = "tenant_entry";
	private static final String TEMPLATE_TENANT_ENTRY_APPROVAL = "君联大厦招商管理办法2023/附件一：企业入驻审批表.docx";

	private final ITagService tagService;
	private final OssBuilder ossBuilder;
	private final HistoryService historyService;
	private final WorkflowApprovalTraceService workflowApprovalTraceService;
	private final IContractTemplateRenderService contractTemplateRenderService;
	private final ContractDocumentPreviewService contractDocumentPreviewService;

	@Override
	public BusinessOpportunity selectBusinessOpportunityById(Long opportunityId) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isNotEmpty(opportunity)) {
			opportunity.setFollowList(selectFollowList(opportunityId));
			opportunity.setFileList(selectFileList(opportunityId));
			opportunity.setCarrierTypeArray(splitCarrierTypes(opportunity.getCarrierTypes()));
			List<Tag> tags = selectTagsByOpportunityId(opportunityId);
			opportunity.setTags(tags);
			opportunity.setTagIds(tags.stream().map(Tag::getTagId).collect(Collectors.toList()));
		}
		return opportunity;
	}

	@Override
	public List<BusinessOpportunity> selectBusinessOpportunityList(BusinessOpportunity opportunity) {
		List<BusinessOpportunity> list = baseMapper.selectBusinessOpportunityList(opportunity);
		list.forEach(item -> item.setTags(selectTagsByOpportunityId(item.getOpportunityId())));
		return list;
	}

	@Override
	public IPage<BusinessOpportunity> selectBusinessOpportunityPage(IPage<BusinessOpportunity> page, BusinessOpportunity opportunity) {
		IPage<BusinessOpportunity> result = baseMapper.selectBusinessOpportunityPage(page, opportunity);
		result.getRecords().forEach(item -> item.setTags(selectTagsByOpportunityId(item.getOpportunityId())));
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertBusinessOpportunity(BusinessOpportunity opportunity) {
		validateUniqueEnterpriseName(opportunity);
		opportunity.setCreateBy(currentUserName());
		opportunity.setCreateTime(DateUtil.now());
		opportunity.setDelFlag(DEL_FLAG_NORMAL);
		opportunity.setOpportunityStatus(normalizeOpportunityStatus(opportunity.getOpportunityStatus()));
		opportunity.setSubmittedAuditFlag(AUDIT_FLAG_NO);
		opportunity.setOpportunityNo(generateOpportunityNo());
		normalizeOpportunity(opportunity);
		linkExistingCustomer(opportunity);
		int rows = baseMapper.insertBusinessOpportunity(opportunity);
		if (Func.isNotEmpty(opportunity.getTagIds())) {
			setOpportunityTags(opportunity.getOpportunityId(), opportunity.getTagIds());
		}
		if (hasFollowContent(opportunity)) {
			BusinessOpportunityFollow follow = new BusinessOpportunityFollow();
			follow.setFollowUser(opportunity.getFollowUser());
			follow.setFollowContent(opportunity.getRemark());
			follow.setOpportunityStatus(opportunity.getOpportunityStatus());
			follow.setNextFollowTime(opportunity.getNextFollowTime());
			addFollowRecord(opportunity.getOpportunityId(), follow);
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateBusinessOpportunity(BusinessOpportunity opportunity) {
		if (Func.isEmpty(opportunity.getOpportunityId())) {
			throw new ServiceException("商机不存在");
		}
		BusinessOpportunity old = baseMapper.selectBusinessOpportunityById(opportunity.getOpportunityId());
		if (Func.isEmpty(old)) {
			throw new ServiceException("商机不存在");
		}
		if (AUDIT_FLAG_YES.equals(old.getSubmittedAuditFlag())) {
			throw new ServiceException("已提交审核的商机不可编辑");
		}
		validateUniqueEnterpriseName(opportunity);
		opportunity.setUpdateBy(currentUserName());
		opportunity.setUpdateTime(DateUtil.now());
		opportunity.setOpportunityStatus(normalizeOpportunityStatus(StringUtil.isBlank(opportunity.getOpportunityStatus())
			? old.getOpportunityStatus() : opportunity.getOpportunityStatus()));
		normalizeOpportunity(opportunity);
		linkExistingCustomer(opportunity);
		int rows = baseMapper.updateBusinessOpportunity(opportunity);
		if (opportunity.getTagIds() != null) {
			setOpportunityTags(opportunity.getOpportunityId(), opportunity.getTagIds());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitBusinessOpportunity(BusinessOpportunity opportunity) {
		return Func.isEmpty(opportunity.getOpportunityId()) ? insertBusinessOpportunity(opportunity) : updateBusinessOpportunity(opportunity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteBusinessOpportunityByIds(String ids) {
		List<Long> opportunityIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (opportunityIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的商机");
		}
		for (Long opportunityId : opportunityIds) {
			tagService.setOpportunityTags(opportunityId, Collections.emptyList());
		}
		return baseMapper.deleteBusinessOpportunityByIds(opportunityIds) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean addFollowRecord(Long opportunityId, BusinessOpportunityFollow follow) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		follow.setOpportunityId(opportunityId);
		follow.setFollowTime(follow.getFollowTime() == null ? new Date() : follow.getFollowTime());
		follow.setCreateBy(currentUserName());
		follow.setCreateTime(DateUtil.now());
		if (StringUtil.isBlank(follow.getFollowUser())) {
			follow.setFollowUser(opportunity.getFollowUser());
		}
		if (Func.isEmpty(follow.getFollowUserId())) {
			follow.setFollowUserId(opportunity.getFollowUserId());
		}
		if (StringUtil.isBlank(follow.getFollowUser())) {
			follow.setFollowUser(currentUserName());
		}
		follow.setOpportunityStatus(normalizeOpportunityStatus(follow.getOpportunityStatus()));
		int rows = baseMapper.insertFollow(follow);

		BusinessOpportunity patch = new BusinessOpportunity();
		patch.setOpportunityId(opportunityId);
		patch.setOpportunityStatus(follow.getOpportunityStatus());
		patch.setFollowUser(follow.getFollowUser());
		patch.setFollowUserId(follow.getFollowUserId());
		patch.setLastFollowTime(follow.getFollowTime());
		patch.setNextFollowTime(follow.getNextFollowTime());
		patch.setUpdateBy(currentUserName());
		baseMapper.updateBusinessOpportunity(patch);
		return rows > 0;
	}

	@Override
	public List<BusinessOpportunityFollow> selectFollowList(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		return baseMapper.selectFollowList(opportunityId);
	}

	@Override
	@SneakyThrows
	@Transactional(rollbackFor = Exception.class)
	public BusinessOpportunityFile uploadFile(Long opportunityId, MultipartFile file) {
		if (Func.isEmpty(opportunityId)) {
			throw new ServiceException("商机不存在");
		}
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		String originalFilename = StringUtil.isBlank(file.getOriginalFilename()) ? "opportunity-file" : file.getOriginalFilename();
		BladeFile bladeFile = ossBuilder.template().putFile(originalFilename, file.getInputStream());

		BusinessOpportunityFile fileEntity = new BusinessOpportunityFile();
		fileEntity.setOpportunityId(opportunityId);
		fileEntity.setFileName(originalFilename);
		fileEntity.setFileUrl(bladeFile.getLink());
		fileEntity.setFileSuffix(FileUtil.getFileExtension(originalFilename));
		fileEntity.setFileSize(file.getSize());
		fileEntity.setCreateBy(currentUserName());
		fileEntity.setCreateTime(DateUtil.now());
		baseMapper.insertFile(fileEntity);
		return fileEntity;
	}

	@Override
	public List<BusinessOpportunityFile> selectFileList(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		return baseMapper.selectFileList(opportunityId);
	}

	@Override
	public List<Tag> selectTagsByOpportunityId(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		List<Tag> tags = tagService.selectTagsByOpportunityId(opportunityId);
		if (Func.isEmpty(tags)) {
			BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
			if (Func.isNotEmpty(opportunity) && Func.isNotEmpty(opportunity.getCustomerId())) {
				return tagService.selectTagsByCustomerId(opportunity.getCustomerId());
			}
		}
		return tags;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setOpportunityTags(Long opportunityId, List<Long> tagIds) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		boolean result = tagService.setOpportunityTags(opportunityId, tagIds);
		if (Func.isNotEmpty(opportunity.getCustomerId())) {
			tagService.setCustomerTags(opportunity.getCustomerId(), tagIds);
		}
		return result;
	}

	@Override
	public Map<String, Object> queryBackgroundInvestigation(Long opportunityId) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		return queryBackgroundInvestigationByName(opportunity.getEnterpriseName());
	}

	@Override
	public Map<String, Object> queryBackgroundInvestigationByName(String enterpriseName) {
		if (StringUtil.isBlank(enterpriseName)) {
			throw new ServiceException("请先填写企业名称");
		}
		Map<String, Object> result = new HashMap<>(8);
		result.put("found", false);
		result.put("enterpriseName", enterpriseName);
		result.put("litigationList", Collections.emptyList());
		result.put("executorList", Collections.emptyList());
		result.put("penaltyList", Collections.emptyList());
		result.put("relatedRiskList", Collections.emptyList());
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BusinessOpportunity createApprovalProjectFromOpportunity(Long opportunityId, Long flowId) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		if (Func.isEmpty(opportunity.getParkId())) {
			throw new ServiceException("请先在商机管理中选择所属园区");
		}
		assertApprovalTablesReady();
		Long selectedFlowId = baseMapper.selectApprovalFlowId(opportunity.getParkId(), flowId);
		if (Func.isEmpty(selectedFlowId)) {
			throw new ServiceException("请先在流程配置中发布「入驻审批」流程");
		}
		Long projectId = baseMapper.selectExistingApprovalProjectId(opportunityId);
		if (Func.isEmpty(projectId)) {
			baseMapper.insertApprovalProject(opportunity, selectedFlowId, currentUserName());
			projectId = baseMapper.selectExistingApprovalProjectId(opportunityId);
		}
		baseMapper.insertApprovalLog(opportunity, projectId, selectedFlowId, currentUserName());
		Map<String, String> firstNode = selectFirstApprovalNode(selectedFlowId);
		baseMapper.updateApprovalProjectStatus(projectId, currentUserName(), firstNode.get("approverLogin"), firstNode.get("nodeName"));

		BusinessOpportunity patch = new BusinessOpportunity();
		patch.setOpportunityId(opportunityId);
		patch.setApprovalProjectId(projectId);
		patch.setSubmittedAuditFlag(AUDIT_FLAG_YES);
		patch.setOpportunityStatus(STATUS_INITIAL);
		patch.setUpdateBy(currentUserName());
		baseMapper.updateBusinessOpportunity(patch);
		return selectBusinessOpportunityById(opportunityId);
	}

	@Override
	public ContractNoticeFileVO exportTenantEntryApprovalForm(Long opportunityId, String processInsId) {
		return buildTenantEntryApprovalDocument(opportunityId, processInsId).document();
	}

	@Override
	public Kv previewTenantEntryApprovalForm(Long opportunityId, String processInsId) {
		TenantEntryApprovalDocument preview = buildTenantEntryApprovalDocument(opportunityId, processInsId);
		Map<String, String> summary = new LinkedHashMap<>();
		summary.put("企业名称", value(preview.opportunity().getEnterpriseName()));
		summary.put("文件格式", "Word");
		List<String> missingFields = List.of("企业名称", "经营范围", "负责人", "联系方式")
			.stream()
			.filter(field -> StringUtil.isBlank(preview.fields().get(field)))
			.toList();
		ContractNoticeFileVO document = preview.document();
		return Kv.create()
			.set("noticeType", document.getNoticeType())
			.set("noticeName", document.getNoticeName())
			.set("fileName", document.getFileName())
			.set("contentType", document.getContentType())
			.set("generatedAt", document.getGeneratedAt())
			.set("summary", summary)
			.set("fields", preview.fields())
			.set("missingFields", missingFields)
			.set("previewMode", "document")
			.set("html", contractDocumentPreviewService.render(document, summary, missingFields));
	}

	private TenantEntryApprovalDocument buildTenantEntryApprovalDocument(Long opportunityId, String processInsId) {
		BusinessOpportunity opportunity = selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		String resolvedProcessInsId = firstNotBlank(processInsId, opportunity.getTenantEntryProcessInsId());
		Map<String, Object> variables = loadProcessVariables(resolvedProcessInsId);
		variables.put("applyUserName", firstNotBlank(opportunity.getCreateBy(), currentUserName()));
		Map<String, String> fields = createTenantEntryApprovalFields(opportunity, variables);
		fields.putAll(workflowApprovalTraceService.approvalFields(resolvedProcessInsId));
		ContractNoticeFileVO document = contractTemplateRenderService.render(
			"tenant_entry_approval",
			"企业入驻审批表",
			TEMPLATE_TENANT_ENTRY_APPROVAL,
			"企业入驻审批表-" + firstNotBlank(opportunity.getEnterpriseName(), String.valueOf(opportunityId)),
			fields,
			Collections.emptyMap()
		);
		return new TenantEntryApprovalDocument(opportunity, fields, document);
	}

	private record TenantEntryApprovalDocument(BusinessOpportunity opportunity,
										 Map<String, String> fields,
										 ContractNoticeFileVO document) {
	}

	private Map<String, String> createTenantEntryApprovalFields(BusinessOpportunity opportunity, Map<String, Object> variables) {
		Map<String, String> fields = new LinkedHashMap<>();
		String applyTime = firstNotBlank(variableText(variables, "applyTime"), formatDate(opportunity.getCreateTime()));
		String applicant = firstNotBlank(
			variableText(variables, "handlerName"),
			variableText(variables, "applicant"),
			variableText(variables, "applyUserName"),
			opportunity.getFollowUser(),
			opportunity.getCreateBy()
		);
		fields.put("企业名称", value(firstNotBlank(variableText(variables, "enterpriseName"), opportunity.getEnterpriseName())));
		fields.put("申请时间", value(applyTime));
		fields.put("股东信息", value(firstNotBlank(
			variableText(variables, "shareholderInfo"),
			opportunity.getEquityStructure(),
			opportunity.getEnterpriseType()
		)));
		fields.put("经营范围", value(firstNotBlank(
			variableText(variables, "businessScope"),
			opportunity.getBusinessScope(),
			opportunity.getMainBusiness()
		)));
		fields.put("负责人", value(firstNotBlank(variableText(variables, "principalName"), opportunity.getContactName())));
		fields.put("联系方式", value(firstNotBlank(variableText(variables, "principalPhone"), opportunity.getContactPhone())));
		fields.put("租赁楼层、面积", value(firstNotBlank(variableText(variables, "leaseFloorArea"), formatArea(opportunity))));
		fields.put("免租期", value(variableText(variables, "rentFreePeriod")));
		fields.put("单价（元）", value(variableText(variables, "unitPrice")));
		fields.put("保证金（元）", value(variableText(variables, "deposit")));
		fields.put("合同有效期", value(firstNotBlank(variableText(variables, "contractPeriod"), opportunity.getLeaseTermLabel())));
		fields.put("经办人", value(applicant));
		fields.put("部门", value(firstNotBlank(variableText(variables, "handlerDept"), variableText(variables, "applicantDept"))));
		fields.put("审批事项", value(firstNotBlank(variableText(variables, "approvalMatter"), opportunity.getRemark(), opportunity.getMainBusiness())));
		return fields;
	}

	private Map<String, Object> loadProcessVariables(String processInsId) {
		Map<String, Object> variables = new HashMap<>();
		if (StringUtil.isBlank(processInsId)) {
			return variables;
		}
		List<HistoricVariableInstance> list = historyService.createHistoricVariableInstanceQuery()
			.processInstanceId(processInsId)
			.list();
		for (HistoricVariableInstance item : list) {
			variables.put(item.getVariableName(), item.getValue());
		}
		return variables;
	}

	@Override
	public Map<String, Object> selectOpportunityStatistics() {
		Map<String, Object> statistics = baseMapper.selectOpportunityStatistics();
		return Func.isEmpty(statistics) ? Collections.emptyMap() : statistics;
	}

	private void normalizeOpportunity(BusinessOpportunity opportunity) {
		if (opportunity.getCarrierTypeArray() != null && opportunity.getCarrierTypeArray().length > 0) {
			opportunity.setCarrierTypes(String.join(",", opportunity.getCarrierTypeArray()));
		}
		if (Func.isNotEmpty(opportunity.getLeaseTermYears()) && StringUtil.isBlank(opportunity.getLeaseTermLabel())) {
			BigDecimal years = opportunity.getLeaseTermYears();
			if (years.compareTo(new BigDecimal("1")) <= 0) {
				opportunity.setLeaseTermLabel("1年以内");
			} else if (years.compareTo(new BigDecimal("3")) <= 0) {
				opportunity.setLeaseTermLabel("1-3年");
			} else if (years.compareTo(new BigDecimal("5")) <= 0) {
				opportunity.setLeaseTermLabel("3-5年");
			} else {
				opportunity.setLeaseTermLabel("5年以上");
			}
		}
		if (StringUtil.isBlank(opportunity.getMajorIllegalFlag())) {
			opportunity.setMajorIllegalFlag("0");
		}
		if (StringUtil.isBlank(opportunity.getDishonestFlag())) {
			opportunity.setDishonestFlag("0");
		}
		if (StringUtil.isBlank(opportunity.getIndustryPenaltyFlag())) {
			opportunity.setIndustryPenaltyFlag("0");
		}
	}

	private void validateUniqueEnterpriseName(BusinessOpportunity opportunity) {
		if (StringUtil.isBlank(opportunity.getEnterpriseName())) {
			return;
		}
		String enterpriseName = opportunity.getEnterpriseName().trim();
		opportunity.setEnterpriseName(enterpriseName);
		Integer count = baseMapper.countByEnterpriseName(enterpriseName, opportunity.getOpportunityId());
		if (Func.isNotEmpty(count) && count > 0) {
			throw new ServiceException("企业名称已存在，请勿重复录入");
		}
	}

	private String normalizeOpportunityStatus(String status) {
		if (StringUtil.isBlank(status) || "DRAFT".equals(status) || "AUDIT".equals(status)) {
			return STATUS_INITIAL;
		}
		return status;
	}

	private void linkExistingCustomer(BusinessOpportunity opportunity) {
		if (Func.isNotEmpty(opportunity.getCustomerId()) && Func.isNotEmpty(baseMapper.selectCustomerIdById(opportunity.getCustomerId()))) {
			return;
		}
		if (StringUtil.isNotBlank(opportunity.getCreditCode())) {
			Long customerId = baseMapper.selectCustomerIdByCreditCode(opportunity.getCreditCode());
			if (Func.isNotEmpty(customerId)) {
				opportunity.setCustomerId(customerId);
			}
		}
	}

	private String[] splitCarrierTypes(String carrierTypes) {
		return StringUtil.isBlank(carrierTypes) ? new String[0] : carrierTypes.split(",");
	}

	private String generateOpportunityNo() {
		String datePrefix = DateUtil.format(new Date(), "yyyyMMdd");
		String lastNo = baseMapper.selectLastOpportunityNoByDate(datePrefix);
		int next = 1;
		if (StringUtil.isNotBlank(lastNo) && lastNo.matches("SJ-\\d{8}-\\d{4}")) {
			next = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
		}
		return "SJ-" + datePrefix + "-" + String.format("%04d", next);
	}

	private void assertApprovalTablesReady() {
		if (!tableExists("biz_approval_project") || !tableExists("biz_approval_flow") || !tableExists("biz_approval_log")) {
			throw new ServiceException("审批流程表尚未迁移，无法提交审核");
		}
	}

	private boolean tableExists(String tableName) {
		Integer count = baseMapper.countTableRows(tableName);
		return Func.isNotEmpty(count) && count > 0;
	}

	private boolean hasFollowContent(BusinessOpportunity opportunity) {
		return StringUtil.isNotBlank(opportunity.getRemark()) && Func.isNotEmpty(opportunity.getOpportunityId());
	}

	private Map<String, String> selectFirstApprovalNode(Long flowId) {
		List<Map<String, Object>> nodes = baseMapper.selectApprovalNodeCandidates(flowId);
		if (Func.isNotEmpty(nodes)) {
			Map<String, Object> node = nodes.get(0);
			return Map.of(
				"nodeName", Func.toStr(firstValue(node, "nodeName", "node_name")),
				"approverLogin", Func.toStr(firstValue(node, "approverLogin", "approver_login"))
			);
		}
		String nodeConfig = baseMapper.selectApprovalFlowNodeConfig(flowId);
		if (StringUtil.isBlank(nodeConfig)) {
			return Collections.emptyMap();
		}
		try {
			List<Map<String, Object>> configNodes = JsonUtil.getInstance().readValue(nodeConfig, new TypeReference<List<Map<String, Object>>>() {
			});
			for (Map<String, Object> item : configNodes) {
				String nodeType = Func.toStr(item.get("nodeType"), "approve");
				String approverLogin = firstNotBlank(Func.toStr(item.get("approverLogin")), Func.toStr(item.get("approverLoginName")));
				if (!"submit".equals(nodeType) && !"cc".equals(nodeType) && StringUtil.isNotBlank(approverLogin)) {
					return Map.of(
						"nodeName", Func.toStr(item.get("nodeName")),
						"approverLogin", approverLogin
					);
				}
			}
		} catch (Exception ignored) {
			return Collections.emptyMap();
		}
		return Collections.emptyMap();
	}

	private Object firstValue(Map<String, Object> row, String first, String second) {
		Object value = row.get(first);
		return value == null ? row.get(second) : value;
	}

	private String firstNotBlank(String first, String second) {
		return StringUtil.isNotBlank(first) ? first : second;
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

	private String variableText(Map<String, Object> variables, String key) {
		if (variables == null || StringUtil.isBlank(key) || variables.get(key) == null) {
			return null;
		}
		return Func.toStr(variables.get(key), "");
	}

	private String value(String value) {
		return StringUtil.isBlank(value) ? "-" : value;
	}

	private String formatDate(Date date) {
		return date == null ? null : DateUtil.format(date, DateUtil.PATTERN_DATE);
	}

	private String formatArea(BusinessOpportunity opportunity) {
		if (opportunity == null) {
			return null;
		}
		String area = formatNumber(opportunity.getIntentArea());
		if (StringUtil.isBlank(area)) {
			return opportunity.getCarrierTypes();
		}
		return firstNotBlank(opportunity.getCarrierTypes(), "") + (StringUtil.isBlank(opportunity.getCarrierTypes()) ? "" : "，") + area + "㎡";
	}

	private String formatNumber(BigDecimal value) {
		return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
