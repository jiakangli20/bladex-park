package org.springblade.plugin.workflow.statistics.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springblade.plugin.workflow.process.model.WfProcess;

/**
 * 统计实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class WfStatisticsQuery extends WfProcess {

	private Boolean isToday;

	private String type;

	private Boolean isTimeout;

	private String timeRange;

	/**
	 * 租户id
	 */
	private String tid;

}
