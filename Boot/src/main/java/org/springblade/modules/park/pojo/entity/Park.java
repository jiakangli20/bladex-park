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
 * 园区实体类
 *
 * @author Chill
 */
@Data
@TableName("ics_park")
@Schema(description = "园区实体类")
public class Park implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 主键
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "id", type = IdType.AUTO)
	@Schema(description = "主键")
	private Long id;

	/**
	 * 租户园区ID，迁移第一阶段保留原字段
	 */
	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "租户园区ID")
	private Long parkId;

	/**
	 * 园区名称
	 */
	@Schema(description = "园区名称")
	private String name;

	/**
	 * 园区编码
	 */
	@Schema(description = "园区编码")
	private String code;

	/**
	 * 地址
	 */
	@Schema(description = "地址")
	private String address;

	/**
	 * 占地面积
	 */
	@Schema(description = "占地面积")
	private BigDecimal area;

	/**
	 * 总房间数
	 */
	@Schema(description = "总房间数")
	private Integer totalRooms;

	/**
	 * 停车费
	 */
	@Schema(description = "停车费")
	private BigDecimal parkingFee;

	/**
	 * 纬度
	 */
	@Schema(description = "纬度")
	private BigDecimal latitude;

	/**
	 * 经度
	 */
	@Schema(description = "经度")
	private BigDecimal longitude;

	/**
	 * 最低租金
	 */
	@Schema(description = "最低租金")
	private BigDecimal rentMin;

	/**
	 * 最高租金
	 */
	@Schema(description = "最高租金")
	private BigDecimal rentMax;

	/**
	 * 租金单位
	 */
	@Schema(description = "租金单位")
	private String rentUnit;

	/**
	 * 所属地区
	 */
	@Schema(description = "所属地区")
	private String region;

	/**
	 * 详细地址
	 */
	@Schema(description = "详细地址")
	private String detailAddress;

	/**
	 * 园区小图
	 */
	@Schema(description = "园区小图")
	private String thumbnailUrl;

	/**
	 * 园区Banner
	 */
	@Schema(description = "园区Banner")
	private String bannerUrl;

	/**
	 * 园区简介
	 */
	@Schema(description = "园区简介")
	private String summary;

	/**
	 * 配套服务
	 */
	@Schema(description = "配套服务")
	private String supportingServices;

	/**
	 * 交通信息
	 */
	@Schema(description = "交通信息")
	private String trafficInfo;

	/**
	 * 园区图文介绍
	 */
	@Schema(description = "园区图文介绍")
	private String content;

	/**
	 * 联系人
	 */
	@Schema(description = "联系人")
	private String contactName;

	/**
	 * 联系电话
	 */
	@Schema(description = "联系电话")
	private String contactPhone;

	/**
	 * 状态，0启用，1停用
	 */
	@Schema(description = "状态")
	private String status;

	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String memo;

	/**
	 * 创建人
	 */
	@Schema(description = "创建人")
	private String createBy;

	/**
	 * 创建时间
	 */
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	/**
	 * 更新人
	 */
	@Schema(description = "更新人")
	private String updateBy;

	/**
	 * 更新时间
	 */
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "更新时间")
	private Date updateTime;

	/**
	 * 管理面积
	 */
	@TableField(exist = false)
	@Schema(description = "管理面积")
	private BigDecimal managementArea;

	/**
	 * 可招商面积
	 */
	@TableField(exist = false)
	@Schema(description = "可招商面积")
	private BigDecimal rentableArea;

	/**
	 * 总房源数量
	 */
	@TableField(exist = false)
	@Schema(description = "总房源数量")
	private Integer totalRoomCount;

	/**
	 * 可招商房源数量
	 */
	@TableField(exist = false)
	@Schema(description = "可招商房源数量")
	private Integer rentableRoomCount;

}
