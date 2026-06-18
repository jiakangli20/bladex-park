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
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.design.entity.WfFormTheme;
import org.springblade.plugin.workflow.design.vo.WfFormThemeVO;
import org.springblade.plugin.workflow.design.wrapper.WfFormThemeWrapper;
import org.springblade.plugin.workflow.design.service.IWfFormThemeService;
import org.springblade.core.boot.ctrl.BladeController;
import java.util.Map;

/**
 * 表单 - 主题 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/form/theme")
@PreAuth(menu = "wf_form_theme")
@Tag(name = "表单 - 主题", description = "表单 - 主题接口")
public class WfFormThemeController extends BladeController {

	private final IWfFormThemeService wfFormThemeService;

	/**
	 * 表单 - 主题 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfFormTheme")
	public R<WfFormThemeVO> detail(WfFormTheme wfFormTheme) {
		WfFormTheme detail = wfFormThemeService.getOne(Condition.getQueryWrapper(wfFormTheme));
		return R.data(WfFormThemeWrapper.build().entityVO(detail));
	}
	/**
	 * 表单 - 主题 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfFormTheme")
	public R<IPage<WfFormThemeVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> wfFormTheme, Query query) {
		IPage<WfFormTheme> pages = wfFormThemeService.page(Condition.getPage(query), Condition.getQueryWrapper(wfFormTheme, WfFormTheme.class));
		return R.data(WfFormThemeWrapper.build().pageVO(pages));
	}

	/**
	 * 表单 - 主题 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入wfFormTheme")
	public R<IPage<WfFormThemeVO>> page(WfFormThemeVO wfFormTheme, Query query) {
		IPage<WfFormThemeVO> pages = wfFormThemeService.selectWfFormThemePage(Condition.getPage(query), wfFormTheme);
		return R.data(pages);
	}

	/**
	 * 表单 - 主题 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfFormTheme")
	public R save(@RequestBody WfFormTheme wfFormTheme) {
		return R.status(wfFormThemeService.save(wfFormTheme));
	}

	/**
	 * 表单 - 主题 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfFormTheme")
	public R update(@RequestBody WfFormTheme wfFormTheme) {
		return R.status(wfFormThemeService.updateById(wfFormTheme));
	}

	/**
	 * 表单 - 主题 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfFormTheme")
	public R submit(@RequestBody WfFormTheme wfFormTheme) {
		return R.status(wfFormThemeService.saveOrUpdate(wfFormTheme));
	}

	/**
	 * 表单 - 主题 删除
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfFormThemeService.deleteLogic(Func.toLongList(ids)));
	}

}
