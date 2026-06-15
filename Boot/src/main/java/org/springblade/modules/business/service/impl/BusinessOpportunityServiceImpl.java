/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.oss.model.BladeFile;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.FileUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.BusinessOpportunityMapper;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springblade.modules.business.service.ITagService;
import org.springblade.modules.resource.builder.OssBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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

	private final ITagService tagService;
	private final OssBuilder ossBuilder;

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
			follow.setFollowUser(currentUserName());
		}
		follow.setOpportunityStatus(normalizeOpportunityStatus(follow.getOpportunityStatus()));
		int rows = baseMapper.insertFollow(follow);

		BusinessOpportunity patch = new BusinessOpportunity();
		patch.setOpportunityId(opportunityId);
		patch.setOpportunityStatus(follow.getOpportunityStatus());
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
		baseMapper.updateApprovalProjectStatus(projectId, currentUserName());

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

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
