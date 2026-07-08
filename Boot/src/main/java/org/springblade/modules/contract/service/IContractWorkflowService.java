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
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.plugin.workflow.process.dto.WfNoticeDTO;

import java.util.List;
import java.util.Map;

/**
 * 合同管理流程记录服务类
 *
 * @author Chill
 */
public interface IContractWorkflowService extends IService<ContractWorkflowRecord> {

	/**
	 * 合同流程记录分页
	 *
	 * @param page   分页
	 * @param record 查询条件
	 * @return 记录分页
	 */
	IPage<ContractWorkflowRecord> selectRecordPage(IPage<ContractWorkflowRecord> page, ContractWorkflowRecord record);

	/**
	 * 合同流程记录列表
	 *
	 * @param contractId 合同ID
	 * @return 记录列表
	 */
	List<ContractWorkflowRecord> selectByContractId(Long contractId);

	/**
	 * 合同最新流程记录
	 *
	 * @param contractId   合同ID
	 * @param businessType 业务类型
	 * @return 流程记录
	 */
	ContractWorkflowRecord selectLatest(Long contractId, String businessType);

	/**
	 * 上传合同流程资料
	 *
	 * @param recordId 流程记录ID
	 * @param payload  资料信息
	 * @return 流程记录
	 */
	ContractWorkflowRecord uploadAttachment(Long recordId, Map<String, Object> payload);

	/**
	 * 删除合同流程资料
	 *
	 * @param recordId      流程记录ID
	 * @param fileUrl       文件地址
	 * @param materialName  资料名称
	 * @return 流程记录
	 */
	ContractWorkflowRecord removeAttachment(Long recordId, String fileUrl, String materialName);

	/**
	 * 是否合同管理流程.
	 *
	 * @param notice 流程通知
	 * @return 是否支持
	 */
	boolean supports(WfNoticeDTO notice);

	/**
	 * Flowable 流程通知回调.
	 *
	 * @param notice 流程通知
	 */
	void businessWithNotice(WfNoticeDTO notice);

}
