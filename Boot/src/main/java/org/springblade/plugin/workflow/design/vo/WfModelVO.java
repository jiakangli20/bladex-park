package org.springblade.plugin.workflow.design.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.flowable.bpmn.model.FormProperty;
import org.springblade.plugin.workflow.design.entity.WfModel;

import java.util.List;

/**
 * 流程模型Vo
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfModelVO extends WfModel {

	@Schema(description = "是否保存为新版本")
	Boolean newVersion;

	@Schema(description = "模型id")
	String modelId;

	@Schema(description = "模型分类")
	String category;

	@Schema(description = "是否回退")
	Boolean rollback;

	@Schema(description = "外置表单")
	List<FormProperty> exForm;
}
