/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.home.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 首页待办提醒.
 *
 * @author BladeX
 */
@Data
@Schema(description = "首页待办提醒")
public class HomeTodoVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "审批待处理数量")
	private Long approvalTodoCount = 0L;

	@Schema(description = "即将到期合同数量")
	private Long expiringContractCount = 0L;

	@Schema(description = "物业工单待办数量")
	private Long workorderTodoCount = 0L;

	@Schema(description = "逾期内部通知未读数量")
	private Long overdueNoticeCount = 0L;

}
