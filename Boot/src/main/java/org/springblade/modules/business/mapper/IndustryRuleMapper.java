/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.business.pojo.entity.IndustryRule;

import java.util.List;

/**
 * 行业准入规则 Mapper.
 *
 * @author BladeX
 */
public interface IndustryRuleMapper extends BaseMapper<IndustryRule> {

	IndustryRule selectIndustryRuleById(@Param("ruleId") Long ruleId);

	List<IndustryRule> selectIndustryRuleList(@Param("rule") IndustryRule rule);

	IPage<IndustryRule> selectIndustryRulePage(IPage<IndustryRule> page, @Param("rule") IndustryRule rule);

	IndustryRule selectSameKeywordRule(@Param("industryKeyword") String industryKeyword,
									   @Param("excludeRuleId") Long excludeRuleId);

	int insertIndustryRule(IndustryRule rule);

	int updateIndustryRule(IndustryRule rule);

	int deleteIndustryRuleByIds(@Param("ruleIds") List<Long> ruleIds);

}
