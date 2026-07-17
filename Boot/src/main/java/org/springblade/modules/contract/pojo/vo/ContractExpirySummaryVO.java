/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 合同到期提醒汇总.
 *
 * @author BladeX
 */
@Data
@Schema(description = "合同到期提醒汇总")
public class ContractExpirySummaryVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "进入提醒周期的合同数量")
	private Long totalCount = 0L;

	@Schema(description = "30天内到期数量")
	private Long within30Days = 0L;

	@Schema(description = "7天内到期数量")
	private Long within7Days = 0L;

}
