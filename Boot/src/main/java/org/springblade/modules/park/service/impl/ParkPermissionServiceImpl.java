package org.springblade.modules.park.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.mp.enums.StatusType;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springblade.modules.system.service.IUserParkService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ParkPermissionServiceImpl implements IParkPermissionService {
	private final IUserParkService userParkService;
	private final MiniMemberMapper miniMemberMapper;

	@Override
	public boolean hasAllParkAccess() {
		return !isMiniAppRequest() && (AuthUtil.isAdministrator() || AuthUtil.isAdmin());
	}

	@Override
	public List<Long> authorizedParkIds() {
		if (isMiniAppPublicRequest()) {
			return null;
		}
		if (isMiniAppRequest()) {
			return miniMemberMapper.selectList(Wrappers.<MiniMember>lambdaQuery()
				.eq(MiniMember::getTenantId, AuthUtil.getTenantId())
				.eq(MiniMember::getUserId, AuthUtil.getUserId())
				.eq(MiniMember::getStatus, StatusType.ACTIVE.getType())
				.eq(MiniMember::getIsDeleted, 0))
				.stream().map(MiniMember::getParkId).filter(Objects::nonNull).distinct().toList();
		}
		return hasAllParkAccess() ? null : userParkService.parkIds(AuthUtil.getUserId());
	}

	@Override
	public void requirePark(Long parkId) {
		if (parkId == null) throw new ServiceException("业务数据未关联园区，禁止访问");
		List<Long> ids = authorizedParkIds();
		if (ids != null && !ids.contains(parkId)) throw new ServiceException("无权访问该园区数据");
	}

	@Override
	public void requireAnyPark() {
		List<Long> ids = authorizedParkIds();
		if (ids != null && ids.isEmpty()) throw new ServiceException("当前用户未授权任何园区");
	}

	private boolean isMiniAppRequest() {
		String requestUri = WebUtil.getRequestURI();
		return requestUri != null && requestUri.startsWith("/blade-miniapp/");
	}

	private boolean isMiniAppPublicRequest() {
		String requestUri = WebUtil.getRequestURI();
		return requestUri != null && requestUri.startsWith("/blade-miniapp/public/");
	}
}
