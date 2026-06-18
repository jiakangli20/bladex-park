
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
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.Func;
import org.springblade.plugin.workflow.core.cache.WfCategoryCache;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.design.entity.WfCategory;
import org.springblade.plugin.workflow.design.vo.WfCategoryVO;
import org.springblade.plugin.workflow.design.wrapper.WfCategoryWrapper;
import org.springblade.plugin.workflow.design.service.IWfCategoryService;
import org.springblade.core.boot.ctrl.BladeController;

import java.util.Map;

/**
 * 流程分类 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/category")
@PreAuth(menu = "wf_category")
@Tag(name = "流程分类", description = "流程分类接口")
public class WfCategoryController extends BladeController {

	private final IWfCategoryService wfCategoryService;

	@GetMapping("tree")
	@Operation(summary = "树形结构")
	public R tree() {
		if (WfTaskUtil.getTenantId().equals(BladeConstant.ADMIN_TENANT_ID)) {
			WfCategoryCache.removeTreeCache();
			return R.data(wfCategoryService.tree());
		}
		return R.data(WfCategoryCache.tree());
	}

	@GetMapping("allTree")
	@Operation(summary = "树形结构")
	public R allTree() {
		return R.data(wfCategoryService.allTree());
	}

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入wfCategory")
	public R<WfCategoryVO> detail(WfCategory category) {
		WfCategory detail = wfCategoryService.getOne(Condition.getQueryWrapper(category));
		return R.data(WfCategoryWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 流程分类
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入wfCategory")
	public R<IPage<WfCategoryVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> category, Query query) {
		IPage<WfCategory> pages = wfCategoryService.page(Condition.getPage(query), Condition.getQueryWrapper(category, WfCategory.class).orderByAsc("sort").orderByDesc("id"));
		return R.data(WfCategoryWrapper.build().pageVO(pages));
	}


	/**
	 * 自定义分页 流程分类
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入wfCategory")
	public R<IPage<WfCategoryVO>> page(WfCategoryVO category, Query query) {
		IPage<WfCategoryVO> pages = wfCategoryService.selectWfCategoryPage(Condition.getPage(query), category);
		return R.data(pages);
	}

	/**
	 * 新增 流程分类
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入wfCategory")
	public R save(@RequestBody WfCategory category) {
		WfCategoryCache.removeTreeCache();
		return R.status(wfCategoryService.save(category));
	}

	/**
	 * 修改 流程分类
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入wfCategory")
	public R update(@RequestBody WfCategory category) {
		WfCategoryCache.removeTreeCache();
		WfCategoryCache.removeById(category.getId());
		return R.status(wfCategoryService.updateById(category));
	}

	/**
	 * 新增或修改 流程分类
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入wfCategory")
	public R submit(@RequestBody WfCategory category) {
		WfCategoryCache.removeTreeCache();
		WfCategoryCache.removeById(category.getId());
		return R.status(wfCategoryService.saveOrUpdate(category));
	}


	/**
	 * 删除 流程分类
	 */
	@PostMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		WfCategoryCache.removeTreeCache();
		return R.status(wfCategoryService.deleteLogic(Func.toLongList(ids)));
	}


}
