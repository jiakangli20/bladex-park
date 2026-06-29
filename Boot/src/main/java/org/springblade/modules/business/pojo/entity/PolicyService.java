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
import java.util.Date;

/**
 * 政策服务发布.
 *
 * @author BladeX
 */
@Data
@TableName("biz_policy_service")
@Schema(description = "政策服务发布")
public class PolicyService implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "policy_id", type = IdType.AUTO)
	@Schema(description = "政策ID")
	private Long policyId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@NotBlank(message = "服务标题不能为空")
	@Schema(description = "服务标题")
	private String serviceTitle;

	@Schema(description = "项目范围")
	private String projectScope;

	@Schema(description = "发布状态")
	private String serviceStatus;

	@Schema(description = "浏览总数")
	private Long viewCount;

	@Schema(description = "是否永久有效")
	private String permanentFlag;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "有效期")
	private Date validTime;

	@Schema(description = "上架状态")
	private String onlineFlag;

	@Schema(description = "封面图")
	private String coverUrl;

	@Schema(description = "政策内容")
	private String content;

	@Schema(description = "附件")
	private String attachmentFiles;

	@Schema(description = "排序")
	private Integer sortOrder;

	@Schema(description = "备注")
	private String remark;

	@Schema(description = "删除标志")
	private String delFlag;

	@Schema(description = "创建者")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "更新者")
	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "更新时间")
	private Date updateTime;

	@TableField(exist = false)
	@Schema(description = "园区名称")
	private String parkName;

}
