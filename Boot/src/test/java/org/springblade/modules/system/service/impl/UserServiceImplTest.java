package org.springblade.modules.system.service.impl;

import org.junit.jupiter.api.Test;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tenant.BladeTenantProperties;
import org.springblade.modules.auth.provider.UserType;
import org.springblade.modules.miniapp.mapper.MiniCustomerMemberMapper;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.service.IRoleService;
import org.springblade.modules.system.service.IUserDeptService;
import org.springblade.modules.system.service.IUserOauthService;
import org.springblade.modules.system.service.IUserParkService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

	@Test
	void miniappGuestDoesNotRequireBackendParkAssignment() {
		IRoleService roleService = mock(IRoleService.class);
		when(roleService.getRoleAliases("101")).thenReturn(List.of("mini_guest"));
		UserServiceImpl service = service(roleService);
		User user = new User();
		user.setUserType(UserType.OTHER.getCategory());
		user.setRoleId("101");

		assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "validateParkAssignment", user, true));
	}

	@Test
	void otherExternalRoleStillRequiresBackendParkAssignment() {
		IRoleService roleService = mock(IRoleService.class);
		when(roleService.getRoleAliases("102")).thenReturn(List.of("external_operator"));
		UserServiceImpl service = service(roleService);
		User user = new User();
		user.setUserType(UserType.OTHER.getCategory());
		user.setRoleId("102");

		assertThrows(ServiceException.class,
			() -> ReflectionTestUtils.invokeMethod(service, "validateParkAssignment", user, true));
	}

	private UserServiceImpl service(IRoleService roleService) {
		return new UserServiceImpl(
			mock(IUserDeptService.class),
			mock(IUserParkService.class),
			mock(IUserOauthService.class),
			roleService,
			mock(BladeTenantProperties.class),
			mock(MiniMemberMapper.class),
			mock(MiniCustomerMemberMapper.class));
	}
}
