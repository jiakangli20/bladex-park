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
package org.springblade.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springblade.core.launch.constant.AppConstant;
import org.springblade.modules.system.pojo.entity.ApiKeyLog;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.IsAdmin;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.system.pojo.vo.ApiKeyLogVO;
import org.springblade.modules.system.service.IApiKeyLogService;
import org.springblade.modules.system.wrapper.ApiKeyLogWrapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 令牌调用日志 控制器
 *
 * @author Chill
 */
@NonDS
@Hidden
@RestController
@AllArgsConstructor
@RequestMapping(AppConstant.APPLICATION_SYSTEM_NAME + "/api-key-log")
public class ApiKeyLogController {

	private final IApiKeyLogService logService;

	/**
	 * 查询单条
	 */
	@IsAdmin
	@GetMapping("/detail")
	public R<ApiKeyLog> detail(ApiKeyLog log) {
		QueryWrapper<ApiKeyLog> queryWrapper = Condition.getQueryWrapper(log);
		return R.data(queryWrapper.isEmptyOfWhere() ? null : logService.getOne(queryWrapper));
	}

	/**
	 * 查询多条(分页)
	 */
	@IsAdmin
	@GetMapping("/list")
	public R<IPage<ApiKeyLogVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> log, Query query) {
		IPage<ApiKeyLog> pages = logService.page(Condition.getPage(query.setDescs("create_time")), Condition.getQueryWrapper(log, ApiKeyLog.class));
		return R.data(ApiKeyLogWrapper.build().pageVO(pages));
	}

}
