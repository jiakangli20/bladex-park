package org.springblade.modules.ai.service.impl;

import org.springblade.modules.business.pojo.entity.Customer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/** 将可信业务字段和 AI 文本渲染为无脚本、自包含的 HTML 报告。 */
@Component
public class AiEnterpriseReportHtmlRenderer {

	private static final String DATE_PATTERN = "yyyy-MM-dd";

	public String render(Customer customer, AiEnterpriseReportDomainHandler.ReportAnalysis analysis, Date generatedTime) {
		return """
			<!DOCTYPE html>
			<html lang="zh-CN">
			<head>
			<meta charset="UTF-8">
			<meta name="viewport" content="width=device-width, initial-scale=1.0">
			<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data:">
			<title>%s - 企业综合信息报告</title>
			<style>
			:root{--brand:#1059c6;--brand-dark:#0b47a0;--brand-soft:#eaf2fd;--bg:#f4f4f6;--paper:#fafafa;--line:#e5e9f0;--text:#283446;--muted:#788496;--ok:#168a55;--ok-bg:#ecf8f2;--warn:#c77700;--warn-bg:#fff6e7;--danger:#d33c4a;--danger-bg:#fff0f1}
			*{box-sizing:border-box;margin:0;padding:0}body{padding:16px 12px;background:var(--bg);color:var(--text);font-family:-apple-system,BlinkMacSystemFont,"Segoe UI","PingFang SC","Microsoft YaHei",sans-serif;-webkit-font-smoothing:antialiased}.paper{max-width:800px;margin:auto;overflow:hidden;border:1px solid var(--line);border-radius:10px;background:var(--paper);box-shadow:0 16px 40px rgba(32,51,78,.12)}.report-header{padding:42px 28px 34px;background:linear-gradient(135deg,var(--brand-dark),var(--brand));color:#fff;text-align:center}.eyebrow{margin-bottom:10px;font-size:11px;font-weight:600;letter-spacing:2px;opacity:.8}.company{font-size:28px;line-height:1.4}.meta{margin-top:18px;padding-top:14px;border-top:1px solid rgba(255,255,255,.2);font-size:12px;opacity:.82}.card{margin:16px;padding:30px;border:1px solid #edf0f4;border-radius:10px;background:#fff;box-shadow:0 1px 3px rgba(24,39,63,.04)}.sec{display:flex;align-items:center;gap:10px;margin-bottom:20px}.sec-icon{display:grid;width:36px;height:36px;place-items:center;border-radius:8px;background:var(--brand-soft);color:var(--brand);font-size:14px;font-weight:700}.sec h2{font-size:18px}.sec-en{margin-left:auto;color:#a0a8b4;font-size:10px;font-weight:600;letter-spacing:2px}.sub{display:flex;align-items:center;gap:8px;margin:20px 0 12px}.sub:first-of-type{margin-top:0}.sub:before{width:3px;height:14px;border-radius:2px;background:var(--brand);content:""}.sub h3{font-size:14px}.sub-note{margin-left:auto;padding:2px 8px;border-radius:999px;background:#f2f4f7;color:#9099a7;font-size:11px}.kv-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px 30px}.kv-item{min-width:0}.kv-item.full{grid-column:1/-1}.kv-item dt{margin-bottom:4px;color:#919aa8;font-size:12px}.kv-item dd{font-size:14px;font-weight:600;line-height:1.65;word-break:break-word}.status,.risk-value{display:inline-flex;align-items:center;gap:6px;padding:5px 11px;border-radius:999px;font-size:12px;font-weight:600}.status:before,.risk-value:before{width:6px;height:6px;border-radius:50%%;background:currentColor;content:""}.ok{color:var(--ok);background:var(--ok-bg)}.warn{color:var(--warn);background:var(--warn-bg)}.danger{color:var(--danger);background:var(--danger-bg)}.insight{position:relative;overflow:hidden;padding:18px 20px;border:1px solid var(--line);border-radius:8px;background:#f8fafc;color:#526075;font-size:14px;line-height:1.9;text-align:justify}.insight:before{position:absolute;inset:0 auto 0 0;width:3px;background:var(--brand);content:""}.risk-list{list-style:none}.risk-list li{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:8px;padding:12px 14px;border:1px solid var(--line);border-radius:8px}.risk-label{font-size:13px;font-weight:600}.risk-label small{display:block;margin-top:2px;color:#9aa3b0;font-size:10px;font-weight:400}.foot{padding:8px 28px 28px;color:#98a1ae;font-size:11px;line-height:1.8;text-align:center}@media(max-width:640px){.company{font-size:22px}.card{margin:12px;padding:22px}.kv-grid{grid-template-columns:1fr}.kv-item.full{grid-column:auto}.sec-en{display:none}}@media print{body{padding:0;background:#fff}.paper{max-width:none;border:0;border-radius:0;box-shadow:none}.card{break-inside:avoid;box-shadow:none}}
			</style>
			</head>
			<body><main class="paper">
			<header class="report-header"><p class="eyebrow">AI 分析企业综合信息报告</p><h1 class="company">%s</h1><p class="meta">生成时间：%s</p></header>
			<section class="card"><header class="sec"><span class="sec-icon">01</span><h2>基本信息</h2><span class="sec-en">BASIC INFO</span></header>
			<div class="sub"><h3>身份信息</h3></div><dl class="kv-grid">
			<div class="kv-item"><dt>企业名称</dt><dd>%s</dd></div><div class="kv-item"><dt>统一社会信用代码</dt><dd>%s</dd></div>
			<div class="kv-item full"><dt>注册地址</dt><dd>%s</dd></div><div class="kv-item"><dt>法定代表人</dt><dd>%s</dd></div>
			<div class="kv-item"><dt>注册资本</dt><dd>%s</dd></div><div class="kv-item full"><dt>企业类型</dt><dd>%s</dd></div>
			<div class="kv-item"><dt>企业联系电话</dt><dd>%s</dd></div><div class="kv-item"><dt>成立日期</dt><dd>%s</dd></div>
			<div class="kv-item"><dt>营业期限</dt><dd>%s</dd></div><div class="kv-item"><dt>行业分类</dt><dd>%s</dd></div>
			<div class="kv-item"><dt>经营状态</dt><dd><span class="status %s">%s</span></dd></div><div class="kv-item full"><dt>经营范围</dt><dd>%s</dd></div>
			</dl></section>
			<section class="card"><header class="sec"><span class="sec-icon">02</span><h2>AI 分析</h2><span class="sec-en">AI INSIGHTS</span></header>
			<div class="sub"><h3>企业基本情况</h3><span class="sub-note">自动生成</span></div><p class="insight">%s</p>
			<div class="sub"><h3>重点风险核验</h3><span class="sub-note">6 项</span></div><p class="insight">%s</p>
			<ul class="risk-list">%s</ul></section>
			<footer class="foot"><p>本报告由系统根据客户管理现有数据自动生成，仅供内部招商评估参考，不构成法律意见或决策依据。</p></footer>
			</main></body></html>
			""".formatted(
			escape(customer.getEnterpriseName()), escape(customer.getEnterpriseName()), formatDate(generatedTime),
			escape(customer.getEnterpriseName()), escape(customer.getCreditCode()), escape(customer.getRegisteredAddress()),
			escape(customer.getLegalRepresentative()), formatCapital(customer.getRegisteredCapital()), escape(customer.getEnterpriseType()),
			escape(customer.getEnterprisePhone()), formatDate(customer.getEstablishDate()), escape(customer.getBusinessTerm()),
			escape(customer.getIndustry()), operatingStatusClass(customer.getOperatingStatus()), escape(customer.getOperatingStatus()),
			escape(customer.getBusinessScope()), escape(analysis.companyOverview()), escape(analysis.riskAnalysis()), renderRisks(customer)
		);
	}

