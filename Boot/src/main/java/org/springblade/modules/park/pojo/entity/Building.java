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
 * 建筑实体类
 *
 * @author Chill
 */
@Data
@TableName("ics_building")
@Schema(description = "建筑实体类")
public class Building implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.AUTO)
	@Schema(description = "主键")
	private Long id;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "所属园区ID")
	private Long parkId;

	@TableField(exist = false)
	@Schema(description = "所属园区")
	private String parkName;

	@Schema(description = "建筑名称")
	private String name;

	@Schema(description = "建筑编码")
	private String code;

	@Schema(description = "建筑地址")
	private String address;

	@Schema(description = "所属地区")
	private String region;

	@Schema(description = "不动产编号")
	private String realEstateNo;

	@Schema(description = "产权编号")
	private String propertyNo;

	@Schema(description = "土地编号")
	private String landNo;

	@Schema(description = "排序值")
	private Integer sortNum;

	@Schema(description = "楼层数")
	private Integer floors;

	@Schema(description = "建筑面积")
	private BigDecimal area;

	@Schema(description = "产权面积")
	private BigDecimal propertyArea;

	@Schema(description = "可租面积")
	private BigDecimal rentableArea;

	@Schema(description = "自用面积")
	private BigDecimal selfUseArea;

	@Schema(description = "配套面积")
	private BigDecimal supportingArea;

	@Schema(description = "车位面积")
	private BigDecimal parkingArea;

	@Schema(description = "标准层高")
	private BigDecimal standardFloorHeight;

	@Schema(description = "建筑类型")
	private String buildingType;

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
	@Schema(description = "管理面积")
	private BigDecimal managementArea;

	@TableField(exist = false)
	@Schema(description = "房源数量")
	private Integer roomCount;

	@TableField(exist = false)
	@Schema(description = "出租率")
	private BigDecimal rentRate;

	@TableField(exist = false)
	@Schema(description = "在租面积")
	private BigDecimal rentedArea;

	@TableField(exist = false)
	@Schema(description = "在租合同数")
	private Integer activeContractCount;

}
