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
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.contract.pojo.entity.ContractSupplementAgreement;
import org.springblade.modules.contract.pojo.vo.ContractArchiveDetailVO;
import org.springblade.modules.contract.pojo.vo.ContractArchiveVO;

import java.util.List;

/**
 * 合同归档服务类
 *
 * @author Chill
 */
public interface IContractArchiveService {

	/**
	 * 合同归档分页
	 *
	 * @param page     分页
	 * @param contract 查询条件
	 * @return 分页
	 */
	IPage<ContractArchiveVO> selectArchivePage(IPage<ContractArchiveVO> page, ContractArchiveVO contract);

	/**
	 * 合同归档详情
	 *
	 * @param contractId 合同ID
	 * @return 归档详情
	 */
	ContractArchiveDetailVO getArchiveDetail(Long contractId);

	/**
	 * 导出合同审批表
	 *
	 * @param contractId 合同ID
	 * @return 导出数据
	 */
	Kv exportApproval(Long contractId);

	/**
	 * 合同打印预览
	 *
	 * @param contractId 合同ID
	 * @return 打印数据
	 */
	Kv printContract(Long contractId);

	/**
	 * 合同补充协议列表
	 *
	 * @param contractId 合同ID
	 * @return 补充协议列表
	 */
	List<ContractSupplementAgreement> listSupplementAgreements(Long contractId);

	/**
	 * 保存合同补充协议
	 *
	 * @param agreement 补充协议
	 * @return 是否成功
	 */
	boolean saveSupplementAgreement(ContractSupplementAgreement agreement);

	/**
	 * 删除合同补充协议
	 *
	 * @param agreementId 补充协议ID
	 * @return 是否成功
	 */
	boolean removeSupplementAgreement(Long agreementId);

}
