/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.service;

import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.Func;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 园区管理数据权限校验器。
 *
 * <p>项目现有账号口径为首个部门 ID 对应园区 ID；平台管理员可跨园区管理，
 * 其他登录账号只能访问其所属园区。</p>
 *
 * @author BladeX
 */
@Component
public class ParkDataAccessService {

	public Long scopedParkId(Long requestedParkId) {
		return requiresDataScope() ? currentParkId() : requestedParkId;
	}

	public void assertAccessible(Long parkId) {
		if (requiresDataScope() && (parkId == null || !Objects.equals(currentParkId(), parkId))) {
			throw new ServiceException("无权访问该园区数据");
		}
	}

	public Long currentParkId() {
		Long parkId = Func.firstLong(AuthUtil.getDeptId());
		if (Func.isEmpty(parkId)) {
			throw new ServiceException("当前账号未绑定所属园区");
		}
		return parkId;
	}

	public boolean requiresDataScope() {
		return !AuthUtil.isAdministrator() && Func.isNotEmpty(AuthUtil.getUserId());
	}

}
