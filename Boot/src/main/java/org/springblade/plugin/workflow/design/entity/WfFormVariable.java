package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import org.springblade.core.tenant.mp.TenantEntity;

import java.io.Serial;

/**
 * 流程表单 - 历史变量 实体类
 *
 * @author ssc
 */
@Data
@TableName("blade_wf_form_variable")
@Schema(name = "WfFormVariable对象", description = "流程表单 - 历史变量")
@EqualsAndHashCode(callSuper = true)
public class WfFormVariable extends TenantEntity {

	/**
	 * 流程实例id
	 */
	@Schema(description = "流程实例id")
	private String processInsId;
	/**
	 * 流程部署id
	 */
	@Schema(description = "流程部署id")
	private String deploymentId;
	/**
	 * 任务id
	 */
	@Schema(description = "任务id")
	private String taskId;
	/**
	 * 任务节点key
	 */
	@Schema(description = "任务节点key")
	private String taskDefKey;
	/**
	 * 任务节点名称
	 */
	@Schema(description = "任务节点名称")
	private String taskName;
	/**
	 * 表单js
	 */
	@Schema(description = "表单js")
	private String formOption;
	/**
	 * 表单值
	 */
	@Schema(description = "表单值")
	private String formVariable;

}
