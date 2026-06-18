package org.springblade.plugin.workflow.design.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义
 */
@Data
@Schema(name = "流程定义")
public class WfProcessDef implements Serializable {

	private String id;

	@Schema(description = "租户id")
	private String tenantId;

	@Schema(description = "流程名称")
	private String name;

	@Schema(description = "流程key")
	private String key;

	@Schema(description = "流程分类")
	private String category;

	@Schema(description = "流程状态")
	private Integer status;

	@Schema(description = "版本")
	private Integer version;

	@Schema(description = "部署时间")
	private Date deployTime;

	@Schema(description = "部署id")
	private String deploymentId;

	@Schema(description = "表单key")
	private String formKey;

	@Schema(description = "表单url")
	private String formUrl;

	@Schema(description = "app表单url")
	private String appFormUrl;

	@Schema(description = "开启流程权限")
	private Boolean scope;

	@Schema(description = "默认抄送人")
	private String copyUser;

	@Schema(description = "默认抄送人名称")
	private String copyUserName;

	@Schema(description = "隐藏抄送人选项")
	private Boolean hideCopy;

	@Schema(description = "隐藏选择下一步审核人选项")
	private Boolean hideExamine;

	@Schema(description = "平台")
	private String platform;

	@Schema(description = "图标")
	private String icon;

	public WfProcessDef() {
	}

	public WfProcessDef(ProcessDefinitionEntityImpl entity) {
		this.id = entity.getId();
		this.tenantId = entity.getTenantId();
		this.name = entity.getName();
		this.key = entity.getKey();
		this.category = entity.getCategory();
		this.status = entity.getSuspensionState();
	}

}
