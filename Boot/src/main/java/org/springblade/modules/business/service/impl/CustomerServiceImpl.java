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
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;
import org.springblade.modules.business.excel.CustomerExcel;
import org.springblade.modules.business.mapper.BusinessOpportunityMapper;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.business.service.ITagService;
import org.springblade.modules.park.pojo.entity.Park;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 入驻客户服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements ICustomerService {

	private static final String BUSINESS_TYPE_TENANT_ENTRY = "tenant_entry";
	private static final String DEL_FLAG_NORMAL = "0";
	private static final String DEL_FLAG_DELETE = "1";
	private static final String STATUS_NORMAL = "0";
	private static final String OPPORTUNITY_STATUS_INITIAL = "INITIAL";
	private static final String OPPORTUNITY_STATUS_DEAL = "DEAL";
	private static final String AUDIT_FLAG_NO = "0";
	private static final String AUDIT_FLAG_YES = "1";
	private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

	private final ITagService tagService;
	private final BusinessOpportunityMapper businessOpportunityMapper;

	@Override
	public Customer selectCustomerById(Long customerId) {
		Customer customer = baseMapper.selectCustomerById(customerId);
		fillTags(customer);
		return customer;
	}

	@Override
	public List<Customer> selectCustomerList(Customer customer) {
		List<Customer> list = baseMapper.selectCustomerList(normalizeQuery(customer));
		list.forEach(this::fillTags);
		return list;
	}

	@Override
	public IPage<Customer> selectCustomerPage(IPage<Customer> page, Customer customer) {
		IPage<Customer> result = baseMapper.selectCustomerPage(page, normalizeQuery(customer));
		result.getRecords().forEach(this::fillTags);
		return result;
	}

	@Override
	public Kv selectCustomerStatistics(Customer customer) {
		Map<String, Object> statistics = baseMapper.selectCustomerStatistics(normalizeQuery(customer));
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("settledCount", toLong(statistics, "settledCount"))
			.set("normalCount", toLong(statistics, "normalCount"))
			.set("monthNewCount", toLong(statistics, "monthNewCount"));
	}

	@Override
	public List<CustomerExcel> exportCustomer(Customer customer) {
		Map<Long, String> parkNameMap = parkNameMap();
		return selectCustomerList(customer).stream()
			.map(item -> toCustomerExcel(item, parkNameMap))
			.collect(Collectors.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void importCustomer(List<CustomerExcel> data) {
		if (Func.isEmpty(data)) {
			throw new ServiceException("导入数据不能为空");
		}
		Map<String, Long> parkIdMap = parkIdMap();
		List<Customer> customers = new ArrayList<>(data.size());
		for (int i = 0; i < data.size(); i++) {
			CustomerExcel excel = data.get(i);
			if (Func.isEmpty(excel) || isBlankRow(excel)) {
				continue;
			}
			customers.add(fromCustomerExcel(excel, i + 2, parkIdMap));
		}
		if (customers.isEmpty()) {
			throw new ServiceException("导入数据不能为空");
		}
		for (Customer customer : customers) {
			Long customerId = resolveImportCustomerId(customer);
			if (Func.isNotEmpty(customerId)) {
				customer.setCustomerId(customerId);
				updateCustomer(customer);
			} else {
				insertCustomer(customer);
			}
		}
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Customer insertCustomer(Customer customer) {
		if (Func.isEmpty(customer)) {
			throw new ServiceException("客户信息不能为空");
		}
		normalizeCustomer(customer);
		validateCustomer(customer, null);
		customer.setCreateBy(currentUserName());
		customer.setCreateTime(DateUtil.now());
		customer.setUpdateBy(currentUserName());
		customer.setUpdateTime(DateUtil.now());
		if (StringUtil.isBlank(customer.getDelFlag())) {
			customer.setDelFlag(DEL_FLAG_NORMAL);
		}
		if (StringUtil.isBlank(customer.getStatus())) {
			customer.setStatus(STATUS_NORMAL);
		}
		if (Func.isEmpty(customer.getSettlementStatus())) {
			customer.setSettlementStatus(0);
		}
		applyLocalCheck(customer);
		baseMapper.insertCustomer(customer);
		if (customer.getTagIds() != null) {
			tagService.setCustomerTags(customer.getCustomerId(), customer.getTagIds());
		}
		return selectCustomerById(customer.getCustomerId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateCustomer(Customer customer) {
		if (Func.isEmpty(customer) || Func.isEmpty(customer.getCustomerId())) {
			throw new ServiceException("客户不存在");
		}
		Customer old = baseMapper.selectCustomerById(customer.getCustomerId());
		if (Func.isEmpty(old)) {
			throw new ServiceException("客户不存在");
		}
		normalizeCustomer(customer);
		validateCustomer(mergeForValidate(old, customer), customer.getCustomerId());
		customer.setUpdateBy(currentUserName());
		customer.setUpdateTime(DateUtil.now());
		applyLocalCheck(customer);
		int rows = baseMapper.updateCustomer(customer);
		if (rows > 0) {
			syncRelatedEnterpriseName(old, customer);
		}
		if (customer.getTagIds() != null) {
			tagService.setCustomerTags(customer.getCustomerId(), customer.getTagIds());
		}
		return rows > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitCustomer(Customer customer) {
		if (Func.isEmpty(customer) || Func.isEmpty(customer.getCustomerId())) {
			insertCustomer(customer);
			return true;
		}
		return updateCustomer(customer);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteCustomerByIds(String ids) {
		List<Long> idList = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的客户");
		}
		for (Long customerId : idList) {
			assertCustomerCanDelete(customerId);
			tagService.setCustomerTags(customerId, Collections.emptyList());
		}
		return baseMapper.deleteCustomerByIds(idList, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean changeStatus(Long customerId, String status) {
		if (Func.isEmpty(customerId)) {
			throw new ServiceException("客户不存在");
		}
		if (!List.of("0", "1", "2").contains(Func.toStr(status))) {
			throw new ServiceException("客户状态不正确");
		}
		return baseMapper.updateCustomerStatus(customerId, status, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Customer checkCustomer(Long customerId) {
		Customer customer = baseMapper.selectCustomerById(customerId);
		if (Func.isEmpty(customer)) {
			throw new ServiceException("客户不存在");
		}
		applyLocalCheck(customer);
		customer.setUpdateBy(currentUserName());
		baseMapper.updateCustomerCheckResult(customer);
		return selectCustomerById(customerId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setCustomerTags(Long customerId, List<Long> tagIds) {
		if (Func.isEmpty(baseMapper.selectCustomerById(customerId))) {
			throw new ServiceException("客户不存在");
		}
		return tagService.setCustomerTags(customerId, tagIds);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Customer completeTenantEntryApproval(ApprovalProject project) {
		if (!isTenantEntryProject(project)) {
			return null;
		}
		BusinessOpportunity opportunity = Func.isEmpty(project.getBusinessId()) ? null
			: businessOpportunityMapper.selectBusinessOpportunityById(project.getBusinessId());
		Long customerId = resolveCustomerId(project, opportunity);
		Customer customer = Func.isEmpty(customerId) ? new Customer() : baseMapper.selectCustomerById(customerId);
		boolean isCreate = Func.isEmpty(customer) || Func.isEmpty(customer.getCustomerId());
		if (isCreate) {
			customer = new Customer();
		}
		mergeApprovalCustomer(customer, project, opportunity);
		customer.setSettlementStatus(3);
		customer.setStatus(STATUS_NORMAL);
		customer.setDelFlag(DEL_FLAG_NORMAL);
		customer.setUpdateBy(currentUserName());
		customer.setUpdateTime(DateUtil.now());
		applyLocalCheck(customer);
		if (isCreate) {
			normalizeCustomer(customer);
			validateCustomer(customer, null);
			customer.setCreateBy(currentUserName());
			customer.setCreateTime(DateUtil.now());
			baseMapper.insertCustomer(customer);
		} else {
			baseMapper.updateCustomer(customer);
		}
		baseMapper.updateApprovalProjectCustomerId(project.getProjectId(), customer.getCustomerId(), currentUserName());
		if (Func.isNotEmpty(project.getBusinessId())) {
			BusinessOpportunity patch = new BusinessOpportunity();
			patch.setOpportunityId(project.getBusinessId());
			patch.setCustomerId(customer.getCustomerId());
			patch.setSubmittedAuditFlag(AUDIT_FLAG_YES);
			patch.setOpportunityStatus(OPPORTUNITY_STATUS_DEAL);
			patch.setUpdateBy(currentUserName());
			businessOpportunityMapper.updateBusinessOpportunity(patch);
			List<Long> tagIds = tagService.selectTagsByOpportunityId(project.getBusinessId()).stream()
				.map(Tag::getTagId)
				.collect(Collectors.toList());
			tagService.setCustomerTags(customer.getCustomerId(), tagIds);
		}
		return selectCustomerById(customer.getCustomerId());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean resetTenantEntryApproval(ApprovalProject project) {
		if (!isTenantEntryProject(project) || Func.isEmpty(project.getBusinessId())) {
			return false;
		}
		BusinessOpportunity patch = new BusinessOpportunity();
		patch.setOpportunityId(project.getBusinessId());
		patch.setSubmittedAuditFlag(AUDIT_FLAG_NO);
		patch.setOpportunityStatus(OPPORTUNITY_STATUS_INITIAL);
		patch.setUpdateBy(currentUserName());
		return businessOpportunityMapper.updateBusinessOpportunity(patch) > 0;
	}

	private Customer normalizeQuery(Customer customer) {
		return Func.isEmpty(customer) ? new Customer() : customer;
	}

	private CustomerExcel toCustomerExcel(Customer customer, Map<Long, String> parkNameMap) {
		CustomerExcel excel = new CustomerExcel();
		excel.setParkName(parkNameMap.getOrDefault(customer.getParkId(), Func.toStr(customer.getParkId(), "")));
		excel.setEnterpriseName(customer.getEnterpriseName());
		excel.setCreditCode(customer.getCreditCode());
		excel.setContactName(customer.getContactName());
		excel.setContactPhone(customer.getContactPhone());
		excel.setContactEmail(customer.getContactEmail());
		excel.setContactPosition(customer.getContactPosition());
		excel.setEstablishDate(customer.getEstablishDate());
		excel.setRegisteredCapital(customer.getRegisteredCapital());
		excel.setEnterpriseType(customer.getEnterpriseType());
		excel.setIndustry(customer.getIndustry());
		excel.setScale(customer.getScale());
		excel.setMainBusiness(customer.getMainBusiness());
		excel.setLastYearRevenue(customer.getLastYearRevenue());
		excel.setBusinessScope(customer.getBusinessScope());
		excel.setRegisteredAddress(customer.getRegisteredAddress());
		excel.setMajorClients(customer.getMajorClients());
		excel.setCarrierTypes(customer.getCarrierTypes());
		excel.setIntentArea(customer.getIntentArea());
		excel.setUsagePurpose(customer.getUsagePurpose());
		excel.setLeaseTermYears(customer.getLeaseTermYears());
		excel.setDecorationRequirement(customer.getDecorationRequirement());
		excel.setSupportingNeeds(customer.getSupportingNeeds());
		excel.setChannel(customer.getChannel());
		excel.setThirdPartyChannelName(customer.getThirdPartyChannelName());
		excel.setSettlementStatusName(settlementStatusText(customer.getSettlementStatus()));
		excel.setStatusName(statusText(customer.getStatus()));
		excel.setCooperationLevelName(cooperationLevelText(customer.getCooperationLevel()));
		excel.setRemark(customer.getRemark());
		return excel;
	}

	private Customer fromCustomerExcel(CustomerExcel excel, int rowNumber, Map<String, Long> parkIdMap) {
		if (Func.isEmpty(excel) || isBlankRow(excel)) {
			throw new ServiceException("第" + rowNumber + "行导入数据为空");
		}
		Customer customer = new Customer();
		customer.setEnterpriseName(required(excel.getEnterpriseName(), rowNumber, "企业名称"));
		customer.setParkId(resolveParkId(excel.getParkName(), rowNumber, parkIdMap));
		customer.setCreditCode(trimToNull(excel.getCreditCode()));
		customer.setContactName(required(excel.getContactName(), rowNumber, "联系人"));
		customer.setContactPhone(required(excel.getContactPhone(), rowNumber, "联系电话"));
		customer.setContactEmail(trimToNull(excel.getContactEmail()));
		customer.setContactPosition(trimToNull(excel.getContactPosition()));
		customer.setEstablishDate(excel.getEstablishDate());
		customer.setRegisteredCapital(excel.getRegisteredCapital());
		customer.setEnterpriseType(trimToNull(excel.getEnterpriseType()));
		customer.setIndustry(trimToNull(excel.getIndustry()));
		customer.setScale(trimToNull(excel.getScale()));
		customer.setMainBusiness(trimToNull(excel.getMainBusiness()));
		customer.setLastYearRevenue(excel.getLastYearRevenue());
		customer.setBusinessScope(trimToNull(excel.getBusinessScope()));
		customer.setRegisteredAddress(trimToNull(excel.getRegisteredAddress()));
		customer.setMajorClients(trimToNull(excel.getMajorClients()));
		customer.setCarrierTypes(trimToNull(excel.getCarrierTypes()));
		customer.setIntentArea(excel.getIntentArea());
		customer.setUsagePurpose(trimToNull(excel.getUsagePurpose()));
		customer.setLeaseTermYears(excel.getLeaseTermYears());
		customer.setDecorationRequirement(trimToNull(excel.getDecorationRequirement()));
		customer.setSupportingNeeds(trimToNull(excel.getSupportingNeeds()));
		customer.setChannel(trimToNull(excel.getChannel()));
		customer.setThirdPartyChannelName(trimToNull(excel.getThirdPartyChannelName()));
		customer.setSettlementStatus(parseSettlementStatus(excel.getSettlementStatusName()));
		customer.setStatus(parseStatus(excel.getStatusName()));
		customer.setCooperationLevel(parseCooperationLevel(excel.getCooperationLevelName()));
		customer.setRemark(trimToNull(excel.getRemark()));
		customer.setMajorIllegalFlag("0");
		customer.setDishonestFlag("0");
		customer.setIndustryPenaltyFlag("0");
		return customer;
	}

	private Long resolveImportCustomerId(Customer customer) {
		if (StringUtil.isNotBlank(customer.getCreditCode())) {
			Long customerId = baseMapper.selectCustomerIdByCreditCode(customer.getCreditCode());
			if (Func.isNotEmpty(customerId)) {
				return customerId;
			}
		}
		return baseMapper.selectCustomerIdByEnterpriseAndPark(customer.getEnterpriseName(), customer.getParkId(), null);
	}

	private void fillTags(Customer customer) {
		if (Func.isEmpty(customer) || Func.isEmpty(customer.getCustomerId())) {
			return;
		}
		List<Tag> tags = tagService.selectTagsByCustomerId(customer.getCustomerId());
		customer.setTags(tags);
		customer.setTagIds(tags.stream().map(Tag::getTagId).collect(Collectors.toList()));
	}

	private void syncRelatedEnterpriseName(Customer old, Customer customer) {
		if (Func.isEmpty(old) || Func.isEmpty(customer) || Func.isEmpty(customer.getCustomerId())) {
			return;
		}
		String enterpriseName = customer.getEnterpriseName();
		if (StringUtil.isBlank(enterpriseName) || Objects.equals(old.getEnterpriseName(), enterpriseName)) {
			return;
		}
		baseMapper.updateOpportunityEnterpriseNameByCustomerId(customer.getCustomerId(), enterpriseName, currentUserName());
		if (tableExists("biz_approval_project")) {
			baseMapper.updateApprovalProjectEnterpriseNameByCustomerId(customer.getCustomerId(), enterpriseName, currentUserName());
		}
	}

	private void normalizeCustomer(Customer customer) {
		customer.setEnterpriseName(trimToNull(customer.getEnterpriseName()));
		customer.setCreditCode(trimToNull(customer.getCreditCode()));
		customer.setContactName(trimToNull(customer.getContactName()));
		customer.setContactPhone(trimToNull(customer.getContactPhone()));
		customer.setRegisteredAddress(trimToNull(customer.getRegisteredAddress()));
		customer.setAddress(StringUtil.isBlank(customer.getAddress()) ? customer.getRegisteredAddress() : customer.getAddress().trim());
		if (StringUtil.isBlank(customer.getMajorIllegalFlag())) {
			customer.setMajorIllegalFlag("0");
		}
		if (StringUtil.isBlank(customer.getDishonestFlag())) {
			customer.setDishonestFlag("0");
		}
		if (StringUtil.isBlank(customer.getIndustryPenaltyFlag())) {
			customer.setIndustryPenaltyFlag("0");
		}
		if (Func.isEmpty(customer.getCooperationLevel())) {
			customer.setCooperationLevel(1);
		}
	}

	private Map<Long, String> parkNameMap() {
		return listPark().stream()
			.collect(Collectors.toMap(Park::getId, Park::getName, (first, second) -> first));
	}

	private Map<String, Long> parkIdMap() {
		return listPark().stream()
			.collect(Collectors.toMap(park -> park.getName().trim(), Park::getId, (first, second) -> first));
	}

	private List<Park> listPark() {
		return baseMapper.selectListPark();
	}

	private void validateCustomer(Customer customer, Long excludeCustomerId) {
		if (StringUtil.isBlank(customer.getEnterpriseName())) {
			throw new ServiceException("企业名称不能为空");
		}
		if (Func.isEmpty(customer.getParkId())) {
			throw new ServiceException("所属园区不能为空");
		}
		if (StringUtil.isBlank(customer.getContactName())) {
			throw new ServiceException("联系人不能为空");
		}
		if (StringUtil.isBlank(customer.getContactPhone()) || !MOBILE_PATTERN.matcher(customer.getContactPhone()).matches()) {
			throw new ServiceException("联系电话必须为合法手机号");
		}
		Long existsId = baseMapper.selectCustomerIdByEnterpriseAndPark(customer.getEnterpriseName(), customer.getParkId(), excludeCustomerId);
		if (Func.isNotEmpty(existsId)) {
			throw new ServiceException("同一园区下企业名称不可重复");
		}
	}

	private Customer mergeForValidate(Customer old, Customer patch) {
		Customer merged = new Customer();
		merged.setCustomerId(old.getCustomerId());
		merged.setEnterpriseName(firstNotBlank(patch.getEnterpriseName(), old.getEnterpriseName()));
		merged.setParkId(Func.isNotEmpty(patch.getParkId()) ? patch.getParkId() : old.getParkId());
		merged.setContactName(firstNotBlank(patch.getContactName(), old.getContactName()));
		merged.setContactPhone(firstNotBlank(patch.getContactPhone(), old.getContactPhone()));
		return merged;
	}

	private void assertCustomerCanDelete(Long customerId) {
		if (Func.isEmpty(baseMapper.selectCustomerById(customerId))) {
			throw new ServiceException("客户不存在");
		}
		if (Func.toInt(baseMapper.countOpportunityByCustomerId(customerId), 0) > 0) {
			throw new ServiceException("客户已关联商机，不能删除");
		}
		if (Func.toInt(baseMapper.countApprovalProjectByCustomerId(customerId), 0) > 0) {
			throw new ServiceException("客户已关联审批项目，不能删除");
		}
		if (tableExists("biz_contract") && Func.toInt(baseMapper.countContractByCustomerId(customerId), 0) > 0) {
			throw new ServiceException("客户已关联合同，不能删除");
		}
	}

	private Long resolveCustomerId(ApprovalProject project, BusinessOpportunity opportunity) {
		if (Func.isNotEmpty(project.getCustomerId()) && Func.isNotEmpty(baseMapper.selectCustomerById(project.getCustomerId()))) {
			return project.getCustomerId();
		}
		String creditCode = firstNotBlank(project.getCreditCode(), opportunity == null ? null : opportunity.getCreditCode());
		if (StringUtil.isNotBlank(creditCode)) {
			Long customerId = baseMapper.selectCustomerIdByCreditCode(creditCode);
			if (Func.isNotEmpty(customerId)) {
				return customerId;
			}
		}
		String enterpriseName = firstNotBlank(project.getEnterpriseName(), opportunity == null ? null : opportunity.getEnterpriseName());
		Long parkId = Func.isNotEmpty(project.getParkId()) ? project.getParkId() : opportunity == null ? null : opportunity.getParkId();
		return StringUtil.isBlank(enterpriseName) ? null : baseMapper.selectCustomerIdByEnterpriseAndPark(enterpriseName, parkId, null);
	}

	private void mergeApprovalCustomer(Customer customer, ApprovalProject project, BusinessOpportunity opportunity) {
		customer.setParkId(Func.isNotEmpty(project.getParkId()) ? project.getParkId() : opportunity == null ? customer.getParkId() : opportunity.getParkId());
		customer.setEnterpriseName(firstNotBlank(project.getEnterpriseName(), opportunity == null ? customer.getEnterpriseName() : opportunity.getEnterpriseName(), customer.getEnterpriseName(), project.getProjectName()));
		customer.setCreditCode(firstNotBlank(project.getCreditCode(), opportunity == null ? customer.getCreditCode() : opportunity.getCreditCode(), customer.getCreditCode()));
		customer.setContactName(firstNotBlank(project.getResponsiblePerson(), opportunity == null ? customer.getContactName() : opportunity.getContactName(), customer.getContactName(), project.getApplicant(), customer.getEnterpriseName()));
		customer.setContactPhone(firstNotBlank(project.getContactPhone(), opportunity == null ? customer.getContactPhone() : opportunity.getContactPhone(), customer.getContactPhone()));
		customer.setIntentArea(Func.isNotEmpty(project.getLeaseArea()) ? project.getLeaseArea() : opportunity == null ? customer.getIntentArea() : opportunity.getIntentArea());
		customer.setBusinessScope(firstNotBlank(project.getBusinessScope(), opportunity == null ? customer.getBusinessScope() : opportunity.getBusinessScope(), customer.getBusinessScope()));
		customer.setRemark(firstNotBlank(project.getSummary(), project.getApprovalMatter(), opportunity == null ? customer.getRemark() : opportunity.getRemark(), customer.getRemark()));
		if (opportunity != null) {
			customer.setEstablishDate(firstNonNull(opportunity.getEstablishDate(), customer.getEstablishDate()));
			customer.setRegisteredCapital(firstNonNull(opportunity.getRegisteredCapital(), customer.getRegisteredCapital()));
			customer.setEnterpriseType(firstNotBlank(opportunity.getEnterpriseType(), customer.getEnterpriseType()));
			customer.setIndustry(firstNotBlank(opportunity.getIndustryType(), customer.getIndustry()));
			customer.setRegisteredAddress(firstNotBlank(opportunity.getRegisteredAddress(), customer.getRegisteredAddress()));
			customer.setAddress(firstNotBlank(customer.getAddress(), opportunity.getRegisteredAddress()));
			customer.setEquityStructure(firstNotBlank(opportunity.getEquityStructure(), customer.getEquityStructure()));
			customer.setOrganizationStructure(firstNotBlank(opportunity.getOrganizationStructure(), customer.getOrganizationStructure()));
			customer.setMainBusiness(firstNotBlank(opportunity.getMainBusiness(), customer.getMainBusiness()));
			customer.setLastYearRevenue(firstNonNull(opportunity.getLastYearRevenue(), customer.getLastYearRevenue()));
			customer.setMajorClients(firstNotBlank(opportunity.getMajorClients(), customer.getMajorClients()));
			customer.setMajorIllegalFlag(firstNotBlank(opportunity.getMajorIllegalFlag(), customer.getMajorIllegalFlag(), "0"));
			customer.setDishonestFlag(firstNotBlank(opportunity.getDishonestFlag(), customer.getDishonestFlag(), "0"));
			customer.setIndustryPenaltyFlag(firstNotBlank(opportunity.getIndustryPenaltyFlag(), customer.getIndustryPenaltyFlag(), "0"));
			customer.setCarrierTypes(firstNotBlank(opportunity.getCarrierTypes(), customer.getCarrierTypes()));
			customer.setUsagePurpose(firstNotBlank(opportunity.getUsagePurpose(), customer.getUsagePurpose()));
			customer.setLeaseTermYears(firstNonNull(opportunity.getLeaseTermYears(), customer.getLeaseTermYears()));
			customer.setLeaseTermLabel(firstNotBlank(opportunity.getLeaseTermLabel(), customer.getLeaseTermLabel()));
			customer.setDecorationRequirement(firstNotBlank(opportunity.getDecorationRequirement(), customer.getDecorationRequirement()));
			customer.setSupportingNeeds(firstNotBlank(opportunity.getSupportingNeeds(), customer.getSupportingNeeds()));
			customer.setContactEmail(firstNotBlank(opportunity.getContactEmail(), customer.getContactEmail()));
			customer.setContactPosition(firstNotBlank(opportunity.getContactPosition(), customer.getContactPosition()));
			customer.setChannel(firstNotBlank(opportunity.getChannel(), customer.getChannel()));
			customer.setThirdPartyChannelName(firstNotBlank(opportunity.getThirdPartyChannelName(), customer.getThirdPartyChannelName()));
		}
	}

	private void applyLocalCheck(Customer customer) {
		boolean creditOk = StringUtil.isNotBlank(customer.getEnterpriseName())
			&& StringUtil.isNotBlank(customer.getCreditCode())
			&& customer.getCreditCode().length() == 18;
		customer.setVerifyStatus(creditOk ? "1" : "2");
		customer.setVerifyMessage(creditOk ? "本地核验通过" : "企业名称或统一社会信用代码不完整");
		customer.setVerifyTime(new Date());
		if (StringUtil.isBlank(customer.getIndustry())) {
			customer.setIndustryAccessResult("2");
			customer.setIndustryAccessReason("未填写行业，需人工复核");
		} else if (containsRiskKeyword(customer.getIndustry())) {
			customer.setIndustryAccessResult("3");
			customer.setIndustryAccessReason("行业命中高风险关键词");
		} else {
			customer.setIndustryAccessResult("1");
			customer.setIndustryAccessReason("本地准入规则通过");
		}
		String riskContent = Func.toStr(customer.getEnterpriseName()) + Func.toStr(customer.getRemark())
			+ Func.toStr(customer.getBusinessScope());
		if (containsRiskKeyword(riskContent)) {
			customer.setRiskLevel("3");
			customer.setRiskSummary("命中失信、被执行、涉诉或处罚等风险关键词");
			customer.setLegalRiskFlag("1");
		} else {
			customer.setRiskLevel("1");
			customer.setRiskSummary("本地风险排查未发现明显异常");
			customer.setLegalRiskFlag("0");
		}
		customer.setExecutiveRiskFlag(firstNotBlank(customer.getExecutiveRiskFlag(), "0"));
		customer.setShareholderRiskFlag(firstNotBlank(customer.getShareholderRiskFlag(), "0"));
	}

	private boolean isTenantEntryProject(ApprovalProject project) {
		return Func.isNotEmpty(project) && Objects.equals(BUSINESS_TYPE_TENANT_ENTRY, project.getBusinessType());
	}

	private boolean containsRiskKeyword(String text) {
		if (StringUtil.isBlank(text)) {
			return false;
		}
		return text.contains("失信") || text.contains("被执行") || text.contains("涉诉") || text.contains("处罚") || text.contains("禁入");
	}

	private boolean tableExists(String tableName) {
		Integer count = baseMapper.countTableRows(tableName);
		return Func.isNotEmpty(count) && count > 0;
	}

	private boolean isBlankRow(CustomerExcel excel) {
		return StringUtil.isBlank(excel.getEnterpriseName())
			&& StringUtil.isBlank(excel.getCreditCode())
			&& StringUtil.isBlank(excel.getContactName())
			&& StringUtil.isBlank(excel.getContactPhone());
	}

	private String required(String value, int rowNumber, String label) {
		String result = trimToNull(value);
		if (StringUtil.isBlank(result)) {
			throw new ServiceException("第" + rowNumber + "行" + label + "不能为空");
		}
		return result;
	}

	private Long resolveParkId(String parkName, int rowNumber, Map<String, Long> parkIdMap) {
		String name = required(parkName, rowNumber, "所属园区");
		Long parkId = parkIdMap.get(name);
		if (Func.isEmpty(parkId)) {
			throw new ServiceException("第" + rowNumber + "行所属园区不存在：" + name);
		}
		return parkId;
	}

	private Integer parseSettlementStatus(String value) {
		String text = trimToNull(value);
		if (StringUtil.isBlank(text)) {
			return 0;
		}
		return switch (text) {
			case "0", "未入驻" -> 0;
			case "1", "意向" -> 1;
			case "2", "签约" -> 2;
			case "3", "入驻", "已入驻" -> 3;
			default -> throw new ServiceException("入驻状态不正确：" + text);
		};
	}

	private String parseStatus(String value) {
		String text = trimToNull(value);
		if (StringUtil.isBlank(text)) {
			return STATUS_NORMAL;
		}
		return switch (text) {
			case "0", "正常" -> "0";
			case "1", "停用" -> "1";
			case "2", "归档" -> "2";
			default -> throw new ServiceException("客户状态不正确：" + text);
		};
	}

	private Integer parseCooperationLevel(String value) {
		String text = trimToNull(value);
		if (StringUtil.isBlank(text)) {
			return 1;
		}
		return switch (text) {
			case "1", "普通" -> 1;
			case "2", "VIP", "vip" -> 2;
			case "3", "战略" -> 3;
			default -> throw new ServiceException("合作等级不正确：" + text);
		};
	}

	private String settlementStatusText(Integer status) {
		if (status == null) {
			return "未入驻";
		}
		return switch (status) {
			case 1 -> "意向";
			case 2 -> "签约";
			case 3 -> "入驻";
			default -> "未入驻";
		};
	}

	private String statusText(String status) {
		return switch (Func.toStr(status, STATUS_NORMAL)) {
			case "1" -> "停用";
			case "2" -> "归档";
			default -> "正常";
		};
	}

	private String cooperationLevelText(Integer level) {
		if (level == null) {
			return "普通";
		}
		return switch (level) {
			case 2 -> "VIP";
			case 3 -> "战略";
			default -> "普通";
		};
	}

	private String trimToNull(String value) {
		return StringUtil.isBlank(value) ? null : value.trim();
	}

	private String firstNotBlank(String... values) {
		for (String value : values) {
			if (StringUtil.isNotBlank(value)) {
				return value.trim();
			}
		}
		return null;
	}

	private <T> T firstNonNull(T first, T second) {
		return first != null ? first : second;
	}

	private long toLong(Map<String, Object> map, String key) {
		if (Func.isEmpty(map)) {
			return 0L;
		}
		Object value = map.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Func.toLong(value, 0L);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
