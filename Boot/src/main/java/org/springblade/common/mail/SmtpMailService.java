/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.StringUtil;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * SMTP 邮件发送服务.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class SmtpMailService implements MailService {

	private final MailProperties mailProperties;

	@Override
	public void send(MailSenderAccount sender, MailMessage message) {
		validate(sender, message);
		try {
			JavaMailSenderImpl mailSender = createMailSender(sender);
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
			if (StringUtil.isBlank(sender.name())) {
				helper.setFrom(sender.address());
			} else {
				helper.setFrom(sender.address(), sender.name());
			}
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

	@Override
	public void testConnection(MailSenderAccount sender) {
		validateSender(sender);
		try {
			createMailSender(sender).testConnection();
		} catch (Exception exception) {
			throw new ServiceException("邮箱连接测试失败：" + rootMessage(exception));
		}
	}

	private JavaMailSenderImpl createMailSender(MailSenderAccount sender) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(mailProperties.getHost());
		if (mailProperties.getPort() != null) {
			mailSender.setPort(mailProperties.getPort());
		}
		mailSender.setProtocol(mailProperties.getProtocol());
		mailSender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
		mailSender.setUsername(sender.address());
		mailSender.setPassword(sender.authCode());
		Properties properties = new Properties();
		properties.putAll(mailProperties.getProperties());
		mailSender.setJavaMailProperties(properties);
		return mailSender;
	}

	private void validate(MailSenderAccount sender, MailMessage message) {
		validateSender(sender);
		if (message == null || message.recipients().isEmpty()) {
			throw new ServiceException("邮件收件人不能为空");
		}
		if (StringUtil.isBlank(message.subject())) {
			throw new ServiceException("邮件主题不能为空");
		}
		if (StringUtil.isBlank(message.content())) {
			throw new ServiceException("邮件正文不能为空");
		}
		for (MailAttachment attachment : message.attachments()) {
			if (attachment == null || StringUtil.isBlank(attachment.fileName())
				|| attachment.content() == null || attachment.content().length == 0) {
				throw new ServiceException("邮件附件不能为空");
			}
		}
	}

	private void validateSender(MailSenderAccount sender) {
		if (StringUtil.isBlank(mailProperties.getHost())) {
			throw new ServiceException("SMTP服务器未配置");
		}
		if (sender == null || StringUtil.isBlank(sender.address()) || StringUtil.isBlank(sender.authCode())) {
			throw new ServiceException("请先在个人中心绑定发件邮箱");
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
