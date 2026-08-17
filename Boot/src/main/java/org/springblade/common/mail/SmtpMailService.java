/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * SMTP 邮件发送服务.
 */
@Service
@RequiredArgsConstructor
public class SmtpMailService implements MailService {

	private final JavaMailSender mailSender;

	@Value("${spring.mail.username:}")
	private String senderAddress;

	@Value("${spring.mail.password:}")
	private String smtpAuthCode;

	@Override
	public void send(MailMessage message) {
		validate(message);
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
			helper.setFrom(senderAddress);
			helper.setTo(message.recipients().toArray(String[]::new));
			helper.setSubject(message.subject());
			helper.setText(message.content(), message.html());
			for (MailAttachment attachment : message.attachments()) {
				helper.addAttachment(attachment.fileName(), new ByteArrayResource(attachment.content()), attachment.contentType());
			}
			mailSender.send(mimeMessage);
		} catch (Exception exception) {
			throw new ServiceException("邮件发送失败：" + rootMessage(exception));
		}
	}

	private void validate(MailMessage message) {
		if (StringUtil.isBlank(senderAddress) || StringUtil.isBlank(smtpAuthCode)) {
			throw new ServiceException("邮件服务未配置，请维护 PARK_MAIL_USERNAME 和 PARK_MAIL_SMTP_AUTH_CODE");
		}
		if (message == null || message.recipients().isEmpty()) {
			throw new ServiceException("邮件收件人不能为空");
		}
		if (StringUtil.isBlank(message.subject())) {
			throw new ServiceException("邮件主题不能为空");
		}
		for (MailAttachment attachment : message.attachments()) {
			if (attachment == null || StringUtil.isBlank(attachment.fileName())
				|| attachment.content() == null || attachment.content().length == 0) {
				throw new ServiceException("邮件附件不能为空");
			}
		}
	}

	private String rootMessage(Throwable throwable) {
		Throwable cause = throwable;
		while (cause.getCause() != null) {
			cause = cause.getCause();
		}
		return StringUtil.isBlank(cause.getMessage()) ? "SMTP 服务异常" : cause.getMessage();
	}

}
