/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.park.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
 * 智能设备台账.
 *
 * @author BladeX
 */
@Data
@TableName("biz_smart_device")
@Schema(description = "智能设备台账")
public class SmartDevice implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "device_id", type = IdType.AUTO)
	@Schema(description = "设备ID")
	private Long deviceId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "楼宇ID")
	private Long buildingId;

	@Schema(description = "楼层")
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private Integer floorNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "房间ID")
	@TableField(updateStrategy = FieldStrategy.ALWAYS)
	private Long roomId;

	@NotBlank(message = "设备名称不能为空")
	@Schema(description = "设备名称")
	private String deviceName;

	@NotBlank(message = "设备编码不能为空")
	@Schema(description = "设备编码")
	private String deviceCode;

	@NotBlank(message = "设备类型不能为空")
	@Schema(description = "设备类型")
	private String deviceType;

	@Schema(description = "品牌")
	private String brand;

	@Schema(description = "型号")
	private String deviceModel;

	@Schema(description = "付费类型：prepaid预付费、postpaid后付费")
	private String paymentType;

	@Schema(description = "表类型：branch分表、total总表、public公摊表")
	private String meterType;

	@Schema(description = "用途")
	private String purpose;

	@Schema(description = "当前读数")
	private BigDecimal currentReading;

	@Schema(description = "当前余额")
	private BigDecimal currentBalance;

	@Schema(description = "倍率")
	private BigDecimal multiplier;

	@Schema(description = "最大读数")
	private BigDecimal maxReading;

	@Schema(description = "预警单位")
	private String warningUnit;

	@Schema(description = "预警规则JSON")
	private String warningRules;

	@Schema(description = "安装位置")
	private String installLocation;

	@Schema(description = "在线状态：0在线、1离线")
	private String onlineStatus;

	@Schema(description = "启用状态：0启用、1停用")
	private String enabledStatus;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "最后在线时间")
	private Date lastOnlineTime;

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

	@TableField(exist = false)
	@Schema(description = "设备分组筛选")
	private String deviceTypeGroup;

}
