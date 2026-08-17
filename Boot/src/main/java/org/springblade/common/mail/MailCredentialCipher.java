/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

import org.springblade.core.log.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SMTP授权码加密组件.
 */
@Component
public class MailCredentialCipher {

	private static final int IV_LENGTH = 12;
	private static final int TAG_LENGTH = 128;
	private final SecureRandom secureRandom = new SecureRandom();
	private final SecretKeySpec secretKey;

	public MailCredentialCipher(@Value("${blade.api.crypto.aes-key}") String key) {
		try {
			byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
			this.secretKey = new SecretKeySpec(keyBytes, "AES");
		} catch (Exception exception) {
			throw new IllegalStateException("邮件授权码加密密钥初始化失败", exception);
		}
	}

	public String encrypt(String source) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
			byte[] encrypted = cipher.doFinal(source.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
				.put(iv).put(encrypted).array());
		} catch (Exception exception) {
			throw new ServiceException("邮件授权码加密失败");
		}
	}

	public String decrypt(String ciphertext) {
		try {
			byte[] payload = Base64.getDecoder().decode(ciphertext);
			ByteBuffer buffer = ByteBuffer.wrap(payload);
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] encrypted = new byte[buffer.remaining()];
			buffer.get(encrypted);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
			return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
		} catch (Exception exception) {
			throw new ServiceException("邮件授权码解密失败，请重新绑定邮箱");
		}
	}

}
