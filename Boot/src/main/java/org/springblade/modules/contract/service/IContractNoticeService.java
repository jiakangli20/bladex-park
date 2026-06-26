/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.service;

import org.springblade.core.tool.support.Kv;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;

import java.util.Map;

/**
 * 合同通知文件服务类.
 *
 * @author BladeX
 */
public interface IContractNoticeService {

	String NOTICE_PAYMENT = "payment-notice";
	String NOTICE_REMINDER = "reminder-notice";
	String NOTICE_INVOICE = "invoice-apply";
	String NOTICE_CONTRACT_APPROVAL = "contract-approval";
	String NOTICE_CONTRACT_FIXED = "contract-fixed";
	String NOTICE_CONTRACT_FLOATING = "contract-floating";
	String NOTICE_PROJECT_APPROVAL = "project-approval";
	String NOTICE_OVERDUE = "overdue-notice";
	String NOTICE_LEGAL = "legal-letter";
	String NOTICE_MOVE_OUT = "move-out-notice";
	String NOTICE_TERMINATION = "termination-approval";
	String NOTICE_TERMINATION_AGREEMENT = "termination-agreement";
	String NOTICE_ROOM_REVIEW = "room-review";

	/**
	 * 构建通知文件.
	 *
	 * @param noticeType 通知类型
	 * @param paymentId  账单ID
	 * @param contractId 合同ID
	 * @return 通知文件
	 */
	ContractNoticeFileVO buildNotice(String noticeType, Long paymentId, Long contractId);

	/**
	 * 上传通知文件并返回文件地址.
	 *
	 * @param noticeType 通知类型
	 * @param paymentId  账单ID
	 * @param contractId 合同ID
	 * @return 通知文件
	 */
	ContractNoticeFileVO uploadNotice(String noticeType, Long paymentId, Long contractId);

	/**
	 * 生成合同审批整套文件.
	 *
	 * @param contractId 合同ID
	 * @return 文件地址集合
	 */
	Map<String, String> uploadContractApprovalPackage(Long contractId);

	/**
	 * 生成付款/开票整套文件.
	 *
	 * @param paymentId 账单ID
	 * @return 文件地址集合
	 */
	Map<String, String> uploadPaymentPackage(Long paymentId);

	/**
	 * 生成逾期处理整套通知文件.
	 *
	 * @param paymentId 账单ID
	 * @return 文件地址集合
	 */
	Map<String, String> uploadOverduePackage(Long paymentId);

	/**
	 * 生成退租审批整套归档文件.
	 *
	 * @param contractId 合同ID
	 * @return 文件地址集合
	 */
	Map<String, String> uploadTerminationPackage(Long contractId);

	/**
	 * 生成退租审批整套归档文件.
	 *
	 * @param contractId   合同ID
	 * @param formDataJson 工作流表单数据
	 * @return 文件地址集合
	 */
	Map<String, String> uploadTerminationPackage(Long contractId, String formDataJson);

	/**
	 * 构建小程序发送载荷.
	 *
	 * @param noticeType 通知类型
	 * @param paymentId  账单ID
	 * @param contractId 合同ID
	 * @return 发送载荷
	 */
	Kv buildMiniAppPayload(String noticeType, Long paymentId, Long contractId);

}
