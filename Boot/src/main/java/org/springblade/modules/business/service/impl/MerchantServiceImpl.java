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
import org.springblade.modules.business.mapper.MerchantMapper;
import org.springblade.modules.business.pojo.entity.Merchant;
import org.springblade.modules.business.service.IMerchantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商户服务实现.
 *
 * @author BladeX
 */
@Service
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements IMerchantService {

	private static final String STATUS_NORMAL = "0";
	private static final String STATUS_DISABLED = "1";
	private static final String STATUS_SUSPENDED = "2";
	private static final String DEL_FLAG_NORMAL = "0";

	@Override
	public Merchant selectMerchantById(Long merchantId) {
		Merchant merchant = baseMapper.selectMerchantById(merchantId);
		if (Func.isNotEmpty(merchant) && !hasAccessToPark(merchant.getParkId())) {
			throw new ServiceException("商户不存在");
		}
		return merchant;
	}

	@Override
	public List<Merchant> selectMerchantList(Merchant merchant) {
		return baseMapper.selectMerchantList(normalizeQuery(merchant));
	}

	@Override
	public IPage<Merchant> selectMerchantPage(IPage<Merchant> page, Merchant merchant) {
		return baseMapper.selectMerchantPage(page, normalizeQuery(merchant));
	}

	@Override
	public Kv selectMerchantStatistics(Merchant merchant) {
		Map<String, Object> statistics = baseMapper.selectMerchantStatistics(normalizeQuery(merchant));
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("normalCount", toLong(statistics, "normalCount"))
			.set("disabledCount", toLong(statistics, "disabledCount"))
			.set("suspendedCount", toLong(statistics, "suspendedCount"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertMerchant(Merchant merchant) {
		validateMerchant(merchant);
		merchant.setParkId(resolveWriteParkId(merchant.getParkId()));
		merchant.setStatus(StringUtil.isBlank(merchant.getStatus()) ? STATUS_NORMAL : merchant.getStatus());
		merchant.setDelFlag(DEL_FLAG_NORMAL);
		merchant.setCreateBy(currentUserName());
		merchant.setCreateTime(DateUtil.now());
		merchant.setUpdateBy(currentUserName());
		merchant.setUpdateTime(DateUtil.now());
		return baseMapper.insertMerchant(merchant) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateMerchant(Merchant merchant) {
		if (Func.isEmpty(merchant) || Func.isEmpty(merchant.getMerchantId())) {
			throw new ServiceException("商户不存在");
		}
		Merchant old = requireWritableMerchant(merchant.getMerchantId());
		validateMerchant(mergeForValidate(old, merchant));
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(merchant.getParkId()) && merchant.getParkId() > 0) {
			merchant.setParkId(merchant.getParkId());
		} else {
			merchant.setParkId(old.getParkId());
		}
		merchant.setUpdateBy(currentUserName());
		merchant.setUpdateTime(DateUtil.now());
		return baseMapper.updateMerchant(merchant) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitMerchant(Merchant merchant) {
		return Func.isEmpty(merchant.getMerchantId()) ? insertMerchant(merchant) : updateMerchant(merchant);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteMerchantByIds(String ids) {
		List<Long> merchantIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (merchantIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的商户");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deleteMerchantByIds(merchantIds, parkId, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean changeStatus(Long merchantId, String status) {
		if (Func.isEmpty(merchantId)) {
			throw new ServiceException("商户不存在");
		}
		if (!List.of(STATUS_NORMAL, STATUS_DISABLED, STATUS_SUSPENDED).contains(Func.toStr(status))) {
			throw new ServiceException("商户状态不正确");
		}
		requireWritableMerchant(merchantId);
		return baseMapper.updateMerchantStatus(merchantId, status, currentUserName()) > 0;
	}

	private Merchant requireWritableMerchant(Long merchantId) {
		Merchant merchant = baseMapper.selectMerchantById(merchantId);
		if (Func.isEmpty(merchant) || !hasAccessToPark(merchant.getParkId())) {
			throw new ServiceException("商户不存在");
		}
		return merchant;
	}

	private Merchant normalizeQuery(Merchant merchant) {
		Merchant query = Func.isEmpty(merchant) ? new Merchant() : merchant;
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

	private void validateMerchant(Merchant merchant) {
		if (Func.isEmpty(merchant) || StringUtil.isBlank(merchant.getMerchantName())) {
			throw new ServiceException("服务商名称不能为空");
		}
		if (StringUtil.isBlank(merchant.getBusinessType())) {
			throw new ServiceException("服务类型不能为空");
		}
		merchant.setMerchantName(merchant.getMerchantName().trim());
		merchant.setBusinessType(merchant.getBusinessType().trim());
	}

	private Merchant mergeForValidate(Merchant old, Merchant patch) {
		Merchant merged = new Merchant();
		merged.setMerchantName(StringUtil.isBlank(patch.getMerchantName()) ? old.getMerchantName() : patch.getMerchantName());
		merged.setBusinessType(StringUtil.isBlank(patch.getBusinessType()) ? old.getBusinessType() : patch.getBusinessType());
		return merged;
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
