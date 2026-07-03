/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;

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

	@Schema(description = "收款通知状态")
	private PaymentNoticeVO paymentNotice;

	@Schema(description = "通知文件生成记录")
	private List<ContractLog> documentRecords = new ArrayList<>();

	@Schema(description = "小程序发送记录")
	private List<ContractLog> miniAppRecords = new ArrayList<>();

	@Schema(description = "逾期相关审批记录")
	private List<ContractWorkflowRecord> workflowRecords = new ArrayList<>();

}
