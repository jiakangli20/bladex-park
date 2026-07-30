/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
 * 收付款账单逐笔确认记录.
 *
 * @author BladeX
 */
@Data
@TableName("biz_contract_payment_record")
@Schema(description = "收付款账单逐笔确认记录")
public class ContractPaymentRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "record_id", type = IdType.AUTO)
	@Schema(description = "记录ID")
	private Long recordId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "账单ID")
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@Schema(description = "本次收付款金额")
	private BigDecimal paymentAmount;

	@Schema(description = "确认后累计收付款金额")
	private BigDecimal cumulativeAmount;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "收付款时间")
	private Date paymentTime;

	@Schema(description = "收付款凭证名称")
	private String voucherName;

	@Schema(description = "收付款凭证地址")
	private String voucherUrl;

	@Schema(description = "收付款备注")
	private String remark;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "操作人用户ID")
	private Long operatorUserId;

	@Schema(description = "操作人账号")
	private String operatorAccount;

	@Schema(description = "操作人姓名")
	private String operatorName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "删除标志")
	private String delFlag;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

}
