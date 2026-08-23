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

/**
 * 小程序入驻意向形成的招商待办。
 */
@Data
@TableName("biz_settlement_todo")
@Schema(description = "招商待办")
public class SettlementTodo implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "todo_id", type = IdType.AUTO)
	private Long todoId;
	private String tenantId;
	private String todoNo;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long customerId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long memberId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long sourceRoomId;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long sourceOpportunityId;
	private String enterpriseName;
	private String creditCode;
	private String industryType;
	private String enterpriseScale;
	private BigDecimal intentArea;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private Date expectedEntryDate;
	private String contactName;
	private String contactPhone;
	private String demandDesc;
	private String todoStatus;
	@JsonSerialize(using = ToStringSerializer.class)
	private Long assigneeUserId;
	private String assigneeName;
	private String processRemark;
	private String rejectReason;
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date processedTime;
	private String createBy;
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;
	private String updateBy;
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date updateTime;
	private String delFlag;

	@TableField(exist = false)
	private String keyword;
}
