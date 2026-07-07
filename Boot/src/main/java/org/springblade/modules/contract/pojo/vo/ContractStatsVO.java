/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.contract.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 合同统计视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "合同统计")
public class ContractStatsVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "合同总数")
	private Long totalCount;

	@Schema(description = "待审批数量")
	private Long pendingCount;

	@Schema(description = "生效数量")
	private Long activeCount;

	@Schema(description = "已到期数量")
	private Long expiredCount;

	@Schema(description = "已续签数量")
	private Long renewedCount;

	@Schema(description = "已退租数量")
	private Long terminatedCount;

	@Schema(description = "月租金合计")
	private BigDecimal monthlyRentTotal;

	@Schema(description = "押金合计")
	private BigDecimal depositTotal;

}
