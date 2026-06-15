/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.pojo.entity;

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
import java.util.Date;

/**
 * 商机跟进记录实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_business_opportunity_follow")
@Schema(description = "商机跟进记录")
public class BusinessOpportunityFollow implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "follow_id", type = IdType.AUTO)
	@Schema(description = "跟进ID")
	private Long followId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "商机ID")
	private Long opportunityId;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "跟进时间")
	private Date followTime;

	@Schema(description = "跟进人")
	private String followUser;

	@Schema(description = "商机状态")
	private String opportunityStatus;

	@Schema(description = "跟进内容")
	private String followContent;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "下次跟进时间")
	private Date nextFollowTime;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

}
