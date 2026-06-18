package org.springblade.plugin.workflow.ops.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.plugin.workflow.process.model.WfProcess;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class WfOps extends WfProcess {

	@Schema(description = "id")
	private String id;

	@Schema(description = "标题")
	private String title;

	@Schema(description = "流程定义名称")
	private String processDefinitionName;

	@Schema(description = "流程定义key")
	private String processDefinitionKey;

	@Schema(description = "流程实例id")
	private String processInstanceId;

	@Schema(description = "审核人")
	private String assignee;

	@Schema(description = "候选人")
	private String candidateUsers;

	@Schema(description = "候选组")
	private String candidateGroups;

	@Schema(description = "任务id")
	private String taskId;

	@Schema(description = "任务名称")
	private String taskName;

	@Schema(description = "节点id")
	private String nodeId;

	@Schema(description = "分类")
	private String category;

	@Schema(description = "流水号")
	private String serialNumber;

	@Schema(description = "创建时间")
	private Date createTime;

	@Schema(description = "结束时间")
	private Date endTime;

	@Schema(description = "是否挂起")
	private Boolean isSuspended;

	@Schema(description = "外置表单key")
	private String formKey;

	@Schema(description = "外置表单url")
	private String formUrl;

	@Schema(description = "是否多实例")
	private Boolean isMultiInstance;

	@Schema(description = "流转信息")
	private String flow;

}
