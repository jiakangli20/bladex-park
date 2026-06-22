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
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 行业准入规则实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_industry_rule")
@Schema(description = "行业准入规则")
public class IndustryRule implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "rule_id", type = IdType.AUTO)
	@Schema(description = "规则ID")
	private Long ruleId;

	@NotBlank(message = "行业关键词不能为空")
	@Length(max = 100, message = "行业关键词不能超过100个字符")
	@Schema(description = "行业关键词")
	private String industryKeyword;

	@NotBlank(message = "准入类型不能为空")
	@Schema(description = "准入类型：2限制 3禁入")
	private String accessType;

	@Length(max = 500, message = "规则说明不能超过500个字符")
	@Schema(description = "规则说明")
	private String reason;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "删除标志")
	private String delFlag;

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

}
