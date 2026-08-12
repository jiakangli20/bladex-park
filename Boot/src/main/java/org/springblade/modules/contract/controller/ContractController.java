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
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.modules.contract.excel.ContractExcel;
import org.springblade.modules.contract.excel.ContractImporter;
import org.springblade.modules.contract.pojo.entity.Contract;
import org.springblade.modules.contract.pojo.entity.ContractChange;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.vo.ContractStatsVO;
import org.springblade.modules.contract.pojo.vo.ContractExpirySummaryVO;
import org.springblade.modules.contract.service.IContractService;
import org.springblade.modules.ics.service.IPaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 合同管理控制器
 *
 * @author Chill
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "contract_contract")
@RequestMapping("/blade-contract/contract")
@Tag(name = "合同管理", description = "合同管理接口")
public class ContractController extends BladeController {

	private final IContractService contractService;
	private final IPaymentService paymentService;

	/**
	 * 合同详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入contractId")
	public R<Contract> detail(@Parameter(description = "合同ID") @RequestParam Long contractId) {
		return R.data(contractService.selectContractById(contractId));
	}

	/**
	 * 合同分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入contract")
	public R<IPage<Contract>> list(Contract contract, Query query) {
		IPage<Contract> pages = contractService.selectContractPage(Condition.getPage(query), contract);
		return R.data(pages);
	}

	/**
	 * 导出合同数据.
	 */
	@GetMapping("/export")
	@PreAuth(menu = "contract_contract_view")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "导出", description = "按当前查询条件导出合同数据")
	public void export(Contract contract, HttpServletResponse response) {
		List<ContractExcel> list = contractService.exportContract(contract);
		ExcelUtil.export(response, "合同数据" + DateUtil.time(), "合同数据表", list, ContractExcel.class);
	}

	/**
	 * 导出合同导入模板.
	 */
	@GetMapping("/export-template")
	@PreAuth(menu = "contract_contract_add")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "导出模板", description = "导出合同导入模板")
	public void exportTemplate(HttpServletResponse response) {
		ExcelUtil.export(response, "合同数据模板", "合同数据表", List.of(), ContractExcel.class);
	}

	/**
	 * 导入合同数据.
	 */
	@PostMapping("/import")
	@PreAuth(menu = "contract_contract_add")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "导入", description = "导入合同 Excel 数据")
	public R importContract(MultipartFile file) {
		ContractImporter importer = new ContractImporter(contractService);
		ExcelUtil.save(file, importer, ContractExcel.class);
		return R.success("操作成功");
	}

	/**
	 * 新增或修改合同
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增或修改", description = "传入contract")
	public R submit(@RequestBody Contract contract) {
		return R.status(contractService.submitContract(contract));
	}

	/**
	 * 删除合同
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(contractService.deleteContracts(ids));
	}

	/**
	 * 续签合同
	 */
	@PostMapping("/renew")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "续签", description = "传入contractId和新合同")
	public R renew(@RequestParam Long contractId, @RequestBody Contract contract) {
		return R.status(contractService.renewContract(contractId, contract));
	}

	/**
	 * 登记合同变更并立即生效
	 */
	@PostMapping("/change")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "合同变更", description = "登记合同变更并立即更新合同，不发起审批流")
	public R<ContractChange> change(@RequestBody ContractChange change) {
		return R.data(contractService.applyContractChange(change));
	}

	/**
	 * 合同变更记录
	 */
	@GetMapping("/change/list")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "合同变更记录", description = "传入contractId")
	public R<List<ContractChange>> changeList(@RequestParam Long contractId) {
		return R.data(contractService.selectContractChanges(contractId));
	}

	/**
	 * 作废待审批合同
	 */
	@PostMapping("/terminate")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "作废", description = "仅允许作废未进入运行中审批的待审批合同")
	public R terminate(@RequestParam Long contractId) {
		return R.status(contractService.terminateContract(contractId));
	}

	/**
	 * 上传盖章合同
	 */
	@PostMapping("/signed-file")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "上传盖章合同", description = "传入contractId和contractFileUrl")
	public R signedFile(@RequestParam Long contractId, @RequestBody Contract contract) {
		return R.status(contractService.uploadSignedContract(contractId, contract));
	}

	/**
	 * 合同统计
	 */
	@GetMapping("/stats")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "统计", description = "传入parkId")
	public R<ContractStatsVO> stats(@RequestParam(required = false) Long parkId) {
		return R.data(contractService.stats(parkId));
	}

	/**
	 * 到期提醒
	 */
	@GetMapping("/expiring")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "到期提醒", description = "传入contract")
	public R<IPage<Contract>> expiring(Contract contract, Query query) {
		IPage<Contract> pages = contractService.selectExpiringPage(Condition.getPage(query), contract);
		return R.data(pages);
	}

	@GetMapping("/expiring/summary")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "到期提醒汇总", description = "按到期设置规则统计提醒合同")
	public R<ContractExpirySummaryVO> expiringSummary(Contract contract) {
		return R.data(contractService.expiringSummary(contract));
	}

	/**
	 * 合同缴费计划
	 */
	@GetMapping("/payment")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "合同缴费计划", description = "传入contractId")
	public R<List<ContractPayment>> payment(@RequestParam Long contractId) {
		return R.data(contractService.selectPaymentByContractId(contractId));
	}

	/**
	 * 查询押金退还付款单
	 */
	@GetMapping("/payment/deposit-refund")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "查询押金退还付款单", description = "传入contractId")
	public R<ContractPayment> getDepositRefundPayment(@RequestParam Long contractId) {
		return R.data(contractService.getDepositRefundPayment(contractId));
	}

	/**
	 * 获取或创建押金退还付款单
	 */
	@PostMapping("/payment/deposit-refund")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "获取或创建押金退还付款单", description = "传入contractId")
	public R<ContractPayment> depositRefundPayment(@RequestParam Long contractId) {
		return R.data(contractService.ensureDepositRefundPayment(contractId));
	}

	/**
	 * 登记退租客户注册地址变更.
	 */
	@PostMapping("/termination/address-change")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "登记退租注册地址变更", description = "传入contractId和变更后的registeredAddress")
	public R<Contract> confirmTerminationAddressChange(@RequestParam Long contractId,
													  @RequestBody Map<String, String> payload) {
		return R.data(contractService.confirmTerminationAddressChange(contractId, payload.get("registeredAddress")));
	}

	/**
	 * 缴费分页
	 */
	@GetMapping("/payment/list")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "缴费分页", description = "传入payment")
	public R<IPage<ContractPayment>> paymentList(ContractPayment payment, Query query) {
		IPage<ContractPayment> pages = contractService.selectPaymentPage(Condition.getPage(query), payment);
		return R.data(pages);
	}

	/**
	 * 催缴
	 */
	@PostMapping("/payment/remind")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "催缴", description = "传入paymentId")
	public R remind(@RequestParam Long paymentId) {
		return R.status(paymentService.remind(paymentId, "contract_detail"));
	}

	/**
	 * 合同日志
	 */
	@GetMapping("/log")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "合同日志", description = "传入contractId")
	public R<List<ContractLog>> log(@RequestParam Long contractId) {
		return R.data(contractService.selectLogByContractId(contractId));
	}

}
