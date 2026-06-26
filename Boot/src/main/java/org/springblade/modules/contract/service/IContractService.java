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
package org.springblade.modules.contract.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.vo.ContractStatsVO;

import java.util.List;

/**
 * 合同主档服务类
 *
 * @author Chill
 */
public interface IContractService extends IService<Contract> {

	/**
	 * 合同分页
	 *
	 * @param page     分页
	 * @param contract 查询条件
	 * @return 分页
	 */
	IPage<Contract> selectContractPage(IPage<Contract> page, Contract contract);

	/**
	 * 合同详情
	 *
	 * @param contractId 合同ID
	 * @return 合同
	 */
	Contract selectContractById(Long contractId);

	/**
	 * 新增或修改合同
	 *
	 * @param contract 合同
	 * @return 是否成功
	 */
	boolean submitContract(Contract contract);

	/**
	 * 逻辑删除合同
	 *
	 * @param ids 主键集合
	 * @return 是否成功
	 */
	boolean deleteContracts(String ids);

	/**
	 * 续签合同
	 *
	 * @param contractId  原合同ID
	 * @param newContract 新合同
	 * @return 是否成功
	 */
	boolean renewContract(Long contractId, Contract newContract);

	/**
	 * 终止合同
	 *
	 * @param contractId 合同ID
	 * @return 是否成功
	 */
	boolean terminateContract(Long contractId);

	/**
	 * 上传盖章合同并生效
	 *
	 * @param contractId 合同ID
	 * @param contract   合同附件
	 * @return 是否成功
	 */
	boolean uploadSignedContract(Long contractId, Contract contract);

	/**
	 * 到期提醒分页
	 *
	 * @param page     分页
	 * @param contract 查询条件
	 * @return 分页
	 */
	IPage<Contract> selectExpiringPage(IPage<Contract> page, Contract contract);

	/**
	 * 合同统计
	 *
	 * @param parkId 园区ID
	 * @return 统计
	 */
	ContractStatsVO stats(Long parkId);

	/**
	 * 缴费分页
	 *
	 * @param page    分页
	 * @param payment 查询条件
	 * @return 分页
	 */
	IPage<ContractPayment> selectPaymentPage(IPage<ContractPayment> page, ContractPayment payment);

	/**
	 * 合同缴费计划
	 *
	 * @param contractId 合同ID
	 * @return 缴费计划
	 */
	List<ContractPayment> selectPaymentByContractId(Long contractId);

	/**
	 * 获取或创建押金退还付款单
	 *
	 * @param contractId 合同ID
	 * @return 押金退还付款单
	 */
	ContractPayment ensureDepositRefundPayment(Long contractId);

	/**
	 * 确认缴费
	 *
	 * @param paymentId 缴费ID
	 * @param payment   缴费数据
	 * @return 是否成功
	 */
	boolean confirmPayment(Long paymentId, ContractPayment payment);

	/**
	 * 催缴
	 *
	 * @param paymentId 缴费ID
	 * @return 是否成功
	 */
	boolean remindPayment(Long paymentId);

	/**
	 * 合同日志
	 *
	 * @param contractId 合同ID
	 * @return 日志列表
	 */
	List<ContractLog> selectLogByContractId(Long contractId);

}
