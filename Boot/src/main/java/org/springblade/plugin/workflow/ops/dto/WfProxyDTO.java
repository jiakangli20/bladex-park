package org.springblade.plugin.workflow.ops.dto;

import org.springblade.plugin.workflow.ops.entity.WfProxy;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 流程代理数据传输对象实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfProxyDTO extends WfProxy {
	@Serial
	private static final long serialVersionUID = 1L;

}
