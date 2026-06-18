package org.springblade.plugin.workflow.design.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.BaseEntity;

import java.io.Serial;

/**
 * 流程部署 - 表单
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blade_wf_deployment_form")
@Schema(name = "流程部署 - 表单")
public class WfDeploymentForm extends BaseEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "流程部署id")
	private String deploymentId;

	@Schema(description = "表单key")
	private String formKey;

	@Schema(description = "表单内容")
	private String content;

	@Schema(description = "app表单内容")
	private String appContent;

	@Schema(description = "节点key")
	private String taskKey;

	@Schema(description = "节点名称")
	private String taskName;

	@Schema(description = "表名")
	private String tableName;

	@Schema(description = "表注释")
	private String tableComment;

}
