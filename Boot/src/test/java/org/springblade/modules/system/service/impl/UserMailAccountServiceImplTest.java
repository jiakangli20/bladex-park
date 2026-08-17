package org.springblade.modules.system.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springblade.common.mail.MailCredentialCipher;
import org.springblade.common.mail.MailService;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.modules.system.mapper.UserMailAccountMapper;
import org.springblade.modules.system.pojo.dto.UserMailAccountDTO;
import org.springblade.modules.system.pojo.entity.UserMailAccount;
import org.springblade.modules.system.service.IUserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMailAccountServiceImplTest {

	private final UserMailAccountMapper mapper = mock(UserMailAccountMapper.class);
	private final IUserService userService = mock(IUserService.class);
	private final MailService mailService = mock(MailService.class);
	private final MailCredentialCipher cipher = new MailCredentialCipher("test-mail-account-key");
	private final UserMailAccountServiceImpl service = new UserMailAccountServiceImpl(
		mapper, userService, cipher, mailService
	);

	@Test
	void requiresNewAuthorizationCodeWhenEmailAddressChanges() {
		UserMailAccount saved = savedAccount("old@163.com", "old-auth-code");
		when(mapper.selectOne(any())).thenReturn(saved);
		UserMailAccountDTO request = new UserMailAccountDTO();
		request.setEmailAddress("new@163.com");
		request.setAuthCode("");

		try (MockedStatic<AuthUtil> auth = mockAuth()) {
			ServiceException exception = assertThrows(ServiceException.class, () -> service.saveCurrent(request));
			assertEquals("修改发件邮箱时请重新填写163 SMTP授权码", exception.getMessage());
		}

		verify(mapper, never()).updateById(any(UserMailAccount.class));
	}

	@Test
	void doesNotPersistTestStatusForUnsavedCredentials() {
		UserMailAccount saved = savedAccount("saved@163.com", "saved-auth-code");
		when(mapper.selectOne(any())).thenReturn(saved);
		UserMailAccountDTO request = new UserMailAccountDTO();
		request.setEmailAddress("draft@163.com");
		request.setAuthCode("draft-auth-code");

		try (MockedStatic<AuthUtil> auth = mockAuth()) {
			var result = service.testCurrent(request);
			assertEquals("success", result.getLastTestStatus());
			assertEquals("draft@163.com", result.getEmailAddress());
		}

		verify(mapper, never()).updateById(any(UserMailAccount.class));
	}

	private UserMailAccount savedAccount(String address, String authCode) {
		UserMailAccount account = new UserMailAccount();
		account.setAccountId(1L);
		account.setTenantId("000000");
		account.setUserId(10L);
		account.setEmailAddress(address);
		account.setAuthCodeCiphertext(cipher.encrypt(authCode));
		account.setEnabled("1");
		account.setDelFlag("0");
		return account;
	}

	private MockedStatic<AuthUtil> mockAuth() {
		MockedStatic<AuthUtil> auth = mockStatic(AuthUtil.class);
		auth.when(AuthUtil::getUserId).thenReturn(10L);
		auth.when(AuthUtil::getTenantId).thenReturn("000000");
		auth.when(AuthUtil::getUserName).thenReturn("tester");
		return auth;
	}

}
