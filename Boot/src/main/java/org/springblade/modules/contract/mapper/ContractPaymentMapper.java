/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
package org.springblade.modules.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.modules.contract.pojo.entity.ContractPayment;

import java.util.List;

/**
 * 合同缴费计划 Mapper 接口
 *
 * @author Chill
 */
public interface ContractPaymentMapper extends BaseMapper<ContractPayment> {

	/**
	 * 缴费分页列表
	 *
	 * @param page    分页
	 * @param payment 查询条件
	 * @return 缴费列表
	 */
	List<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, @Param("payment") ContractPayment payment);

	/**
	 * 合同缴费计划
	 *
	 * @param contractId 合同ID
	 * @return 缴费计划
	 */
	List<ContractPayment> selectByContractId(@Param("contractId") Long contractId);

}
