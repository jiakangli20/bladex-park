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
import org.springblade.modules.approval.pojo.entity.ApprovalFlow;
import org.springblade.modules.approval.pojo.entity.ApprovalNode;
import org.springblade.modules.approval.service.IApprovalFlowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批流程配置控制器.
 *
 * @author BladeX
 */
@NonDS
@TenantIgnore
@RestController
@AllArgsConstructor
@PreAuth(menu = "approval_flow_config")
@RequestMapping("/blade-approval/flow")
@Tag(name = "审批流程配置", description = "审批流程配置接口")
public class ApprovalFlowController extends BladeController {

	private final IApprovalFlowService approvalFlowService;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入flowId")
	public R<ApprovalFlow> detail(@Parameter(description = "流程ID") @RequestParam Long flowId) {
		return R.data(approvalFlowService.selectApprovalFlowById(flowId));
	}

	@GetMapping("/get/{flowId}")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "详情", description = "兼容源接口")
	public R<ApprovalFlow> get(@PathVariable Long flowId) {
		return R.data(approvalFlowService.selectApprovalFlowById(flowId));
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "审批流程分页")
	public R<IPage<ApprovalFlow>> list(ApprovalFlow flow, Query query) {
		return R.data(approvalFlowService.selectApprovalFlowPage(Condition.getPage(query), flow));
	}

	@GetMapping("/all")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "列表", description = "审批流程列表")
	public R<List<ApprovalFlow>> all(ApprovalFlow flow) {
		return R.data(approvalFlowService.selectApprovalFlowList(flow));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增", description = "新增审批流程")
	public R<ApprovalFlow> save(@RequestBody ApprovalFlow flow) {
		return R.data(approvalFlowService.insertApprovalFlow(flow));
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "修改", description = "修改审批流程")
	public R update(@RequestBody ApprovalFlow flow) {
		return R.status(approvalFlowService.updateApprovalFlow(flow));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "新增或修改", description = "新增或修改审批流程")
	public R submit(@RequestBody ApprovalFlow flow) {
		return R.status(approvalFlowService.submitApprovalFlow(flow));
	}

	@PostMapping("/remove")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "删除", description = "失效审批流程")
	public R remove(@Parameter(description = "主键集合") @RequestParam String ids) {
		return R.status(approvalFlowService.deleteApprovalFlowByIds(ids));
	}

	@GetMapping("/nodes/{flowId}")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "节点列表", description = "查询结构化节点")
	public R<List<ApprovalNode>> nodes(@PathVariable Long flowId) {
		return R.data(approvalFlowService.getStructuredNodes(flowId));
	}

	@PostMapping("/nodes/{flowId}")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "保存节点", description = "保存结构化节点")
	public R saveNodes(@PathVariable Long flowId, @RequestBody List<ApprovalNode> nodes) {
		return R.status(approvalFlowService.saveFlowNodes(flowId, nodes));
	}

	@PostMapping("/publish/{flowId}")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "发布流程", description = "发布审批流程")
	public R publish(@PathVariable Long flowId) {
		return R.status(approvalFlowService.publishFlow(flowId));
	}

	@PostMapping("/copy/{flowId}")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "复制流程", description = "复制审批流程")
	public R<ApprovalFlow> copy(@PathVariable Long flowId) {
		return R.data(approvalFlowService.copyFlow(flowId));
	}

}
