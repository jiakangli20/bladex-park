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
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 物业服务工单日志.
 *
 * @author BladeX
 */
@Data
@TableName("biz_workorder_log")
@Schema(description = "物业服务工单日志")
public class WorkorderLog implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "log_id", type = IdType.AUTO)
	@Schema(description = "日志ID")
	private Long logId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "工单ID")
	private Long orderId;

	@Schema(description = "操作类型")
	private String action;

	@Schema(description = "操作描述")
	private String actionDesc;

	@Schema(description = "操作人")
	private String operator;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "操作时间")
	private Date operateTime;

}
