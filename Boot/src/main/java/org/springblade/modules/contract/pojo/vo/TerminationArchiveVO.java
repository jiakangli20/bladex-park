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
 * 退租归档视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "退租归档视图对象")
public class TerminationArchiveVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "退租ID")
	private Long terminationId;

	@Schema(description = "退租单号")
	private String terminationNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@Schema(description = "合同名称")
	private String contractName;

	@Schema(description = "客户名称")
	private String customerName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "房源ID")
	private Long roomId;

	@Schema(description = "房源名称")
	private String roomName;

	@Schema(description = "应收金额")
	private BigDecimal receivableAmount;

	@Schema(description = "应退金额")
	private BigDecimal refundableAmount;

	@Schema(description = "合计金额")
	private BigDecimal totalAmount;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "退租时间")
	private Date terminationTime;

	@Schema(description = "退租原因")
	private String reason;

	@Schema(description = "结算状态")
	private Integer status;

	@Schema(description = "审批状态")
	private Integer approvalStatus;

	@Schema(description = "审批意见")
	private String approvalOpinion;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

}
