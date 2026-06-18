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
public class WfProcessStatistics implements Serializable {

	@Schema(description = "流程总数")
	private Long process;

	@Schema(description = "进行中流程数量")
	private Long unfinished;

	@Schema(description = "已结束流程数量")
	private Long finished;

	@Schema(description = "被驳回流程总数")
	private Long reject;

	@Schema(description = "被终结流程总数")
	private Long terminate;

	@Schema(description = "撤销流程总数")
	private Long withdraw;

	@Schema(description = "撤回流程总数")
	private Long recall;

	@Schema(description = "删除流程总数")
	private Long deleted;

	@Schema(description = "今日流程总数")
	private Long todayProcess;

	@Schema(description = "今日进行中流程总数")
	private Long todayUnfinished;

	@Schema(description = "今日结束流程总数")
	private Long todayFinished;

	@Schema(description = "今日被驳回流程总数")
	private Long todayReject;
}
