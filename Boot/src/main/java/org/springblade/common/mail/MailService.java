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
	 * @param message 邮件消息
	 */
	void send(MailMessage message);

}
