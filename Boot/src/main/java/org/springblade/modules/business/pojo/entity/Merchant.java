/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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
 * 商户服务实体.
 *
 * @author BladeX
 */
@Data
@TableName("biz_merchant")
@Schema(description = "商户服务")
public class Merchant implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "merchant_id", type = IdType.AUTO)
	@Schema(description = "商户ID")
	private Long merchantId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@NotBlank(message = "服务商名称不能为空")
	@Schema(description = "服务商名称")
	private String merchantName;

	@NotBlank(message = "服务类型不能为空")
	@Schema(description = "服务类型")
	private String businessType;

	@Schema(description = "服务范围")
	private String serviceScope;

	@Schema(description = "服务区域")
	private String serviceArea;

	@Schema(description = "联系人")
	private String contactName;

	@Schema(description = "联系方式")
	private String contactPhone;

	@Schema(description = "地址")
	private String address;

	@Schema(description = "资质附件")
	private String qualificationFiles;

	@Schema(description = "备注")
	private String remark;

	@Schema(description = "状态")
	private String status;

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

}
