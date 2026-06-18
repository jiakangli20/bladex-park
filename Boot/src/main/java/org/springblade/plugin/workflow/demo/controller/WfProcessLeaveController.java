package org.springblade.plugin.workflow.demo.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import lombok.AllArgsConstructor;

import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import org.springblade.plugin.workflow.demo.vo.WfProcessLeaveVO;
import org.springblade.plugin.workflow.demo.wrapper.WfProcessLeaveWrapper;
import org.springblade.plugin.workflow.demo.service.IWfProcessLeaveService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 请假流程业务示例 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/process/leave")
@PreAuth(menu = "wf_demo_leave")
@Tag(name = "请假流程业务示例", description = "请假流程业务示例接口")
public class WfProcessLeaveController extends BladeController {

	private final IWfProcessLeaveService wfProcessLeaveService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfProcessLeave")
	public R<WfProcessLeaveVO> detail(WfProcessLeave wfProcessLeave) {
		WfProcessLeave detail = wfProcessLeaveService.getOne(Condition.getQueryWrapper(wfProcessLeave));
		return R.data(WfProcessLeaveWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 请假流程业务示例
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfProcessLeave")
	public R<IPage<WfProcessLeaveVO>> list(WfProcessLeave wfProcessLeave, Query query) {
		IPage<WfProcessLeave> pages = wfProcessLeaveService.page(Condition.getPage(query), Condition.getQueryWrapper(wfProcessLeave)
			.orderByDesc("id"));
		return R.data(WfProcessLeaveWrapper.build().pageVO(pages));
	}

	/**
	 * 新增 请假流程业务示例
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfProcessLeave")
	public R save(@RequestBody WfProcessLeave wfProcessLeave) {
		return R.status(wfProcessLeaveService.save(wfProcessLeave));
	}

	/**
	 * 修改 请假流程业务示例
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfProcessLeave")
	public R update(@RequestBody WfProcessLeave wfProcessLeave) {
		return R.status(wfProcessLeaveService.updateById(wfProcessLeave));
	}

	/**
	 * 新增或修改 请假流程业务示例
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfProcessLeave")
	public R submit(@RequestBody WfProcessLeave wfProcessLeave) {
		wfProcessLeaveService.saveOrUpdate(wfProcessLeave);
		return R.data(wfProcessLeave);
	}


	/**
	 * 删除 请假流程业务示例
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfProcessLeaveService.deleteLogic(Func.toLongList(ids)));
	}

}
