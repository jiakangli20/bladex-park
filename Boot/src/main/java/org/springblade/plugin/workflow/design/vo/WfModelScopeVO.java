
package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfModelScope;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.util.List;

/**
 * 流程模型权限视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfModelScopeVO对象", description = "流程模型权限")
public class WfModelScopeVO extends WfModelScope {
	@Serial
	private static final long serialVersionUID = 1L;

	List<WfModelScope> scopeList;

}
