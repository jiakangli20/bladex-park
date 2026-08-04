/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.flowable.engine.HistoryService;
import org.flowable.engine.history.HistoricProcessInstance;
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
import org.springblade.modules.business.mapper.BackgroundInvestigationMapper;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.BackgroundInvestigation;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springblade.modules.business.service.ITagService;
import org.springblade.modules.approval.service.impl.WorkflowApprovalTraceService;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractTemplateRenderService;
import org.springblade.modules.contract.service.impl.ContractDocumentPreviewService;
import org.springblade.modules.resource.builder.OssBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
	private static final String PROCESS_KEY_TENANT_ENTRY = "entry";
	private static final String PROCESS_KEY_TENANT_ENTRY_CUSTOM_LEGACY = "tenant_entry-1";
	private static final String TEMPLATE_TENANT_ENTRY_APPROVAL = "君联大厦招商管理办法2023/附件一：企业入驻审批表.docx";
	private static final Set<String> OPPORTUNITY_FILE_SUFFIXES = Set.of("doc", "docx", "xls", "xlsx", "pdf", "jpg", "jpeg", "png");
	private static final Set<String> DANGEROUS_SUFFIXES = Set.of("exe", "com", "dll", "msi", "bat", "cmd", "sh", "js", "jar", "php", "jsp", "html", "htm", "svg", "scr");
	private static final long MAX_OPENXML_EXPANDED_BYTES = 200L * 1024L * 1024L;

	@Value("${blade.business.opportunity-file-max-bytes:20971520}")
	private long opportunityFileMaxBytes;

	private final ITagService tagService;
	private final OssBuilder ossBuilder;
	private final HistoryService historyService;
	private final WorkflowApprovalTraceService workflowApprovalTraceService;
	private final IContractTemplateRenderService contractTemplateRenderService;
	private final ContractDocumentPreviewService contractDocumentPreviewService;
	private final BackgroundInvestigationMapper backgroundInvestigationMapper;
	private final CustomerMapper customerMapper;

	@Override
	public BusinessOpportunity selectBusinessOpportunityById(Long opportunityId) {
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
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
		List<BusinessOpportunity> list = baseMapper.selectBusinessOpportunityList(normalizeQuery(opportunity));
		list.forEach(item -> item.setTags(selectTagsByOpportunityId(item.getOpportunityId())));
		return list;
	}

	@Override
	public IPage<BusinessOpportunity> selectBusinessOpportunityPage(IPage<BusinessOpportunity> page, BusinessOpportunity opportunity) {
		IPage<BusinessOpportunity> result = baseMapper.selectBusinessOpportunityPage(page, normalizeQuery(opportunity));
		result.getRecords().forEach(item -> item.setTags(selectTagsByOpportunityId(item.getOpportunityId())));
		return result;
	}

	@Override
	public IPage<BusinessOpportunity> selectBackgroundInvestigationPage(IPage<BusinessOpportunity> page, BusinessOpportunity opportunity) {
		return baseMapper.selectBackgroundInvestigationPage(page, normalizeQuery(opportunity));
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
		int rows;
		try {
			rows = baseMapper.insertBusinessOpportunity(opportunity);
		} catch (DuplicateKeyException exception) {
			throw new ServiceException("企业名称已存在或商机编号冲突，请刷新后重试");
		}
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
		BusinessOpportunity old = requireAccessibleOpportunity(opportunity.getOpportunityId());
		if (AUDIT_FLAG_YES.equals(old.getSubmittedAuditFlag())) {
			throw new ServiceException("已提交审核的商机不可编辑");
		}
		// 园区归属不允许通过普通编辑迁移，避免背景调查、标签和后续审批形成跨园区关系。
		opportunity.setParkId(old.getParkId());
		validateUniqueEnterpriseName(opportunity);
		opportunity.setUpdateBy(currentUserName());
		opportunity.setUpdateTime(DateUtil.now());
		opportunity.setOpportunityStatus(normalizeOpportunityStatus(StringUtil.isBlank(opportunity.getOpportunityStatus())
			? old.getOpportunityStatus() : opportunity.getOpportunityStatus()));
		normalizeOpportunity(opportunity);
		linkExistingCustomer(opportunity);
		int rows;
		try {
			rows = baseMapper.updateBusinessOpportunity(opportunity);
		} catch (DuplicateKeyException exception) {
			throw new ServiceException("企业名称已存在，请勿重复录入");
		}
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
			BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
			if (AUDIT_FLAG_YES.equals(opportunity.getSubmittedAuditFlag())
				|| "running".equalsIgnoreCase(Func.toStr(opportunity.getTenantEntryStatus()))) {
				throw new ServiceException("商机已进入审批流程，不能删除");
			}
			tagService.setOpportunityTags(opportunityId, Collections.emptyList());
		}
		return baseMapper.deleteBusinessOpportunityByIds(opportunityIds) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean addFollowRecord(Long opportunityId, BusinessOpportunityFollow follow) {
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
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
		requireAccessibleOpportunity(opportunityId);
		return baseMapper.selectFollowList(opportunityId);
	}

	@Override
	@SneakyThrows
	@Transactional(rollbackFor = Exception.class)
	public BusinessOpportunityFile uploadFile(Long opportunityId, MultipartFile file) {
		if (Func.isEmpty(opportunityId)) {
			throw new ServiceException("商机不存在");
		}
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
		validateOpportunityFile(file);
		String originalFilename = file.getOriginalFilename().trim();
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

	private void validateOpportunityFile(MultipartFile file) throws Exception {
		if (file == null || file.isEmpty() || file.getSize() <= 0) {
			throw new ServiceException("上传文件不能为空");
		}
		if (file.getSize() > Math.max(1L, opportunityFileMaxBytes)) {
			throw new ServiceException("上传文件不能超过 " + Math.max(1L, opportunityFileMaxBytes / 1024L / 1024L) + "MB");
		}
		String originalFilename = StringUtil.isBlank(file.getOriginalFilename()) ? "" : file.getOriginalFilename().trim();
		if (StringUtil.isBlank(originalFilename) || originalFilename.contains("/") || originalFilename.contains("\\")) {
			throw new ServiceException("文件名不合法");
		}
		String lowerName = originalFilename.toLowerCase(Locale.ROOT);
		String suffix = FileUtil.getFileExtension(lowerName);
		if (!OPPORTUNITY_FILE_SUFFIXES.contains(suffix)) {
			throw new ServiceException("仅支持 Word、Excel、PDF、JPG、PNG 文件");
		}
		String[] nameParts = lowerName.split("\\.");
		for (int index = 0; index < nameParts.length - 1; index++) {
			if (DANGEROUS_SUFFIXES.contains(nameParts[index])) {
				throw new ServiceException("文件名包含危险的双扩展名");
			}
		}
		validateDeclaredMime(file.getContentType(), suffix);
		byte[] header = new byte[8];
		int read;
		try (BufferedInputStream input = new BufferedInputStream(file.getInputStream())) {
			read = input.read(header);
		}
		boolean validHeader = switch (suffix) {
			case "pdf" -> read >= 4 && header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
			case "jpg", "jpeg" -> read >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
			case "png" -> read >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
			case "doc", "xls" -> read >= 4 && (header[0] & 0xff) == 0xd0 && (header[1] & 0xff) == 0xcf
				&& (header[2] & 0xff) == 0x11 && (header[3] & 0xff) == 0xe0;
			case "docx", "xlsx" -> read >= 4 && header[0] == 'P' && header[1] == 'K';
			default -> false;
		};
		if (!validHeader) {
			throw new ServiceException("文件扩展名与真实内容不一致");
		}
		if ("docx".equals(suffix) || "xlsx".equals(suffix)) {
			validateOpenXmlPackage(file, suffix);
		}
	}

	private void validateDeclaredMime(String contentType, String suffix) {
		String mime = StringUtil.isBlank(contentType) ? "" : contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		Set<String> allowed = switch (suffix) {
			case "doc" -> Set.of("application/msword", "application/octet-stream");
			case "docx" -> Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/zip", "application/octet-stream");
			case "xls" -> Set.of("application/vnd.ms-excel", "application/octet-stream");
			case "xlsx" -> Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip", "application/octet-stream");
			case "pdf" -> Set.of("application/pdf", "application/octet-stream");
			case "jpg", "jpeg" -> Set.of("image/jpeg", "application/octet-stream");
			case "png" -> Set.of("image/png", "application/octet-stream");
			default -> Collections.emptySet();
		};
		if (!allowed.contains(mime)) {
			throw new ServiceException("文件 MIME 类型与扩展名不一致");
		}
	}

	private void validateOpenXmlPackage(MultipartFile file, String suffix) throws Exception {
		boolean contentTypesFound = false;
		boolean documentRootFound = false;
		long expandedBytes = 0L;
		int entryCount = 0;
		byte[] buffer = new byte[8192];
		try (InputStream raw = file.getInputStream(); ZipInputStream zip = new ZipInputStream(raw)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				if (++entryCount > 10000) {
					throw new ServiceException("Office 文件包含过多内部条目");
				}
				String name = entry.getName() == null ? "" : entry.getName().replace('\\', '/');
				contentTypesFound |= "[Content_Types].xml".equals(name);
				documentRootFound |= ("docx".equals(suffix) && name.startsWith("word/"))
					|| ("xlsx".equals(suffix) && name.startsWith("xl/"));
				int count;
				while ((count = zip.read(buffer)) != -1) {
					expandedBytes += count;
					if (expandedBytes > MAX_OPENXML_EXPANDED_BYTES) {
						throw new ServiceException("Office 文件解压后内容过大");
					}
				}
				zip.closeEntry();
			}
		}
		if (!contentTypesFound || !documentRootFound) {
			throw new ServiceException("Office 文件结构与扩展名不一致");
		}
	}

	@Override
	public List<BusinessOpportunityFile> selectFileList(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		requireAccessibleOpportunity(opportunityId);
		return baseMapper.selectFileList(opportunityId);
	}

	@Override
	public List<Tag> selectTagsByOpportunityId(Long opportunityId) {
		if (Func.isEmpty(opportunityId)) {
			return Collections.emptyList();
		}
		requireAccessibleOpportunity(opportunityId);
		List<Tag> tags = tagService.selectTagsByOpportunityId(opportunityId);
		if (Func.isEmpty(tags)) {
			BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
			Customer customer = findActiveLinkedCustomer(opportunity);
			if (customer != null) {
				return tagService.selectTagsByCustomerId(customer.getCustomerId());
			}
		}
		return tags;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean setOpportunityTags(Long opportunityId, List<Long> tagIds) {
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
		boolean result = tagService.setOpportunityTags(opportunityId, tagIds);
		Customer customer = findActiveLinkedCustomer(opportunity);
		if (customer != null) {
			tagService.setCustomerTags(customer.getCustomerId(), tagIds);
		}
		return result;
	}

	@Override
	public Map<String, Object> queryBackgroundInvestigation(Long opportunityId) {
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
		return buildBackgroundInvestigationResult(opportunity.getEnterpriseName(), opportunity.getParkId(), opportunity);
	}

	@Override
	public Map<String, Object> queryBackgroundInvestigationByName(String enterpriseName, Long parkId) {
		if (StringUtil.isBlank(enterpriseName)) {
			throw new ServiceException("请先填写企业名称");
		}
		Long scopedParkId = parkId;
		BusinessOpportunity opportunity = baseMapper.selectOne(Wrappers.<BusinessOpportunity>lambdaQuery()
			.eq(BusinessOpportunity::getEnterpriseName, enterpriseName.trim())
			.eq(Func.isNotEmpty(scopedParkId), BusinessOpportunity::getParkId, scopedParkId)
			.eq(BusinessOpportunity::getDelFlag, DEL_FLAG_NORMAL)
			.orderByDesc(BusinessOpportunity::getCreateTime)
			.last("limit 1"));
		if (opportunity != null) {
			scopedParkId = opportunity.getParkId();
		}
		return buildBackgroundInvestigationResult(enterpriseName.trim(), scopedParkId, opportunity);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> saveBackgroundInvestigation(BackgroundInvestigation investigation) {
		if (investigation == null || StringUtil.isBlank(investigation.getEnterpriseName())) {
			throw new ServiceException("企业名称不能为空");
		}
		String enterpriseName = investigation.getEnterpriseName().trim();
		Long scopedParkId = investigation.getParkId();
		BusinessOpportunity opportunity = investigation.getOpportunityId() == null ? null
			: requireAccessibleOpportunity(investigation.getOpportunityId());
		if (opportunity == null) {
			opportunity = baseMapper.selectOne(Wrappers.<BusinessOpportunity>lambdaQuery()
				.eq(BusinessOpportunity::getEnterpriseName, enterpriseName)
				.eq(Func.isNotEmpty(scopedParkId), BusinessOpportunity::getParkId, scopedParkId)
				.eq(BusinessOpportunity::getDelFlag, DEL_FLAG_NORMAL)
				.orderByDesc(BusinessOpportunity::getCreateTime)
				.last("limit 1"));
		}
		if (opportunity != null) {
			scopedParkId = opportunity.getParkId();
			investigation.setOpportunityId(opportunity.getOpportunityId());
			Customer linkedCustomer = findActiveLinkedCustomer(opportunity);
			investigation.setCustomerId(linkedCustomer == null ? null : linkedCustomer.getCustomerId());
		}
		if (!isValidCustomerId(investigation.getCustomerId())) {
			investigation.setCustomerId(null);
		}
		if (investigation.getCustomerId() != null) {
			Customer customer = customerMapper.selectById(investigation.getCustomerId());
			if (customer == null || !DEL_FLAG_NORMAL.equals(customer.getDelFlag())) {
				throw new ServiceException("关联客户不存在");
			}
			if (opportunity != null && !java.util.Objects.equals(opportunity.getParkId(), customer.getParkId())) {
				throw new ServiceException("商机与关联客户必须属于同一园区");
			}
			scopedParkId = customer.getParkId();
		}
		if (investigation.getCustomerId() == null) {
			Customer customer = customerMapper.selectOne(Wrappers.<Customer>lambdaQuery()
				.eq(Customer::getEnterpriseName, enterpriseName)
				.eq(Func.isNotEmpty(scopedParkId), Customer::getParkId, scopedParkId)
				.eq(Customer::getDelFlag, DEL_FLAG_NORMAL)
				.orderByDesc(Customer::getCreateTime)
				.last("limit 1"));
			if (customer != null) {
				scopedParkId = customer.getParkId();
				investigation.setCustomerId(customer.getCustomerId());
			}
		}
		if (Func.isEmpty(scopedParkId)) {
			throw new ServiceException("未找到该企业所属园区，无法保存核验记录");
		}
		investigation.setInvestigationId(null);
		investigation.setParkId(scopedParkId);
		investigation.setEnterpriseName(enterpriseName);
		investigation.setVerifyStatus(firstNotBlank(investigation.getVerifyStatus(), "1"));
		investigation.setRiskLevel(firstNotBlank(investigation.getRiskLevel(), "0"));
		investigation.setLegalRiskFlag(normalizeRiskFlag(investigation.getLegalRiskFlag()));
		investigation.setExecutiveRiskFlag(normalizeRiskFlag(investigation.getExecutiveRiskFlag()));
		investigation.setShareholderRiskFlag(normalizeRiskFlag(investigation.getShareholderRiskFlag()));
		investigation.setExternalStatus("reserved");
		investigation.setCreateBy(currentUserName());
		investigation.setCreateTime(DateUtil.now());
		if (backgroundInvestigationMapper.insert(investigation) <= 0) {
			throw new ServiceException("背景调查结果保存失败");
		}
		if (investigation.getCustomerId() != null) {
			Customer customer = new Customer();
			customer.setCustomerId(investigation.getCustomerId());
			customer.setVerifyStatus(investigation.getVerifyStatus());
			customer.setVerifyMessage(investigation.getSourceRemark());
			customer.setVerifyTime(investigation.getCreateTime());
			customer.setRiskLevel(investigation.getRiskLevel());
			customer.setRiskSummary(investigation.getRiskSummary());
			customer.setLegalRiskFlag(investigation.getLegalRiskFlag());
			customer.setExecutiveRiskFlag(investigation.getExecutiveRiskFlag());
			customer.setShareholderRiskFlag(investigation.getShareholderRiskFlag());
			customer.setUpdateBy(currentUserName());
			customerMapper.updateCustomerCheckResult(customer);
		}
		return buildBackgroundInvestigationResult(enterpriseName, scopedParkId, opportunity);
	}

	private Map<String, Object> buildBackgroundInvestigationResult(String enterpriseName, Long parkId, BusinessOpportunity opportunity) {
		List<BackgroundInvestigation> history = backgroundInvestigationMapper.selectList(
			Wrappers.<BackgroundInvestigation>lambdaQuery()
				.eq(BackgroundInvestigation::getEnterpriseName, enterpriseName)
				.eq(Func.isNotEmpty(parkId), BackgroundInvestigation::getParkId, parkId)
				.orderByDesc(BackgroundInvestigation::getCreateTime, BackgroundInvestigation::getInvestigationId));
		BackgroundInvestigation latest = history.isEmpty() ? null : history.get(0);
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("found", latest != null);
		result.put("enterpriseName", enterpriseName);
		result.put("parkId", parkId);
		result.put("opportunityId", opportunity == null ? null : opportunity.getOpportunityId());
		Customer linkedCustomer = findActiveLinkedCustomer(opportunity);
		Long latestCustomerId = latest == null || !isValidCustomerId(latest.getCustomerId()) ? null : latest.getCustomerId();
		result.put("customerId", linkedCustomer == null ? latestCustomerId : linkedCustomer.getCustomerId());
		result.put("latest", latest);
		result.put("history", history);
		result.put("externalStatus", "reserved");
		result.put("externalMessage", "第三方工商及司法风险查询待接入，当前结果为人工核验记录");
		result.put("litigationList", Collections.emptyList());
		result.put("executorList", Collections.emptyList());
		result.put("penaltyList", Collections.emptyList());
		result.put("relatedRiskList", Collections.emptyList());
		return result;
	}

	private String normalizeRiskFlag(String value) {
		return "1".equals(value) ? "1" : "0";
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public BusinessOpportunity createApprovalProjectFromOpportunity(Long opportunityId, Long flowId) {
		BusinessOpportunity opportunity = requireAccessibleOpportunity(opportunityId);
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
		List<String> missingFields = List.of("企业名称", "经营范围", "法人、联系方式")
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
		validateTenantEntryProcessBinding(opportunity, resolvedProcessInsId, variables);
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
		String department = firstNotBlank(variableText(variables, "handlerDept"), variableText(variables, "applicantDept"));
		String legalContact = joinNonBlank("，",
			firstNotBlank(variableText(variables, "legalRepresentative"), variableText(variables, "principalName"), opportunity.getContactName()),
			firstNotBlank(variableText(variables, "legalPhone"), variableText(variables, "principalPhone"), opportunity.getContactPhone())
		);
		String financeContact = joinNonBlank("，",
			firstNotBlank(variableText(variables, "financeContactName"), variableText(variables, "financialContactName")),
			firstNotBlank(variableText(variables, "financeContactPhone"), variableText(variables, "financialContactPhone"))
		);
		String intentFloor = firstNotBlank(variableText(variables, "intentFloor"), variableText(variables, "leaseFloorArea"), formatArea(opportunity));
		String rent = firstNotBlank(variableText(variables, "rent"), variableText(variables, "unitPrice"));
		String description = firstNotBlank(variableText(variables, "situationDescription"), variableText(variables, "approvalMatter"), opportunity.getRemark(), opportunity.getMainBusiness());
		fields.put("申请人", value(applicant));
		fields.put("部门", value(department));
		fields.put("申请日期", value(applyTime));
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
		fields.put("税收", value(firstNotBlank(variableText(variables, "taxRevenue"), variableText(variables, "taxAmount"), variableText(variables, "taxDescription"))));
		fields.put("法人、联系方式", value(legalContact));
		fields.put("财务、联系方式", value(financeContact));
		fields.put("情况说明", value(description));
		fields.put("意向楼层", value(intentFloor));
		fields.put("租金", value(rent));
		fields.put("租赁楼层、面积", value(firstNotBlank(variableText(variables, "leaseFloorArea"), formatArea(opportunity))));
		fields.put("免租期", value(variableText(variables, "rentFreePeriod")));
		fields.put("单价（元）", value(variableText(variables, "unitPrice")));
		fields.put("保证金（元）", value(variableText(variables, "deposit")));
		fields.put("合同有效期", value(firstNotBlank(variableText(variables, "contractPeriod"), opportunity.getLeaseTermLabel())));
		fields.put("经办人", value(applicant));
		fields.put("审批事项", value(description));
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

	private void validateTenantEntryProcessBinding(BusinessOpportunity opportunity, String processInsId,
											   Map<String, Object> variables) {
		if (StringUtil.isBlank(processInsId)) {
			return;
		}
		HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
			.processInstanceId(processInsId)
			.singleResult();
		if (processInstance == null) {
			throw new ServiceException("入驻审批流程不存在");
		}
		String processDefinitionKey = processInstance.getProcessDefinitionKey();
		String businessType = variableText(variables, "businessType");
		boolean tenantEntryProcess = PROCESS_KEY_TENANT_ENTRY.equalsIgnoreCase(processDefinitionKey)
			|| BUSINESS_TYPE_TENANT_ENTRY.equalsIgnoreCase(processDefinitionKey)
			|| PROCESS_KEY_TENANT_ENTRY_CUSTOM_LEGACY.equalsIgnoreCase(processDefinitionKey)
			|| (StringUtil.isNotBlank(processDefinitionKey)
			&& processDefinitionKey.toLowerCase(Locale.ROOT).startsWith(BUSINESS_TYPE_TENANT_ENTRY + "-"));
		if (!tenantEntryProcess) {
			throw new ServiceException("流程实例不是企业入驻审批流程");
		}
		if (StringUtil.isNotBlank(businessType) && !BUSINESS_TYPE_TENANT_ENTRY.equalsIgnoreCase(businessType)) {
			throw new ServiceException("流程实例业务类型与企业入驻审批不一致");
		}
		Long businessKeyOpportunityId = parseLong(processInstance.getBusinessKey());
		Long variableOpportunityId = parseLong(variables.get("opportunityId"));
		if (businessKeyOpportunityId != null && variableOpportunityId != null
			&& !businessKeyOpportunityId.equals(variableOpportunityId)) {
			throw new ServiceException("入驻流程商机ID与业务主键不一致");
		}
		Long boundOpportunityId = businessKeyOpportunityId == null ? variableOpportunityId : businessKeyOpportunityId;
		if (boundOpportunityId != null && !boundOpportunityId.equals(opportunity.getOpportunityId())) {
			throw new ServiceException("流程实例不属于当前商机");
		}
		if (boundOpportunityId == null && !processInsId.equals(opportunity.getTenantEntryProcessInsId())) {
			throw new ServiceException("流程实例未绑定当前商机");
		}
	}

	private Long parseLong(Object value) {
		if (value == null || StringUtil.isBlank(String.valueOf(value))) {
			return null;
		}
		if (value instanceof Number number) {
			return number.longValue();
		}
		try {
			return Long.valueOf(String.valueOf(value).trim());
		} catch (NumberFormatException exception) {
			throw new ServiceException("入驻流程商机ID格式不正确");
		}
	}

	@Override
	public Map<String, Object> selectOpportunityStatistics() {
		Map<String, Object> statistics = baseMapper.selectOpportunityStatistics(null);
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
		if (isValidCustomerId(opportunity.getCustomerId())) {
			Customer customer = customerMapper.selectById(opportunity.getCustomerId());
			if (Func.isEmpty(customer) || !DEL_FLAG_NORMAL.equals(customer.getDelFlag())) {
				throw new ServiceException("关联客户不存在");
			}
			if (!java.util.Objects.equals(opportunity.getParkId(), customer.getParkId())) {
				throw new ServiceException("商机与关联客户必须属于同一园区");
			}
			return;
		}
		if (StringUtil.isNotBlank(opportunity.getCreditCode())) {
			Long customerId = baseMapper.selectCustomerIdByCreditCode(opportunity.getCreditCode(), opportunity.getParkId());
			if (Func.isNotEmpty(customerId)) {
				opportunity.setCustomerId(customerId);
			}
		}
	}

	private boolean isValidCustomerId(Long customerId) {
		return customerId != null && customerId > 0;
	}

	private Customer findActiveLinkedCustomer(BusinessOpportunity opportunity) {
		if (opportunity == null || !isValidCustomerId(opportunity.getCustomerId())) {
			return null;
		}
		Customer customer = customerMapper.selectById(opportunity.getCustomerId());
		if (customer == null || !DEL_FLAG_NORMAL.equals(customer.getDelFlag())) {
			return null;
		}
		return java.util.Objects.equals(opportunity.getParkId(), customer.getParkId()) ? customer : null;
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

	private BusinessOpportunity normalizeQuery(BusinessOpportunity opportunity) {
		BusinessOpportunity query = Func.isEmpty(opportunity) ? new BusinessOpportunity() : opportunity;
		return query;
	}

	private BusinessOpportunity requireAccessibleOpportunity(Long opportunityId) {
		BusinessOpportunity opportunity = baseMapper.selectBusinessOpportunityById(opportunityId);
		if (Func.isEmpty(opportunity)) {
			throw new ServiceException("商机不存在");
		}
		return opportunity;
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

	private String joinNonBlank(String delimiter, String... values) {
		if (values == null) {
			return null;
		}
		List<String> parts = Arrays.stream(values)
			.filter(StringUtil::isNotBlank)
			.map(String::trim)
			.filter(item -> !"-".equals(item))
			.toList();
		return parts.isEmpty() ? null : String.join(delimiter, parts);
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
