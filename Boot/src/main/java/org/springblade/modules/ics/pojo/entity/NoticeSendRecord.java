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
import lombok.Data;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知发送明细记录.
 */
@Data
@TableName("biz_notice_send_record")
public class NoticeSendRecord implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@TableId(value = "record_id", type = IdType.AUTO)
	@JsonSerialize(using = ToStringSerializer.class)
	private Long recordId;

	private String tenantId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long noticeId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long contractId;

	private String noticeType;
	private String channel;

	@JsonSerialize(using = ToStringSerializer.class)
	private Long senderUserId;

	private String senderName;
	private String senderEmail;
	private String recipientEmail;
	private String subject;
	private String contentSnapshot;
	private String attachmentName;
	private String attachmentUrl;
	private String sendStatus;
	private String failureReason;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date sentTime;

	private String delFlag;
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date createTime;

	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date updateTime;

}
