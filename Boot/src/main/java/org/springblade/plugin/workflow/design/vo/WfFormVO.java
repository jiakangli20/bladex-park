package org.springblade.plugin.workflow.design.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.plugin.workflow.design.entity.WfForm;

import java.io.Serial;

/**
 * 流程表单视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfFormVO对象", description = "流程表单")
public class WfFormVO extends WfForm {
	@Serial
	private static final long serialVersionUID = 1L;

	private Boolean newVersion;

	@Schema(description = "是否回退")
	private Boolean rollback;

}
