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
package org.springblade.modules.ics.pojo.entity;

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
 * 楼层实体类
 *
 * @author Chill
 */
@Data
@TableName("ics_floor")
@Schema(description = "楼层实体类")
public class Floor implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.AUTO)
	@Schema(description = "主键")
	private Long id;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@TableField(exist = false)
	@Schema(description = "园区名称")
	private String parkName;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "建筑ID")
	private Long buildingId;

	@TableField(exist = false)
	@Schema(description = "建筑名称")
	private String buildingName;

	@Schema(description = "楼层号")
	private Integer floorNo;

	@Schema(description = "楼层面积")
	private BigDecimal area;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "备注")
	private String memo;

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
	@Schema(description = "已用面积")
	private BigDecimal usedArea;

	@TableField(exist = false)
	@Schema(description = "房源总数")
	private Integer totalCount;

	@TableField(exist = false)
	@Schema(description = "已租数量")
	private Integer rentedCount;

	@TableField(exist = false)
	@Schema(description = "空置数量")
	private Integer vacantCount;

	@TableField(exist = false)
	@Schema(description = "预定数量")
	private Integer reservedCount;

	@TableField(exist = false)
	@Schema(description = "装修中数量")
	private Integer renovatingCount;

	@TableField(exist = false)
	@Schema(description = "停用数量")
	private Integer disabledCount;

}
