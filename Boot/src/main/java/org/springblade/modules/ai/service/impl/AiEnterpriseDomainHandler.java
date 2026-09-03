package org.springblade.modules.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.ai.pojo.entity.AiMessage;
import org.springblade.modules.ai.service.AiDomainHandler;
import org.springblade.modules.ai.service.DeepSeekChatClient;
import org.springblade.modules.ai.service.IAiEnterpriseReportService;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** 客户档案企业助手：普通问题返回文本，明确报告意图时返回持久化报告附件。 */
@Component
@RequiredArgsConstructor
public class AiEnterpriseDomainHandler implements AiDomainHandler {

	private static final List<String> ENTERPRISE_KEYWORDS = List.of(
		"企业", "公司", "工商", "法人", "注册资本", "经营", "行业", "规模", "联系人", "招商", "入驻", "租赁意向", "风险", "合规", "资质", "信用代码", "报告"
	);
	private static final List<String> REPORT_INTENT_KEYWORDS = List.of("报告", "出一份", "生成一份", "制作一份", "导出", "下载");
	private static final List<String> FOLLOW_UP_KEYWORDS = List.of("它", "该企业", "这家企业", "还有", "风险呢", "详细", "继续", "报告呢");

	private final AiEnterpriseCustomerResolver customerResolver;
	private final IAiEnterpriseReportService reportService;
	private final DeepSeekChatClient deepSeekChatClient;
	private final ObjectMapper objectMapper;

	@Override
	public String domain() {
		return "enterprise";
	}

	@Override
	public boolean supports(String question, List<AiMessage> recentMessages) {
		Optional<Boolean> aiDecision = deepSeekChatClient.classifyEnterpriseQuestion(recentMessages, question);
		return aiDecision.orElseGet(() -> supportsByLocalRule(question, recentMessages));
	}

	@Override
	public DomainAnswer answer(String question, List<AiMessage> recentMessages, AiAccessContext accessContext) {
		Customer customer = customerResolver.resolve(null, resolutionContext(question, recentMessages), accessContext);
		if (isReportIntent(question)) {
			AiEnterpriseReport report = reportService.generateForCustomer(customer, question, accessContext);
			return new DomainAnswer("已根据客户管理中的最新档案生成“" + report.getTitle() + "”，可在下方查看或下载 HTML 文件。", report.getId());
		}
		Map<String, Object> snapshot = buildSnapshot(customer);
		String prompt = "你是园区运营平台的企业助手。只回答当前企业客户档案中的基本信息、经营情况、招商入驻信息和风险字段。"
			+ "必须仅依据实时快照，不得编造，不得声称完成了外部核验；缺失字段明确回答待补充。使用简洁中文。实时客户快照：" + toJson(snapshot);
		String content = deepSeekChatClient.complete(prompt, recentMessages, question)
			.orElseGet(() -> fallbackAnswer(question, customer));
		return DomainAnswer.text(content);
	}

	@Override
	public String outOfScopeReply() {
		return "企业助手支持查询客户档案中的基本信息、经营状态、入驻需求和风险信息，也可以生成企业综合分析报告。";
	}

	private boolean supportsByLocalRule(String question, List<AiMessage> recentMessages) {
		String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
		if (ENTERPRISE_KEYWORDS.stream().anyMatch(normalized::contains)) return true;
		boolean hasEnterpriseContext = recentMessages.stream()
			.anyMatch(message -> domain().equals(message.getDomain()) && Boolean.TRUE.equals(message.getInScope()));
		return hasEnterpriseContext && FOLLOW_UP_KEYWORDS.stream().anyMatch(normalized::contains);
	}

	private boolean isReportIntent(String question) {
		String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
		return REPORT_INTENT_KEYWORDS.stream().anyMatch(normalized::contains);
	}

	private String resolutionContext(String question, List<AiMessage> recentMessages) {
		StringBuilder context = new StringBuilder(question == null ? "" : question);
		for (int index = recentMessages.size() - 1; index >= 0; index--) {
			AiMessage message = recentMessages.get(index);
			if ("user".equals(message.getRole()) && domain().equals(message.getDomain())) {
				context.append(' ').append(message.getContent());
			}
		}
		return context.toString();
	}

