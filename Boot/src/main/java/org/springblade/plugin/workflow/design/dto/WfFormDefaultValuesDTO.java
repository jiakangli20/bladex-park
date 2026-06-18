
package org.springblade.plugin.workflow.design.dto;

import org.springblade.plugin.workflow.design.entity.WfFormDefaultValues;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程表单字段默认值数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfFormDefaultValuesDTO extends WfFormDefaultValues {
	@Serial
	private static final long serialVersionUID = 1L;

}
