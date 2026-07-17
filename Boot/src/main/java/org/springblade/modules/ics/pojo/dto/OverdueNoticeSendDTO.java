/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首次逾期通知发送参数.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首次逾期通知发送参数")
public class OverdueNoticeSendDTO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "账单ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private Long paymentId;

	@Schema(description = "接收用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<Long> recipientUserIds = new ArrayList<>();

}
