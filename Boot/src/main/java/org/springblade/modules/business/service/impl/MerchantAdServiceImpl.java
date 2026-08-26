/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.MerchantAdMapper;
import org.springblade.modules.business.pojo.entity.Merchant;
import org.springblade.modules.business.pojo.entity.MerchantAd;
import org.springblade.modules.business.service.IMerchantAdService;
import org.springblade.modules.business.service.IMerchantService;
import org.springblade.modules.miniapp.mapper.MerchantAdAuditLogMapper;
import org.springblade.modules.miniapp.pojo.entity.MerchantAdAuditLog;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 商户小程序广告服务实现.
 *
 * @author BladeX
 */
@Service
@RequiredArgsConstructor
public class MerchantAdServiceImpl extends ServiceImpl<MerchantAdMapper, MerchantAd> implements IMerchantAdService {

	private static final String STATUS_ONLINE = "0";
	private static final String STATUS_OFFLINE = "1";
	private static final String AUDIT_DRAFT = "DRAFT";
	private static final String AUDIT_PENDING = "PENDING";
	private static final String AUDIT_APPROVED = "APPROVED";
	private static final String AUDIT_REJECTED = "REJECTED";
	private static final String DEL_FLAG_NORMAL = "0";
	private static final String LINK_TYPE_MERCHANT = "merchant";
	private static final String LINK_TYPE_URL = "url";

	private final IMerchantService merchantService;
	private final MerchantAdAuditLogMapper auditLogMapper;
	private final IParkPermissionService parkPermissionService;

	@Override
	public MerchantAd selectAdById(Long adId) {
		MerchantAd ad = baseMapper.selectAdById(adId);
		if (Func.isEmpty(ad)) {
			throw new ServiceException("广告不存在");
		}
		parkPermissionService.requirePark(ad.getParkId());
		return ad;
	}

	@Override
	public List<MerchantAd> selectAdList(MerchantAd ad) {
		return baseMapper.selectAdList(normalizeQuery(ad), parkPermissionService.authorizedParkIds());
	}

	@Override
	public List<MerchantAd> selectPublicAdList(Long parkId, String adPosition) {
		if (parkId == null) {
			return List.of();
		}
		return baseMapper.selectPublicAdList(parkId, adPosition, DateUtil.now());
	}

	@Override
	public IPage<MerchantAd> selectAdPage(IPage<MerchantAd> page, MerchantAd ad) {
		return baseMapper.selectAdPage(page, normalizeQuery(ad), parkPermissionService.authorizedParkIds());
	}

