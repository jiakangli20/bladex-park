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
 * 合同管理流程记录实体类
 *
 * @author Chill
 */
@Data
@TableName("biz_contract_workflow_record")
@Schema(description = "合同管理流程记录")
public class ContractWorkflowRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "record_id", type = IdType.AUTO)
	@Schema(description = "记录ID")
	private Long recordId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "业务类型")
	private String businessType;

	@Schema(description = "业务主键")
	private String businessKey;

	@Schema(description = "流程定义Key")
	private String processDefKey;

	@Schema(description = "流程定义ID")
	private String processDefId;

	@Schema(description = "流程名称")
	private String processName;

	@Schema(description = "流程实例ID")
	private String processInsId;

	@Schema(description = "流程状态")
	private String processStatus;

	@Schema(description = "当前节点Key")
	private String currentNodeKey;

	@Schema(description = "当前节点名称")
	private String currentNode;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "付款计划ID")
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "房源ID集合")
	private String roomIds;

	@Schema(description = "打印模板Key")
	private String templateKey;

	@Schema(description = "内置表单Key")
	private String formKey;

	@Schema(description = "表单数据快照")
	private String formDataJson;

	@Schema(description = "附件数据")
	private String attachmentJson;

	@Schema(description = "打印文件地址")
	private String printFileUrl;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "审批完成时间")
	private Date approvalTime;

	@Schema(description = "备注")
	private String remark;

	@Schema(description = "删除标志")
	private String delFlag;

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
	@Schema(description = "合同名称")
	private String contractName;

	@TableField(exist = false)
	@Schema(description = "客户名称")
	private String customerName;

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
	@Schema(description = "月租金")
	private BigDecimal monthlyRent;

	@TableField(exist = false)
	@Schema(description = "押金")
	private BigDecimal deposit;

	@TableField(exist = false)
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "合同开始日期")
	private Date startDate;

	@TableField(exist = false)
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "合同结束日期")
	private Date endDate;

}
