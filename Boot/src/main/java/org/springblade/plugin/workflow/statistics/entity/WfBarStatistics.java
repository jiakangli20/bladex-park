package org.springblade.plugin.workflow.statistics.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author ssc
 */
@Data
@Builder
public class WfBarStatistics implements Serializable {

	@JsonProperty("xData")
	private List<String> xData;

	@JsonProperty("yData")
	private List<String> yData;
}
