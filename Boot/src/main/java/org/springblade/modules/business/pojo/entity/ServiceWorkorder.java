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
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物业服务工单.
 *
 * @author BladeX
 */
@Data
@TableName("biz_service_workorder")
@Schema(description = "物业服务工单")
public class ServiceWorkorder implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "order_id", type = IdType.AUTO)
	@Schema(description = "工单ID")
	private Long orderId;

	@Schema(description = "工单编号")
	private String orderNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "服务项ID")
	private Long serviceId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "客户名称")
	private String customerName;

	@Schema(description = "联系人")
	private String contactName;

	@Schema(description = "联系电话")
	private String contactPhone;

	@Schema(description = "房源ID")
	private String roomIds;

	@Schema(description = "房间信息")
	private String roomInfo;

	@Schema(description = "需求描述")
	private String demandDesc;

	@Schema(description = "需求图片")
	private String demandImages;

	@Schema(description = "工单状态")
	private String orderStatus;

	@Schema(description = "优先级")
	private String priority;

	@Schema(description = "指派人")
	private String assignTo;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "指派时间")
	private Date assignTime;

	@Schema(description = "处置内容")
	private String disposalContent;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "完成时间")
	private Date finishTime;

	@Schema(description = "评分")
	private Integer rating;

	@Schema(description = "评价内容")
	private String ratingContent;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "评价时间")
	private Date ratingTime;

	@Schema(description = "处理人")
	private String processor;

	@Schema(description = "处理说明")
	private String processRemark;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "下次跟进时间")
	private Date nextFollowTime;

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
	@Schema(description = "服务名称")
	private String serviceName;

	@TableField(exist = false)
	@Schema(description = "服务类型")
	private String serviceType;

	@TableField(exist = false)
	@Schema(description = "园区名称")
	private String parkName;

	@TableField(exist = false)
	@Schema(description = "是否只看我的工单")
	private Boolean mine;

	@TableField(exist = false)
	@Schema(description = "当前用户")
	private String currentUser;

	@TableField(exist = false)
	@Schema(description = "租控房源范围筛选，多个房间ID用英文逗号分隔")
	private String filterRoomIds;

}
