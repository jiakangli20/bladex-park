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
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.contract.pojo.vo.ContractArchiveDetailVO;
import org.springblade.modules.contract.pojo.vo.ContractArchiveVO;
import org.springblade.modules.contract.service.IContractArchiveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合同归档控制器
 *
 * @author Chill
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "contract_archive")
@RequestMapping("/blade-contract/archive")
@Tag(name = "合同归档", description = "合同归档接口")
public class ContractArchiveController extends BladeController {

	private final IContractArchiveService contractArchiveService;

	/**
	 * 合同归档分页
	 */
	@GetMapping("/page")
	@PreAuth(menu = "contract_archive_list")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "分页", description = "传入contract")
	public R<IPage<ContractArchiveVO>> page(ContractArchiveVO contract, Query query) {
		IPage<ContractArchiveVO> pages = contractArchiveService.selectArchivePage(Condition.getPage(query), contract);
		return R.data(pages);
	}

	/**
	 * 合同归档详情
	 */
	@GetMapping("/detail/{contractId}")
	@PreAuth(menu = "contract_archive_detail")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "传入contractId")
	public R<ContractArchiveDetailVO> detail(@Parameter(description = "合同ID") @PathVariable Long contractId) {
		return R.data(contractArchiveService.getArchiveDetail(contractId));
	}

	/**
	 * 导出合同审批表
	 */
	@GetMapping("/export-approval/{contractId}")
	@PreAuth(menu = "contract_archive_export_approval")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "导出合同审批表", description = "传入contractId")
	public R<Kv> exportApproval(@Parameter(description = "合同ID") @PathVariable Long contractId) {
		return R.data(contractArchiveService.exportApproval(contractId));
	}

	/**
	 * 合同打印预览
	 */
	@GetMapping("/print/{contractId}")
	@PreAuth(menu = "contract_archive_print")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "打印预览", description = "传入contractId")
	public R<Kv> print(@Parameter(description = "合同ID") @PathVariable Long contractId) {
		return R.data(contractArchiveService.printContract(contractId));
	}

}
