/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

/**
 * 通用邮件发送服务.
 */
public interface MailService {

	/**
	 * 发送邮件.
	 *
	 * @param sender  发件账号
	 * @param message 邮件消息
	 */
	void send(MailSenderAccount sender, MailMessage message);

	/**
	 * 测试发件账号连接.
	 *
	 * @param sender 发件账号
	 */
	void testConnection(MailSenderAccount sender);

}
