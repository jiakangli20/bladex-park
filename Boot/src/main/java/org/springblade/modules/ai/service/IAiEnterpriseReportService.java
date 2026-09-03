package org.springblade.modules.ai.service;

import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiEnterpriseReportRequest;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.business.pojo.entity.Customer;

import java.util.List;

public interface IAiEnterpriseReportService {

	List<AiEnterpriseReport> list();

	AiEnterpriseReport detail(Long reportId);

	AiEnterpriseReport generate(AiEnterpriseReportRequest request, AiAccessContext accessContext);

	AiEnterpriseReport generateForCustomer(Customer customer, String requestContent, AiAccessContext accessContext);

	void remove(Long reportId);
}
