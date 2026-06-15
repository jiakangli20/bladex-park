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

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.mapper.ExpenseSettingsMapper;
import org.springblade.modules.contract.pojo.entity.ExpenseSettings;
import org.springblade.modules.contract.service.IExpenseSettingsService;
import org.springframework.stereotype.Service;

/**
 * 费项配置服务实现类
 *
 * @author Chill
 */
@Service
public class ExpenseSettingsServiceImpl extends ServiceImpl<ExpenseSettingsMapper, ExpenseSettings> implements IExpenseSettingsService {

	@Override
	public boolean submit(ExpenseSettings expenseSettings) {
		String userName = AuthUtil.getUserName();
		if (Func.isEmpty(expenseSettings.getId())) {
			expenseSettings.setCreateBy(userName);
			expenseSettings.setCreateTime(DateUtil.now());
			if (expenseSettings.getIsEnabled() == null) {
				expenseSettings.setIsEnabled(Boolean.FALSE);
			}
		} else {
			expenseSettings.setUpdateBy(userName);
			expenseSettings.setUpdateTime(DateUtil.now());
		}
		return saveOrUpdate(expenseSettings);
	}

}
