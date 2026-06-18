
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
import org.springblade.plugin.workflow.design.entity.WfSerial;
import org.springblade.plugin.workflow.design.vo.WfSerialVO;
import org.springblade.plugin.workflow.design.wrapper.WfSerialWrapper;
import org.springblade.plugin.workflow.design.service.IWfSerialService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程流水号 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/serial")
@PreAuth(menu = "wf_design")
@Tag(name = "流程流水号", description = "流程流水号接口")
public class WfSerialController extends BladeController {

	private final IWfSerialService wfSerialService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfSerial")
	public R<WfSerialVO> detail(WfSerial wfSerial) {
		WfSerial detail = wfSerialService.getOne(Condition.getQueryWrapper(wfSerial));
		return R.data(WfSerialWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 流程流水号
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfSerial")
	public R<IPage<WfSerialVO>> list(WfSerial wfSerial, Query query) {
		IPage<WfSerial> pages = wfSerialService.page(Condition.getPage(query), Condition.getQueryWrapper(wfSerial));
		return R.data(WfSerialWrapper.build().pageVO(pages));
	}


	/**
	 * 新增 流程流水号
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfSerial")
	public R save(@RequestBody WfSerial wfSerial) {
		return R.status(wfSerialService.save(wfSerial));
	}

	/**
	 * 修改 流程流水号
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfSerial")
	public R update(@RequestBody WfSerial wfSerial) {
		return R.status(wfSerialService.updateById(wfSerial));
	}

	/**
	 * 新增或修改 流程流水号
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfSerial")
	public R submit(@RequestBody WfSerial wfSerial) {
		return R.status(wfSerialService.saveOrUpdate(wfSerial));
	}


	/**
	 * 删除 流程流水号
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfSerialService.deleteLogic(Func.toLongList(ids)));
	}


}
