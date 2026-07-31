/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 合同业务园区数据权限校验器.
 *
 * @author BladeX
 */
@Component
public class ContractParkAccessService {

	/**
	 * 将查询园区收口到当前账号所属园区.
	 *
	 * @param requestedParkId 前端传入园区
	 * @return 实际查询园区
	 */
	public Long scopedParkId(Long requestedParkId) {
		return requiresDataScope() ? currentParkId() : requestedParkId;
	}

	/**
	 * 校验当前账号是否可以访问指定园区数据.
	 *
	 * @param parkId 数据所属园区
	 */
	public void assertAccessible(Long parkId) {
		if (!requiresDataScope()) {
			return;
		}
		if (parkId == null || !Objects.equals(currentParkId(), parkId)) {
			throw new ServiceException("无权访问该园区合同数据");
		}
	}

	/**
	 * 当前账号所属园区ID，项目现有口径为部门ID对应园区ID.
	 */
	public Long currentParkId() {
		Long parkId = Func.firstLong(AuthUtil.getDeptId());
		if (Func.isEmpty(parkId)) {
			throw new ServiceException("当前账号未绑定所属园区");
		}
		return parkId;
	}

	private boolean requiresDataScope() {
		return !AuthUtil.isAdministrator() && Func.isNotEmpty(AuthUtil.getUserId());
	}

}
