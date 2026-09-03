package org.springblade.modules.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.business.mapper.CustomerMapper;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** 在已授权客户集合内解析问题所指企业，不向模型暴露客户目录。 */
@Component
@RequiredArgsConstructor
public class AiEnterpriseCustomerResolver {

	private final CustomerMapper customerMapper;
	private final ICustomerService customerService;

	public Customer resolve(Long customerId, String question, AiAccessContext context) {
		if (customerId != null) {
			Customer customer = customerService.selectCustomerById(customerId);
			if (customer == null || !isParkAuthorized(customer.getParkId(), context.authorizedParkIds())) {
				throw new ServiceException("企业不存在或无权访问");
			}
			return customer;
		}
		List<CustomerMatch> matches = customerMapper.selectCustomerList(new Customer(), context.authorizedParkIds()).stream()
			.filter(customer -> isParkAuthorized(customer.getParkId(), context.authorizedParkIds()))
			.map(customer -> new CustomerMatch(customer, matchScore(customer, question)))
			.filter(match -> match.score() > 0)
			.sorted((left, right) -> Integer.compare(right.score(), left.score()))
			.toList();
		if (matches.isEmpty()) {
			throw new ServiceException("未从问题中识别到有权限的企业，请使用客户管理中的完整企业名称重新提问");
		}
		int bestScore = matches.get(0).score();
		List<CustomerMatch> bestMatches = matches.stream().filter(match -> match.score() == bestScore).toList();
		if (bestMatches.size() > 1) {
			String names = bestMatches.stream().limit(3).map(match -> match.customer().getEnterpriseName())
				.collect(Collectors.joining("、"));
			throw new ServiceException("识别到多个匹配企业：" + names + "，请补充完整企业名称");
		}
		return bestMatches.get(0).customer();
	}

	private int matchScore(Customer customer, String questionText) {
		String question = normalizeForMatch(questionText);
		String creditCode = customer.getCreditCode();
		if (StringUtil.isNotBlank(creditCode) && questionText != null
			&& questionText.toUpperCase(Locale.ROOT).contains(creditCode.trim().toUpperCase(Locale.ROOT))) {
			return 4000 + creditCode.length();
		}
		String enterpriseName = normalizeForMatch(customer.getEnterpriseName());
		if (enterpriseName.length() >= 2 && question.contains(enterpriseName)) {
			return 3000 + enterpriseName.length();
		}
		String shortName = enterpriseName.replaceFirst("(有限责任公司|股份有限公司|集团有限公司|有限公司|集团|公司)$", "");
		return shortName.length() >= 4 && question.contains(shortName) ? 1000 + shortName.length() : 0;
	}

	private boolean isParkAuthorized(Long parkId, List<Long> authorizedParkIds) {
		return authorizedParkIds == null || (parkId != null && authorizedParkIds.contains(parkId));
	}

	private String normalizeForMatch(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{P}\\p{S}]", "");
	}

	private record CustomerMatch(Customer customer, int score) {
	}
}
