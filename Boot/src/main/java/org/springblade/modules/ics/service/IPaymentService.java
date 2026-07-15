/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.ics.pojo.vo.OverdueDisposalDetailVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;

import java.util.Date;
import java.util.List;

/**
 * 财务缴费服务.
 *
 * @author BladeX
 */
public interface IPaymentService {

	/**
	 * 账单分页.
	 *
	 * @param page    分页
	 * @param payment 查询条件
	 * @param scope   入口口径
	 * @return 分页
	 */
	IPage<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, ContractPayment payment, String scope);

	/**
	 * 账单详情.
	 *
	 * @param paymentId 账单ID
	 * @return 账单
	 */
	ContractPayment selectPaymentById(Long paymentId);

	/**
	 * 按合同查询账单.
	 *
	 * @param contractId 合同ID
	 * @return 账单列表
	 */
	List<ContractPayment> selectPaymentByContract(Long contractId);

	/**
	 * 汇总统计.
	 *
	 * @param payment 查询条件
	 * @return 汇总
	 */
	PaymentSummaryVO summary(ContractPayment payment);

	/**
	 * 创建手工账单.
	 *
	 * @param payment 账单数据
	 * @return 新账单
	 */
	ContractPayment create(ContractPayment payment);

	/**
	 * 查询可创建账单的合同选项.
	 *
	 * @param keyword 关键字
	 * @return 合同选项
	 */
	List<Contract> contractOptions(String keyword);

	/**
	 * 确认缴费.
	 *
	 * @param paymentId 账单ID
	 * @param payment   缴费数据
	 * @return 是否成功
	 */
	boolean confirm(Long paymentId, ContractPayment payment);

	/**
	 * 调整账单日期.
	 *
	 * @param paymentId   账单ID
	 * @param payDeadline 应收/应付日期
	 * @return 是否成功
	 */
	boolean updateDeadline(Long paymentId, Date payDeadline);

	/**
	 * 更新账单附件.
	 *
	 * @param paymentId 账单ID
	 * @param payment   附件数据
	 * @return 更新后的账单
	 */
	ContractPayment updateAttachment(Long paymentId, ContractPayment payment);

	/**
	 * 催缴.
	 *
	 * @param paymentId 账单ID
	 * @return 是否成功
	 */
	boolean remind(Long paymentId);

	/**
	 * 合同日志.
	 *
	 * @param contractId 合同ID
	 * @return 日志列表
	 */
	List<ContractLog> logList(Long contractId);

	/**
	 * 逾期处置闭环详情.
	 *
	 * @param paymentId 账单ID
	 * @return 闭环详情
	 */
	OverdueDisposalDetailVO overdueDisposalDetail(Long paymentId);

	/**
	 * 收款通知占位.
	 *
	 * @return 占位说明
	 */
	PaymentNoticePlaceholderVO noticePlaceholder();

	/**
	 * 收款通知分页.
	 *
	 * @param page  分页
	 * @param query 查询条件
	 * @return 收款通知分页
	 */
	IPage<PaymentNoticeVO> selectNoticePage(IPage<PaymentNoticeVO> page, PaymentNoticeVO query);

	/**
	 * 收款通知汇总.
	 *
	 * @param query 查询条件
	 * @return 汇总
	 */
	PaymentNoticeSummaryVO noticeSummary(PaymentNoticeVO query);

	/**
	 * 楼宇下拉选项.
	 *
	 * @param query 查询条件
	 * @return 楼宇名称
	 */
	List<String> noticeBuildingOptions(PaymentNoticeVO query);

	/**
	 * 重新发送收款通知.
	 *
	 * @param paymentId 账单ID
	 * @return 通知详情
	 */
	PaymentNoticeVO resendNotice(Long paymentId);

	/**
	 * 生成并返回收款通知文件.
	 *
	 * @param paymentId 账单ID
	 * @return 文件
	 */
	ContractNoticeFileVO generatePaymentNoticeFile(Long paymentId);

	/**
	 * 小程序发送预留接口.
	 *
	 * @param paymentId 账单ID
	 * @return 通知详情
	 */
	PaymentNoticeVO sendMiniAppNotice(Long paymentId);

	/**
	 * 短信发送入口.
	 *
	 * @param paymentId 账单ID
	 * @return 通知详情
	 */
	PaymentNoticeVO sendSmsNotice(Long paymentId);

	/**
	 * 邮件发送入口.
	 *
	 * @param paymentId 账单ID
	 * @return 通知详情
	 */
	PaymentNoticeVO sendEmailNotice(Long paymentId);

}
