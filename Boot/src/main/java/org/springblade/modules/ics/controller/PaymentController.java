/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.ics.controller;

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
import org.springblade.core.tool.api.R;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springblade.modules.ics.constant.IcsConstant;
import org.springblade.modules.ics.pojo.vo.PaymentNoticePlaceholderVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeSummaryVO;
import org.springblade.modules.ics.pojo.vo.PaymentNoticeVO;
import org.springblade.modules.ics.pojo.vo.PaymentSummaryVO;
import org.springblade.modules.ics.service.IPaymentService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 财务缴费控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "finance")
@RequestMapping(IcsConstant.APPLICATION_ICS_NAME + "/payment")
@Tag(name = "财务缴费", description = "缴费管理接口")
public class PaymentController extends BladeController {

	private final IPaymentService paymentService;

	@GetMapping("/page")
	@PreAuth(menu = "finance_bills_all_list")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "账单分页", description = "传入缴费查询条件")
	public R<IPage<ContractPayment>> page(ContractPayment payment, Query query, @RequestParam(required = false) String scope) {
		return R.data(paymentService.selectPaymentPage(Condition.getPage(query), payment, scope));
	}

	@GetMapping("/overdue-page")
	@PreAuth(menu = "finance_bills_overdue")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "逾期账单分页", description = "动态逾期口径")
	public R<IPage<ContractPayment>> overduePage(ContractPayment payment, Query query) {
		return R.data(paymentService.selectPaymentPage(Condition.getPage(query), payment, "overdue"));
	}

	@GetMapping("/overdue-reminder-page")
	@PreAuth(menu = "finance_overdue_reminder")
	@ApiOperationSupport(order = 21)
	@Operation(summary = "逾期提醒分页", description = "动态逾期提醒口径")
	public R<IPage<ContractPayment>> overdueReminderPage(ContractPayment payment, Query query) {
		return R.data(paymentService.selectPaymentPage(Condition.getPage(query), payment, "overdue"));
	}

	@GetMapping("/overdue-reminder-summary")
	@PreAuth(menu = "finance_overdue_reminder")
	@ApiOperationSupport(order = 22)
	@Operation(summary = "逾期提醒汇总", description = "动态逾期提醒汇总")
	public R<PaymentSummaryVO> overdueReminderSummary(ContractPayment payment) {
		payment.setPayStatus("2");
		return R.data(paymentService.summary(payment));
	}

	@GetMapping("/detail")
	@PreAuth(menu = "finance_bills_all_view")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "账单详情", description = "传入paymentId")
	public R<ContractPayment> detail(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.data(paymentService.selectPaymentById(paymentId));
	}

	@GetMapping("/by-contract")
	@PreAuth(menu = "finance")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "合同账单", description = "传入contractId")
	public R<List<ContractPayment>> byContract(@Parameter(description = "合同ID") @RequestParam Long contractId) {
		return R.data(paymentService.selectPaymentByContract(contractId));
	}

	@GetMapping("/summary")
	@PreAuth(menu = "finance")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "账单汇总", description = "传入缴费查询条件")
	public R<PaymentSummaryVO> summary(ContractPayment payment) {
		return R.data(paymentService.summary(payment));
	}

	@PostMapping("/confirm")
	@PreAuth(menu = "finance")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "确认缴费", description = "传入paymentId和实收金额")
	public R confirm(@Parameter(description = "账单ID") @RequestParam Long paymentId, @RequestBody ContractPayment payment) {
		return R.status(paymentService.confirm(paymentId, payment));
	}

	@PostMapping("/remind")
	@PreAuth(menu = "finance")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "催缴", description = "传入paymentId")
	public R remind(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.status(paymentService.remind(paymentId));
	}

	@PostMapping("/overdue-reminder-remind")
	@PreAuth(menu = "finance_overdue_reminder")
	@ApiOperationSupport(order = 23)
	@Operation(summary = "逾期提醒催缴", description = "传入paymentId")
	public R overdueReminderRemind(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.status(paymentService.remind(paymentId));
	}

	@GetMapping("/log-list")
	@PreAuth(menu = "finance_bills_all_log")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "账单合同日志", description = "传入contractId")
	public R<List<ContractLog>> logList(@Parameter(description = "合同ID") @RequestParam Long contractId) {
		return R.data(paymentService.logList(contractId));
	}

	@GetMapping("/notice-placeholder")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "收款通知占位", description = "当前阶段暂不开发通知单主流程")
	public R<PaymentNoticePlaceholderVO> noticePlaceholder() {
		return R.data(paymentService.noticePlaceholder());
	}

	@GetMapping("/notice-page")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 30)
	@Operation(summary = "收款通知分页", description = "传入收款通知查询条件")
	public R<IPage<PaymentNoticeVO>> noticePage(PaymentNoticeVO query, Query pageQuery) {
		return R.data(paymentService.selectNoticePage(Condition.getPage(pageQuery), query));
	}

	@GetMapping("/notice-summary")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 31)
	@Operation(summary = "收款通知汇总", description = "传入收款通知查询条件")
	public R<PaymentNoticeSummaryVO> noticeSummary(PaymentNoticeVO query) {
		return R.data(paymentService.noticeSummary(query));
	}

	@GetMapping("/notice-buildings")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 32)
	@Operation(summary = "收款通知楼宇选项", description = "传入收款通知查询条件")
	public R<List<String>> noticeBuildings(PaymentNoticeVO query) {
		return R.data(paymentService.noticeBuildingOptions(query));
	}

	@PostMapping("/notice-resend")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 33)
	@Operation(summary = "重新发送收款通知", description = "传入paymentId")
	public R<PaymentNoticeVO> noticeResend(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.data(paymentService.resendNotice(paymentId));
	}

	@PostMapping("/notice-miniapp-send")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 34)
	@Operation(summary = "小程序发送预留接口", description = "传入paymentId")
	public R<PaymentNoticeVO> noticeMiniAppSend(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.data(paymentService.sendMiniAppNotice(paymentId));
	}

	@PostMapping("/notice-generate")
	@PreAuth(menu = "finance_payment_notice")
	@ApiOperationSupport(order = 35)
	@Operation(summary = "生成收款通知文件", description = "传入paymentId")
	public R<ContractNoticeFileVO> noticeGenerate(@Parameter(description = "账单ID") @RequestParam Long paymentId) {
		return R.data(paymentService.generatePaymentNoticeFile(paymentId));
	}

}
