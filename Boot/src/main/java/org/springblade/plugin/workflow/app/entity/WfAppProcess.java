package org.springblade.plugin.workflow.app.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.plugin.workflow.core.user.WfUser;
import org.springblade.plugin.workflow.process.model.WfProcess;

import java.util.Date;

/**
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfAppProcess extends WfProcess {

	@Schema(description = "id")
	private String id;

	@Schema(description = "流程定义名称")
	private String processDefinitionName;

	@Schema(description = "流程定义key")
	private String processDefinitionKey;

	@Schema(description = "流程实例id")
	private String processInstanceId;

	@Schema(description = "申请人")
	private WfUser applyUser;

	@Schema(description = "审核人")
	private String assignee;

	@Schema(description = "任务id")
	private String taskId;

	@Schema(description = "任务名称")
	private String taskName;

	@Schema(description = "节点id")
	private String nodeId;

	@Schema(description = "申请人名称")
	private String applyUserName;

	@Schema(description = "流水号")
	private String serialNumber;

	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "结束时间")
	private Date endTime;

	@Schema(description = "外置表单key")
	private String formKey;

	@Schema(description = "外置表单url")
	private String formUrl;

	@Schema(description = "是否多实例")
	private Boolean isMultiInstance;

	@Schema(description = "平台")
	private String platform;

	@Schema(description = "图标")
	private String icon;
}
