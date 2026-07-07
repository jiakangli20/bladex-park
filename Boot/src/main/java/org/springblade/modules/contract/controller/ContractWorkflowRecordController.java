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
package org.springblade.modules.contract.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.modules.contract.pojo.entity.ContractWorkflowRecord;
import org.springblade.modules.contract.service.IContractWorkflowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 合同管理流程记录控制器
 *
 * @author Chill
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "contract_contract")
@RequestMapping("/blade-contract/workflow-record")
@Tag(name = "合同流程记录", description = "合同流程记录接口")
public class ContractWorkflowRecordController extends BladeController {

	private final IContractWorkflowService contractWorkflowService;

	/**
	 * 合同流程记录分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入record")
	public R<IPage<ContractWorkflowRecord>> page(ContractWorkflowRecord record, Query query) {
		IPage<ContractWorkflowRecord> pages = contractWorkflowService.selectRecordPage(Condition.getPage(query), record);
		return R.data(pages);
	}

	/**
	 * 合同流程记录详情
	 */
	@GetMapping("/detail/{recordId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "传入recordId")
	public R<ContractWorkflowRecord> detail(@Parameter(description = "记录ID") @PathVariable Long recordId) {
		return R.data(contractWorkflowService.getById(recordId));
	}

	/**
	 * 合同流程记录列表
	 */
	@GetMapping("/contract/{contractId}")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "合同流程记录列表", description = "传入contractId")
	public R<List<ContractWorkflowRecord>> listByContract(@Parameter(description = "合同ID") @PathVariable Long contractId) {
		return R.data(contractWorkflowService.selectByContractId(contractId));
	}

	/**
	 * 合同最新流程记录
	 */
	@GetMapping("/latest")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "合同最新流程记录", description = "传入contractId和businessType")
	public R<ContractWorkflowRecord> latest(@RequestParam Long contractId, @RequestParam String businessType) {
		return R.data(contractWorkflowService.selectLatest(contractId, businessType));
	}

	/**
	 * 上传流程资料
	 */
	@PostMapping("/attachment/{recordId}")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "上传流程资料", description = "传入recordId和资料信息")
	public R<ContractWorkflowRecord> uploadAttachment(@Parameter(description = "记录ID") @PathVariable Long recordId, @RequestBody Map<String, Object> payload) {
		return R.data(contractWorkflowService.uploadAttachment(recordId, payload));
	}

}
