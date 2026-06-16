/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 */
package org.springblade.modules.approval.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tenant.annotation.NonDS;
import org.springblade.core.tenant.annotation.TenantIgnore;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.support.Kv;
import org.springblade.modules.approval.pojo.entity.ApprovalLog;
import org.springblade.modules.approval.pojo.entity.ApprovalMaterial;
import org.springblade.modules.approval.pojo.entity.ApprovalProject;
import org.springblade.modules.approval.service.IApprovalProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 我的审批控制器.
 *
 * @author BladeX
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "approval_my_flow")
@RequestMapping("/blade-approval")
@Tag(name = "我的审批", description = "我的审批接口")
public class ApprovalProjectController extends BladeController {

	private final IApprovalProjectService approvalProjectService;

	@GetMapping("/project/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入projectId")
	public R<ApprovalProject> detail(@Parameter(description = "审批项目ID") @RequestParam Long projectId) {
		return R.data(approvalProjectService.selectApprovalProjectById(projectId));
	}

	@GetMapping("/project/get/{projectId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<ApprovalProject> get(@PathVariable Long projectId) {
		return R.data(approvalProjectService.selectApprovalProjectById(projectId));
	}

	@GetMapping("/project/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "审批项目分页")
	public R<IPage<ApprovalProject>> list(ApprovalProject project, Query query) {
		return R.data(approvalProjectService.selectApprovalProjectPage(Condition.getPage(query), project));
	}

	@GetMapping("/project/page")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "分页", description = "审批项目分页")
	public R<IPage<ApprovalProject>> page(ApprovalProject project, Query query) {
		return list(project, query);
	}

	@GetMapping("/project/statistics")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "统计", description = "审批项目统计")
	public R<Kv> statistics(ApprovalProject project) {
		return R.data(approvalProjectService.selectApprovalProjectStatistics(project));
	}

	@PostMapping("/project/save")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增", description = "新增审批项目")
	public R<ApprovalProject> save(@RequestBody ApprovalProject project) {
		return R.data(approvalProjectService.insertApprovalProject(project));
	}

	@PostMapping("/project/update")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "修改", description = "修改审批项目")
	public R update(@RequestBody ApprovalProject project) {
		return R.status(approvalProjectService.updateApprovalProject(project));
	}

	@PostMapping("/project/remove")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "逻辑删除审批项目")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(approvalProjectService.deleteApprovalProjectByIds(ids));
	}

	@PostMapping("/project/submit/{projectId}")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "提交", description = "提交审批项目")
	public R submit(@PathVariable Long projectId) {
		return R.status(approvalProjectService.submitProject(projectId));
	}

	@PostMapping("/project/withdraw/{projectId}")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "撤回", description = "撤回审批项目")
	public R withdraw(@PathVariable Long projectId) {
		return R.status(approvalProjectService.withdrawProject(projectId));
	}

	@PostMapping("/project/approve/{projectId}")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "审批通过", description = "审批通过")
	public R approve(@PathVariable Long projectId, @RequestBody(required = false) ApprovalLog log) {
		return R.status(approvalProjectService.approveProject(projectId, log));
	}

	@PostMapping("/project/reject/{projectId}")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "审批驳回", description = "审批驳回")
	public R reject(@PathVariable Long projectId, @RequestBody(required = false) ApprovalLog log) {
		return R.status(approvalProjectService.rejectProject(projectId, log));
	}

	@PostMapping("/project/transfer/{projectId}")
	@ApiOperationSupport(order = 13)
	@Operation(summary = "转审", description = "转审审批项目")
	public R transfer(@PathVariable Long projectId, @RequestBody ApprovalLog log) {
		return R.status(approvalProjectService.transferProject(projectId, log));
	}

	@PostMapping("/project/resubmit/{projectId}")
	@ApiOperationSupport(order = 14)
	@Operation(summary = "重新提交", description = "重新提交审批项目")
	public R resubmit(@PathVariable Long projectId, @RequestBody(required = false) ApprovalLog log) {
		return R.status(approvalProjectService.resubmitProject(projectId, log));
	}

	@PostMapping("/project/archive/{projectId}")
	@ApiOperationSupport(order = 15)
	@Operation(summary = "归档", description = "归档审批项目")
	public R archive(@PathVariable Long projectId) {
		return R.status(approvalProjectService.archiveProject(projectId));
	}

	@GetMapping("/project/form/{projectId}")
	@ApiOperationSupport(order = 16)
	@Operation(summary = "审批表", description = "获取审批表")
	public R<Kv> form(@PathVariable Long projectId) {
		return R.data(approvalProjectService.generateApprovalForm(projectId));
	}

	@GetMapping("/project/form/export/{projectId}")
	@ApiOperationSupport(order = 17)
	@Operation(summary = "导出审批表", description = "返回审批表数据")
	public R<Kv> exportForm(@PathVariable Long projectId) {
		return R.data(approvalProjectService.generateApprovalForm(projectId));
	}

	@GetMapping("/material/list")
	@ApiOperationSupport(order = 18)
	@Operation(summary = "资料列表", description = "查询审批资料")
	public R<List<ApprovalMaterial>> materialList(ApprovalMaterial material) {
		return R.data(approvalProjectService.selectApprovalMaterialList(material));
	}

	@GetMapping("/log/list")
	@ApiOperationSupport(order = 19)
	@Operation(summary = "日志列表", description = "查询审批日志")
	public R<List<ApprovalLog>> logList(ApprovalLog log) {
		return R.data(approvalProjectService.selectApprovalLogList(log));
	}

	@GetMapping("/project/print/{projectId}")
	@ApiOperationSupport(order = 20)
	@Operation(summary = "打印审批表", description = "获取打印审批表")
	public R<Kv> print(@PathVariable Long projectId) {
		return R.data(approvalProjectService.generateApprovalForm(projectId));
	}

}
