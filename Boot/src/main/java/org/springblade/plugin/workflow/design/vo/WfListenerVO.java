package org.springblade.plugin.workflow.design.vo;

import org.springblade.plugin.workflow.design.entity.WfListenerEntity;
import org.springblade.core.tool.node.INode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 任务/执行监听器 视图实体类
 *
 * @author ssc
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WfListenerVO extends WfListenerEntity {
	@Serial
	private static final long serialVersionUID = 1L;

}
