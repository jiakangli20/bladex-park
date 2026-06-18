package org.springblade.plugin.workflow.design.controller;

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
import org.springblade.plugin.workflow.design.entity.WfButton;
import org.springblade.plugin.workflow.design.vo.WfButtonVO;
import org.springblade.plugin.workflow.design.wrapper.WfButtonWrapper;
import org.springblade.plugin.workflow.design.service.IWfButtonService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程按钮 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/button")
@PreAuth(menu = "wf_button")
@Tag(name = "流程按钮", description = "流程按钮接口")
public class WfButtonController extends BladeController {

	private final IWfButtonService wfButtonService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfButton")
	public R<WfButtonVO> detail(WfButton wfButton) {
		WfButton detail = wfButtonService.getOne(Condition.getQueryWrapper(wfButton));
		return R.data(WfButtonWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 流程按钮
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfButton")
	public R<IPage<WfButtonVO>> list(WfButton wfButton, Query query) {
		IPage<WfButton> pages = wfButtonService.page(Condition.getPage(query), Condition.getQueryWrapper(wfButton));
		return R.data(WfButtonWrapper.build().pageVO(pages));
	}


	/**
	 * 自定义分页 流程按钮
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入wfButton")
	public R<IPage<WfButtonVO>> page(WfButtonVO wfButton, Query query) {
		IPage<WfButtonVO> pages = wfButtonService.selectWfButtonPage(Condition.getPage(query), wfButton);
		return R.data(pages);
	}

	/**
	 * 新增 流程按钮
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfButton")
	public R save(@RequestBody WfButton wfButton) {
		return R.status(wfButtonService.save(wfButton));
	}

	/**
	 * 修改 流程按钮
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfButton")
	public R update(@RequestBody WfButton wfButton) {
		return R.status(wfButtonService.updateById(wfButton));
	}

	/**
	 * 新增或修改 流程按钮
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfButton")
	public R submit(@RequestBody WfButton wfButton) {
		return R.status(wfButtonService.saveOrUpdate(wfButton));
	}


	/**
	 * 删除 流程按钮
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfButtonService.deleteLogic(Func.toLongList(ids)));
	}


}
