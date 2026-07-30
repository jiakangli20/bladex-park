/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springblade.modules.contract.pojo.entity.ContractPaymentRecord;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 收付款账单逐笔确认记录 Mapper.
 *
 * @author BladeX
 */
public interface ContractPaymentRecordMapper extends BaseMapper<ContractPaymentRecord> {

	/**
	 * 撤回中间收付款时，扣减其后的累计收付款金额.
	 */
	int rollbackFollowingCumulativeAmount(@Param("paymentId") Long paymentId,
										  @Param("recordId") Long recordId,
										  @Param("paymentTime") Date paymentTime,
										  @Param("paymentAmount") BigDecimal paymentAmount);
}
