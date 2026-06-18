package org.springblade.plugin.workflow.statistics.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ssc
 */
@Data
@Builder
public class WfTaskStatistics implements Serializable {

	@Schema(description = "任务总数")
	private Long task;

	@Schema(description = "进行中任务数量")
	private Long unfinished;

	@Schema(description = "超时任务总数")
	private Long timeout;

	@Schema(description = "今日任务总数")
	private Long todayTask;

	@Schema(description = "今日进行中任务总数")
	private Long todayUnfinished;

	@Schema(description = "今日超时任务总数")
	private Long todayTimeout;

}
