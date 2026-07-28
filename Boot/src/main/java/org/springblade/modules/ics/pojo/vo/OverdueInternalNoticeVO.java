/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.ics.pojo.entity.OverdueInternalNotice;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 当前账号逾期通知视图对象.
 *
 * @author BladeX
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "当前账号逾期通知视图对象")
public class OverdueInternalNoticeVO extends OverdueInternalNotice {

	@Serial
	private static final long serialVersionUID = 1L;

	private String contractNo;

	private String customerName;

	private String feeName;

	private BigDecimal amountDue;

	private BigDecimal amountPaid;

	@DateTimeFormat(pattern = DateUtil.PATTERN_DATE)
	@JsonFormat(pattern = DateUtil.PATTERN_DATE)
	private Date payDeadline;

	private String roomName;

	private String buildingName;

	@Schema(description = "记录唯一键")
	private String recordKey;

	@Schema(description = "记录类型：notice逾期通知 reminder催缴记录")
	private String recordType;

	@Schema(description = "催缴操作人")
	private String operatorName;

	@Schema(description = "催缴来源")
	private String source;

}
