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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同变更归档视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "合同变更归档视图对象")
public class ContractChangeArchiveVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "变更ID")
	private Long changeId;

	@Schema(description = "变更单号")
	private String changeNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@Schema(description = "合同编号")
	private String contractNo;

	@Schema(description = "合同名称")
	private String contractName;

	@Schema(description = "客户名称")
	private String customerName;

	@Schema(description = "变更类型")
	private String changeType;

	@Schema(description = "原租金单价")
	private BigDecimal oldRentPrice;

	@Schema(description = "新租金单价")
	private BigDecimal newRentPrice;

	@Schema(description = "原月租金")
	private BigDecimal oldMonthlyRent;

	@Schema(description = "新月租金")
	private BigDecimal newMonthlyRent;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "原结束日期")
	private Date oldEndDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "新结束日期")
	private Date newEndDate;

	@Schema(description = "变更原因")
	private String reason;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "审批意见")
	private String approvalOpinion;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

}
