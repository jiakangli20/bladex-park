/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.common.mail;

import java.util.List;

/**
 * 通用邮件消息.
 *
 * @param recipients 收件人
 * @param subject    主题
 * @param content    正文
 * @param html       是否为 HTML 正文
 * @param attachments 附件
 */
public record MailMessage(List<String> recipients, String subject, String content, boolean html,
						  List<MailAttachment> attachments) {

	public MailMessage {
		recipients = recipients == null ? List.of() : List.copyOf(recipients);
		attachments = attachments == null ? List.of() : List.copyOf(attachments);
	}

}
