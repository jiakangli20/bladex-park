package org.springblade.modules.park.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.WebUtil;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.system.service.IUserParkService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParkPermissionServiceImplTest {

	private final IUserParkService userParkService = mock(IUserParkService.class);
	private final MiniMemberMapper miniMemberMapper = mock(MiniMemberMapper.class);
	private final ParkPermissionServiceImpl service = new ParkPermissionServiceImpl(userParkService, miniMemberMapper);

	@Test
	void backendUserUsesExplicitUserParkAssignments() {
		when(userParkService.parkIds(100L)).thenReturn(List.of(1L, 2L));
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class);
			 MockedStatic<WebUtil> web = mockStatic(WebUtil.class)) {
			auth.when(AuthUtil::getUserId).thenReturn(100L);
			web.when(WebUtil::getRequestURI).thenReturn("/blade-contract/contract/list");

			assertEquals(List.of(1L, 2L), service.authorizedParkIds());
			verify(miniMemberMapper, never()).selectList(any());
		}
	}

	@Test
	void backendAdministratorHasAllParkAccess() {
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class);
			 MockedStatic<WebUtil> web = mockStatic(WebUtil.class)) {
			auth.when(AuthUtil::isAdministrator).thenReturn(true);
			web.when(WebUtil::getRequestURI).thenReturn("/blade-park/park/list");

			assertNull(service.authorizedParkIds());
			verify(userParkService, never()).parkIds(any());
		}
	}

	@Test
	void miniappRequestUsesMemberParksEvenForBladeAdministrator() {
		MiniMember first = member(3L);
		MiniMember duplicate = member(3L);
		MiniMember second = member(4L);
		when(miniMemberMapper.selectList(any())).thenReturn(List.of(first, duplicate, second));
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class);
			 MockedStatic<WebUtil> web = mockStatic(WebUtil.class)) {
			auth.when(AuthUtil::isAdministrator).thenReturn(true);
			auth.when(AuthUtil::getTenantId).thenReturn("000000");
			auth.when(AuthUtil::getUserId).thenReturn(100L);
			web.when(WebUtil::getRequestURI).thenReturn("/blade-miniapp/admin/work-orders");

			assertEquals(List.of(3L, 4L), service.authorizedParkIds());
			verify(userParkService, never()).parkIds(any());
		}
	}

	@Test
	void miniappPublicRequestCanReadAllPublicParksWithoutMemberIdentity() {
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class);
			 MockedStatic<WebUtil> web = mockStatic(WebUtil.class)) {
			web.when(WebUtil::getRequestURI).thenReturn("/blade-miniapp/public/houses");

			assertNull(service.authorizedParkIds());
			verify(miniMemberMapper, never()).selectList(any());
			verify(userParkService, never()).parkIds(any());
		}
	}

	private MiniMember member(Long parkId) {
		MiniMember member = new MiniMember();
		member.setParkId(parkId);
		return member;
	}
}
