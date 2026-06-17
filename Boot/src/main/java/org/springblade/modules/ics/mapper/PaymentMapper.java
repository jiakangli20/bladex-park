/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;

import java.util.List;

/**
 * 财务缴费 Mapper.
 *
 * @author BladeX
 */
public interface PaymentMapper {

	/**
	 * 账单分页.
	 *
	 * @param page    分页对象
	 * @param payment 查询条件
	 * @param overdue 是否动态逾期口径
	 * @return 账单列表
	 */
	List<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, @Param("payment") ContractPayment payment, @Param("overdue") Boolean overdue);

	/**
	 * 账单汇总.
	 *
	 * @param payment 查询条件
	 * @return 汇总
	 */
	PaymentSummaryVO selectSummary(@Param("payment") ContractPayment payment);

	/**
	 * 账单详情.
	 *
	 * @param paymentId 账单ID
	 * @return 账单
	 */
	ContractPayment selectPaymentById(@Param("paymentId") Long paymentId);

}
