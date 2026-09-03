package org.springblade.modules.ai.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.ai.pojo.dto.AiAccessContext;
import org.springblade.modules.ai.pojo.dto.AiEnterpriseReportRequest;
import org.springblade.modules.ai.pojo.entity.AiEnterpriseReport;
import org.springblade.modules.ai.service.IAiEnterpriseReportService;
import org.springblade.modules.park.service.IParkPermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NonDS
@RestController
@RequiredArgsConstructor
@PreAuth(menu = "settlement_customer")
@RequestMapping("/blade-ai/report")
@Tag(name = "AI 企业报告", description = "企业综合信息分析报告")
public class AiEnterpriseReportController extends BladeController {

	private final IAiEnterpriseReportService reportService;
	private final IParkPermissionService parkPermissionService;

	@GetMapping("/list")
	@Operation(summary = "当前用户企业报告列表")
	public R<List<AiEnterpriseReport>> list() {
		return R.data(reportService.list());
	}

	@GetMapping("/detail")
	@Operation(summary = "企业报告详情")
	public R<AiEnterpriseReport> detail(@RequestParam Long reportId) {
		return R.data(reportService.detail(reportId));
	}

	@PostMapping("/generate")
	@PreAuth(menu = "settlement_customer_view")
	@Operation(summary = "生成并保存企业分析报告")
	public R<AiEnterpriseReport> generate(@Valid @RequestBody AiEnterpriseReportRequest request) {
		return R.data(reportService.generate(request, captureAccessContext()));
	}

	@PostMapping("/remove")
	@Operation(summary = "删除当前用户企业报告")
	public R<Boolean> remove(@RequestParam Long reportId) {
		reportService.remove(reportId);
		return R.data(true);
	}

	AiAccessContext captureAccessContext() {
		return new AiAccessContext(AuthUtil.getUserId(), AuthUtil.getTenantId(), parkPermissionService.authorizedParkIds());
	}
}