	private String renderRisks(Customer customer) {
		return riskRow("法律风险", "Legal Risk", customer.getLegalRiskFlag())
			+ riskRow("高管风险", "Executive Risk", customer.getExecutiveRiskFlag())
			+ riskRow("股东风险", "Shareholder Risk", customer.getShareholderRiskFlag())
			+ riskRow("重大违法违规", "Serious Violations", customer.getMajorIllegalFlag())
			+ riskRow("失信记录", "Dishonesty Records", customer.getDishonestFlag())
			+ riskRow("行业处罚", "Industry Penalties", customer.getIndustryPenaltyFlag());
	}

	private String riskRow(String label, String english, String flag) {
		String cssClass = "1".equals(flag) ? "danger" : "0".equals(flag) ? "ok" : "warn";
		String value = "1".equals(flag) ? "已标记" : "0".equals(flag) ? "未标记" : "待核验";
		return "<li><span class=\"risk-label\">" + escape(label) + "<small>" + escape(english)
			+ "</small></span><span class=\"risk-value " + cssClass + "\">" + value + "</span></li>";
	}

	private String operatingStatusClass(String status) {
		if (status == null || status.isBlank()) return "warn";
		return status.contains("存续") || status.contains("在业") || status.contains("正常") ? "ok" : "warn";
	}

	private String formatCapital(BigDecimal value) {
		return value == null ? "待补充" : escape(value.stripTrailingZeros().toPlainString() + " 万元");
	}

	private String formatDate(Date value) {
		return value == null ? "待补充" : new SimpleDateFormat(DATE_PATTERN).format(value);
	}

	private String escape(Object value) {
		if (value == null || String.valueOf(value).isBlank()) return "待补充";
		return HtmlUtils.htmlEscape(String.valueOf(value));
	}
}
