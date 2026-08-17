/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知邮件编写初始化数据.
 */
@Data
@Schema(description = "通知邮件编写初始化数据")
public class PaymentEmailComposeVO {

	private Long paymentId;
	private String noticeType;
	private String senderEmail;
	private Boolean senderConfigured;
	private String recipientEmail;
	private String subject;
	private String content;
	private String attachmentName;
	private String attachmentUrl;

}
