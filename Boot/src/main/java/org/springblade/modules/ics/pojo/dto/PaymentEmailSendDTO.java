/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通知邮件发送请求.
 */
@Data
@Schema(description = "通知邮件发送请求")
public class PaymentEmailSendDTO {

	private Long paymentId;
	private String noticeType;
	private String recipientEmail;
	private String subject;
	private String content;

}
