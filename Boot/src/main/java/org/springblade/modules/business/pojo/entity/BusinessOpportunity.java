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
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 商机实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_business_opportunity")
@Schema(description = "商机")
public class BusinessOpportunity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "opportunity_id", type = IdType.AUTO)
	@Schema(description = "商机ID")
	private Long opportunityId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "商机编号")
	private String opportunityNo;

	@NotBlank(message = "企业名称不能为空")
	@Schema(description = "企业名称")
	private String enterpriseName;

	@NotBlank(message = "统一社会信用代码不能为空")
	@Schema(description = "统一社会信用代码")
	private String creditCode;

	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@JsonFormat(pattern = "yyyy-MM-dd")
	@Schema(description = "成立日期")
	private Date establishDate;

	@Schema(description = "注册资本")
	private BigDecimal registeredCapital;

	@Schema(description = "企业类型")
	private String enterpriseType;

	@Schema(description = "行业类型")
	private String industryType;

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

	@Schema(description = "有无重大违法违规记录")
	private String majorIllegalFlag;

	@Schema(description = "有无失信被执行记录")
	private String dishonestFlag;

	@Schema(description = "有无严重行业处罚记录")
	private String industryPenaltyFlag;

	@Schema(description = "意向载体类型")
	private String carrierTypes;

	@Schema(description = "意向面积")
	private BigDecimal intentArea;

	@Schema(description = "使用用途")
	private String usagePurpose;

	@Schema(description = "租赁期限")
	private BigDecimal leaseTermYears;

	@Schema(description = "租期分档")
	private String leaseTermLabel;

	@Schema(description = "装修要求")
	private String decorationRequirement;

	@Schema(description = "配套需求")
	private String supportingNeeds;

	@NotBlank(message = "负责人姓名不能为空")
	@Schema(description = "负责人姓名")
	private String contactName;

	@NotBlank(message = "联系电话不能为空")
	@Schema(description = "联系电话")
	private String contactPhone;

	@Schema(description = "邮箱")
	private String contactEmail;

	@Schema(description = "联系地址")
	private String contactAddress;

	@Schema(description = "身份证附件")
	private String idCardFiles;

	@Schema(description = "职务")
	private String contactPosition;

	@NotBlank(message = "招商渠道不能为空")
	@Schema(description = "招商渠道")
	private String channel;

	@Schema(description = "第三方渠道")
	private String thirdPartyChannelName;

	@Schema(description = "跟进人")
	private String followUser;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "跟进人用户ID")
	private Long followUserId;

	@Schema(description = "商机状态")
	private String opportunityStatus;

	@Schema(description = "备注")
	private String remark;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "审批项目ID")
	private Long approvalProjectId;

	@Schema(description = "是否提交审核")
	private String submittedAuditFlag;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "最后跟进时间")
	private Date lastFollowTime;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "下次跟进时间")
	private Date nextFollowTime;

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
	@Schema(description = "跟进记录")
	private List<BusinessOpportunityFollow> followList;

	@TableField(exist = false)
	@Schema(description = "附件列表")
	private List<BusinessOpportunityFile> fileList;

	@TableField(exist = false)
	@Schema(description = "标签列表")
	private List<Tag> tags;

	@TableField(exist = false)
	@Schema(description = "标签ID列表")
	private List<Long> tagIds;

	@TableField(exist = false)
	@Schema(description = "意向载体类型数组")
	private String[] carrierTypeArray;

	@TableField(exist = false)
	@Schema(description = "意向面积最小值")
	private BigDecimal intentAreaMin;

	@TableField(exist = false)
	@Schema(description = "意向面积最大值")
	private BigDecimal intentAreaMax;

	@TableField(exist = false)
	@Schema(description = "创建开始时间")
	private String beginTime;

	@TableField(exist = false)
	@Schema(description = "创建结束时间")
	private String endTime;

	@TableField(exist = false)
	@Schema(description = "企业名/联系人关键词")
	private String keyword;

	@TableField(exist = false)
	@Schema(description = "联系人/联系电话关键词")
	private String contactKeyword;

}
