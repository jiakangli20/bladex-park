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
 * 收款通知记录.
 *
 * @author BladeX
 */
@Data
@TableName("biz_payment_notice")
@Schema(description = "收款通知记录")
public class PaymentNotice implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@TableId(value = "notice_id", type = IdType.AUTO)
	@Schema(description = "通知ID")
	private Long noticeId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "账单ID")
	private Long paymentId;

	@Schema(description = "通知编号")
	private String noticeNo;

	@Schema(description = "通知类型")
	private String noticeType;

	@Schema(description = "短信发送状态")
	private String smsStatus;

	@Schema(description = "邮箱发送状态")
	private String emailStatus;

	@Schema(description = "站内信发送状态")
	private String inboxStatus;

	@Schema(description = "发送次数")
	private Integer sendCount;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "最后发送时间")
	private Date lastSendTime;

	@Schema(description = "文件名称")
	private String fileName;

	@Schema(description = "文件地址")
	private String fileUrl;

	@Schema(description = "小程序发送状态")
	private String miniappStatus;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "小程序发送时间")
	private Date miniappSendTime;

	@Schema(description = "备注")
	private String remark;

	@Schema(description = "删除标志")
	private String delFlag;

	@Schema(description = "创建人")
	private String createBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "更新人")
	private String updateBy;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	@Schema(description = "更新时间")
	private Date updateTime;

}
