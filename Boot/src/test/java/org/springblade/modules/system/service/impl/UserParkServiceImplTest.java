package org.springblade.modules.system.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.park.mapper.ParkMapper;
import org.springblade.modules.system.mapper.UserParkMapper;
import org.springblade.modules.system.pojo.entity.UserPark;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserParkServiceImplTest {

	@Test
	void loadsPageAssignmentsWithSingleQuery() {
		UserParkMapper userParkMapper = mock(UserParkMapper.class);
		UserParkServiceImpl service = new UserParkServiceImpl(mock(ParkMapper.class));
		ReflectionTestUtils.setField(service, "baseMapper", userParkMapper);
		UserPark assignment = new UserPark();
		assignment.setUserId(100L);
		assignment.setParkId(1L);
		when(userParkMapper.selectList(any())).thenReturn(List.of(assignment));

		org.junit.jupiter.api.Assertions.assertEquals(List.of(assignment), service.userParks(List.of(100L, 200L)));
		verify(userParkMapper, times(1)).selectList(any());
	}

	@Test
	void ordinaryOperatorCannotGrantParkOutsideOwnScope() {
		UserParkMapper userParkMapper = mock(UserParkMapper.class);
		UserParkServiceImpl service = new UserParkServiceImpl(mock(ParkMapper.class));
		ReflectionTestUtils.setField(service, "baseMapper", userParkMapper);
		UserPark ownPark = new UserPark();
		ownPark.setParkId(1L);
		when(userParkMapper.selectList(any())).thenReturn(List.of(ownPark));

		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class)) {
			auth.when(AuthUtil::getTenantId).thenReturn("000000");
			auth.when(AuthUtil::getUserId).thenReturn(100L);

			assertThrows(ServiceException.class,
				() -> service.saveUserParks(200L, "000000", List.of(1L, 2L), 1L));
		}
	}

	@Test
	void ordinaryOperatorCannotConfigureAnotherTenant() {
		UserParkServiceImpl service = new UserParkServiceImpl(mock(ParkMapper.class));
		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class)) {
			auth.when(AuthUtil::getTenantId).thenReturn("000000");

			assertThrows(ServiceException.class,
				() -> service.saveUserParks(200L, "111111", List.of(1L), 1L));
		}
	}
}
