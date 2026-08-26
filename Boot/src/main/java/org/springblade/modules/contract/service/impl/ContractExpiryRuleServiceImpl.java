/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.contract.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.mapper.ContractExpiryRuleMapper;
import org.springblade.modules.contract.mapper.ContractMapper;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractExpiryRule;
import org.springblade.modules.contract.service.IContractExpiryRuleService;
import org.springblade.modules.park.service.IBuildingService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 合同到期提醒规则服务实现类
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class ContractExpiryRuleServiceImpl extends ServiceImpl<ContractExpiryRuleMapper, ContractExpiryRule> implements IContractExpiryRuleService {

	private static final String DEFAULT_DEL_FLAG = "0";
	private static final String DELETED_FLAG = "1";

	private final ContractMapper contractMapper;
	private final IBuildingService buildingService;
	private final IParkPermissionService parkPermissionService;

	@Override
	public IPage<ContractExpiryRule> selectRulePage(IPage<ContractExpiryRule> page, ContractExpiryRule rule) {
		return baseMapper.selectRulePage(page, rule == null ? null : rule.getRuleName(), null,
			parkPermissionService.authorizedParkIds());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitRule(ContractExpiryRule rule) {
		validateRule(rule);
		if (rule.getRuleId() != null) {
			ContractExpiryRule existing = getById(rule.getRuleId());
			if (existing == null || !DEFAULT_DEL_FLAG.equals(existing.getDelFlag())) {
				throw new ServiceException("规则不存在或已删除");
			}
			validateRuleBuildings(existing);
		}
		Date now = DateUtil.now();
		String userName = currentUserName();
		rule.setDelFlag(DEFAULT_DEL_FLAG);
		if (rule.getRuleId() == null) {
			rule.setCreateBy(userName);
			rule.setCreateTime(now);
		} else {
			rule.setUpdateBy(userName);
			rule.setUpdateTime(now);
		}
		boolean result = saveOrUpdate(rule);
		if (result) {
			applyRuleToContracts(rule);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeRules(String ids) {
		List<Long> idList = Func.toLongList(ids).stream().filter(Func::isNotEmpty).toList();
		if (idList.isEmpty()) {
			throw new ServiceException("请选择需要删除的规则");
		}
		List<ContractExpiryRule> rules = list(Wrappers.<ContractExpiryRule>lambdaQuery()
			.in(ContractExpiryRule::getRuleId, idList)
			.eq(ContractExpiryRule::getDelFlag, DEFAULT_DEL_FLAG));
		if (rules.size() != idList.size()) {
			throw new ServiceException("部分规则不存在或已删除");
		}
		rules.forEach(this::validateRuleBuildings);
		ContractExpiryRule update = new ContractExpiryRule();
		update.setDelFlag(DELETED_FLAG);
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		return update(update, Wrappers.<ContractExpiryRule>lambdaUpdate()
			.in(ContractExpiryRule::getRuleId, idList)
			.eq(ContractExpiryRule::getDelFlag, DEFAULT_DEL_FLAG));
	}

	private void validateRule(ContractExpiryRule rule) {
		if (rule == null) {
			throw new ServiceException("规则不能为空");
		}
		if (Func.isBlank(rule.getRuleName())) {
			throw new ServiceException("规则名称不能为空");
		}
		if (Func.isBlank(rule.getBuildingIds())) {
			throw new ServiceException("请选择关联楼宇");
		}
		if (rule.getRemindDays() == null || rule.getRemindDays() < 1) {
			throw new ServiceException("提前提醒天数必须大于0");
		}
		validateRuleBuildings(rule);
	}

	private void validateRuleBuildings(ContractExpiryRule rule) {
		List<Long> buildingIds = normalizeBuildingIds(rule.getBuildingIds());
		if (buildingIds.isEmpty()) {
			throw new ServiceException("请选择有效的关联楼宇");
		}
		for (Long buildingId : buildingIds) {
			buildingService.selectBuildingById(buildingId);
			Long count = contractMapper.existsBuilding(buildingId);
			if (count == null || count == 0) {
				throw new ServiceException("存在无效的关联楼宇");
			}
		}
	}

	private void applyRuleToContracts(ContractExpiryRule rule) {
		List<Long> buildingIds = normalizeBuildingIds(rule.getBuildingIds());
		if (buildingIds.isEmpty()) {
			return;
		}
		contractMapper.updateRenewalRemindDaysByBuildingIds(buildingIds, rule.getRemindDays(),
			currentUserName(), DateUtil.now());
	}

	private List<Long> normalizeBuildingIds(String buildingIds) {
		return Func.toLongList(Func.toStr(buildingIds, "").replace("building_", ""))
			.stream().filter(Func::isNotEmpty).distinct().toList();
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return Func.isBlank(userName) ? "system" : userName;
	}

}
