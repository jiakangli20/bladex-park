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
package org.springblade.modules.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.contract.pojo.entity.ContractExpiryRule;

/**
 * 合同到期提醒规则服务类
 *
 * @author Chill
 */
public interface IContractExpiryRuleService extends IService<ContractExpiryRule> {

	/**
	 * 分页
	 *
	 * @param page 分页
	 * @param rule 查询条件
	 * @return 分页
	 */
	IPage<ContractExpiryRule> selectRulePage(IPage<ContractExpiryRule> page, ContractExpiryRule rule);

	/**
	 * 新增或修改
	 *
	 * @param rule 规则
	 * @return 是否成功
	 */
	boolean submitRule(ContractExpiryRule rule);

	/**
	 * 删除规则
	 *
	 * @param ids 主键集合
	 * @return 是否成功
	 */
	boolean removeRules(String ids);

}
