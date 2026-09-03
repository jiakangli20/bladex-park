package org.springblade.modules.ai.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 企业分析报告生成请求。 */
@Data
public class AiEnterpriseReportRequest {

	private Long customerId;

	@NotBlank(message = "请输入企业报告问题")
	@Size(max = 500, message = "企业报告问题不能超过500字")
	private String requestContent;
}
