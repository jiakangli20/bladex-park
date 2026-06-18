package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程表单 - 历史变量 视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormVariableVO extends WfFormVariable {
	@Serial
	private static final long serialVersionUID = 1L;

	private String createUsername;
}
