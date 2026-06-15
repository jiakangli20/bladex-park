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
 * 客户标签类型实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_tag_type")
@Schema(description = "客户标签类型")
public class TagType implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "type_id", type = IdType.AUTO)
	@Schema(description = "类型ID")
	private Integer typeId;

	@NotBlank(message = "标签类型名称不能为空")
	@Length(max = 50, message = "标签类型名称不能超过50个字符")
	@Schema(description = "类型名称")
	private String typeName;

	@Schema(description = "排序")
	private Integer sortOrder;

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

	@TableField(exist = false)
	@Schema(description = "有效标签数量")
	private Integer tagCount;

}
