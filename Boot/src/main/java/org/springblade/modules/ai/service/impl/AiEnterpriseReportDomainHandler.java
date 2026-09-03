package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** 企业报告领域，只向模型提供选定客户的最小业务快照。 */
@Component
@RequiredArgsConstructor
public class AiEnterpriseReportDomainHandler {

	private static final String DEFAULT_REQUEST = "生成企业综合分析报告，概括企业经营情况并提示需要关注的风险。";
	private static final List<String> REPORT_KEYWORDS = List.of(
		"企业", "公司", "报告", "分析", "工商", "经营", "行业", "招商", "入驻", "租赁意向", "风险", "合规", "资质"
	);

	private final DeepSeekChatClient deepSeekChatClient;
	private final ObjectMapper objectMapper;

	public boolean supports(String requestContent) {
		String request = normalizeRequest(requestContent);
		Optional<Boolean> aiDecision = deepSeekChatClient.classifyEnterpriseReportRequest(request);
		return aiDecision.orElseGet(() -> supportsByLocalRule(request));
	}

	public ReportAnalysis analyze(Customer customer, String requestContent) {
		Map<String, Object> snapshot = buildSnapshot(customer);
		String prompt = "你是园区招商运营平台的企业分析助手。仅依据提供的客户管理实时快照撰写分析，不得编造、补全或推断未提供的事实。"
			+ "缺失字段应视为待补充，不得声称已经外部核验。输出必须是严格 JSON，不能使用 Markdown，格式为："
			+ "{\"companyOverview\":\"一段企业经营和招商匹配概述\",\"riskAnalysis\":\"一段风险提示和后续核验建议\"}。"
			+ "每段不超过260个汉字。客户管理实时快照：" + toJson(snapshot);
		Optional<String> response = deepSeekChatClient.complete(prompt, List.of(), normalizeRequest(requestContent));
		return response.flatMap(this::parseAnalysis).orElseGet(() -> fallbackAnalysis(customer));
	}

	public String normalizeRequest(String requestContent) {
		return requestContent == null || requestContent.isBlank() ? DEFAULT_REQUEST : requestContent.trim();
	}

	private boolean supportsByLocalRule(String requestContent) {
		String normalized = requestContent.toLowerCase(Locale.ROOT);
		return REPORT_KEYWORDS.stream().anyMatch(normalized::contains);
	}

	private Map<String, Object> buildSnapshot(Customer customer) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("企业名称", customer.getEnterpriseName());
		snapshot.put("统一社会信用代码", customer.getCreditCode());
		snapshot.put("法定代表人", customer.getLegalRepresentative());
		snapshot.put("成立日期", customer.getEstablishDate());
		snapshot.put("注册资本", customer.getRegisteredCapital());
		snapshot.put("企业类型", customer.getEnterpriseType());
		snapshot.put("营业期限", customer.getBusinessTerm());
		snapshot.put("经营状态", customer.getOperatingStatus());
		snapshot.put("所属行业", customer.getIndustry());
		snapshot.put("经营范围", customer.getBusinessScope());
		snapshot.put("主营业务", customer.getMainBusiness());
		snapshot.put("企业规模", customer.getScale());
		snapshot.put("上年度营收", customer.getLastYearRevenue());
		snapshot.put("主要合作客户", customer.getMajorClients());
		snapshot.put("意向载体类型", customer.getCarrierTypes());
		snapshot.put("意向面积", customer.getIntentArea());
		snapshot.put("使用用途", customer.getUsagePurpose());
		snapshot.put("租赁期限", customer.getLeaseTermLabel());
		snapshot.put("基础核验状态", customer.getVerifyStatus());
		snapshot.put("行业准入结果", customer.getIndustryAccessResult());
		snapshot.put("行业准入说明", customer.getIndustryAccessReason());
		snapshot.put("风险等级", customer.getRiskLevel());
		snapshot.put("风险摘要", customer.getRiskSummary());
		snapshot.put("法律风险", flagText(customer.getLegalRiskFlag()));
		snapshot.put("高管风险", flagText(customer.getExecutiveRiskFlag()));
		snapshot.put("股东风险", flagText(customer.getShareholderRiskFlag()));
		snapshot.put("重大违法违规", flagText(customer.getMajorIllegalFlag()));
		snapshot.put("失信记录", flagText(customer.getDishonestFlag()));
		snapshot.put("行业处罚", flagText(customer.getIndustryPenaltyFlag()));
		return snapshot;
	}

	private Optional<ReportAnalysis> parseAnalysis(String response) {
		try {
			String content = response.trim().replaceAll("^```json\\s*|\\s*```$", "");
			JsonNode node = objectMapper.readTree(content);
			String overview = node.path("companyOverview").asText("").trim();
			String risk = node.path("riskAnalysis").asText("").trim();
			if (!overview.isBlank() && !risk.isBlank()) {
				return Optional.of(new ReportAnalysis(overview, risk));
			}
		} catch (Exception ignored) {
			// 模型输出不符合契约时使用确定性报告内容。
		}
		return Optional.empty();
	}

	private ReportAnalysis fallbackAnalysis(Customer customer) {
		StringBuilder overview = new StringBuilder(value(customer.getEnterpriseName())).append("，");
		if (customer.getEstablishDate() != null) overview.append("已建立企业档案，");
		overview.append("所属行业为").append(value(customer.getIndustry())).append("，经营状态为")
			.append(value(customer.getOperatingStatus())).append("，企业规模为").append(value(customer.getScale())).append("。")
			.append("主营业务为").append(value(customer.getMainBusiness())).append("；现有客户管理信息可作为招商评估依据，缺失资料建议后续补充核验。");

		List<String> riskLabels = new java.util.ArrayList<>();
		addRisk(riskLabels, "法律风险", customer.getLegalRiskFlag());
		addRisk(riskLabels, "高管风险", customer.getExecutiveRiskFlag());
		addRisk(riskLabels, "股东风险", customer.getShareholderRiskFlag());
		addRisk(riskLabels, "重大违法违规", customer.getMajorIllegalFlag());
		addRisk(riskLabels, "失信记录", customer.getDishonestFlag());
		addRisk(riskLabels, "行业处罚", customer.getIndustryPenaltyFlag());
		String risk = riskLabels.isEmpty()
			? "当前客户档案未标记法律、高管、股东、重大违法违规、失信或行业处罚风险。该结果仅反映系统现有数据，建议结合最新公开信息持续复核。"
			: "当前客户档案已标记" + String.join("、", riskLabels) + "。建议在招商决策前核验风险发生时间、当前状态、影响范围及整改结果。";
		return new ReportAnalysis(overview.toString(), risk);
	}

	private void addRisk(List<String> labels, String label, String flag) {
		if ("1".equals(flag)) labels.add(label);
	}

	private String flagText(String flag) {
		if ("1".equals(flag)) return "已标记";
		if ("0".equals(flag)) return "未标记";
		return "待核验";
	}

	private String value(Object value) {
		if (value == null || (value instanceof String text && text.isBlank())) return "待补充";
		if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
		return String.valueOf(value);
	}

	private String toJson(Map<String, Object> snapshot) {
		try {
			return objectMapper.writeValueAsString(snapshot);
		} catch (Exception exception) {
			return "{}";
		}
	}

	public record ReportAnalysis(String companyOverview, String riskAnalysis) {
	}
}
