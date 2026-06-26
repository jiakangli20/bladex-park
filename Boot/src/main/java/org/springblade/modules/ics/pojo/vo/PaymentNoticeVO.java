/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

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
 * 收款通知视图对象.
 *
 * @author BladeX
 */
@Data
@Schema(description = "收款通知视图对象")
public class PaymentNoticeVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "通知ID")
	private Long noticeId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "账单ID")
	private Long paymentId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "合同ID")
	private Long contractId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "客户ID")
	private Long customerId;

	@JsonSerialize(using = ToStringSerializer.class)
	@Schema(description = "园区ID")
	private Long parkId;

	@Schema(description = "通知编号")
	private String noticeNo;

	@Schema(description = "账单编号")
	private String paymentNo;

	@Schema(description = "合同编号")
	private String contractNo;

	@Schema(description = "客商名称")
	private String customerName;

	@Schema(description = "楼宇名称")
	private String buildingName;

	@Schema(description = "楼号/房号")
	private String roomName;

	@Schema(description = "联系方式")
	private String contactPhone;

	@Schema(description = "联系邮箱")
	private String contactEmail;

	@Schema(description = "费用名称")
	private String feeName;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "账期开始")
	private Date periodStart;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "账期结束")
	private Date periodEnd;

	@Schema(description = "应收金额")
	private BigDecimal amountDue;

	@Schema(description = "实收金额")
	private BigDecimal amountPaid;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "应缴日期")
	private Date payDeadline;

	@Schema(description = "短信发送状态")
	private String smsStatus;

	@Schema(description = "邮箱发送状态")
	private String emailStatus;

	@Schema(description = "站内信发送状态")
	private String inboxStatus;

	@Schema(description = "小程序发送状态")
	private String miniappStatus;

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

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "生成日期")
	private Date generatedDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "生成日期开始")
	private Date createStartDate;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	@Schema(description = "生成日期结束")
	private Date createEndDate;

}
