/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement.
 * <p>
 * Redistribution of this software's source code to any third party without
 * a commercial license is strictly prohibited.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.miniapp.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.miniapp.pojo.dto.UtilityBillingDTO;
import org.springblade.modules.miniapp.pojo.entity.MiniPaymentSubmission;
import org.springblade.modules.miniapp.pojo.entity.UtilityBillDetail;
import org.springblade.modules.miniapp.pojo.vo.UtilityBillingPreviewVO;

/**
 * 园区水电计费与付款凭证审核服务。
 *
 * @author Chill
 */
public interface IUtilityBillingService {

	UtilityBillingPreviewVO preview(UtilityBillingDTO.BillingRequest request);

	UtilityBillDetail generate(UtilityBillingDTO.BillingRequest request);

	boolean publish(Long detailId);

	IPage<MiniPaymentSubmission> paymentSubmissionPage(IPage<MiniPaymentSubmission> page,
		UtilityBillingDTO.SubmissionQuery query);

	boolean confirmSubmission(Long submissionId, UtilityBillingDTO.AuditRequest request);

	boolean rejectSubmission(Long submissionId, UtilityBillingDTO.AuditRequest request);
}
