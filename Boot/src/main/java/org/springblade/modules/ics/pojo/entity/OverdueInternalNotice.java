/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
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
 * 逾期内部通知实体类.
 *
 * @author Chill
 */
@Data
@TableName("biz_overdue_internal_notice")
@Schema(description = "逾期内部通知")
public class OverdueInternalNotice implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "notice_id", type = IdType.AUTO)
	private Long noticeId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long contractId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long recipientUserId;

	private String recipientAccount;

	private String recipientName;

	private String recipientRoles;

	private String noticeTitle;

	private String noticeContent;

	private String readStatus;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date readTime;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long parkId;

	private String delFlag;

	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;

}
