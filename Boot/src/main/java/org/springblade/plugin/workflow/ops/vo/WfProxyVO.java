package org.springblade.plugin.workflow.ops.vo;

import org.springblade.plugin.workflow.ops.entity.WfProxy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;

/**
 * 流程代理视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(name = "WfProxyVO对象", description = "流程代理")
public class WfProxyVO extends WfProxy {
	@Serial
	private static final long serialVersionUID = 1L;

}
