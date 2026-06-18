package org.springblade.plugin.workflow.statistics.service.impl;

import lombok.RequiredArgsConstructor;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.springblade.core.tool.constant.BladeConstant;
import org.springblade.core.tool.utils.BeanUtil;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.plugin.workflow.core.constant.WfProcessConstant;
import org.springblade.plugin.workflow.core.utils.WfSearchUtil;
import org.springblade.plugin.workflow.process.model.WfProcess;
import org.springblade.plugin.workflow.statistics.entity.WfBarStatistics;
import org.springblade.plugin.workflow.statistics.entity.WfStatisticsQuery;
import org.springblade.plugin.workflow.statistics.service.WfStatisticsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;

/**
 * 统计服务实现类
 *
 * @author ssc
 */
@Service
@RequiredArgsConstructor
public class WfStatisticsServiceImpl implements WfStatisticsService {

	private final HistoryService historyService;
	private final TaskService taskService;

	@Async
	@Override
	public Future<Long> processCount(WfStatisticsQuery statistics) {
		HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

		// 租户
		String tenantId = statistics.getTid();
		if (!tenantId.equals(BladeConstant.ADMIN_TENANT_ID)) {
			query.processInstanceTenantId(tenantId);
		}

		if (statistics.getIsToday() != null && statistics.getIsToday()) { // 今日
			query.startedAfter(todayBeginDate());
			query.startedBefore(todayEndDate());
		}

		statistics.setProcessIsFinished(statistics.getType());
		statistics.setStartTimeRange(statistics.getTimeRange());
		WfSearchUtil.buildSearchQuery(query, statistics);

		return new AsyncResult<>(query.count());
	}

	@Async
	@Override
	public Future<Long> taskCount(WfStatisticsQuery statistics) {
		statistics.setStartTimeRange(statistics.getTimeRange());
		String tenantId = statistics.getTid();
		if (WfProcessConstant.STATUS_DONE.equals(statistics.getType())) { // 全部任务
			HistoricTaskInstanceQuery query = historyService.createHistoricTaskInstanceQuery();

			// 租户
			if (!tenantId.equals(BladeConstant.ADMIN_TENANT_ID)) {
				query.taskTenantId(tenantId);
			}

			if (statistics.getIsToday() != null && statistics.getIsToday()) { // 今日
				query.taskCreatedAfter(todayBeginDate());
				query.taskCreatedBefore(todayEndDate());
			}

			WfSearchUtil.buildSearchQuery(query, statistics);

			return new AsyncResult<>(query.count());
		} else { // 进行中任务
			TaskQuery query = taskService.createTaskQuery();

			// 租户
			if (!tenantId.equals(BladeConstant.ADMIN_TENANT_ID)) {
				query.taskTenantId(tenantId);
			}

			if (statistics.getIsToday() != null && statistics.getIsToday()) { // 今日
				query.taskCreatedAfter(todayBeginDate());
				query.taskCreatedBefore(todayEndDate());
			}
			if (statistics.getIsTimeout() != null && statistics.getIsTimeout()) { // 超时任务
				query.taskDueBefore(new Date());
			}

			WfSearchUtil.buildSearchQuery(query, statistics);

			return new AsyncResult<>(query.count());
		}
	}

	@Override
	public Future<WfBarStatistics> barCount(WfStatisticsQuery statistics) {
		HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();

		// 租户
		String tenantId = statistics.getTid();
		if (!tenantId.equals(BladeConstant.ADMIN_TENANT_ID)) {
			query.processInstanceTenantId(tenantId);
		}

		Map<String, List<HistoricProcessInstance>> groupBy = new HashMap<>();
		if (StringUtil.isBlank(statistics.getTimeRange())) { // 时间查询为空，默认查询最近10天数据
			Date end = todayEndDate();
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(end);
			calendar.add(Calendar.DAY_OF_YEAR, -10);
			Date begin = calendar.getTime();

			query.startedAfter(begin);
			query.startedBefore(end);

			for (int i = 0; i < 10; i++) {
				calendar.add(Calendar.DAY_OF_YEAR, 1);
				groupBy.put(DateUtil.format(calendar.getTime(), "yy-MM-dd"), new ArrayList<>());
			}
		} else {
			try {
				String startTimeRange = statistics.getTimeRange();
				String[] split = startTimeRange.split(",");
				Date begin = DateUtil.parse(split[0], DateUtil.PATTERN_DATETIME);
				Date end = DateUtil.parse(split[1], DateUtil.PATTERN_DATETIME);
				Duration between = DateUtil.between(begin, end);
				long days = between.toDays();

				Calendar calendar = Calendar.getInstance();
				calendar.setTime(begin);
				for (int i = 0; i <= days; i++) {
					groupBy.put(DateUtil.format(calendar.getTime(), "yy-MM-dd"), new ArrayList<>());
					calendar.add(Calendar.DAY_OF_YEAR, 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		WfProcess process = BeanUtil.copyProperties(statistics, WfProcess.class);
		assert process != null;
		process.setProcessIsFinished(statistics.getType());
		process.setStartTimeRange(statistics.getTimeRange());
		WfSearchUtil.buildSearchQuery(query, process);

		List<HistoricProcessInstance> list = query.list();
		list.forEach(p -> {
			Date date = p.getStartTime();
			String format = DateUtil.format(date, "yy-MM-dd");
			List<HistoricProcessInstance> l = groupBy.get(format);
			if (l == null) {
				l = new ArrayList<>();
			}
			l.add(p);
			groupBy.put(format, l);
		});

		// 日期排序
		List<Map.Entry<String, List<HistoricProcessInstance>>> toSort = new ArrayList<>(groupBy.entrySet());
		toSort.sort(Map.Entry.comparingByKey());
		LinkedHashMap<String, List<HistoricProcessInstance>> collect = new LinkedHashMap<>();
		for (Map.Entry<String, List<HistoricProcessInstance>> stringListEntry : toSort) {
			collect.putIfAbsent(stringListEntry.getKey(), stringListEntry.getValue());
		}

		List<String> xData = new ArrayList<>();
		List<String> yData = new ArrayList<>();
		collect.forEach((k, v) -> {
			xData.add(k.substring(3));
			yData.add(v.size() + "");
		});

		return new AsyncResult<>(WfBarStatistics.builder()
			.xData(xData)
			.yData(yData)
			.build());
	}

	private Date todayBeginDate() {
		String today = DateUtil.format(new Date(), DateUtil.PATTERN_DATE);
		return DateUtil.parse(today + " 00:00:00", DateUtil.PATTERN_DATETIME);
	}

	private Date todayEndDate() {
		String today = DateUtil.format(new Date(), DateUtil.PATTERN_DATE);
		return DateUtil.parse(today + " 23:59:59", DateUtil.PATTERN_DATETIME);
	}
}
