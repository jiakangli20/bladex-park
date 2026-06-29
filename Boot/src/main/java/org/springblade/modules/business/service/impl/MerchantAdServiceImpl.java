/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.mapper.MerchantAdMapper;
import org.springblade.modules.business.pojo.entity.MerchantAd;
import org.springblade.modules.business.pojo.entity.Merchant;
import org.springblade.modules.business.service.IMerchantAdService;
import org.springblade.modules.business.service.IMerchantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商户小程序广告服务实现.
 *
 * @author BladeX
 */
@Service
public class MerchantAdServiceImpl extends ServiceImpl<MerchantAdMapper, MerchantAd> implements IMerchantAdService {

	private static final String STATUS_ONLINE = "0";
	private static final String STATUS_OFFLINE = "1";
	private static final String DEL_FLAG_NORMAL = "0";
	private static final String LINK_TYPE_MERCHANT = "merchant";
	private static final String LINK_TYPE_URL = "url";

	private final IMerchantService merchantService;

	public MerchantAdServiceImpl(IMerchantService merchantService) {
		this.merchantService = merchantService;
	}

	@Override
	public MerchantAd selectAdById(Long adId) {
		MerchantAd ad = baseMapper.selectAdById(adId);
		if (Func.isNotEmpty(ad) && !hasAccessToPark(ad.getParkId())) {
			throw new ServiceException("广告不存在");
		}
		return ad;
	}

	@Override
	public List<MerchantAd> selectAdList(MerchantAd ad) {
		return baseMapper.selectAdList(normalizeQuery(ad));
	}

	@Override
	public IPage<MerchantAd> selectAdPage(IPage<MerchantAd> page, MerchantAd ad) {
		return baseMapper.selectAdPage(page, normalizeQuery(ad));
	}

	@Override
	public Kv selectAdStatistics(MerchantAd ad) {
		Map<String, Object> statistics = baseMapper.selectAdStatistics(normalizeQuery(ad));
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("onlineCount", toLong(statistics, "onlineCount"))
			.set("offlineCount", toLong(statistics, "offlineCount"))
			.set("waitingCount", toLong(statistics, "waitingCount"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertAd(MerchantAd ad) {
		validateAd(ad);
		Date now = DateUtil.now();
		ad.setParkId(resolveWriteParkId(ad.getParkId()));
		ad.setStatus(StringUtil.isBlank(ad.getStatus()) ? STATUS_OFFLINE : ad.getStatus());
		ad.setSortOrder(Func.isEmpty(ad.getSortOrder()) ? 0 : ad.getSortOrder());
		ad.setDelFlag(DEL_FLAG_NORMAL);
		ad.setCreateBy(currentUserName());
		ad.setCreateTime(now);
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(now);
		return baseMapper.insertAd(ad) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateAd(MerchantAd ad) {
		if (Func.isEmpty(ad) || Func.isEmpty(ad.getAdId())) {
			throw new ServiceException("广告不存在");
		}
		MerchantAd old = requireWritableAd(ad.getAdId());
		validateAd(mergeForValidate(old, ad));
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(ad.getParkId()) && ad.getParkId() > 0) {
			ad.setParkId(ad.getParkId());
		} else {
			ad.setParkId(old.getParkId());
		}
		ad.setUpdateBy(currentUserName());
		ad.setUpdateTime(DateUtil.now());
		return baseMapper.updateAd(ad) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitAd(MerchantAd ad) {
		return Func.isEmpty(ad.getAdId()) ? insertAd(ad) : updateAd(ad);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteAdByIds(String ids) {
		List<Long> adIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (adIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的广告");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteAdByIds(adIds, parkId, currentUserName()) > 0;
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
		requireWritableAd(adId);
		return baseMapper.updateAdStatus(adId, status, currentUserName()) > 0;
	}

	private MerchantAd requireWritableAd(Long adId) {
		MerchantAd ad = baseMapper.selectAdById(adId);
		if (Func.isEmpty(ad) || !hasAccessToPark(ad.getParkId())) {
			throw new ServiceException("广告不存在");
		}
		return ad;
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
		if (LINK_TYPE_MERCHANT.equals(ad.getLinkType())) {
			if (Func.isEmpty(ad.getMerchantId())) {
				throw new ServiceException("请选择关联商户");
			}
			Merchant merchant = merchantService.selectMerchantById(ad.getMerchantId());
			if (Func.isEmpty(merchant)) {
				throw new ServiceException("关联商户不存在");
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
		return merged;
	}

	private MerchantAd normalizeQuery(MerchantAd ad) {
		MerchantAd query = Func.isEmpty(ad) ? new MerchantAd() : ad;
		if (!AuthUtil.isAdministrator()) {
			query.setParkId(currentParkId());
		}
		return query;
	}

	private Long resolveWriteParkId(Long parkId) {
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(parkId) && parkId > 0) {
			return parkId;
		}
		return currentParkId();
	}

	private boolean hasAccessToPark(Long parkId) {
		return AuthUtil.isAdministrator() || Func.isEmpty(parkId) || currentParkId().equals(parkId);
	}

	private Long currentParkId() {
		Long deptId = Func.firstLong(AuthUtil.getDeptId());
		return Func.isEmpty(deptId) ? 1L : deptId;
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
