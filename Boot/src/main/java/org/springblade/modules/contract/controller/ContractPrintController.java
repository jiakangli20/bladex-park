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
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
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
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;

	@GetMapping("/payment-notice/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "付款通知单", description = "传入paymentId")
	public void paymentNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_PAYMENT, paymentId, null), response);
	}

	@GetMapping("/invoice-apply/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "开票申请单", description = "传入paymentId")
	public void invoiceApply(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_INVOICE, paymentId, null), response);
	}

	@GetMapping("/reminder-notice/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "催款通知书", description = "传入paymentId")
	public void reminderNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_REMINDER, paymentId, null), response);
	}

	@GetMapping("/contract-approval/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "合同会签审批表", description = "传入contractId")
	public void contractApproval(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_APPROVAL, null, contractId), response);
	}

	@GetMapping("/contract-text/fixed/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "合同正文固定租金版", description = "传入contractId")
	public void contractTextFixed(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_FIXED, null, contractId), response);
	}

	@GetMapping("/contract-text/floating/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "合同正文浮动租金版", description = "传入contractId")
	public void contractTextFloating(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_CONTRACT_FLOATING, null, contractId), response);
	}

	@GetMapping("/project-approval/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "项目审批表", description = "传入paymentId")
	public void projectApproval(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_PROJECT_APPROVAL, paymentId, null), response);
	}

	@GetMapping("/overdue-notice/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "租金逾期处理通知书", description = "传入paymentId")
	public void overdueNotice(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_OVERDUE, paymentId, null), response);
	}

	@GetMapping("/legal-letter/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "律师函", description = "传入paymentId")
	public void legalLetter(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_LEGAL, paymentId, null), response);
	}

	@GetMapping("/move-out-notice/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "限期搬离通知书", description = "传入contractId")
	public void moveOutNotice(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_MOVE_OUT, null, contractId), response);
	}

	@GetMapping("/move-out-notice/payment/{paymentId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "限期搬离通知书", description = "传入paymentId")
	public void moveOutNoticeByPayment(@Parameter(description = "账单ID") @PathVariable Long paymentId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_MOVE_OUT, paymentId, null), response);
	}

	@GetMapping("/termination-approval/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "退租审批表", description = "传入contractId")
	public void terminationApproval(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_TERMINATION, null, contractId), response);
	}

	@GetMapping("/termination-agreement/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "合同解除补充协议", description = "传入contractId")
	public void terminationAgreement(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_TERMINATION_AGREEMENT, null, contractId), response);
	}

	@GetMapping("/room-review/{contractId}")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "房屋验收单", description = "传入contractId")
	public void roomReview(@Parameter(description = "合同ID") @PathVariable Long contractId, HttpServletResponse response) {
		writeDocument(contractNoticeService.buildNotice(IContractNoticeService.NOTICE_ROOM_REVIEW, null, contractId), response);
	}

	@PostMapping("/generate")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "生成通知文件", description = "传入noticeType、paymentId或contractId")
	public R<ContractNoticeFileVO> generate(@RequestParam String noticeType,
										 @RequestParam(required = false) Long paymentId,
										 @RequestParam(required = false) Long contractId) {
		ContractNoticeFileVO file = contractNoticeService.uploadNotice(noticeType, paymentId, contractId);
		addLog(resolveContractId(paymentId, contractId), "notice_generate", buildNoticeLogDesc("生成通知文件", noticeType, paymentId));
		return R.data(file);
	}

	@GetMapping("/preview")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "通知文件预览", description = "传入noticeType、paymentId或contractId")
	public R<Kv> preview(@RequestParam String noticeType,
						 @RequestParam(required = false) Long paymentId,
						 @RequestParam(required = false) Long contractId,
						 @RequestParam(required = false) String formDataJson) {
		return R.data(contractNoticeService.buildNoticePreview(noticeType, paymentId, contractId, formDataJson));
	}

	@PostMapping("/miniapp/send")
	@PreAuth("hasAuth()")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "小程序发送预留接口", description = "传入noticeType、paymentId或contractId")
	public R<Kv> miniAppSend(@RequestParam String noticeType,
							 @RequestParam(required = false) Long paymentId,
							 @RequestParam(required = false) Long contractId) {
		Kv payload = contractNoticeService.buildMiniAppPayload(noticeType, paymentId, contractId);
		addLog(resolveContractId(paymentId, contractId), "notice_miniapp", buildNoticeLogDesc("生成小程序发送数据", noticeType, paymentId));
		return R.data(payload);
	}

	private String buildNoticeLogDesc(String action, String noticeType, Long paymentId) {
		String desc = action + "：" + noticeDisplayName(noticeType);
		return paymentId == null ? desc : desc + "（账单ID：" + paymentId + "）";
	}

	private Long resolveContractId(Long paymentId, Long contractId) {
		if (contractId != null) {
			return contractId;
		}
		if (paymentId == null) {
			return null;
		}
		ContractPayment payment = contractPaymentMapper.selectById(paymentId);
		return payment == null ? null : payment.getContractId();
	}

	private void addLog(Long contractId, String action, String actionDesc) {
		if (contractId == null) {
			return;
		}
		ContractLog contractLog = new ContractLog();
		contractLog.setContractId(contractId);
		contractLog.setAction(action);
		contractLog.setActionDesc(actionDesc);
		contractLog.setOperator(currentUserName());
		contractLog.setOperateTime(DateUtil.now());
		contractLogMapper.insert(contractLog);
	}

	private String currentUserName() {
		String userName = AuthUtil.getUserName();
		return StringUtil.isBlank(userName) ? AuthUtil.getNickName() : userName;
	}

	private String noticeDisplayName(String noticeType) {
		return switch (StringUtil.isBlank(noticeType) ? "" : noticeType) {
			case IContractNoticeService.NOTICE_PAYMENT -> "付款通知单";
			case IContractNoticeService.NOTICE_REMINDER -> "催款通知书";
			case IContractNoticeService.NOTICE_INVOICE -> "开票申请单";
			case IContractNoticeService.NOTICE_CONTRACT_APPROVAL -> "合同会签审批表";
			case IContractNoticeService.NOTICE_CONTRACT_FIXED -> "合同正文固定租金版";
			case IContractNoticeService.NOTICE_CONTRACT_FLOATING -> "合同正文浮动租金版";
			case IContractNoticeService.NOTICE_PROJECT_APPROVAL -> "项目审批表";
			case IContractNoticeService.NOTICE_OVERDUE -> "租金逾期处理通知书";
			case IContractNoticeService.NOTICE_LEGAL -> "律师函";
			case IContractNoticeService.NOTICE_MOVE_OUT -> "限期搬离通知书";
			case IContractNoticeService.NOTICE_TERMINATION -> "退租审批表";
			case IContractNoticeService.NOTICE_TERMINATION_AGREEMENT -> "合同解除补充协议";
			case IContractNoticeService.NOTICE_ROOM_REVIEW -> "房屋退租交接验收单";
			default -> noticeType;
		};
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
