package org.springblade.common.mail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MailCredentialCipherTest {

	@Test
	void encryptsAndDecryptsAuthorizationCode() {
		MailCredentialCipher cipher = new MailCredentialCipher("test-mail-credential-key");
		String source = "smtp-authorization-code";

		String ciphertext = cipher.encrypt(source);

		assertNotEquals(source, ciphertext);
		assertEquals(source, cipher.decrypt(ciphertext));
	}

}
