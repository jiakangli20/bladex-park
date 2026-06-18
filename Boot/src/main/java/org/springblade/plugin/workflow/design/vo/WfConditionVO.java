
package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfCondition;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程表达式视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfConditionVO对象", description = "流程表达式")
public class WfConditionVO extends WfCondition {
	@Serial
	private static final long serialVersionUID = 1L;

}
