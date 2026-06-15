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
 * 合同归档视图对象
 *
 * @author Chill
 */
@Data
@Schema(description = "合同归档视图对象")
public class ContractArchiveVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@Schema(description = "合同编号")
	private String contractNo;

	@Schema(description = "合同名称")
	private String contractName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "审批项目ID")
	private Long projectId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "客户名称")
	private String customerName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "房源ID")
	private Long roomId;

	@Schema(description = "房源名称")
	private String roomName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "楼宇ID")
	private Long buildingId;

	@Schema(description = "楼宇名称")
	private String buildingName;

	@Schema(description = "租金单价")
	private BigDecimal rentPrice;

	@Schema(description = "租赁面积")
	private BigDecimal rentArea;

	@Schema(description = "月租金")
	private BigDecimal monthlyRent;

	@Schema(description = "物业费")
	private BigDecimal propertyFee;

	@Schema(description = "押金")
	private BigDecimal deposit;

	@Schema(description = "跟进人")
	private String followUser;

	@Schema(description = "租金递增节点")
	private String rentIncreaseNode;

	@Schema(description = "租金递增率")
	private BigDecimal rentIncreaseRate;

	@Schema(description = "租金递增单位")
	private String rentIncreaseUnit;

	@Schema(description = "滞纳金比例")
	private BigDecimal lateFeeRatio;

	@Schema(description = "滞纳金单位")
	private String lateFeeUnit;

	@Schema(description = "滞纳金上限")
	private BigDecimal lateFeeCap;

	@Schema(description = "管业管理费")
	private BigDecimal managementFee;

	@Schema(description = "公摊费")
	private BigDecimal publicFee;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "开始日期")
	private Date startDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "结束日期")
	private Date endDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "签约日期")
	private Date signDate;

	@Schema(description = "合同状态")
	private String contractStatus;

	@Schema(description = "合同状态名称")
	private String contractStatusName;

	@Schema(description = "缴费周期")
	private String paymentCycle;

	@Schema(description = "合同附件URL")
	private String contractFileUrl;

	@Schema(description = "备注")
	private String remark;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "园区名称")
	private String parkName;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "审批项目ID")
	private Long approvalProjectId;

	@Schema(description = "审批状态")
	private String approvalStatus;

	@Schema(description = "审批当前节点")
	private String approvalCurrentNodeName;

}
