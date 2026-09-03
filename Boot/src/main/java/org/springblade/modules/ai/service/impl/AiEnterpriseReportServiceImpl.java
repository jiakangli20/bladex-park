package org.springblade.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.ai.mapper.AiEnterpriseReportMapper;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiEnterpriseReportRequest;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.ai.service.IAiEnterpriseReportService;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/** 企业报告生成、持久化与用户隔离。 */
@Service
@RequiredArgsConstructor
public class AiEnterpriseReportServiceImpl implements IAiEnterpriseReportService {

	private static final String STATUS_COMPLETED = "completed";

	private final AiEnterpriseReportMapper reportMapper;
	private final AiEnterpriseCustomerResolver customerResolver;
	private final AiEnterpriseReportDomainHandler domainHandler;
	private final AiEnterpriseReportHtmlRenderer htmlRenderer;

	@Override
	public List<AiEnterpriseReport> list() {
		return reportMapper.selectList(Wrappers.<AiEnterpriseReport>lambdaQuery()
			.select(AiEnterpriseReport::getId, AiEnterpriseReport::getCustomerId,
				AiEnterpriseReport::getEnterpriseName, AiEnterpriseReport::getTitle,
				AiEnterpriseReport::getRequestContent, AiEnterpriseReport::getStatus,
				AiEnterpriseReport::getGeneratedTime, AiEnterpriseReport::getCreateTime)
			.eq(AiEnterpriseReport::getTenantId, currentTenantId())
			.eq(AiEnterpriseReport::getUserId, currentUserId())
			.orderByDesc(AiEnterpriseReport::getGeneratedTime)
			.orderByDesc(AiEnterpriseReport::getId));
	}

	@Override
	public AiEnterpriseReport detail(Long reportId) {
		return requireReport(reportId, currentUserId(), currentTenantId());
	}

	@Override
	public AiEnterpriseReport generate(AiEnterpriseReportRequest request, AiAccessContext accessContext) {
		AiAccessContext context = normalizeContext(accessContext);
		String requestContent = domainHandler.normalizeRequest(request.getRequestContent());
		if (!domainHandler.supports(requestContent)) {
			throw new ServiceException("企业报告仅支持工商、经营、招商入驻、合规和风险相关分析");
		}
		Customer customer = customerResolver.resolve(request.getCustomerId(), requestContent, context);
		return generateForCustomer(customer, requestContent, context);
	}

	@Override
	public AiEnterpriseReport generateForCustomer(Customer customer, String requestContent, AiAccessContext accessContext) {
		AiAccessContext context = normalizeContext(accessContext);
		if (customer == null || !isParkAuthorized(customer.getParkId(), context.authorizedParkIds())) {
			throw new ServiceException("企业不存在或无权访问");
		}
		AiEnterpriseReportDomainHandler.ReportAnalysis analysis = domainHandler.analyze(customer, requestContent);
		Date now = new Date();
		AiEnterpriseReport report = new AiEnterpriseReport();
		report.setTenantId(context.tenantId());
		report.setUserId(context.userId());
		report.setCustomerId(customer.getCustomerId());
		report.setEnterpriseName(customer.getEnterpriseName());
		report.setTitle(customer.getEnterpriseName() + "企业综合信息报告");
		report.setRequestContent(requestContent);
		report.setCompanyOverview(analysis.companyOverview());
		report.setRiskAnalysis(analysis.riskAnalysis());
		report.setHtmlContent(htmlRenderer.render(customer, analysis, now));
		report.setStatus(STATUS_COMPLETED);
		report.setGeneratedTime(now);
		report.setCreateTime(now);
		report.setUpdateTime(now);
		reportMapper.insert(report);
		return report;
	}

	@Override
	public void remove(Long reportId) {
		requireReport(reportId, currentUserId(), currentTenantId());
		reportMapper.delete(Wrappers.<AiEnterpriseReport>lambdaQuery()
			.eq(AiEnterpriseReport::getId, reportId)
			.eq(AiEnterpriseReport::getTenantId, currentTenantId())
			.eq(AiEnterpriseReport::getUserId, currentUserId()));
	}

	private AiEnterpriseReport requireReport(Long reportId, Long userId, String tenantId) {
		if (reportId == null) throw new ServiceException("报告不存在或无权访问");
		AiEnterpriseReport report = reportMapper.selectOne(Wrappers.<AiEnterpriseReport>lambdaQuery()
			.eq(AiEnterpriseReport::getId, reportId)
			.eq(AiEnterpriseReport::getTenantId, tenantId)
			.eq(AiEnterpriseReport::getUserId, userId));
		if (report == null) throw new ServiceException("报告不存在或无权访问");
		return report;
	}

	private AiAccessContext normalizeContext(AiAccessContext accessContext) {
		if (accessContext == null || accessContext.userId() == null) {
			throw new ServiceException("未获取到当前登录用户");
		}
		String tenantId = StringUtil.isBlank(accessContext.tenantId()) ? "000000" : accessContext.tenantId();
		return new AiAccessContext(accessContext.userId(), tenantId, accessContext.authorizedParkIds());
	}

	private boolean isParkAuthorized(Long parkId, List<Long> authorizedParkIds) {
		return authorizedParkIds == null || (parkId != null && authorizedParkIds.contains(parkId));
	}

	private String currentTenantId() {
		String tenantId = AuthUtil.getTenantId();
		return StringUtil.isBlank(tenantId) ? "000000" : tenantId;
	}

	private Long currentUserId() {
		Long userId = AuthUtil.getUserId();
		if (userId == null) throw new ServiceException("未获取到当前登录用户");
		return userId;
	}
}
