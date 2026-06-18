package org.springblade.plugin.workflow.ops.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;

import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.utils.ObjectUtil;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestParam;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.plugin.workflow.ops.entity.WfProxy;
import org.springblade.plugin.workflow.ops.vo.WfProxyVO;
import org.springblade.plugin.workflow.ops.wrapper.WfProxyWrapper;
import org.springblade.plugin.workflow.ops.service.IWfProxyService;
import org.springblade.core.boot.ctrl.BladeController;

/**
 * 流程代理 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/ops/proxy")
@PreAuth(menu = "wf_ops_proxy")
@Tag(name = "流程代理", description = "流程代理接口")
public class WfProxyController extends BladeController {

	private final IWfProxyService wfProxyService;

	/**
	 * 详情
	 */
	@GetMapping("/detail")
	@Operation(summary = "详情", description = "传入wfProxy")
	public R<WfProxyVO> detail(WfProxy wfProxy) {
		WfProxy detail = wfProxyService.getOne(Condition.getQueryWrapper(wfProxy));
		return R.data(WfProxyWrapper.build().entityVO(detail));
	}

	/**
	 * 分页 流程代理
	 */
	@GetMapping("/list")
	@Operation(summary = "分页", description = "传入wfProxy")
	public R<IPage<WfProxyVO>> list(WfProxy wfProxy, Query query) {
		QueryWrapper<WfProxy> wrapper = Condition.getQueryWrapper(wfProxy)
			.orderByDesc("id");
		if (StringUtil.isNotBlank(wfProxy.getProcessDefKey())) {
			wrapper.like("process_def_key", wfProxy.getProcessDefKey());
			wfProxy.setProcessDefKey(null);
		}
		IPage<WfProxy> pages = wfProxyService.page(Condition.getPage(query), wrapper);
		return R.data(WfProxyWrapper.build().pageVO(pages));
	}


	/**
	 * 新增或修改 流程代理
	 */
	@PostMapping("/submit")
	@Operation(summary = "新增或修改", description = "传入wfProxy")
	public R submit(@RequestBody WfProxy wfProxy) {
		if (ObjectUtil.isNotEmpty(wfProxy.getId())) {
			try {
				wfProxyService.updateById(wfProxy);
				return R.success("操作成功");
			} catch (DuplicateKeyException ignore) {
				return R.fail("当前委托人已存在相同流程的代理");
			}
		} else {
			return (R) wfProxyService.create(wfProxy);
		}
	}


	/**
	 * 删除 流程代理
	 */
	@PostMapping("/remove")
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
		return R.status(wfProxyService.removeByIds(Func.toLongList(ids)));
	}


}
