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
import org.springblade.modules.business.mapper.IndustryRuleMapper;
import org.springblade.modules.business.pojo.entity.IndustryRule;
import org.springblade.modules.business.service.IIndustryRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 行业准入规则服务实现.
 *
 * @author BladeX
 */
@Service
public class IndustryRuleServiceImpl extends ServiceImpl<IndustryRuleMapper, IndustryRule> implements IIndustryRuleService {

	private static final String ACCESS_PASS = "1";
	private static final String ACCESS_LIMIT = "2";
	private static final String ACCESS_FORBID = "3";
	private static final String STATUS_NORMAL = "0";
	private static final String DEL_FLAG_NORMAL = "0";

	@Override
	public IndustryRule selectIndustryRuleById(Long ruleId) {
		return baseMapper.selectIndustryRuleById(ruleId);
	}

	@Override
	public List<IndustryRule> selectIndustryRuleList(IndustryRule rule) {
		return baseMapper.selectIndustryRuleList(rule);
	}

	@Override
	public IPage<IndustryRule> selectIndustryRulePage(IPage<IndustryRule> page, IndustryRule rule) {
		return baseMapper.selectIndustryRulePage(page, rule);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean insertIndustryRule(IndustryRule rule) {
		validateRule(rule, null);
		rule.setCreateBy(currentUserName());
		rule.setUpdateBy(currentUserName());
		rule.setCreateTime(DateUtil.now());
		rule.setUpdateTime(DateUtil.now());
		if (StringUtil.isBlank(rule.getStatus())) {
			rule.setStatus(STATUS_NORMAL);
		}
		if (StringUtil.isBlank(rule.getDelFlag())) {
			rule.setDelFlag(DEL_FLAG_NORMAL);
		}
		return baseMapper.insertIndustryRule(rule) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateIndustryRule(IndustryRule rule) {
		if (Func.isEmpty(rule.getRuleId())) {
			throw new ServiceException("规则不存在");
		}
		validateRule(rule, rule.getRuleId());
		rule.setUpdateBy(currentUserName());
		rule.setUpdateTime(DateUtil.now());
		return baseMapper.updateIndustryRule(rule) > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitIndustryRule(IndustryRule rule) {
		return Func.isEmpty(rule.getRuleId()) ? insertIndustryRule(rule) : updateIndustryRule(rule);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteIndustryRuleByIds(String ids) {
		List<Long> ruleIds = Func.toLongList(ids).stream().filter(Func::isNotEmpty).collect(Collectors.toList());
		if (ruleIds.isEmpty()) {
			throw new ServiceException("请选择需要删除的规则");
		}
		return baseMapper.deleteIndustryRuleByIds(ruleIds) > 0;
	}

	@Override
	public Kv checkIndustry(String industryKeyword) {
		String keyword = industryKeyword == null ? "" : industryKeyword.trim();
		if (StringUtil.isBlank(keyword)) {
			return Kv.create()
				.set("accessType", "0")
				.set("accessText", "未判定")
				.set("industryKeyword", "")
				.set("reason", "请先填写行业类型");
		}
		List<IndustryRule> rules = selectIndustryRuleList(new IndustryRule()).stream()
			.filter(rule -> STATUS_NORMAL.equals(rule.getStatus()))
			.filter(rule -> StringUtil.isNotBlank(rule.getIndustryKeyword()))
			.filter(rule -> keyword.contains(rule.getIndustryKeyword()) || rule.getIndustryKeyword().contains(keyword))
			.collect(Collectors.toList());
		IndustryRule matched = rules.stream()
			.filter(rule -> ACCESS_FORBID.equals(rule.getAccessType()))
			.findFirst()
			.orElseGet(() -> rules.stream().filter(rule -> ACCESS_LIMIT.equals(rule.getAccessType())).findFirst().orElse(null));
		if (matched == null) {
			return Kv.create()
				.set("accessType", ACCESS_PASS)
				.set("accessText", "通过")
				.set("industryKeyword", keyword)
				.set("reason", "未命中限制或禁入规则");
		}
		String accessText = ACCESS_FORBID.equals(matched.getAccessType()) ? "禁入" : "限制";
		return Kv.create()
			.set("accessType", matched.getAccessType())
			.set("accessText", accessText)
			.set("industryKeyword", matched.getIndustryKeyword())
			.set("reason", StringUtil.isBlank(matched.getReason()) ? "命中园区行业准入规则" : matched.getReason())
			.set("ruleId", matched.getRuleId());
	}

	private void validateRule(IndustryRule rule, Long excludeRuleId) {
		String industryKeyword = rule.getIndustryKeyword() == null ? "" : rule.getIndustryKeyword().trim();
		if (StringUtil.isBlank(industryKeyword)) {
			throw new ServiceException("行业关键词不能为空");
		}
		if (!ACCESS_LIMIT.equals(rule.getAccessType()) && !ACCESS_FORBID.equals(rule.getAccessType())) {
			throw new ServiceException("准入类型只能为限制或禁入");
		}
		IndustryRule sameRule = baseMapper.selectSameKeywordRule(industryKeyword, excludeRuleId);
		if (Func.isNotEmpty(sameRule)) {
			throw new ServiceException("行业关键词规则已存在");
		}
		rule.setIndustryKeyword(industryKeyword);
		if (StringUtil.isNotBlank(rule.getReason())) {
			rule.setReason(rule.getReason().trim());
		}
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? "system" : userName;
	}

}
