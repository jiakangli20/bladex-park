package org.springblade.plugin.workflow.demo.vo;

import org.springblade.plugin.workflow.demo.entity.WfProcessLeave;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 请假流程业务示例视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfProcessLeaveVO对象", description = "请假流程业务示例")
public class WfProcessLeaveVO extends WfProcessLeave {
	@Serial
	private static final long serialVersionUID = 1L;

	private String creator;

	private String creatorDept;

}
