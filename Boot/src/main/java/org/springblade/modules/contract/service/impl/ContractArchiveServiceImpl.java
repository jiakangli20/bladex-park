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
package org.springblade.modules.contract.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.modules.contract.mapper.ContractArchiveMapper;
import org.springblade.modules.contract.mapper.ContractLogMapper;
import org.springblade.modules.contract.mapper.ContractPaymentMapper;
import org.springblade.modules.contract.mapper.ContractSupplementAgreementMapper;
import org.springblade.modules.contract.pojo.entity.ContractLog;
import org.springblade.modules.contract.pojo.entity.ContractPayment;
import org.springblade.modules.contract.pojo.entity.ContractSupplementAgreement;
import org.springblade.modules.contract.pojo.vo.ContractArchiveDetailVO;
import org.springblade.modules.contract.pojo.vo.ContractArchiveVO;
import org.springblade.modules.contract.pojo.vo.ContractChangeArchiveVO;
import org.springblade.modules.contract.pojo.vo.TerminationArchiveVO;
import org.springblade.modules.contract.service.IContractArchiveService;
import org.springblade.modules.contract.service.IContractNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 合同归档服务实现类
 *
 * @author Chill
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractArchiveServiceImpl implements IContractArchiveService {

	private static final String PAID_STATUS = "1";
	private static final Integer TERMINATION_SETTLED = 1;

	private final ContractArchiveMapper contractArchiveMapper;
	private final ContractPaymentMapper contractPaymentMapper;
	private final ContractLogMapper contractLogMapper;
	private final ContractSupplementAgreementMapper contractSupplementAgreementMapper;
	private final IContractNoticeService contractNoticeService;

	@Override
	public IPage<ContractArchiveVO> selectArchivePage(IPage<ContractArchiveVO> page, ContractArchiveVO contract) {
		return page.setRecords(contractArchiveMapper.selectArchivePage(page, contract));
	}

	@Override
	public ContractArchiveDetailVO getArchiveDetail(Long contractId) {
		ContractArchiveVO contract = requireContract(contractId);
		List<ContractPayment> payments = safePayments(contractId);
		List<ContractChangeArchiveVO> changes = safeChanges(contractId);
		List<ContractSupplementAgreement> supplements = safeSupplements(contractId);
		List<TerminationArchiveVO> terminations = safeTerminations(contractId);
		List<ContractLog> logs = safeLogs(contractId);

		ContractArchiveDetailVO detail = new ContractArchiveDetailVO();
		detail.setContract(contract);
		detail.setPayments(payments);
		detail.setChanges(changes);
		detail.setSupplements(supplements);
		detail.setTerminations(terminations);
		detail.setLogs(logs);
		detail.setArchiveStep(calculateArchiveStep(payments, supplements, terminations));
		return detail;
	}

	@Override
	public Kv exportApproval(Long contractId) {
		requireContract(contractId);
		return contractNoticeService
			.buildNoticePreview(IContractNoticeService.NOTICE_CONTRACT_APPROVAL, null, contractId, null)
			.set("downloadUrl", "/blade-contract/print/contract-approval/" + contractId);
	}

	@Override
	public Kv printContract(Long contractId) {
		ContractArchiveVO contract = requireContract(contractId);
		return Kv.create()
			.set("contractId", contract.getContractId())
			.set("contractNo", contract.getContractNo())
			.set("templateName", "园区租赁合同打印模板")
			.set("html", buildContractPrintHtml(contract))
			.set("generatedAt", DateUtil.formatDateTime(new Date()));
	}

	@Override
	public List<ContractSupplementAgreement> listSupplementAgreements(Long contractId) {
		requireContract(contractId);
		return listSupplements(contractId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveSupplementAgreement(ContractSupplementAgreement agreement) {
		if (agreement == null || agreement.getContractId() == null) {
			throw new ServiceException("合同ID不能为空");
		}
		if (Func.isBlank(agreement.getAgreementName())) {
			throw new ServiceException("协议名称不能为空");
		}
		if (Func.isBlank(agreement.getFileUrl())) {
			throw new ServiceException("请上传补充协议文件");
		}
		requireContract(agreement.getContractId());
		Date now = DateUtil.now();
		String userName = currentUserName();
		agreement.setDelFlag("0");
		if (agreement.getAgreementId() == null) {
			agreement.setCreateBy(userName);
			agreement.setCreateTime(now);
			boolean result = contractSupplementAgreementMapper.insert(agreement) > 0;
			if (result) {
				addLog(agreement.getContractId(), "supplement_agreement", "归档补充协议：" + agreement.getAgreementName());
			}
			return result;
		}
		agreement.setUpdateBy(userName);
		agreement.setUpdateTime(now);
		boolean result = contractSupplementAgreementMapper.updateById(agreement) > 0;
		if (result) {
			addLog(agreement.getContractId(), "supplement_agreement_update", "更新补充协议：" + agreement.getAgreementName());
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean removeSupplementAgreement(Long agreementId) {
		if (agreementId == null) {
			throw new ServiceException("补充协议ID不能为空");
		}
		ContractSupplementAgreement agreement = contractSupplementAgreementMapper.selectById(agreementId);
		if (agreement == null || !"0".equals(agreement.getDelFlag())) {
			throw new ServiceException("补充协议不存在");
		}
		ContractSupplementAgreement update = new ContractSupplementAgreement();
		update.setAgreementId(agreementId);
		update.setDelFlag("1");
		update.setUpdateBy(currentUserName());
		update.setUpdateTime(DateUtil.now());
		boolean result = contractSupplementAgreementMapper.updateById(update) > 0;
		if (result) {
			addLog(agreement.getContractId(), "supplement_agreement_delete", "删除补充协议：" + agreement.getAgreementName());
		}
		return result;
	}

	private ContractArchiveVO requireContract(Long contractId) {
		if (contractId == null) {
			throw new ServiceException("合同ID不能为空");
		}
		ContractArchiveVO contract = contractArchiveMapper.selectArchiveById(contractId);
		if (contract == null) {
			throw new ServiceException("合同不存在");
		}
		return contract;
	}

	private List<ContractPayment> safePayments(Long contractId) {
		try {
			return contractPaymentMapper.selectByContractId(contractId);
		} catch (Exception exception) {
			log.warn("合同归档缴费记录加载失败，contractId={}", contractId, exception);
			return new ArrayList<>();
		}
	}

	private List<ContractSupplementAgreement> safeSupplements(Long contractId) {
		try {
			return listSupplements(contractId);
		} catch (Exception exception) {
			log.warn("合同归档补充协议加载失败，contractId={}", contractId, exception);
			return new ArrayList<>();
		}
	}

	private List<ContractChangeArchiveVO> safeChanges(Long contractId) {
		try {
			return contractArchiveMapper.selectChangesByContractId(contractId);
		} catch (Exception exception) {
			log.warn("合同归档变更记录加载失败，contractId={}", contractId, exception);
			return new ArrayList<>();
		}
	}

	private List<TerminationArchiveVO> safeTerminations(Long contractId) {
		try {
			return contractArchiveMapper.selectTerminationsByContractId(contractId);
		} catch (Exception exception) {
			log.warn("合同归档退租记录加载失败，contractId={}", contractId, exception);
			return new ArrayList<>();
		}
	}

	private List<ContractLog> safeLogs(Long contractId) {
		try {
			return contractLogMapper.selectByContractId(contractId);
		} catch (Exception exception) {
			log.warn("合同归档日志加载失败，contractId={}", contractId, exception);
			return new ArrayList<>();
		}
	}

	private Integer calculateArchiveStep(List<ContractPayment> payments, List<ContractSupplementAgreement> supplements, List<TerminationArchiveVO> terminations) {
		if (terminations.stream().anyMatch(termination -> TERMINATION_SETTLED.equals(termination.getStatus()))) {
			return 3;
		}
		if (!supplements.isEmpty() || !terminations.isEmpty()) {
			return 2;
		}
		if (payments.stream().anyMatch(payment -> PAID_STATUS.equals(payment.getPayStatus()))) {
			return 1;
		}
		return 0;
	}

	private List<ContractSupplementAgreement> listSupplements(Long contractId) {
		return contractSupplementAgreementMapper.selectList(Wrappers.<ContractSupplementAgreement>lambdaQuery()
			.eq(ContractSupplementAgreement::getContractId, contractId)
			.eq(ContractSupplementAgreement::getDelFlag, "0")
			.orderByDesc(ContractSupplementAgreement::getCreateTime)
			.orderByDesc(ContractSupplementAgreement::getAgreementId));
	}

	private void addLog(Long contractId, String action, String actionDesc) {
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
		return Func.isBlank(userName) ? "system" : userName;
	}

	private String buildApprovalHtml(ContractArchiveVO contract) {
		Map<String, String> values = buildContractApprovalValues(contract);
		StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.8;padding:24px;\">");
		html.append("<h2 style=\"text-align:center;\">合同会签审批表</h2>");
		html.append("<table style=\"width:100%;border-collapse:collapse;\">");
		values.forEach((key, val) -> html.append("<tr>")
			.append("<th style=\"width:180px;text-align:left;border:1px solid #dcdfe6;background:#f5f7fa;padding:8px;\">")
			.append(escapeHtml(key))
			.append("</th><td style=\"border:1px solid #dcdfe6;padding:8px;\">")
			.append(escapeHtml(val))
			.append("</td></tr>"));
		html.append("</table>");
		html.append("<p style=\"margin-top:24px;color:#606266;\">本文件为迁移阶段 HTML 审批表，模板导出模块迁移完成后可替换为正式模板。</p>");
		html.append("</div>");
		return html.toString();
	}

	private Map<String, String> buildContractApprovalValues(ContractArchiveVO contract) {
		Map<String, String> values = new LinkedHashMap<>();
		values.put("申请单位（个人）", value(contract.getCustomerName()));
		values.put("申请时间", formatDate(contract.getCreateTime(), "yyyy-MM-dd"));
		values.put("合同编号", value(contract.getContractNo()));
		values.put("合同名称", value(contract.getContractName()));
		values.put("项目编号", contract.getProjectId() == null ? "" : String.valueOf(contract.getProjectId()));
		values.put("所属园区", value(contract.getParkName()));
		values.put("租赁楼层", value(firstNonBlank(contract.getRoomName(), contract.getBuildingName())));
		values.put("租赁楼层、面积", value(firstNonBlank(contract.getRoomName(), contract.getBuildingName())) + " " + formatNumber(contract.getRentArea()) + "㎡");
		values.put("合同有效期", formatDate(contract.getStartDate(), "yyyy-MM-dd") + " 至 " + formatDate(contract.getEndDate(), "yyyy-MM-dd"));
		values.put("月租金（元）", formatNumber(contract.getMonthlyRent()));
		values.put("单价（元）", formatNumber(contract.getRentPrice()));
		values.put("保证金（元）", formatNumber(contract.getDeposit()));
		values.put("跟进人", value(contract.getFollowUser()));
		values.put("管理费", formatNumber(contract.getManagementFee()));
		values.put("公摊费", formatNumber(contract.getPublicFee()));
		values.put("租金递增", buildRentIncreaseSummary(contract));
		values.put("滞纳金", buildLateFeeSummary(contract));
		values.put("经办人", value(contract.getCreateBy()));
		values.put("申请内容", buildContractSummary(contract));
		values.put("备注", value(contract.getRemark()));
		return values;
	}

	private String buildContractSummary(ContractArchiveVO contract) {
		List<String> parts = new ArrayList<>();
		if (Func.isNotBlank(contract.getCustomerName())) {
			parts.add(contract.getCustomerName());
		}
		if (Func.isNotBlank(contract.getRoomName())) {
			parts.add("租赁房源：" + contract.getRoomName());
		}
		if (contract.getRentArea() != null) {
			parts.add("面积：" + formatNumber(contract.getRentArea()) + "㎡");
		}
		if (contract.getMonthlyRent() != null) {
			parts.add("月租金：" + formatNumber(contract.getMonthlyRent()) + "元");
		}
		if (contract.getManagementFee() != null) {
			parts.add("管理费：" + formatNumber(contract.getManagementFee()) + "元/月");
		}
		if (contract.getPublicFee() != null) {
			parts.add("公摊费：" + formatNumber(contract.getPublicFee()) + "元/月");
		}
		if (contract.getStartDate() != null || contract.getEndDate() != null) {
			parts.add("合同期：" + formatDate(contract.getStartDate(), "yyyy-MM-dd") + " 至 " + formatDate(contract.getEndDate(), "yyyy-MM-dd"));
		}
		return String.join("；", parts);
	}

	private String buildContractPrintHtml(ContractArchiveVO contract) {
		StringBuilder html = new StringBuilder();
		html.append("<div style=\"font-family:Arial,'Microsoft YaHei',sans-serif;line-height:1.8;padding:24px;\">");
		html.append("<h2 style=\"text-align:center;\">园区租赁合同（模拟模板）</h2>");
		html.append("<p><strong>合同编号：</strong>").append(escapeHtml(value(contract.getContractNo()))).append("</p>");
		html.append("<p><strong>甲方：</strong>园区运营方</p>");
		html.append("<p><strong>乙方：</strong>").append(escapeHtml(value(contract.getCustomerName()))).append("</p>");
		html.append("<p><strong>租赁房源：</strong>").append(escapeHtml(value(firstNonBlank(contract.getRoomName(), contract.getBuildingName())))).append("</p>");
		html.append("<p><strong>所属园区：</strong>").append(escapeHtml(value(contract.getParkName()))).append("</p>");
		html.append("<p><strong>租赁面积：</strong>").append(escapeHtml(formatNumber(contract.getRentArea()))).append(" ㎡</p>");
		html.append("<p><strong>租金标准：</strong>").append(escapeHtml(formatNumber(contract.getRentPrice()))).append(" 元/㎡/月，月租金 ")
			.append(escapeHtml(formatNumber(contract.getMonthlyRent()))).append(" 元</p>");
		html.append("<p><strong>租赁周期：</strong>").append(escapeHtml(formatDate(contract.getStartDate(), "yyyy-MM-dd"))).append(" 至 ")
			.append(escapeHtml(formatDate(contract.getEndDate(), "yyyy-MM-dd"))).append("</p>");
		html.append("<p><strong>保证金：</strong>").append(escapeHtml(formatNumber(contract.getDeposit()))).append(" 元</p>");
		html.append("<p><strong>管理费：</strong>").append(escapeHtml(formatNumber(contract.getManagementFee()))).append(" 元/月，公摊费 ")
			.append(escapeHtml(formatNumber(contract.getPublicFee()))).append(" 元/月</p>");
		html.append("<p><strong>租金递增：</strong>").append(escapeHtml(value(buildRentIncreaseSummary(contract)))).append("</p>");
		html.append("<p><strong>滞纳金：</strong>").append(escapeHtml(value(buildLateFeeSummary(contract)))).append("</p>");
		html.append("<p style=\"margin-top:32px;color:#666;\">本文件为系统模拟打印模板，正式合同文本以后续上传模板为准。</p>");
		html.append("</div>");
		return html.toString();
	}

	private String buildRentIncreaseSummary(ContractArchiveVO contract) {
		if (Func.isBlank(contract.getRentIncreaseNode()) || "none".equals(contract.getRentIncreaseNode())) {
			return "";
		}
		Map<String, String> nodeMap = new HashMap<>();
		nodeMap.put("annual", "年度递增");
		nodeMap.put("halfYear", "半年递增");
		nodeMap.put("custom", "自定义节点");
		String unit = "amount".equals(contract.getRentIncreaseUnit()) ? "元" : "%";
		String rate = contract.getRentIncreaseRate() == null ? "" : "，" + formatNumber(contract.getRentIncreaseRate()) + unit;
		return nodeMap.getOrDefault(contract.getRentIncreaseNode(), contract.getRentIncreaseNode()) + rate;
	}

	private String buildLateFeeSummary(ContractArchiveVO contract) {
		if (contract.getLateFeeRatio() == null) {
			return "";
		}
		Map<String, String> unitMap = new HashMap<>();
		unitMap.put("percentPerDay", "%/日");
		unitMap.put("percentPerMonth", "%/月");
		unitMap.put("amountPerDay", "元/日");
		String cap = contract.getLateFeeCap() == null ? "" : "，上限" + formatNumber(contract.getLateFeeCap()) + "元";
		return formatNumber(contract.getLateFeeRatio()) + unitMap.getOrDefault(contract.getLateFeeUnit(), "") + cap;
	}

	private String value(String value) {
		return value == null ? "" : value;
	}

	private String firstNonBlank(String first, String second) {
		return Func.isNotBlank(first) ? first : second;
	}

	private String formatNumber(BigDecimal value) {
		return value == null ? "" : value.stripTrailingZeros().toPlainString();
	}

	private String formatDate(Date value, String pattern) {
		return value == null ? "" : new SimpleDateFormat(pattern).format(value);
	}

	private String escapeHtml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&#39;");
	}

}
