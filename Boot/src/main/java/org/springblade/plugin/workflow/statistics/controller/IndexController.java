package org.springblade.plugin.workflow.statistics.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springblade.core.secure.annotation.PreAuth;
import org.springblade.core.tool.api.R;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.WfTaskUtil;
import org.springblade.plugin.workflow.statistics.entity.WfProcessStatistics;
import org.springblade.plugin.workflow.statistics.entity.WfStatisticsQuery;
import org.springblade.plugin.workflow.statistics.entity.WfTaskStatistics;
import org.springblade.plugin.workflow.statistics.service.WfStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 统计控制器
 *
 * @author ssc
 */
@RestController("statisticsIndexController")
@RequestMapping("/blade-workflow/statistics")
@RequiredArgsConstructor
@PreAuth(menu = "wf_statistics")
@Tag(name = "流程统计")
public class IndexController {

	private final WfStatisticsService wfStatisticsService;

	@GetMapping("index")
	@Operation(summary = "总览")
	@Parameters({
		@Parameter(name = "startTimeRange", description = "时间范围，逗号分隔"),
	})
	public R index(WfStatisticsQuery statistics) throws ExecutionException, InterruptedException {
		String timeRange = statistics.getStartTimeRange();
		String tenantId = WfTaskUtil.getTenantId();
		// 全部流程数量
		Future<Long> processFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange));
		// 全部任务数量
		Future<Long> taskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_DONE));
		// 进行中任务数量
		Future<Long> unfinishedTaskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_TODO));
		// 超时任务数量
		Future<Long> timeoutTaskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setIsTimeout(true));
		// 进行中流程
		Future<Long> unfinishedProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_UNFINISHED));
		// 已结束流程，默认包含驳回/终结/撤销/删除。若需排除请打开WfSearchUtil.buildHistoricProcessQuery()中的相关注释
		Future<Long> finishedProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_FINISHED));
		// 被驳回
		Future<Long> rejectProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_REJECT));
		// 已终结
		Future<Long> terminateProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_TERMINATE));
		// 已撤销
		Future<Long> withdrawProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_WITHDRAW));
		// 已撤回
		Future<Long> recallProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_RECALL));
		// 已删除
		Future<Long> deletedProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setTimeRange(timeRange)
			.setType(WfProcessConstant.STATUS_DELETED));
		// 今日 - 流程数量
		Future<Long> todayProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true));
		// 今日 - 进行中流程
		Future<Long> todayUnfinishedProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setType(WfProcessConstant.STATUS_UNFINISHED));
		// 今日 - 已结束流程
		Future<Long> todayFinishedProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setType(WfProcessConstant.STATUS_FINISHED));
		// 今日 - 被驳回流程
		Future<Long> todayRejectProcessFuture = wfStatisticsService.processCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setType(WfProcessConstant.STATUS_REJECT));
		// 今日 - 任务数量
		Future<Long> todayTaskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setType(WfProcessConstant.STATUS_DONE));
		// 今日 - 进行中任务数量
		Future<Long> todayUnfinishedTaskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setType(WfProcessConstant.STATUS_TODO));
		// 今日 - 超时任务数量
		Future<Long> todayTimeoutTaskCount = wfStatisticsService.taskCount(new WfStatisticsQuery()
			.setTid(tenantId)
			.setIsToday(true)
			.setIsTimeout(true));

		Map<String, Object> result = new HashMap<>();
		result.put("process", WfProcessStatistics.builder()
			.process(processFuture.get())
			.unfinished(unfinishedProcessFuture.get())
			.finished(finishedProcessFuture.get())
			.reject(rejectProcessFuture.get())
			.terminate(terminateProcessFuture.get())
			.withdraw(withdrawProcessFuture.get())
			.recall(recallProcessFuture.get())
			.deleted(deletedProcessFuture.get())
			.todayProcess(todayProcessFuture.get())
			.todayUnfinished(todayUnfinishedProcessFuture.get())
			.todayFinished(todayFinishedProcessFuture.get())
			.todayReject(todayRejectProcessFuture.get())
			.build());

		result.put("task", WfTaskStatistics.builder()
			.task(taskCount.get())
			.unfinished(unfinishedTaskCount.get())
			.timeout(timeoutTaskCount.get())
			.todayTask(todayTaskCount.get())
			.todayUnfinished(todayUnfinishedTaskCount.get())
			.todayTimeout(todayTimeoutTaskCount.get())
			.build());

		return R.data(result);
	}

	@GetMapping("bar")
	@Operation(summary = "柱状图")
	@Parameters({
		@Parameter(name = "startTimeRange", description = "时间范围，逗号分隔"),
	})
	public R indexBar(WfStatisticsQuery statistics) throws ExecutionException, InterruptedException {
		statistics.setTimeRange(statistics.getStartTimeRange());
		statistics.setTid(WfTaskUtil.getTenantId());
		return R.data(wfStatisticsService.barCount(statistics).get());
	}
}
