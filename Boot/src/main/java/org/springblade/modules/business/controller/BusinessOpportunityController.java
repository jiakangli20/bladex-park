/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.business.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tool.api.R;
import org.springblade.modules.business.pojo.entity.BusinessOpportunity;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFile;
import org.springblade.modules.business.pojo.entity.BusinessOpportunityFollow;
import org.springblade.modules.business.pojo.entity.Tag;
import org.springblade.modules.business.service.IBusinessOpportunityService;
import org.springblade.modules.contract.pojo.vo.ContractNoticeFileVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 商机管理控制器.
 *
 * @author BladeX
 */
@NonDS
@RestController
@AllArgsConstructor
@PreAuth(menu = "business_opportunity")
@RequestMapping("/blade-park/opportunity")
@io.swagger.v3.oas.annotations.tags.Tag(name = "商机管理", description = "商机管理接口")
public class BusinessOpportunityController extends BladeController {

	private final IBusinessOpportunityService businessOpportunityService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入opportunityId")
	public R<BusinessOpportunity> detail(@Parameter(description = "商机ID") @RequestParam Long opportunityId) {
		return R.data(businessOpportunityService.selectBusinessOpportunityById(opportunityId));
	}

	@GetMapping("/get/{opportunityId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<BusinessOpportunity> get(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.selectBusinessOpportunityById(opportunityId));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "商机分页")
	public R<IPage<BusinessOpportunity>> list(BusinessOpportunity opportunity, Query query) {
		return R.data(businessOpportunityService.selectBusinessOpportunityPage(Condition.getPage(query), opportunity));
	}

	@GetMapping("/page")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "分页", description = "商机分页")
	public R<IPage<BusinessOpportunity>> page(BusinessOpportunity opportunity, Query query) {
		return list(opportunity, query);
	}

	@GetMapping("/statistics")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "统计", description = "商机统计")
	public R<Map<String, Object>> statistics() {
		return R.data(businessOpportunityService.selectOpportunityStatistics());
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增", description = "新增商机")
	public R<BusinessOpportunity> save(@Valid @RequestBody BusinessOpportunity opportunity) {
		businessOpportunityService.insertBusinessOpportunity(opportunity);
		return R.data(opportunity);
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "修改", description = "修改商机")
	public R update(@Valid @RequestBody BusinessOpportunity opportunity) {
		return R.status(businessOpportunityService.updateBusinessOpportunity(opportunity));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "新增或修改", description = "新增或修改商机")
	public R submit(@Valid @RequestBody BusinessOpportunity opportunity) {
		return R.status(businessOpportunityService.submitBusinessOpportunity(opportunity));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "删除", description = "逻辑删除商机")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(businessOpportunityService.deleteBusinessOpportunityByIds(ids));
	}

	@GetMapping("/follow/list/{opportunityId}")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "跟进列表", description = "查询商机跟进记录")
	public R<List<BusinessOpportunityFollow>> followList(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.selectFollowList(opportunityId));
	}

	@PostMapping("/follow/add/{opportunityId}")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "新增跟进", description = "新增商机跟进记录")
	public R addFollow(@PathVariable Long opportunityId, @RequestBody BusinessOpportunityFollow follow) {
		return R.status(businessOpportunityService.addFollowRecord(opportunityId, follow));
	}

	@GetMapping("/file/list/{opportunityId}")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "附件列表", description = "查询商机附件")
	public R<List<BusinessOpportunityFile>> fileList(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.selectFileList(opportunityId));
	}

	@PostMapping("/file/upload")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "上传附件", description = "上传商机附件")
	public R<BusinessOpportunityFile> uploadFile(@RequestParam("file") MultipartFile file,
												 @RequestParam("opportunityId") Long opportunityId) {
		return R.data(businessOpportunityService.uploadFile(opportunityId, file));
	}

	@GetMapping("/tag/list/{opportunityId}")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "标签列表", description = "查询商机标签")
	public R<List<Tag>> tagList(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.selectTagsByOpportunityId(opportunityId));
	}

	@PostMapping("/tag/set/{opportunityId}")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "设置标签", description = "设置商机标签")
	public R setTags(@PathVariable Long opportunityId, @RequestBody(required = false) List<Long> tagIds) {
		return R.status(businessOpportunityService.setOpportunityTags(opportunityId, tagIds));
	}

	@GetMapping("/background/{opportunityId}")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "背景调查", description = "按商机查询背景调查")
	public R<Map<String, Object>> background(@PathVariable Long opportunityId) {
		return R.data(businessOpportunityService.queryBackgroundInvestigation(opportunityId));
	}

	@GetMapping("/background/byName")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "背景调查", description = "按企业名称查询背景调查")
	public R<Map<String, Object>> backgroundByName(@RequestParam("enterpriseName") String enterpriseName) {
		return R.data(businessOpportunityService.queryBackgroundInvestigationByName(enterpriseName));
	}

	@GetMapping("/tenant-entry/approval-form/{opportunityId}")
	@ApiOperationSupport(order = 18)
	@Operation(summary = "企业入驻审批表", description = "按原始模板导出入驻审批表文件")
	public void tenantEntryApprovalForm(@PathVariable Long opportunityId,
										@RequestParam(value = "processInsId", required = false) String processInsId,
										HttpServletResponse response) {
		writeDocument(businessOpportunityService.exportTenantEntryApprovalForm(opportunityId, processInsId), response);
	}

	@PostMapping("/submitAudit/{opportunityId}")
	@ApiOperationSupport(order = 19)
	@Operation(summary = "提交审核", description = "提交入驻审核")
	public R<BusinessOpportunity> submitAudit(@PathVariable Long opportunityId,
											  @RequestParam(value = "flowId", required = false) Long flowId) {
		return R.data(businessOpportunityService.createApprovalProjectFromOpportunity(opportunityId, flowId));
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
