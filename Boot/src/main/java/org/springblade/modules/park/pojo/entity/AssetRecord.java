/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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

/**
 * 资产登记台账.
 *
 * @author BladeX
 */
@Data
@TableName("biz_asset_record")
@Schema(description = "资产登记台账")
public class AssetRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "asset_id", type = IdType.AUTO)
	@Schema(description = "资产ID")
	private Long assetId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "楼宇ID")
	private Long buildingId;

	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	@Schema(description = "楼层")
	private Integer floorNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	@Schema(description = "房间ID")
	private Long roomId;

	@NotBlank(message = "资产编号不能为空")
	@Schema(description = "资产编号")
	private String assetCode;

	@NotBlank(message = "资产名称不能为空")
	@Schema(description = "资产名称")
	private String assetName;

	@NotBlank(message = "资产分类不能为空")
	@Schema(description = "资产分类")
	private String assetCategory;

	@Schema(description = "品牌型号")
	private String brandModel;

	@Schema(description = "数量")
	private Integer quantity;

	@Schema(description = "计量单位")
	private String unit;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "购置日期")
	private Date purchaseDate;

	@Schema(description = "资产原值")
	private BigDecimal originalValue;

	@Schema(description = "资产状态")
	private String assetStatus;

	@Schema(description = "责任人")
	private String responsiblePerson;

	@Schema(description = "安装位置补充")
	private String locationNote;

	@Schema(description = "备注")
	private String remark;

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
	@Schema(description = "园区名称")
	private String parkName;

	@TableField(exist = false)
	@Schema(description = "楼宇名称")
	private String buildingName;

	@TableField(exist = false)
	@Schema(description = "房间名称")
	private String roomName;

}
