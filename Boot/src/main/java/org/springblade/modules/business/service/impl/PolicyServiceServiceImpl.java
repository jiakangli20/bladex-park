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
import org.springblade.modules.business.mapper.PolicyServiceMapper;
import org.springblade.modules.business.pojo.entity.PolicyService;
import org.springblade.modules.business.service.IPolicyServiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 政策服务发布服务实现.
 *
 * @author BladeX
 */
@Service
public class PolicyServiceServiceImpl extends ServiceImpl<PolicyServiceMapper, PolicyService> implements IPolicyServiceService {

	private static final String STATUS_PUBLISHED = "0";
	private static final String STATUS_DRAFT = "1";
	private static final String STATUS_OFFLINE = "2";
	private static final String ONLINE_YES = "0";
	private static final String ONLINE_NO = "1";
	private static final String PERMANENT_YES = "0";
	private static final String PERMANENT_NO = "1";
	private static final String DEL_FLAG_NORMAL = "0";

	@Override
	public PolicyService selectPolicyById(Long policyId) {
		PolicyService policy = baseMapper.selectPolicyById(policyId);
		if (Func.isNotEmpty(policy) && !hasAccessToPark(policy.getParkId())) {
			throw new ServiceException("政策服务不存在");
		}
		return policy;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public PolicyService selectMiniAppPolicyById(Long policyId) {
		PolicyService policy = selectPolicyById(policyId);
		if (Func.isEmpty(policy) || !STATUS_PUBLISHED.equals(policy.getServiceStatus()) || !ONLINE_YES.equals(policy.getOnlineFlag())) {
			throw new ServiceException("政策服务不存在或未上架");
		}
		if (PERMANENT_NO.equals(policy.getPermanentFlag()) && Func.isNotEmpty(policy.getValidTime()) && policy.getValidTime().before(DateUtil.now())) {
			throw new ServiceException("政策服务已过期");
		}
		baseMapper.increaseViewCount(policyId);
		policy.setViewCount((Func.isEmpty(policy.getViewCount()) ? 0L : policy.getViewCount()) + 1L);
		return policy;
	}

	@Override
	public List<PolicyService> selectPolicyList(PolicyService policy) {
		return baseMapper.selectPolicyList(normalizeQuery(policy));
	}

	@Override
	public IPage<PolicyService> selectPolicyPage(IPage<PolicyService> page, PolicyService policy) {
		return baseMapper.selectPolicyPage(page, normalizeQuery(policy));
	}

	@Override
	public Kv selectPolicyStatistics(PolicyService policy) {
		Map<String, Object> statistics = baseMapper.selectPolicyStatistics(normalizeQuery(policy));
		return Kv.create()
			.set("totalCount", toLong(statistics, "totalCount"))
			.set("publishedCount", toLong(statistics, "publishedCount"))
			.set("draftCount", toLong(statistics, "draftCount"))
			.set("onlineCount", toLong(statistics, "onlineCount"))
			.set("viewCount", toLong(statistics, "viewCount"));
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertPolicy(PolicyService policy) {
		validatePolicy(policy);
		Date now = DateUtil.now();
		policy.setParkId(resolveWriteParkId(policy.getParkId()));
		policy.setServiceStatus(StringUtil.isBlank(policy.getServiceStatus()) ? STATUS_PUBLISHED : policy.getServiceStatus());
		policy.setOnlineFlag(StringUtil.isBlank(policy.getOnlineFlag()) ? ONLINE_YES : policy.getOnlineFlag());
		policy.setPermanentFlag(StringUtil.isBlank(policy.getPermanentFlag()) ? PERMANENT_YES : policy.getPermanentFlag());
		policy.setSortOrder(Func.isEmpty(policy.getSortOrder()) ? 0 : policy.getSortOrder());
		policy.setViewCount(Func.isEmpty(policy.getViewCount()) ? 0L : policy.getViewCount());
		policy.setDelFlag(DEL_FLAG_NORMAL);
		policy.setCreateBy(currentUserName());
		policy.setCreateTime(now);
		policy.setUpdateBy(currentUserName());
		policy.setUpdateTime(now);
		return baseMapper.insertPolicy(policy) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updatePolicy(PolicyService policy) {
		if (Func.isEmpty(policy) || Func.isEmpty(policy.getPolicyId())) {
			throw new ServiceException("政策服务不存在");
		}
		PolicyService old = requireWritablePolicy(policy.getPolicyId());
		validatePolicy(mergeForValidate(old, policy));
		if (AuthUtil.isAdministrator() && Func.isNotEmpty(policy.getParkId()) && policy.getParkId() > 0) {
			policy.setParkId(policy.getParkId());
		} else {
			policy.setParkId(old.getParkId());
		}
		policy.setUpdateBy(currentUserName());
		policy.setUpdateTime(DateUtil.now());
		return baseMapper.updatePolicy(policy) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitPolicy(PolicyService policy) {
		return Func.isEmpty(policy.getPolicyId()) ? insertPolicy(policy) : updatePolicy(policy);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deletePolicyByIds(String ids) {
		List<Long> policyIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (policyIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的政策服务");
		}
		Long parkId = AuthUtil.isAdministrator() ? null : currentParkId();
		return baseMapper.deletePolicyByIds(policyIds, parkId, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean changeOnlineFlag(Long policyId, String onlineFlag) {
		if (Func.isEmpty(policyId)) {
			throw new ServiceException("政策服务不存在");
		}
		if (!List.of(ONLINE_YES, ONLINE_NO).contains(Func.toStr(onlineFlag))) {
			throw new ServiceException("上架状态不正确");
		}
		requireWritablePolicy(policyId);
		return baseMapper.updateOnlineFlag(policyId, onlineFlag, currentUserName()) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean copyPolicy(Long policyId) {
		PolicyService old = requireWritablePolicy(policyId);
		PolicyService copy = new PolicyService();
		copy.setParkId(old.getParkId());
		copy.setServiceTitle(old.getServiceTitle() + "-副本");
		copy.setProjectScope(old.getProjectScope());
		copy.setServiceStatus(STATUS_DRAFT);
		copy.setViewCount(0L);
		copy.setPermanentFlag(old.getPermanentFlag());
		copy.setValidTime(old.getValidTime());
		copy.setOnlineFlag(ONLINE_NO);
		copy.setCoverUrl(old.getCoverUrl());
		copy.setContent(old.getContent());
		copy.setAttachmentFiles(old.getAttachmentFiles());
		copy.setSortOrder(old.getSortOrder());
		copy.setRemark(old.getRemark());
		return insertPolicy(copy);
	}

	private PolicyService requireWritablePolicy(Long policyId) {
		PolicyService policy = baseMapper.selectPolicyById(policyId);
		if (Func.isEmpty(policy) || !hasAccessToPark(policy.getParkId())) {
			throw new ServiceException("政策服务不存在");
		}
		return policy;
	}

	private void validatePolicy(PolicyService policy) {
		if (Func.isEmpty(policy) || StringUtil.isBlank(policy.getServiceTitle())) {
			throw new ServiceException("服务标题不能为空");
		}
		if (StringUtil.isBlank(policy.getProjectScope())) {
			throw new ServiceException("请选择项目范围");
		}
		if (PERMANENT_NO.equals(policy.getPermanentFlag()) && Func.isEmpty(policy.getValidTime())) {
			throw new ServiceException("请选择有效期");
		}
		policy.setServiceTitle(policy.getServiceTitle().trim());
	}

	private PolicyService mergeForValidate(PolicyService old, PolicyService patch) {
		PolicyService merged = new PolicyService();
		merged.setServiceTitle(StringUtil.isBlank(patch.getServiceTitle()) ? old.getServiceTitle() : patch.getServiceTitle());
		merged.setProjectScope(StringUtil.isBlank(patch.getProjectScope()) ? old.getProjectScope() : patch.getProjectScope());
		merged.setPermanentFlag(patch.getPermanentFlag() == null ? old.getPermanentFlag() : patch.getPermanentFlag());
		merged.setValidTime(patch.getValidTime() == null ? old.getValidTime() : patch.getValidTime());
		return merged;
	}

	private PolicyService normalizeQuery(PolicyService policy) {
		PolicyService query = Func.isEmpty(policy) ? new PolicyService() : policy;
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
