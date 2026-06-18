package org.springblade.plugin.workflow.statistics.service;

import org.springblade.plugin.workflow.statistics.entity.WfBarStatistics;
import org.springblade.plugin.workflow.statistics.entity.WfStatisticsQuery;

import java.util.concurrent.Future;

/**
 * 统计服务类
 *
 * @author ssc
 */
public interface WfStatisticsService {

	/**
	 * 流程数量统计
	 */
	Future<Long> processCount(WfStatisticsQuery statistics);

	/**
	 * 任务数量统计
	 */
	Future<Long> taskCount(WfStatisticsQuery statistics);

	/**
	 * 柱状图
	 */
	Future<WfBarStatistics> barCount(WfStatisticsQuery statistics);

}
