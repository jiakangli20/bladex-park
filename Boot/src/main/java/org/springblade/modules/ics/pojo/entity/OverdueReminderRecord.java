/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.entity;

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
 * 逾期催缴记录实体类.
 *
 * @author BladeX
 */
@Data
@TableName("biz_overdue_reminder_record")
@Schema(description = "逾期催缴记录")
public class OverdueReminderRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "record_id", type = IdType.AUTO)
	private Long recordId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long contractId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long operatorUserId;

	private String operatorAccount;

	private String operatorName;

	private String source;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date remindTime;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	private String delFlag;

	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;

}