	private Map<String, Object> buildSnapshot(Customer customer) {
		Map<String, Object> snapshot = new LinkedHashMap<>();
		snapshot.put("企业名称", customer.getEnterpriseName());
		snapshot.put("统一社会信用代码", customer.getCreditCode());
		snapshot.put("法定代表人", customer.getLegalRepresentative());
		snapshot.put("注册资本万元", customer.getRegisteredCapital());
		snapshot.put("企业类型", customer.getEnterpriseType());
		snapshot.put("成立日期", customer.getEstablishDate());
		snapshot.put("营业期限", customer.getBusinessTerm());
		snapshot.put("经营状态", customer.getOperatingStatus());
		snapshot.put("所属行业", customer.getIndustry());
		snapshot.put("经营范围", customer.getBusinessScope());
		snapshot.put("主营业务", customer.getMainBusiness());
		snapshot.put("企业规模", customer.getScale());
		snapshot.put("上年度营收", customer.getLastYearRevenue());
		snapshot.put("意向面积", customer.getIntentArea());
		snapshot.put("使用用途", customer.getUsagePurpose());
		snapshot.put("租赁期限", customer.getLeaseTermLabel());
		snapshot.put("入驻状态", customer.getSettlementStatus());
		snapshot.put("基础核验状态", customer.getVerifyStatus());
		snapshot.put("行业准入结果", customer.getIndustryAccessResult());
		snapshot.put("风险等级", customer.getRiskLevel());
		snapshot.put("风险摘要", customer.getRiskSummary());
		snapshot.put("法律风险标识", flagText(customer.getLegalRiskFlag()));
		snapshot.put("高管风险标识", flagText(customer.getExecutiveRiskFlag()));
		snapshot.put("股东风险标识", flagText(customer.getShareholderRiskFlag()));
		snapshot.put("重大违法违规标识", flagText(customer.getMajorIllegalFlag()));
		snapshot.put("失信记录标识", flagText(customer.getDishonestFlag()));
		snapshot.put("行业处罚标识", flagText(customer.getIndustryPenaltyFlag()));
		return snapshot;
	}

	private String fallbackAnswer(String question, Customer customer) {
		String normalized = question == null ? "" : question.toLowerCase(Locale.ROOT);
		if (normalized.contains("风险") || normalized.contains("合规")) {
			return customer.getEnterpriseName() + "当前风险等级为" + value(customer.getRiskLevel()) + "，风险摘要为" + value(customer.getRiskSummary())
				+ "。法律风险、高管风险、股东风险、重大违法违规、失信记录、行业处罚标识依次为："
				+ flagText(customer.getLegalRiskFlag()) + "、" + flagText(customer.getExecutiveRiskFlag()) + "、"
				+ flagText(customer.getShareholderRiskFlag()) + "、" + flagText(customer.getMajorIllegalFlag()) + "、"
				+ flagText(customer.getDishonestFlag()) + "、" + flagText(customer.getIndustryPenaltyFlag()) + "。";
		}
		return customer.getEnterpriseName() + "的经营状态为" + value(customer.getOperatingStatus()) + "，所属行业为"
			+ value(customer.getIndustry()) + "，法定代表人为" + value(customer.getLegalRepresentative()) + "，注册资本为"
			+ value(customer.getRegisteredCapital()) + "万元，主营业务为" + value(customer.getMainBusiness()) + "。";
	}

	private String flagText(String flag) {
		if ("1".equals(flag)) return "已标记";
		if ("0".equals(flag)) return "未标记";
		return "待核验";
	}

	private String value(Object value) {
		return value == null || String.valueOf(value).isBlank() ? "待补充" : String.valueOf(value);
	}

	private String toJson(Map<String, Object> snapshot) {
		try {
			return objectMapper.writeValueAsString(snapshot);
		} catch (Exception exception) {
			return "{}";
		}
	}
}
