/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.pojo.entity;

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
 * 审批项目实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_approval_project")
@Schema(description = "审批项目")
public class ApprovalProject implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "project_id", type = IdType.AUTO)
	@Schema(description = "项目ID")
	private Long projectId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "流程ID")
	private Long flowId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "业务ID")
	private Long businessId;

	@Schema(description = "业务类型")
	private String businessType;

	@Schema(description = "项目名称")
	private String projectName;

	@Schema(description = "企业名称")
	private String enterpriseName;

	@Schema(description = "统一社会信用代码")
	private String creditCode;

	@Schema(description = "发起人")
	private String applicant;

	@Schema(description = "发起部门")
	private String applicantDept;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "发起时间")
	private Date applicantTime;

	@Schema(description = "股东信息")
	private String shareholderInfo;

	@Schema(description = "经营范围")
	private String businessScope;

	@Schema(description = "负责人")
	private String responsiblePerson;

	@Schema(description = "联系方式")
	private String contactPhone;

	@Schema(description = "租赁楼层")
	private String leaseFloor;

	@Schema(description = "租赁面积")
	private BigDecimal leaseArea;

	@Schema(description = "租金单价")
	private BigDecimal rentPrice;

	@Schema(description = "保证金")
	private BigDecimal deposit;

	@Schema(description = "免租期")
	private String rentFreePeriod;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "租赁开始日期")
	private Date leaseStartDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "租赁结束日期")
	private Date leaseEndDate;

	@Schema(description = "当前节点审批人")
	private String currentNode;

	@Schema(description = "当前节点名称")
	private String currentNodeName;

	@Schema(description = "流程状态")
	private String processStatus;

	@Schema(description = "审批结果")
	private String approvalResult;

	@Schema(description = "摘要")
	private String summary;

	@Schema(description = "审批事项")
	private String approvalMatter;

	@Schema(description = "审批表JSON")
	private String approvalFormJson;

	@Schema(description = "审批表地址")
	private String approvalFormUrl;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "归档时间")
	private Date archiveTime;

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
	@Schema(description = "资料数")
	private Integer materialCount;

	@TableField(exist = false)
	@Schema(description = "日志数")
	private Integer logCount;

	@TableField(exist = false)
	@Schema(description = "查询范围")
	private String scope;

	@TableField(exist = false)
	@Schema(description = "排除状态")
	private String excludeProcessStatus;

	@TableField(exist = false)
	@Schema(description = "当前用户")
	private String currentUser;

	@TableField(exist = false)
	@Schema(description = "是否管理员")
	private Boolean admin;

}
