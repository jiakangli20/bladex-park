
package org.springblade.plugin.workflow.process.vo;

import org.springblade.plugin.workflow.process.entity.WfCopy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程抄送视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfCopyVO对象", description = "流程抄送")
public class WfCopyVO extends WfCopy {
	@Serial
	private static final long serialVersionUID = 1L;

}
