/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 律师函发送登记参数.
 */
@Data
@Schema(description = "律师函发送登记参数")
public class LegalLetterSendDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long paymentId;

	@Schema(description = "发送方式", requiredMode = Schema.RequiredMode.REQUIRED)
	private String channel;

	@Schema(description = "收件人", requiredMode = Schema.RequiredMode.REQUIRED)
	private String recipient;

	@Schema(description = "收件地址、邮箱或联系方式")
	private String destination;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "发送时间", requiredMode = Schema.RequiredMode.REQUIRED)
	private Date sendTime;

	@Schema(description = "发送凭证地址")
	private String proofUrl;

	@Schema(description = "备注")
	private String remark;

}
