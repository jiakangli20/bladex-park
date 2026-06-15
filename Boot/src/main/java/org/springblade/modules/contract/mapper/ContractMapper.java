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
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.vo.ContractStatsVO;

import java.util.List;

/**
 * 合同主档 Mapper 接口
 *
 * @author Chill
 */
public interface ContractMapper extends BaseMapper<Contract> {

	/**
	 * 合同分页列表
	 *
	 * @param page     分页
	 * @param contract 查询条件
	 * @return 合同列表
	 */
	List<Contract> selectContractPage(IPage<Contract> page, @Param("contract") Contract contract);

	/**
	 * 合同详情
	 *
	 * @param contractId 合同ID
	 * @return 合同
	 */
	Contract selectContractById(@Param("contractId") Long contractId);

	/**
	 * 到期提醒列表
	 *
	 * @param page     分页
	 * @param contract 查询条件
	 * @return 合同列表
	 */
	List<Contract> selectExpiringPage(IPage<Contract> page, @Param("contract") Contract contract);

	/**
	 * 合同统计
	 *
	 * @param parkId 园区ID
	 * @return 统计
	 */
	ContractStatsVO selectStats(@Param("parkId") Long parkId);

	/**
	 * 逻辑删除合同
	 *
	 * @param ids 主键集合
	 * @return 行数
	 */
	int deleteContractByIds(@Param("ids") List<Long> ids);

	/**
	 * 园区是否存在
	 *
	 * @param parkId 园区ID
	 * @return 数量
	 */
	Long existsPark(@Param("parkId") Long parkId);

	/**
	 * 楼宇是否存在
	 *
	 * @param buildingId 楼宇ID
	 * @return 数量
	 */
	Long existsBuilding(@Param("buildingId") Long buildingId);

	/**
	 * 房源是否存在
	 *
	 * @param roomId 房源ID
	 * @return 数量
	 */
	Long existsRoom(@Param("roomId") Long roomId);

	/**
	 * 已入驻客户是否存在
	 *
	 * @param customerId 客户ID
	 * @return 数量
	 */
	Long existsSettledCustomer(@Param("customerId") Long customerId);

}
