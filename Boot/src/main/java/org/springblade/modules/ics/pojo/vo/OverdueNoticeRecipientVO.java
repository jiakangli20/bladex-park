/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首次逾期通知收件人候选.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首次逾期通知收件人候选")
public class OverdueNoticeRecipientVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "用户ID")
	private Long userId;

	@Schema(description = "账号")
	private String account;

	@Schema(description = "用户姓名")
	private String userName;

	@Schema(description = "所属部门")
	private String deptName;

	@Schema(description = "所属角色")
	private String roleNames;

	@Schema(description = "建议通知职责")
	private String suggestedRoles;

	@Schema(description = "是否默认选中")
	private Boolean defaultSelected;

	@Schema(description = "是否已发送")
	private Boolean alreadySent;

}
