/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;

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
	 * 确认缴费.
	 *
	 * @param paymentId 账单ID
	 * @param payment   缴费数据
	 * @return 是否成功
	 */
	boolean confirm(Long paymentId, ContractPayment payment);

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
	 * 收款通知占位.
	 *
	 * @return 占位说明
	 */
	PaymentNoticePlaceholderVO noticePlaceholder();

}
