/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.miniapp.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 园区后台操作范围校验。
 *
 * <p>平台超级管理员保留全园区能力；普通后台账号必须具有与当前 BladeX 用户、
 * 租户和目标园区一致的 {@code mini_park_admin} 绑定，客户端传入的园区 ID 不作为授权依据。</p>
 *
 * @author Chill
 */
@Service
@RequiredArgsConstructor
public class AdminParkScopeService {

	private final MiniMemberMapper memberMapper;

	/**
	 * 校验当前后台用户是否可以操作目标园区。
	 *
	 * @param parkId 目标园区 ID
	 */
	public void assertAccess(Long parkId) {
		if (Func.isEmpty(parkId)) {
			throw new ServiceException("园区信息不能为空");
		}
		if (AuthUtil.isAdministrator()) {
			return;
		}
		Long userId = AuthUtil.getUserId();
		Long count = memberMapper.selectCount(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, AuthUtil.getTenantId())
			.eq(MiniMember::getUserId, userId)
			.eq(MiniMember::getParkId, parkId)
			.eq(MiniMember::getRoleCode, MiniAppConstant.ROLE_PARK_ADMIN)
			.eq(MiniMember::getStatus, StatusType.ACTIVE.getType())
			.eq(MiniMember::getIsDeleted, 0));
		if (count == null || count == 0L) {
			throw new ServiceException("当前账号没有该园区的管理权限");
		}
	}

	/** 当前后台账号可管理的园区；超级管理员返回空列表表示不限制。 */
	public List<Long> currentParkIds() {
		if (AuthUtil.isAdministrator()) return List.of();
		return memberMapper.selectList(Wrappers.<MiniMember>lambdaQuery()
			.eq(MiniMember::getTenantId, AuthUtil.getTenantId())
			.eq(MiniMember::getUserId, AuthUtil.getUserId())
			.eq(MiniMember::getRoleCode, MiniAppConstant.ROLE_PARK_ADMIN)
			.eq(MiniMember::getStatus, StatusType.ACTIVE.getType())
			.eq(MiniMember::getIsDeleted, 0)).stream().map(MiniMember::getParkId).distinct().toList();
	}
}
