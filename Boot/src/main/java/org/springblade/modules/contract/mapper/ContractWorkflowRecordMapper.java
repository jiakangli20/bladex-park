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
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;

import java.util.List;

/**
 * 合同管理流程记录 Mapper 接口
 *
 * @author Chill
 */
public interface ContractWorkflowRecordMapper extends BaseMapper<ContractWorkflowRecord> {

	/**
	 * 合同流程记录分页
	 *
	 * @param page   分页
	 * @param record 查询条件
	 * @return 记录列表
	 */
	List<ContractWorkflowRecord> selectRecordPage(IPage<ContractWorkflowRecord> page, @Param("record") ContractWorkflowRecord record);

	/**
	 * 合同流程记录列表
	 *
	 * @param contractId 合同ID
	 * @return 记录列表
	 */
	List<ContractWorkflowRecord> selectByContractId(@Param("contractId") Long contractId);

	/**
	 * 根据流程实例查询记录
	 *
	 * @param processInsId 流程实例ID
	 * @return 流程记录
	 */
	ContractWorkflowRecord selectByProcessInsId(@Param("processInsId") String processInsId);

	/**
	 * 查询指定合同和业务类型的最新流程记录
	 *
	 * @param contractId    合同ID
	 * @param businessType  业务类型
	 * @return 流程记录
	 */
	ContractWorkflowRecord selectLatest(@Param("contractId") Long contractId, @Param("businessType") String businessType);

}
