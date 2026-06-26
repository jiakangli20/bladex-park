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
package org.springblade.modules.contract.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
 * 合同缴费计划实体类
 *
 * @author Chill
 */
@Data
@TableName("biz_contract_payment")
@Schema(description = "合同缴费计划")
public class ContractPayment implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "payment_id", type = IdType.AUTO)
	@Schema(description = "缴费ID")
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@Schema(description = "费用类型")
	private String feeType;

	@Schema(description = "费用名称")
	private String feeName;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "账期开始")
	private Date periodStart;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "账期结束")
	private Date periodEnd;

	@Schema(description = "应收金额")
	private BigDecimal amountDue;

	@Schema(description = "实收金额")
	private BigDecimal amountPaid;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "应缴日期")
	private Date payDeadline;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "实缴时间")
	private Date payTime;

	@Schema(description = "缴费状态")
	private String payStatus;

	@Schema(description = "催缴状态")
	private String remindStatus;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "催缴时间")
	private Date remindTime;

	@Schema(description = "备注")
	private String remark;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "更新人")
	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "更新时间")
	private Date updateTime;

	@TableField(exist = false)
	@Schema(description = "合同编号")
	private String contractNo;

	@TableField(exist = false)
	@Schema(description = "客户名称")
	private String customerName;

	@TableField(exist = false)
	@Schema(description = "合同名称")
	private String contractName;

	@TableField(exist = false)
	@Schema(description = "房源名称")
	private String roomName;

	@TableField(exist = false)
	@Schema(description = "楼宇名称")
	private String buildingName;

	@TableField(exist = false)
	@Schema(description = "合同状态")
	private String contractStatus;

	@TableField(exist = false)
	@Schema(description = "合同状态名称")
	private String contractStatusName;

	@TableField(exist = false)
	@Schema(description = "付款流程实例ID")
	private String paymentProcessInsId;

	@TableField(exist = false)
	@Schema(description = "付款审批状态")
	private String paymentApprovalStatus;

	@TableField(exist = false)
	@Schema(description = "付款审批当前节点")
	private String paymentCurrentNodeName;

	@TableField(exist = false)
	@Schema(description = "付款审批文件地址")
	private String paymentFileUrl;

	@TableField(exist = false)
	@Schema(description = "逾期律师函流程实例ID")
	private String overdueProcessInsId;

	@TableField(exist = false)
	@Schema(description = "逾期律师函审批状态")
	private String overdueApprovalStatus;

	@TableField(exist = false)
	@Schema(description = "逾期律师函当前节点")
	private String overdueCurrentNodeName;

	@TableField(exist = false)
	@Schema(description = "逾期律师函文件地址")
	private String overdueFileUrl;

	@TableField(exist = false)
	@Schema(description = "退租流程实例ID")
	private String terminationProcessInsId;

	@TableField(exist = false)
	@Schema(description = "退租审批状态")
	private String terminationApprovalStatus;

	@TableField(exist = false)
	@Schema(description = "退租审批当前节点")
	private String terminationCurrentNodeName;

	@TableField(exist = false)
	@Schema(description = "退租审批文件地址")
	private String terminationFileUrl;

	@TableField(exist = false)
	@Schema(description = "账单方向：receivable 收款，payable 付款")
	private String direction;

	@TableField(exist = false)
	@Schema(description = "应收/应付开始日期")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date deadlineStartDate;

	@TableField(exist = false)
	@Schema(description = "应收/应付结束日期")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date deadlineEndDate;

	@TableField(exist = false)
	@Schema(description = "隐藏未到应收/应付期账单")
	private Boolean hideFuture;

	@TableField(exist = false)
	@Schema(description = "账期开始日期-起")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodStartBegin;

	@TableField(exist = false)
	@Schema(description = "账期开始日期-止")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodStartEnd;

	@TableField(exist = false)
	@Schema(description = "账期结束日期-起")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodEndBegin;

	@TableField(exist = false)
	@Schema(description = "账期结束日期-止")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date periodEndEnd;

	@TableField(exist = false)
	@Schema(description = "创建开始日期")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date createStartDate;

	@TableField(exist = false)
	@Schema(description = "创建结束日期")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date createEndDate;

	@TableField(exist = false)
	@Schema(description = "楼宇名称查询")
	private String buildingNameQuery;

	@TableField(exist = false)
	@Schema(description = "结清状态：settled 已结清，unsettled 未结清")
	private String settleStatus;

}
