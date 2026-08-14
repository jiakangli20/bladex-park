/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 入驻审核业务入口，避免审核人员依赖商机管理按钮权限。
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth("hasMenu('business_opportunity') || hasMenu('settlement_project_approval')")
@RequestMapping("/blade-park/tenant-entry")
@io.swagger.v3.oas.annotations.tags.Tag(name = "入驻审核", description = "入驻审核候选商机及审批表接口")
public class TenantEntryApprovalController extends BladeController {

	private final IBusinessOpportunityService businessOpportunityService;

	@GetMapping("/candidate-page")
	@PreAuth(menu = "settlement_project_approval_add")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "可发起商机", description = "查询未发起或驳回后可重新发起的商机")
	public R<IPage<BusinessOpportunity>> candidatePage(BusinessOpportunity opportunity, Query query) {
		opportunity.setTenantEntryCandidate(true);
		return R.data(businessOpportunityService.selectBusinessOpportunityPage(Condition.getPage(query), opportunity));
	}

	@GetMapping("/approval-form-preview/{opportunityId}")
	@PreAuth(menu = "settlement_project_approval_form")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "企业入驻审批表预览")
	public R<Kv> preview(@PathVariable Long opportunityId,
					 @RequestParam(value = "processInsId", required = false) String processInsId) {
		return R.data(businessOpportunityService.previewTenantEntryApprovalForm(opportunityId, processInsId));
	}

	@GetMapping("/candidate-detail/{opportunityId}")
	@PreAuth("hasMenu('business_opportunity_audit') || hasMenu('settlement_project_approval_add')")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "可发起商机详情")
	public R<BusinessOpportunity> candidateDetail(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.selectBusinessOpportunityById(opportunityId));
	}

	@GetMapping("/approval-form/{opportunityId}")
	@PreAuth(menu = "settlement_project_approval_form")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "企业入驻审批表下载")
	public void export(@PathVariable Long opportunityId,
				   @RequestParam(value = "processInsId", required = false) String processInsId,
				   HttpServletResponse response) {
		writeDocument(businessOpportunityService.exportTenantEntryApprovalForm(opportunityId, processInsId), response);
	}

	private void writeDocument(ContractNoticeFileVO document, HttpServletResponse response) {
		try {
			String encodedFileName = URLEncoder.encode(document.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			response.setContentType(document.getContentType());
			response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
			response.setContentLength(document.getFileBytes().length);
			response.getOutputStream().write(document.getFileBytes());
			response.getOutputStream().flush();
		} catch (Exception exception) {
			throw new RuntimeException("导出企业入驻审批表失败", exception);
		}
	}

}