	@Override
	public Kv selectAdStatistics(MerchantAd ad) {
		Map<String, Object> statistics = baseMapper.selectAdStatistics(normalizeQuery(ad), parkPermissionService.authorizedParkIds());
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("onlineCount", toLong(statistics, "onlineCount"))
			.set("offlineCount", toLong(statistics, "offlineCount"))
			.set("waitingCount", toLong(statistics, "waitingCount"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertAd(MerchantAd ad) {
		if (Func.isEmpty(ad)) {
			throw new ServiceException("广告不能为空");
		}
		ad.setParkId(resolveWriteParkId(ad.getParkId()));
		ad.setCustomerId(null);
		ad.setMemberId(null);
		validateAd(ad);
		Date now = DateUtil.now();
		ad.setAuditStatus(AUDIT_APPROVED);
		ad.setAuditUserId(AuthUtil.getUserId());
		ad.setAuditUserName(currentUserName());
		ad.setAuditTime(now);
		ad.setAuditOpinion("园区后台创建");
		ad.setStatus(STATUS_ONLINE);
		ad.setSortOrder(Func.isEmpty(ad.getSortOrder()) ? 0 : ad.getSortOrder());
		ad.setDelFlag(DEL_FLAG_NORMAL);
		ad.setCreateBy(currentUserName());
		ad.setCreateTime(now);
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(now);
		boolean inserted = baseMapper.insertAd(ad) > 0;
		if (inserted) {
			addAuditLog(ad, "ADMIN_CREATE", null, AUDIT_APPROVED, null, STATUS_ONLINE, ad.getAuditOpinion());
		}
		return inserted;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateAd(MerchantAd ad) {
		if (Func.isEmpty(ad) || Func.isEmpty(ad.getAdId())) {
			throw new ServiceException("广告不存在");
		}
		MerchantAd old = requireWritableAd(ad.getAdId());
		Long targetParkId = resolveWriteParkId(Func.isNotEmpty(ad.getParkId()) && ad.getParkId() > 0
			? ad.getParkId() : old.getParkId());
		MerchantAd merged = mergeForValidate(old, ad);
		merged.setParkId(targetParkId);
		validateAd(merged);
		ad.setParkId(targetParkId);
		ad.setCustomerId(null);
		ad.setMemberId(null);
		ad.setAuditStatus(null);
		ad.setAuditUserId(null);
		ad.setAuditUserName(null);
		ad.setAuditTime(null);
		ad.setAuditOpinion(null);
		ad.setStatus(null);
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(DateUtil.now());
		boolean updated = baseMapper.updateAd(ad) > 0;
		if (updated) {
			addAuditLog(old, "ADMIN_UPDATE", old.getAuditStatus(), old.getAuditStatus(),
				old.getStatus(), old.getStatus(), "园区后台修改广告内容");
		}
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitAd(MerchantAd ad) {
		return Func.isEmpty(ad) || Func.isEmpty(ad.getAdId()) ? insertAd(ad) : updateAd(ad);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteAdByIds(String ids) {
		List<Long> adIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (adIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的广告");
		}
		List<MerchantAd> ads = adIds.stream().map(this::requireWritableAd).toList();
		boolean deleted = baseMapper.deleteAdByIds(adIds, null, currentUserName()) > 0;
		if (deleted) {
			ads.forEach(ad -> addAuditLog(ad, "DELETE", ad.getAuditStatus(), ad.getAuditStatus(),
				ad.getStatus(), STATUS_OFFLINE, "园区后台删除广告"));
		}
		return deleted;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean changeStatus(Long adId, String status) {
		if (Func.isEmpty(adId)) {
			throw new ServiceException("广告不存在");
		}
		if (!List.of(STATUS_ONLINE, STATUS_OFFLINE).contains(Func.toStr(status))) {
			throw new ServiceException("广告状态不正确");
		}
		MerchantAd old = lockAd(adId);
		parkPermissionService.requirePark(old.getParkId());
		if (Objects.equals(old.getStatus(), status)) {
			throw new ServiceException(STATUS_ONLINE.equals(status) ? "广告已上架" : "广告已下架");
		}
		if (STATUS_ONLINE.equals(status) && !AUDIT_APPROVED.equals(old.getAuditStatus())) {
			throw new ServiceException("广告审核通过后才能上架");
		}
		boolean updated = baseMapper.updateAdStatus(adId, status, currentUserName()) > 0;
		if (updated) {
			addAuditLog(old, STATUS_ONLINE.equals(status) ? "ONLINE" : "OFFLINE",
				old.getAuditStatus(), old.getAuditStatus(), old.getStatus(), status, null);
		}
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean audit(Long adId, String auditStatus, String opinion) {
		String normalizedStatus = StringUtil.isBlank(auditStatus) ? "" : auditStatus.trim().toUpperCase();
		if (!List.of(AUDIT_APPROVED, AUDIT_REJECTED).contains(normalizedStatus)) {
			throw new ServiceException("审核结果不正确");
		}
		if (AUDIT_REJECTED.equals(normalizedStatus) && StringUtil.isBlank(opinion)) {
			throw new ServiceException("请填写驳回原因");
		}
		MerchantAd old = lockAd(adId);
		parkPermissionService.requirePark(old.getParkId());
		if (!AUDIT_PENDING.equals(old.getAuditStatus())) {
			throw new ServiceException("仅待审核广告可以执行审核");
		}
		Date now = DateUtil.now();
		boolean updated = baseMapper.update(null, Wrappers.<MerchantAd>lambdaUpdate()
			.eq(MerchantAd::getAdId, adId)
			.eq(MerchantAd::getAuditStatus, AUDIT_PENDING)
			.set(MerchantAd::getAuditStatus, normalizedStatus)
			.set(MerchantAd::getAuditUserId, AuthUtil.getUserId())
			.set(MerchantAd::getAuditUserName, currentUserName())
			.set(MerchantAd::getAuditTime, now)
			.set(MerchantAd::getAuditOpinion, StringUtil.isBlank(opinion) ? null : opinion.trim())
			.set(MerchantAd::getStatus, STATUS_OFFLINE)
			.set(MerchantAd::getUpdateBy, currentUserName())
			.set(MerchantAd::getUpdateTime, now)) > 0;
		if (updated) {
			addAuditLog(old, AUDIT_APPROVED.equals(normalizedStatus) ? "APPROVE" : "REJECT",
				old.getAuditStatus(), normalizedStatus, old.getStatus(), STATUS_OFFLINE, opinion);
		}
		return updated;
	}

	@Override
	public List<MerchantAdAuditLog> auditLogs(Long adId) {
		selectAdById(adId);
		return auditLogMapper.selectList(Wrappers.<MerchantAdAuditLog>lambdaQuery()
			.eq(MerchantAdAuditLog::getAdId, adId)
			.orderByDesc(MerchantAdAuditLog::getOperateTime)
			.orderByDesc(MerchantAdAuditLog::getId));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MerchantAd createCustomerAd(MerchantAd ad, Long parkId, Long customerId, Long memberId) {
		if (ad == null || parkId == null || customerId == null || memberId == null) {
			throw new ServiceException("企业广告归属信息不完整");
		}
		ad.setAdId(null);
		ad.setParkId(parkId);
		ad.setCustomerId(customerId);
		ad.setMemberId(memberId);
		ad.setAdPosition(StringUtil.isBlank(ad.getAdPosition()) ? "miniapp_home" : ad.getAdPosition());
		validateAd(ad);
		Date now = DateUtil.now();
		ad.setAuditStatus(AUDIT_DRAFT);
		ad.setAuditUserId(null);
		ad.setAuditUserName(null);
		ad.setAuditTime(null);
		ad.setAuditOpinion(null);
		ad.setStatus(STATUS_OFFLINE);
		ad.setSortOrder(ad.getSortOrder() == null ? 0 : ad.getSortOrder());
		ad.setDelFlag(DEL_FLAG_NORMAL);
		ad.setCreateBy(currentUserName());
		ad.setCreateTime(now);
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(now);
		if (baseMapper.insertAd(ad) <= 0) {
			throw new ServiceException("企业广告保存失败");
		}
		addAuditLog(ad, "CREATE_DRAFT", null, AUDIT_DRAFT, null, STATUS_OFFLINE, null);
		return selectCustomerAdById(ad.getAdId(), parkId, customerId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MerchantAd updateCustomerAd(MerchantAd ad, Long parkId, Long customerId) {
		if (ad == null || ad.getAdId() == null) {
			throw new ServiceException("广告不存在");
		}
		MerchantAd old = lockCustomerAd(ad.getAdId(), parkId, customerId);
		if (!List.of(AUDIT_DRAFT, AUDIT_REJECTED).contains(old.getAuditStatus())) {
			throw new ServiceException("仅草稿或已驳回广告可以修改");
		}
		MerchantAd merged = mergeForValidate(old, ad);
		merged.setParkId(old.getParkId());
		validateAd(merged);
		ad.setParkId(null);
		ad.setCustomerId(null);
		ad.setMemberId(null);
		ad.setAuditStatus(null);
		ad.setAuditUserId(null);
		ad.setAuditUserName(null);
		ad.setAuditTime(null);
		ad.setAuditOpinion(null);
		ad.setStatus(null);
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(DateUtil.now());
		if (baseMapper.updateAd(ad) <= 0) {
			throw new ServiceException("企业广告修改失败");
		}
		baseMapper.update(null, Wrappers.<MerchantAd>lambdaUpdate()
			.eq(MerchantAd::getAdId, old.getAdId())
			.set(MerchantAd::getAuditStatus, AUDIT_DRAFT)
			.set(MerchantAd::getAuditUserId, null)
			.set(MerchantAd::getAuditUserName, null)
			.set(MerchantAd::getAuditTime, null)
			.set(MerchantAd::getAuditOpinion, null)
			.set(MerchantAd::getStatus, STATUS_OFFLINE));
		addAuditLog(old, "UPDATE_DRAFT", old.getAuditStatus(), AUDIT_DRAFT,
			old.getStatus(), STATUS_OFFLINE, null);
		return selectCustomerAdById(old.getAdId(), parkId, customerId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitCustomerAd(Long adId, Long parkId, Long customerId) {
		MerchantAd old = lockCustomerAd(adId, parkId, customerId);
		if (!List.of(AUDIT_DRAFT, AUDIT_REJECTED).contains(old.getAuditStatus())) {
			throw new ServiceException("当前广告不能提交审核");
		}
		validateAd(old);
		boolean updated = updateCustomerStatus(old, AUDIT_PENDING);
		if (updated) {
			addAuditLog(old, "SUBMIT", old.getAuditStatus(), AUDIT_PENDING,
				old.getStatus(), STATUS_OFFLINE, null);
		}
		return updated;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean withdrawCustomerAd(Long adId, Long parkId, Long customerId) {
		MerchantAd old = lockCustomerAd(adId, parkId, customerId);
		if (!AUDIT_PENDING.equals(old.getAuditStatus())) {
			throw new ServiceException("仅待审核广告可以撤回");
		}
		boolean updated = updateCustomerStatus(old, AUDIT_DRAFT);
		if (updated) {
			addAuditLog(old, "WITHDRAW", AUDIT_PENDING, AUDIT_DRAFT,
				old.getStatus(), STATUS_OFFLINE, null);
		}
		return updated;
	}

	@Override
	public List<MerchantAd> selectCustomerAdList(Long parkId, Long customerId) {
		if (parkId == null || customerId == null) {
			throw new ServiceException("企业广告归属信息不完整");
		}
		MerchantAd query = new MerchantAd();
		query.setParkId(parkId);
		query.setCustomerId(customerId);
		return baseMapper.selectAdList(query, null);
	}

	@Override
	public MerchantAd selectCustomerAdById(Long adId, Long parkId, Long customerId) {
		MerchantAd ad = baseMapper.selectAdById(adId);
		if (!Objects.equals(ad.getParkId(), parkId) || !Objects.equals(ad.getCustomerId(), customerId)) {
			throw new ServiceException("广告不存在或无权访问");
		}
		return ad;
	}

	private MerchantAd requireWritableAd(Long adId) {
		MerchantAd ad = baseMapper.selectAdById(adId);
		if (Func.isEmpty(ad)) {
			throw new ServiceException("广告不存在");
		}
		parkPermissionService.requirePark(ad.getParkId());
		return ad;
	}

	private MerchantAd lockAd(Long adId) {
		if (adId == null) {
			throw new ServiceException("广告不存在");
		}
		MerchantAd ad = baseMapper.selectOne(Wrappers.<MerchantAd>lambdaQuery()
			.eq(MerchantAd::getAdId, adId)
			.eq(MerchantAd::getDelFlag, DEL_FLAG_NORMAL)
			.last("FOR UPDATE"));
		if (ad == null) {
			throw new ServiceException("广告不存在");
		}
		return ad;
	}

	private MerchantAd lockCustomerAd(Long adId, Long parkId, Long customerId) {
		MerchantAd ad = lockAd(adId);
		if (!Objects.equals(ad.getParkId(), parkId) || !Objects.equals(ad.getCustomerId(), customerId)) {
			throw new ServiceException("广告不存在或无权访问");
		}
		return ad;
	}

	private boolean updateCustomerStatus(MerchantAd old, String auditStatus) {
		Date now = DateUtil.now();
		return baseMapper.update(null, Wrappers.<MerchantAd>lambdaUpdate()
			.eq(MerchantAd::getAdId, old.getAdId())
			.eq(MerchantAd::getAuditStatus, old.getAuditStatus())
			.set(MerchantAd::getAuditStatus, auditStatus)
			.set(MerchantAd::getAuditUserId, null)
			.set(MerchantAd::getAuditUserName, null)
			.set(MerchantAd::getAuditTime, null)
			.set(MerchantAd::getAuditOpinion, null)
			.set(MerchantAd::getStatus, STATUS_OFFLINE)
			.set(MerchantAd::getUpdateBy, currentUserName())
			.set(MerchantAd::getUpdateTime, now)) > 0;
	}

	private void validateAd(MerchantAd ad) {
		if (Func.isEmpty(ad) || StringUtil.isBlank(ad.getAdTitle())) {
			throw new ServiceException("广告标题不能为空");
		}
		if (StringUtil.isBlank(ad.getAdPosition())) {
			throw new ServiceException("广告位置不能为空");
		}
		if (StringUtil.isBlank(ad.getCoverUrl())) {
			throw new ServiceException("请上传或填写广告封面图");
		}
		ad.setLinkType(StringUtil.isBlank(ad.getLinkType()) ? "none" : ad.getLinkType());
		if (!List.of("none", LINK_TYPE_MERCHANT, LINK_TYPE_URL).contains(ad.getLinkType())) {
			throw new ServiceException("广告跳转类型不正确");
		}
		if (StringUtil.isNotBlank(ad.getStatus()) && !List.of(STATUS_ONLINE, STATUS_OFFLINE).contains(ad.getStatus())) {
			throw new ServiceException("广告状态不正确");
		}
		if (LINK_TYPE_MERCHANT.equals(ad.getLinkType())) {
			if (Func.isEmpty(ad.getMerchantId())) {
				throw new ServiceException("请选择关联商户");
			}
			Merchant merchant = merchantService.selectPublicMerchantById(ad.getMerchantId());
			if (Func.isEmpty(merchant)) {
				throw new ServiceException("关联商户不存在");
			}
			if (Func.isEmpty(ad.getParkId()) || Func.isEmpty(merchant.getParkId()) || !ad.getParkId().equals(merchant.getParkId())) {
				throw new ServiceException("广告与关联商户必须属于同一园区");
			}
		}
		if (LINK_TYPE_URL.equals(ad.getLinkType()) && StringUtil.isBlank(ad.getLinkUrl())) {
			throw new ServiceException("请填写跳转链接");
		}
		if (Func.isNotEmpty(ad.getStartTime()) && Func.isNotEmpty(ad.getEndTime()) && ad.getStartTime().after(ad.getEndTime())) {
			throw new ServiceException("结束时间不能早于开始时间");
		}
		ad.setAdTitle(ad.getAdTitle().trim());
		ad.setAdPosition(ad.getAdPosition().trim());
		ad.setLinkType(StringUtil.isBlank(ad.getLinkType()) ? "none" : ad.getLinkType());
	}

	private MerchantAd mergeForValidate(MerchantAd old, MerchantAd patch) {
		MerchantAd merged = new MerchantAd();
		merged.setAdTitle(StringUtil.isBlank(patch.getAdTitle()) ? old.getAdTitle() : patch.getAdTitle());
		merged.setAdPosition(StringUtil.isBlank(patch.getAdPosition()) ? old.getAdPosition() : patch.getAdPosition());
		merged.setCoverUrl(patch.getCoverUrl() == null ? old.getCoverUrl() : patch.getCoverUrl());
		merged.setLinkType(patch.getLinkType() == null ? old.getLinkType() : patch.getLinkType());
		merged.setLinkUrl(patch.getLinkUrl() == null ? old.getLinkUrl() : patch.getLinkUrl());
		merged.setMerchantId(patch.getMerchantId() == null ? old.getMerchantId() : patch.getMerchantId());
		merged.setStartTime(patch.getStartTime() == null ? old.getStartTime() : patch.getStartTime());
		merged.setEndTime(patch.getEndTime() == null ? old.getEndTime() : patch.getEndTime());
		merged.setStatus(old.getStatus());
		merged.setParkId(old.getParkId());
		return merged;
	}

	private MerchantAd normalizeQuery(MerchantAd ad) {
		return Func.isEmpty(ad) ? new MerchantAd() : ad;
	}

	private Long resolveWriteParkId(Long parkId) {
		if (Func.isEmpty(parkId)) {
			throw new ServiceException("请选择园区");
		}
		parkPermissionService.requirePark(parkId);
		return parkId;
	}

	private void addAuditLog(MerchantAd ad, String actionType, String beforeAuditStatus,
		String afterAuditStatus, String beforeOnlineStatus, String afterOnlineStatus, String opinion) {
		MerchantAdAuditLog log = new MerchantAdAuditLog();
		log.setAdId(ad.getAdId());
		log.setParkId(ad.getParkId());
		log.setCustomerId(ad.getCustomerId());
		log.setActionType(actionType);
		log.setBeforeAuditStatus(beforeAuditStatus);
		log.setAfterAuditStatus(afterAuditStatus);
		log.setBeforeOnlineStatus(beforeOnlineStatus);
		log.setAfterOnlineStatus(afterOnlineStatus);
		log.setOperatorUserId(AuthUtil.getUserId());
		log.setOperatorName(currentUserName());
		log.setOpinion(StringUtil.isBlank(opinion) ? null : opinion.trim());
		log.setOperateTime(DateUtil.now());
		log.setStatus(1);
		auditLogMapper.insert(log);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

	private long toLong(Map<String, Object> map, String key) {
		if (Func.isEmpty(map) || !map.containsKey(key) || map.get(key) == null) {
			return 0L;
		}
		Object value = map.get(key);
		if (value instanceof Number number) {
			return number.longValue();
		}
		return Func.toLong(value, 0L);
	}
}
