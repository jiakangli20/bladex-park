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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商户增值服务处理单.
 *
 * @author BladeX
 */
@Data
@TableName("biz_merchant_service_order")
@Schema(description = "商户增值服务处理单")
public class MerchantServiceOrder implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "order_id", type = IdType.AUTO)
	@Schema(description = "服务单ID")
	private Long orderId;

	@Schema(description = "服务单号")
	private String orderNo;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "商户ID")
	private Long merchantId;

	@Schema(description = "服务商名称")
	private String merchantName;

	@Schema(description = "服务类型")
	private String serviceType;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@Schema(description = "客户名称")
	private String customerName;

	@Schema(description = "联系人")
	private String contactName;

	@Schema(description = "联系电话")
	private String contactPhone;

	@Schema(description = "服务范围")
	private String serviceScope;

	@Schema(description = "需求描述")
	private String demandDesc;

	@Schema(description = "需求图片")
	private String demandImages;

	@Schema(description = "服务单状态")
	private String orderStatus;

	@Schema(description = "优先级")
	private String priority;

	@Schema(description = "处理人")
	private String assignTo;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "指派时间")
	private Date assignTime;

	@Schema(description = "处理进展")
	private String processContent;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "下次跟进时间")
	private Date nextFollowTime;

	@Schema(description = "成交金额")
	private BigDecimal dealAmount;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "成交时间")
	private Date dealTime;

	@Schema(description = "关闭原因")
	private String closeReason;

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

	@TableField(exist = false)
	@Schema(description = "是否只看我的处理单")
	private Boolean mine;

	@TableField(exist = false)
	@Schema(description = "当前用户")
	private String currentUser;

}
