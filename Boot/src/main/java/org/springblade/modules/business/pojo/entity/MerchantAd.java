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
 * 商户小程序广告.
 *
 * @author BladeX
 */
@Data
@TableName("biz_merchant_ad")
@Schema(description = "商户小程序广告")
public class MerchantAd implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "ad_id", type = IdType.AUTO)
	@Schema(description = "广告ID")
	private Long adId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@NotBlank(message = "广告标题不能为空")
	@Schema(description = "广告标题")
	private String adTitle;

	@NotBlank(message = "广告位置不能为空")
	@Schema(description = "广告位置")
	private String adPosition;

	@Schema(description = "封面图")
	private String coverUrl;

	@Schema(description = "跳转类型")
	private String linkType;

	@Schema(description = "跳转地址")
	private String linkUrl;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "关联商户ID")
	private Long merchantId;

	@Schema(description = "排序")
	private Integer sortOrder;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "开始展示时间")
	private Date startTime;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "结束展示时间")
	private Date endTime;

	@Schema(description = "状态")
	private String status;

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
	@Schema(description = "商户名称")
	private String merchantName;

}
