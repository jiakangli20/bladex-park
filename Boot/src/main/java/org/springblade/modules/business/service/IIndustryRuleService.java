/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.IndustryRule;

import java.util.List;

/**
 * 行业准入规则服务.
 *
 * @author BladeX
 */
public interface IIndustryRuleService extends IService<IndustryRule> {

	IndustryRule selectIndustryRuleById(Long ruleId);

	List<IndustryRule> selectIndustryRuleList(IndustryRule rule);

	IPage<IndustryRule> selectIndustryRulePage(IPage<IndustryRule> page, IndustryRule rule);

	boolean insertIndustryRule(IndustryRule rule);

	boolean updateIndustryRule(IndustryRule rule);

	boolean submitIndustryRule(IndustryRule rule);

	boolean deleteIndustryRuleByIds(String ids);

	Kv checkIndustry(String industryKeyword);

}
