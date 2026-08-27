package org.springblade.modules.ai.controller;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.service.IAiChatService;
import org.springblade.modules.park.service.IParkPermissionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class AiChatControllerTest {

	@Test
	void capturesAuthorizedParksBeforeStartingAsyncWork() {
		IParkPermissionService parkPermissionService = mock(IParkPermissionService.class);
		when(parkPermissionService.authorizedParkIds()).thenReturn(List.of(2001L));
		AiChatController controller = new AiChatController(mock(IAiChatService.class), parkPermissionService);

		try (MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class)) {
			auth.when(AuthUtil::getUserId).thenReturn(100L);
			auth.when(AuthUtil::getTenantId).thenReturn("000000");

			AiAccessContext context = controller.captureAccessContext();

			assertEquals(100L, context.userId());
			assertEquals("000000", context.tenantId());
			assertEquals(List.of(2001L), context.authorizedParkIds());
		}
	}
}
