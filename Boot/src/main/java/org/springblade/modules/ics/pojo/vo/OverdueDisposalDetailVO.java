/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.ics.pojo.entity.OverdueInternalNotice;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 逾期处置闭环详情.
 *
 * @author BladeX
 */
@Data
@Schema(description = "逾期处置闭环详情")
public class OverdueDisposalDetailVO implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "客户逾期通知状态")
	private PaymentNoticeVO paymentNotice;

	@Schema(description = "客户催款通知状态")
	private PaymentNoticeVO reminderNotice;

	@Schema(description = "律师函发送登记记录")
	private List<ContractLog> legalSendRecords = new ArrayList<>();

	@Schema(description = "逾期相关审批记录")
	private List<ContractWorkflowRecord> workflowRecords = new ArrayList<>();

	@Schema(description = "内部通知记录")
	private List<OverdueInternalNotice> internalNotices = new ArrayList<>();

}
