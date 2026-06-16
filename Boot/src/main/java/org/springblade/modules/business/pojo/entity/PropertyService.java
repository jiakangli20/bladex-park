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
 * 物业服务项.
 *
 * @author BladeX
 */
@Data
@TableName("biz_property_service")
@Schema(description = "物业服务项")
public class PropertyService implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "service_id", type = IdType.AUTO)
	@Schema(description = "服务ID")
	private Long serviceId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@NotBlank(message = "服务名称不能为空")
	@Schema(description = "服务名称")
	private String serviceName;

	@NotBlank(message = "服务类型不能为空")
	@Schema(description = "服务类型")
	private String serviceType;

	@Schema(description = "服务说明")
	private String serviceDesc;

	@Schema(description = "申请所需材料")
	private String requiredMaterials;

	@Schema(description = "服务流程说明")
	private String serviceFlow;

	@Schema(description = "收费标准")
	private String chargeStandard;

	@Schema(description = "服务时限")
	private Integer timeLimit;

	@Schema(description = "状态")
	private String status;

	@Schema(description = "排序")
	private Integer sortOrder;

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
