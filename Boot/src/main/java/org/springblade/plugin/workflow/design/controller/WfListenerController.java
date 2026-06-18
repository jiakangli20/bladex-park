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
import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.plugin.workflow.design.vo.WfListenerVO;
import org.springblade.plugin.workflow.design.wrapper.WfListenerWrapper;
import org.springblade.plugin.workflow.design.service.IWfListenerService;
import org.springblade.core.boot.ctrl.BladeController;

import java.util.Map;

/**
 * 任务/执行监听器 控制器
 *
 * @author ssc
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blade-workflow/design/listener")
@PreAuth(menu = "wf_listener")
@Tag(name = "任务/执行监听器", description = "任务/执行监听器接口")
public class WfListenerController extends BladeController {

    private final IWfListenerService wfListenerService;

    /**
     * 任务/执行监听器 详情
     */
    @GetMapping("/detail")
    @ApiOperationSupport(order = 1)
    @Operation(summary = "详情", description = "传入wfListener")
    public R<WfListenerVO> detail(WfListenerEntity wfListener) {
        WfListenerEntity detail = wfListenerService.getOne(Condition.getQueryWrapper(wfListener));
        return R.data(WfListenerWrapper.build().entityVO(detail));
    }

    /**
     * 任务/执行监听器 分页
     */
    @GetMapping("/list")
    @ApiOperationSupport(order = 2)
    @Operation(summary = "分页", description = "传入wfListener")
    public R<IPage<WfListenerVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> wfListener, Query query) {
        IPage<WfListenerEntity> pages = wfListenerService.page(Condition.getPage(query), Condition.getQueryWrapper(wfListener, WfListenerEntity.class));
        return R.data(WfListenerWrapper.build().pageVO(pages));
    }

    /**
     * 任务/执行监听器 自定义分页
     */
    @GetMapping("/page")
    @ApiOperationSupport(order = 3)
    @Operation(summary = "分页", description = "传入wfListener")
    public R<IPage<WfListenerVO>> page(WfListenerVO wfListener, Query query) {
        IPage<WfListenerVO> pages = wfListenerService.selectWfListenerPage(Condition.getPage(query), wfListener);
        return R.data(pages);
    }

    /**
     * 任务/执行监听器 新增
     */
    @PostMapping("/save")
    @ApiOperationSupport(order = 4)
    @Operation(summary = "新增", description = "传入wfListener")
    public R save(@RequestBody WfListenerEntity wfListener) {
        return R.status(wfListenerService.save(wfListener));
    }

    /**
     * 任务/执行监听器 修改
     */
    @PostMapping("/update")
    @ApiOperationSupport(order = 5)
    @Operation(summary = "修改", description = "传入wfListener")
    public R update(@RequestBody WfListenerEntity wfListener) {
        return R.status(wfListenerService.updateById(wfListener));
    }

    /**
     * 任务/执行监听器 新增或修改
     */
    @PostMapping("/submit")
    @ApiOperationSupport(order = 6)
    @Operation(summary = "新增或修改", description = "传入wfListener")
    public R submit(@RequestBody WfListenerEntity wfListener) {
        return R.status(wfListenerService.saveOrUpdate(wfListener));
    }

    /**
     * 任务/执行监听器 删除
     */
    @PostMapping("/remove")
    @ApiOperationSupport(order = 7)
    @Operation(summary = "逻辑删除", description = "传入ids")
    public R remove(@Parameter(name = "主键集合", required = true) @RequestParam String ids) {
        return R.status(wfListenerService.deleteLogic(Func.toLongList(ids)));
    }

}
