package org.springblade.plugin.workflow.design.dto;

import org.springblade.plugin.workflow.design.entity.WfFormVariable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程表单 - 历史变量 数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormVariableDTO extends WfFormVariable {
	@Serial
	private static final long serialVersionUID = 1L;

}
