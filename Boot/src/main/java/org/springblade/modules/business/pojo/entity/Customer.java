/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.pojo.entity;

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
import java.util.List;

/**
 * 入驻客户实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_customer")
@Schema(description = "入驻客户")
public class Customer implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "customer_id", type = IdType.AUTO)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "企业名称")
	private String enterpriseName;

	@Schema(description = "统一社会信用代码")
	private String creditCode;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "成立日期")
	private Date establishDate;

	@Schema(description = "注册资本")
	private BigDecimal registeredCapital;

	@Schema(description = "企业类型")
	private String enterpriseType;

	@Schema(description = "所属行业")
	private String industry;

	@Schema(description = "经营范围")
	private String businessScope;

	@Schema(description = "注册地址")
	private String registeredAddress;

	@Schema(description = "股权结构")
	private String equityStructure;

	@Schema(description = "组织架构")
	private String organizationStructure;

	@Schema(description = "主营业务")
	private String mainBusiness;

	@Schema(description = "上年度营收")
	private BigDecimal lastYearRevenue;

	@Schema(description = "主要合作客户")
	private String majorClients;

	@Schema(description = "重大违法违规标识")
	private String majorIllegalFlag;

	@Schema(description = "失信记录标识")
	private String dishonestFlag;

	@Schema(description = "行业处罚标识")
	private String industryPenaltyFlag;

	@Schema(description = "意向载体类型")
	private String carrierTypes;

	@Schema(description = "意向面积")
	private BigDecimal intentArea;

	@Schema(description = "使用用途")
	private String usagePurpose;

	@Schema(description = "租赁期限")
	private BigDecimal leaseTermYears;

	@Schema(description = "租赁期限展示")
	private String leaseTermLabel;

	@Schema(description = "装修要求")
	private String decorationRequirement;

	@Schema(description = "配套需求")
	private String supportingNeeds;

	@Schema(description = "企业规模")
	private String scale;

	@Schema(description = "迁入地")
	private String migrationPlace;

	@Schema(description = "迁入原因")
	private String migrationReason;

	@Schema(description = "联系人姓名")
	private String contactName;

	@Schema(description = "联系人电话")
	private String contactPhone;

	@Schema(description = "联系人邮箱")
	private String contactEmail;

	@Schema(description = "联系人职务")
	private String contactPosition;

	@Schema(description = "招商渠道")
	private String channel;

	@Schema(description = "第三方渠道名称")
	private String thirdPartyChannelName;

	@Schema(description = "企业地址")
	private String address;

	@Schema(description = "合作等级")
	private Integer cooperationLevel;

	@Schema(description = "入驻状态")
	private Integer settlementStatus;

	@Schema(description = "入驻流程实例ID")
	private String tenantEntryProcessInsId;

	@Schema(description = "入驻流程状态")
	private String tenantEntryStatus;

	@Schema(description = "入驻流程当前节点")
	private String tenantEntryCurrentNode;

	@Schema(description = "入驻审批表文件地址")
	private String tenantEntryApprovalPdfUrl;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "入驻审批通过时间")
	private Date tenantEntryApprovalTime;

	@Schema(description = "客户状态")
	private String status;

	@Schema(description = "小程序OpenID")
	private String wxOpenid;

	@Schema(description = "微信UnionID")
	private String wxUnionid;

	@Schema(description = "备注")
	private String remark;

	@Schema(description = "基础核验状态")
	private String verifyStatus;

	@Schema(description = "基础核验说明")
	private String verifyMessage;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "基础核验时间")
	private Date verifyTime;

	@Schema(description = "行业准入结果")
	private String industryAccessResult;

	@Schema(description = "行业准入说明")
	private String industryAccessReason;

	@Schema(description = "风险等级")
	private String riskLevel;

	@Schema(description = "风险摘要")
	private String riskSummary;

	@Schema(description = "法律风险标识")
	private String legalRiskFlag;

	@Schema(description = "高管风险标识")
	private String executiveRiskFlag;

	@Schema(description = "股东风险标识")
	private String shareholderRiskFlag;

	@Schema(description = "删除标志")
	private String delFlag;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "创建人")
	private String createBy;

	@Schema(description = "更新人")
	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "更新时间")
	private Date updateTime;

	@TableField(exist = false)
	@Schema(description = "标签列表")
	private List<Tag> tags;

	@TableField(exist = false)
	@Schema(description = "标签ID列表")
	private List<Long> tagIds;

	@TableField(exist = false)
	@Schema(description = "查询关键词")
	private String keyword;

	@TableField(exist = false)
	@Schema(description = "创建开始时间")
	private String beginTime;

	@TableField(exist = false)
	@Schema(description = "创建结束时间")
	private String endTime;

}
