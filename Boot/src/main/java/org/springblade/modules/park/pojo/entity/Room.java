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
package org.springblade.modules.park.pojo.entity;

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
 * 房源实体类
 *
 * @author Chill
 */
@Data
@TableName("ics_room")
@Schema(description = "房源实体类")
public class Room implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.AUTO)
	@Schema(description = "主键")
	private Long id;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "所属建筑ID")
	private Long buildingId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "所属园区ID")
	private Long parkId;

	@TableField(exist = false)
	@Schema(description = "所属建筑")
	private String buildingName;

	@TableField(exist = false)
	@Schema(description = "所属园区")
	private String parkName;

	@Schema(description = "房间名称")
	private String name;

	@Schema(description = "楼层")
	private Integer floor;

	@Schema(description = "面积")
	private BigDecimal area;

	@Schema(description = "月租金")
	private BigDecimal rentPrice;

	@Schema(description = "物业费")
	private BigDecimal propertyFee;

	@Schema(description = "户型")
	private String houseType;

	@Schema(description = "朝向")
	private String orientation;

	@Schema(description = "房源地址")
	private String address;

	@Schema(description = "配套设施")
	private String facilities;

	@Schema(description = "核心卖点")
	private String highlights;

	@Schema(description = "实景图")
	private String sceneImages;

	@Schema(description = "平面图")
	private String floorPlanImages;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "同步状态")
	private String syncStatus;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "最近同步时间")
	private Date syncTime;

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
	@Schema(description = "最小面积")
	private BigDecimal minArea;

	@TableField(exist = false)
	@Schema(description = "最大面积")
	private BigDecimal maxArea;

	@TableField(exist = false)
	@Schema(description = "最低租金")
	private BigDecimal minRentPrice;

	@TableField(exist = false)
	@Schema(description = "最高租金")
	private BigDecimal maxRentPrice;

}
