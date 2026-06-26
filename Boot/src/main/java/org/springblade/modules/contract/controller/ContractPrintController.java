/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.contract.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.constant.AuthConstant;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 合同打印文件控制器.
 *
 * @author BladeX
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@RequestMapping("/blade-contract/print")
@Tag(name = "合同打印文件", description = "合同打印文件接口")
public class ContractPrintController extends BladeController {

	private final IContractNoticeService contractNoticeService;

	@GetMapping("/payment-notice/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 1)
	@Operation(summary = "付款通知单", description = "传入paymentId")
	public void paymentNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_PAYMENT, paymentId, null), response);
	}

	@GetMapping("/invoice-apply/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 2)
	@Operation(summary = "开票申请单", description = "传入paymentId")
	public void invoiceApply(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_INVOICE, paymentId, null), response);
	}

	@GetMapping("/reminder-notice/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 3)
	@Operation(summary = "催款通知书", description = "传入paymentId")
	public void reminderNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_REMINDER, paymentId, null), response);
	}

	@GetMapping("/contract-approval/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 4)
	@Operation(summary = "合同会签审批表", description = "传入contractId")
	public void contractApproval(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_APPROVAL, null, contractId), response);
	}

	@GetMapping("/contract-text/fixed/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 5)
	@Operation(summary = "合同正文固定租金版", description = "传入contractId")
	public void contractTextFixed(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_FIXED, null, contractId), response);
	}

	@GetMapping("/contract-text/floating/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 6)
	@Operation(summary = "合同正文浮动租金版", description = "传入contractId")
	public void contractTextFloating(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_FLOATING, null, contractId), response);
	}

	@GetMapping("/project-approval/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 7)
	@Operation(summary = "项目审批表", description = "传入paymentId")
	public void projectApproval(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_PROJECT_APPROVAL, paymentId, null), response);
	}

	@GetMapping("/overdue-notice/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 8)
	@Operation(summary = "租金逾期处理通知书", description = "传入paymentId")
	public void overdueNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_OVERDUE, paymentId, null), response);
	}

	@GetMapping("/legal-letter/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 9)
	@Operation(summary = "律师函", description = "传入paymentId")
	public void legalLetter(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_LEGAL, paymentId, null), response);
	}

	@GetMapping("/move-out-notice/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 10)
	@Operation(summary = "限期搬离通知书", description = "传入contractId")
	public void moveOutNotice(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_MOVE_OUT, null, contractId), response);
	}

	@GetMapping("/move-out-notice/payment/{paymentId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 11)
	@Operation(summary = "限期搬离通知书", description = "传入paymentId")
	public void moveOutNoticeByPayment(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_MOVE_OUT, paymentId, null), response);
	}

	@GetMapping("/termination-approval/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 12)
	@Operation(summary = "退租审批表", description = "传入contractId")
	public void terminationApproval(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_TERMINATION, null, contractId), response);
	}

	@GetMapping("/termination-agreement/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 13)
	@Operation(summary = "合同解除补充协议", description = "传入contractId")
	public void terminationAgreement(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_TERMINATION_AGREEMENT, null, contractId), response);
	}

	@GetMapping("/room-review/{contractId}")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 14)
	@Operation(summary = "房屋验收单", description = "传入contractId")
	public void roomReview(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_ROOM_REVIEW, null, contractId), response);
	}

	@PostMapping("/generate")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 15)
	@Operation(summary = "生成通知文件", description = "传入noticeType、paymentId或contractId")
	public R<ContractNoticeFileVO> generate(@RequestParam String noticeType,
										 @RequestParam(required = false) Long paymentId,
										 @RequestParam(required = false) Long contractId) {
		return R.data(contractNoticeService.uploadNotice(noticeType, paymentId, contractId));
	}

	@PostMapping("/miniapp/send")
	@PreAuth(AuthConstant.PERMIT_ALL)
	@ApiOperationSupport(order = 16)
	@Operation(summary = "小程序发送预留接口", description = "传入noticeType、paymentId或contractId")
	public R<Kv> miniAppSend(@RequestParam String noticeType,
							 @RequestParam(required = false) Long paymentId,
							 @RequestParam(required = false) Long contractId) {
		return R.data(contractNoticeService.buildMiniAppPayload(noticeType, paymentId, contractId));
	}

	@SneakyThrows
	private void writeDocument(ContractNoticeFileVO document, HttpServletResponse response) {
		String encodedFileName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(document.getContentType());
		response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
		response.setContentLength(document.getFileBytes().length);
		response.getOutputStream().write(document.getFileBytes());
		response.getOutputStream().flush();
	}

}
